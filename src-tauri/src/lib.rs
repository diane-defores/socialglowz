use std::collections::HashMap;
#[cfg(target_os = "android")]
use std::collections::HashSet;

use tauri::{AppHandle, Manager};

mod backup;

/// Desktop User-Agent — standard Chrome UA for the platform.
/// On desktop (webkit2gtk), there is no `wv` token issue, so a Chrome UA works fine.
/// Keep the version reasonably current to avoid fingerprint mismatches.
const CHROME_UA: &str = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";

/// Anti-fingerprint JS injected before every page load.
/// Patches the most common WebView detection vectors used by Akamai, PerimeterX, etc.
#[cfg(not(target_os = "android"))]
const STEALTH_SCRIPT: &str = r#"
(function(){
  // 1. navigator.webdriver — WebView/automation flag
  Object.defineProperty(navigator, 'webdriver', { get: () => false });

  // 2. window.chrome — real Chrome exposes this, WebViews don't
  if (!window.chrome) {
    window.chrome = {
      runtime: {},
      loadTimes: function(){},
      csi: function(){},
      app: { isInstalled: false, InstallState: { DISABLED: 'disabled', INSTALLED: 'installed', NOT_INSTALLED: 'not_installed' }, getDetails: function(){}, getIsInstalled: function(){}, runningState: function(){ return 'cannot_run'; } },
    };
  }

  // 3. navigator.plugins — WebViews report empty, Chrome has defaults
  Object.defineProperty(navigator, 'plugins', {
    get: () => {
      const arr = [
        { name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer', description: 'Portable Document Format' },
        { name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai', description: '' },
        { name: 'Native Client', filename: 'internal-nacl-plugin', description: '' },
      ];
      arr.item = (i) => arr[i] || null;
      arr.namedItem = (n) => arr.find(p => p.name === n) || null;
      arr.refresh = () => {};
      return arr;
    }
  });

  // 4. navigator.languages — must match UA locale
  Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });

  // 5. permissions.query — Notification permission detection
  const origQuery = window.Permissions && Permissions.prototype.query;
  if (origQuery) {
    Permissions.prototype.query = function(params) {
      return params.name === 'notifications'
        ? Promise.resolve({ state: Notification.permission })
        : origQuery.call(this, params);
    };
  }

  // 6. WebGL vendor/renderer — avoid "Google SwiftShader" (headless signal)
  const getParameter = WebGLRenderingContext.prototype.getParameter;
  WebGLRenderingContext.prototype.getParameter = function(param) {
    if (param === 37445) return 'Google Inc. (NVIDIA)';       // UNMASKED_VENDOR_WEBGL
    if (param === 37446) return 'ANGLE (NVIDIA, NVIDIA GeForce GTX 1650, OpenGL 4.5)'; // UNMASKED_RENDERER_WEBGL
    return getParameter.call(this, param);
  };
  if (typeof WebGL2RenderingContext !== 'undefined') {
    const getParameter2 = WebGL2RenderingContext.prototype.getParameter;
    WebGL2RenderingContext.prototype.getParameter = function(param) {
      if (param === 37445) return 'Google Inc. (NVIDIA)';
      if (param === 37446) return 'ANGLE (NVIDIA, NVIDIA GeForce GTX 1650, OpenGL 4.5)';
      return getParameter2.call(this, param);
    };
  }
})();
"#;

// ── Desktop-only imports ─────────────────────────────────────────────────────
#[cfg(not(target_os = "android"))]
use tauri::{
    image::Image,
    menu::{Menu, MenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    Emitter, WebviewBuilder, WebviewUrl,
};

#[cfg(target_os = "android")]
use tauri_plugin_android_webview::AndroidWebviewExt;

#[cfg(target_os = "android")]
const OAUTH_CALLBACK_TTL_MS: i64 = 5 * 60 * 1000;

#[cfg(target_os = "android")]
fn android_allowed_oauth_callback_hosts() -> &'static [&'static str] {
    &["auth-callback", "socialglowz.com", "www.socialglowz.com"]
}

#[cfg(target_os = "android")]
fn host_matches_allowlist(host: &str, allowed_host: &str) -> bool {
    host == allowed_host || host.ends_with(&format!(".{allowed_host}"))
}

#[cfg(target_os = "android")]
fn android_allowed_hosts_for_network(network_id: &str) -> &'static [&'static str] {
    match network_id {
        "twitter" => &["x.com", "twitter.com"],
        "facebook" => &["facebook.com"],
        "instagram" => &["instagram.com"],
        "linkedin" => &["linkedin.com"],
        "tiktok" => &["tiktok.com"],
        "threads" => &["threads.net"],
        "discord" => &["discord.com"],
        "reddit" => &["reddit.com"],
        "snapchat" => &["web.snapchat.com"],
        "cinderreels" => &["cinderreels.com"],
        "quora" => &["quora.com"],
        "pinterest" => &["pinterest.com"],
        "telegram" => &["web.telegram.org", "telegram.org", "t.me"],
        "nextdoor" => &["nextdoor.com"],
        "patreon" => &["patreon.com"],
        "theresanaiforthat" => &["theresanaiforthat.com"],
        "industrysocial" | "industrysocial-waitlist" => &["industrysocial.net"],
        "bluesky" => &["bsky.app"],
        "mastodon" => &["mastodon.social"],
        "substack" => &["substack.com"],
        "ko-fi" => &["ko-fi.com"],
        "buymeacoffee" => &["buymeacoffee.com"],
        "producthunt" => &["producthunt.com"],
        "indiehackers" => &["indiehackers.com"],
        "hackernews" => &["news.ycombinator.com"],
        "folloverse" => &["folloverse.com"],
        "koru" => &["koru.now"],
        "medium" => &["medium.com"],
        _ => &[],
    }
}

#[cfg(target_os = "android")]
fn is_disallowed_host_value(host: &str) -> bool {
    if host.eq_ignore_ascii_case("localhost") || host.eq_ignore_ascii_case("127.0.0.1") {
        return true;
    }

    if let Ok(ip) = host.parse::<std::net::IpAddr>() {
        return match ip {
            std::net::IpAddr::V4(ipv4) => {
                ipv4.is_private()
                    || ipv4.is_loopback()
                    || ipv4.is_link_local()
                    || ipv4.is_documentation()
                    || ipv4.is_unspecified()
            }
            std::net::IpAddr::V6(ipv6) => {
                ipv6.is_loopback()
                    || ipv6.is_unspecified()
                    || ipv6.is_unique_local()
                    || ipv6.is_unicast_link_local()
            }
        };
    }

    false
}

#[cfg(target_os = "android")]
fn validate_android_webview_url(url: &str, network_id: &str) -> Result<url::Url, String> {
    let parsed: url::Url = url
        .parse()
        .map_err(|e: url::ParseError| format!("invalid Android URL: {e}"))?;

    if parsed.scheme() != "https" {
        return Err("Android webview URL rejected: only https scheme is allowed".to_string());
    }

    let host = parsed
        .host_str()
        .ok_or_else(|| "Android webview URL rejected: host is missing".to_string())?
        .to_ascii_lowercase();

    if is_disallowed_host_value(&host) {
        return Err(format!("Android webview URL rejected: host `{host}` is not allowed"));
    }

    let allowed_hosts = android_allowed_hosts_for_network(network_id);
    if allowed_hosts.is_empty() {
        return Err(format!(
            "Android webview URL rejected: network `{network_id}` is not allowlisted"
        ));
    }

    if !allowed_hosts
        .iter()
        .any(|allowed| host_matches_allowlist(&host, allowed))
    {
        return Err(format!(
            "Android webview URL rejected: host `{host}` is not allowed for `{network_id}`"
        ));
    }

    Ok(parsed)
}

#[cfg(target_os = "android")]
fn validate_android_storage_origins(
    storage_origins: Option<Vec<String>>,
    network_id: &str,
) -> Result<Vec<String>, String> {
    let Some(raw_origins) = storage_origins else {
        return Ok(Vec::new());
    };

    let allowed_hosts = android_allowed_hosts_for_network(network_id);
    if allowed_hosts.is_empty() {
        return Err(format!(
            "Android storage origins rejected: network `{network_id}` is not allowlisted"
        ));
    }

    let mut seen = std::collections::HashSet::new();
    let mut normalized_origins = Vec::new();

    for raw in raw_origins {
        let parsed: url::Url = raw
            .parse()
            .map_err(|e: url::ParseError| format!("invalid Android storage origin: {e}"))?;

        if parsed.scheme() != "https" {
            return Err(
                "Android storage origins rejected: only https scheme is allowed".to_string()
            );
        }

        let host = parsed
            .host_str()
            .ok_or_else(|| "Android storage origins rejected: host is missing".to_string())?
            .to_ascii_lowercase();

        if is_disallowed_host_value(&host) {
            return Err(format!(
                "Android storage origins rejected: host `{host}` is not allowed"
            ));
        }

        if !allowed_hosts
            .iter()
            .any(|allowed| host_matches_allowlist(&host, allowed))
        {
            return Err(format!(
                "Android storage origins rejected: host `{host}` is not allowed for `{network_id}`"
            ));
        }

        let normalized = match parsed.port() {
            Some(443) | None => format!("https://{host}"),
            Some(port) => format!("https://{host}:{port}"),
        };

        if seen.insert(normalized.clone()) {
            normalized_origins.push(normalized);
        }
    }

    Ok(normalized_origins)
}

#[cfg(target_os = "android")]
fn validate_android_storage_origins_by_network(
    storage_origins_by_network: Option<HashMap<String, Vec<String>>>,
    network_ids: &[String],
) -> Result<HashMap<String, Vec<String>>, String> {
    let Some(raw_origins_by_network) = storage_origins_by_network else {
        return Ok(HashMap::new());
    };

    let visible_network_ids = network_ids.iter().map(String::as_str).collect::<HashSet<_>>();
    let mut validated = HashMap::new();

    for (network_id, origins) in raw_origins_by_network {
        if !visible_network_ids.contains(network_id.as_str()) {
            return Err(format!(
                "Android storage origins rejected: network `{network_id}` is not visible"
            ));
        }

        let normalized = validate_android_storage_origins(Some(origins), &network_id)?;
        if !normalized.is_empty() {
            validated.insert(network_id, normalized);
        }
    }

    Ok(validated)
}

#[derive(Default)]
struct AndroidOAuthReplayState(std::sync::Mutex<std::collections::HashMap<String, i64>>);

#[cfg(target_os = "android")]
fn now_millis() -> i64 {
    chrono::Utc::now().timestamp_millis()
}

#[cfg(target_os = "android")]
fn prune_consumed_oauth_states(consumed_states: &mut std::collections::HashMap<String, i64>, now: i64) {
    consumed_states.retain(|_, timestamp| now - *timestamp <= OAUTH_CALLBACK_TTL_MS);
}

#[cfg(target_os = "android")]
fn validate_android_oauth_callback_url(callback_url: &str) -> Result<url::Url, String> {
    let parsed: url::Url = callback_url
        .parse()
        .map_err(|e: url::ParseError| format!("invalid Android OAuth callback URL: {e}"))?;

    let scheme = parsed.scheme();
    if scheme != "https" && scheme != "socialglowz" {
        return Err("Android OAuth callback rejected: scheme is not allowlisted".to_string());
    }

    let host = parsed
        .host_str()
        .ok_or_else(|| "Android OAuth callback rejected: host is missing".to_string())?
        .to_ascii_lowercase();

    if is_disallowed_host_value(&host) {
        return Err(format!("Android OAuth callback rejected: host `{host}` is not allowed"));
    }

    if !android_allowed_oauth_callback_hosts()
        .iter()
        .any(|allowed| host_matches_allowlist(&host, allowed))
    {
        return Err(format!(
            "Android OAuth callback rejected: host `{host}` is not in callback allowlist"
        ));
    }

    Ok(parsed)
}

// ─── Helpers ────────────────────────────────────────────────────────────────

/// Unique label per (profile, network) pair — ensures isolated webviews.
#[cfg(not(target_os = "android"))]
fn webview_label(profile_id: &str, network_id: &str) -> String {
    format!("social-{}-{}", profile_id, network_id)
}

// ─── Tray setup (desktop only) ───────────────────────────────────────────────

#[cfg(not(target_os = "android"))]
fn build_tray(app: &AppHandle) -> tauri::Result<()> {
    let show = MenuItem::with_id(app, "show", "Show SocialGlowz", true, None::<&str>)?;
    let separator = MenuItem::with_id(app, "sep", "──────────────", false, None::<&str>)?;
    let twitter = MenuItem::with_id(app, "tray:twitter", "Twitter / X", true, None::<&str>)?;
    let instagram = MenuItem::with_id(app, "tray:instagram", "Instagram", true, None::<&str>)?;
    let linkedin = MenuItem::with_id(app, "tray:linkedin", "LinkedIn", true, None::<&str>)?;
    let facebook = MenuItem::with_id(app, "tray:facebook", "Facebook", true, None::<&str>)?;
    let tiktok = MenuItem::with_id(app, "tray:tiktok", "TikTok", true, None::<&str>)?;
    let threads = MenuItem::with_id(app, "tray:threads", "Threads", true, None::<&str>)?;
    let discord = MenuItem::with_id(app, "tray:discord", "Discord", true, None::<&str>)?;
    let reddit = MenuItem::with_id(app, "tray:reddit", "Reddit", true, None::<&str>)?;
    let snapchat = MenuItem::with_id(app, "tray:snapchat", "Snapchat", true, None::<&str>)?;
    let quora = MenuItem::with_id(app, "tray:quora", "Quora", true, None::<&str>)?;
    let pinterest = MenuItem::with_id(app, "tray:pinterest", "Pinterest", true, None::<&str>)?;
    let whatsapp = MenuItem::with_id(app, "tray:whatsapp", "WhatsApp", true, None::<&str>)?;
    let telegram = MenuItem::with_id(app, "tray:telegram", "Telegram", true, None::<&str>)?;
    let nextdoor = MenuItem::with_id(app, "tray:nextdoor", "Nextdoor", true, None::<&str>)?;
    let patreon = MenuItem::with_id(app, "tray:patreon", "Patreon", true, None::<&str>)?;
    let theresanaiforthat = MenuItem::with_id(
        app,
        "tray:theresanaiforthat",
        "There's An AI For That",
        true,
        None::<&str>,
    )?;
    let industrysocial = MenuItem::with_id(
        app,
        "tray:industrysocial",
        "Industry Social",
        true,
        None::<&str>,
    )?;
    let bluesky = MenuItem::with_id(app, "tray:bluesky", "Bluesky", true, None::<&str>)?;
    let mastodon = MenuItem::with_id(app, "tray:mastodon", "Mastodon", true, None::<&str>)?;
    let substack = MenuItem::with_id(app, "tray:substack", "Substack", true, None::<&str>)?;
    let ko_fi = MenuItem::with_id(app, "tray:ko-fi", "Ko-fi", true, None::<&str>)?;
    let buymeacoffee =
        MenuItem::with_id(app, "tray:buymeacoffee", "Buy Me a Coffee", true, None::<&str>)?;
    let producthunt =
        MenuItem::with_id(app, "tray:producthunt", "Product Hunt", true, None::<&str>)?;
    let indiehackers =
        MenuItem::with_id(app, "tray:indiehackers", "Indie Hackers", true, None::<&str>)?;
    let hackernews = MenuItem::with_id(
        app,
        "tray:hackernews",
        "Hacker News / Show HN",
        true,
        None::<&str>,
    )?;
    let folloverse = MenuItem::with_id(app, "tray:folloverse", "Folloverse", true, None::<&str>)?;
    let industrysocial_waitlist = MenuItem::with_id(
        app,
        "tray:industrysocial-waitlist",
        "Industry Social Waitlist",
        true,
        None::<&str>,
    )?;
    let koru = MenuItem::with_id(app, "tray:koru", "Koru", true, None::<&str>)?;
    let medium = MenuItem::with_id(app, "tray:medium", "Medium", true, None::<&str>)?;
    let sep2 = MenuItem::with_id(app, "sep2", "──────────────", false, None::<&str>)?;
    let quit = MenuItem::with_id(app, "quit", "Quit", true, None::<&str>)?;

    let menu = Menu::with_items(
        app,
        &[
            &show,
            &separator,
            &twitter,
            &instagram,
            &linkedin,
            &facebook,
            &tiktok,
            &threads,
            &discord,
            &reddit,
            &snapchat,
            &quora,
            &pinterest,
            &whatsapp,
            &telegram,
            &nextdoor,
            &patreon,
            &theresanaiforthat,
            &industrysocial,
            &bluesky,
            &mastodon,
            &substack,
            &ko_fi,
            &buymeacoffee,
            &producthunt,
            &indiehackers,
            &hackernews,
            &folloverse,
            &industrysocial_waitlist,
            &koru,
            &medium,
            &sep2,
            &quit,
        ],
    )?;

    let icon = Image::from_bytes(include_bytes!("../icons/32x32.png")).ok();

    let mut tray_builder = TrayIconBuilder::new();
    if let Some(icon) = icon {
        tray_builder = tray_builder.icon(icon);
    }

    tray_builder
        .tooltip("SocialGlowz")
        .menu(&menu)
        .on_menu_event(move |app, event| match event.id().as_ref() {
            "show" => show_window(app),
            "quit" => app.exit(0),
            id if id.starts_with("tray:") => {
                let network = id.trim_start_matches("tray:");
                show_window(app);
                let _ = app.emit("tray:open-network", network.to_string());
            }
            _ => {}
        })
        .on_tray_icon_event(|tray, event| {
            if let TrayIconEvent::Click {
                button: MouseButton::Left,
                button_state: MouseButtonState::Up,
                ..
            } = event
            {
                let app = tray.app_handle();
                toggle_window(app);
            }
        })
        .build(app)?;

    Ok(())
}

#[cfg(not(target_os = "android"))]
fn show_window(app: &AppHandle) {
    if let Some(window) = app.get_webview_window("main") {
        let _ = window.show();
        let _ = window.set_focus();
        let _ = window.unminimize();
    }
}

#[cfg(not(target_os = "android"))]
fn toggle_window(app: &AppHandle) {
    if let Some(window) = app.get_webview_window("main") {
        if window.is_visible().unwrap_or(false) {
            let _ = window.hide();
        } else {
            let _ = window.show();
            let _ = window.set_focus();
        }
    }
}

// ─── IPC commands ────────────────────────────────────────────────────────────

// ── Desktop: native child webviews via add_child ─────────────────────────────

#[tauri::command]
#[cfg(not(target_os = "android"))]
fn open_webview(
    app: AppHandle,
    url: String,
    profile_id: String,
    network_id: String,
    _storage_origins: Option<Vec<String>>,
    x: f64,
    y: f64,
    width: f64,
    height: f64,
) -> Result<(), String> {
    let label = webview_label(&profile_id, &network_id);
    let parsed: url::Url = url.parse().map_err(|e: url::ParseError| e.to_string())?;

    if let Some(wv) = app.get_webview(&label) {
        wv.navigate(parsed).map_err(|e| e.to_string())?;
        wv.set_bounds(tauri::Rect {
            position: tauri::Position::Logical(tauri::LogicalPosition::new(x, y)),
            size: tauri::Size::Logical(tauri::LogicalSize::new(width, height)),
        })
        .map_err(|e| e.to_string())?;
        return Ok(());
    }

    // Session data isolated per (profile, network) — cookies/localStorage/IndexedDB
    let data_dir = app
        .path()
        .app_data_dir()
        .map_err(|e| e.to_string())?
        .join("sessions")
        .join(&profile_id)
        .join(&network_id);

    let window = app
        .get_window("main")
        .ok_or_else(|| "main window not found".to_string())?;

    window
        .add_child(
            WebviewBuilder::new(&label, WebviewUrl::External(parsed))
                .user_agent(CHROME_UA)
                .initialization_script(STEALTH_SCRIPT)
                .data_directory(data_dir),
            tauri::LogicalPosition::new(x, y),
            tauri::LogicalSize::new(width, height),
        )
        .map_err(|e| e.to_string())?;

    Ok(())
}

#[tauri::command]
#[cfg(not(target_os = "android"))]
fn resize_webview(
    app: AppHandle,
    profile_id: String,
    network_id: String,
    x: f64,
    y: f64,
    width: f64,
    height: f64,
) -> Result<(), String> {
    let label = webview_label(&profile_id, &network_id);
    if let Some(wv) = app.get_webview(&label) {
        wv.set_bounds(tauri::Rect {
            position: tauri::Position::Logical(tauri::LogicalPosition::new(x, y)),
            size: tauri::Size::Logical(tauri::LogicalSize::new(width, height)),
        })
        .map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
#[cfg(not(target_os = "android"))]
fn close_webview(app: AppHandle, profile_id: String, network_id: String) -> Result<(), String> {
    let label = webview_label(&profile_id, &network_id);
    if let Some(wv) = app.get_webview(&label) {
        wv.close().map_err(|e| e.to_string())?;
    }
    Ok(())
}

/// Hide a webview without destroying it (webview pooling).
/// Moves it off-screen so it stays alive with full page state.
#[tauri::command]
#[cfg(not(target_os = "android"))]
fn hide_webview(app: AppHandle, profile_id: String, network_id: String) -> Result<(), String> {
    let label = webview_label(&profile_id, &network_id);
    if let Some(wv) = app.get_webview(&label) {
        wv.set_bounds(tauri::Rect {
            position: tauri::Position::Logical(tauri::LogicalPosition::new(-10000.0, -10000.0)),
            size: tauri::Size::Logical(tauri::LogicalSize::new(0.0, 0.0)),
        })
        .map_err(|e| e.to_string())?;
    }
    Ok(())
}

/// Show a previously hidden pooled webview. Returns true if the webview
/// existed (and was repositioned), false if it needs to be created.
#[tauri::command]
#[cfg(not(target_os = "android"))]
fn show_webview(
    app: AppHandle,
    profile_id: String,
    network_id: String,
    x: f64,
    y: f64,
    width: f64,
    height: f64,
) -> Result<bool, String> {
    let label = webview_label(&profile_id, &network_id);
    if let Some(wv) = app.get_webview(&label) {
        wv.set_bounds(tauri::Rect {
            position: tauri::Position::Logical(tauri::LogicalPosition::new(x, y)),
            size: tauri::Size::Logical(tauri::LogicalSize::new(width, height)),
        })
        .map_err(|e| e.to_string())?;
        Ok(true)
    } else {
        Ok(false)
    }
}

// ── Android: delegate to Kotlin plugin ───────────────────────────────────────

#[tauri::command]
#[cfg(target_os = "android")]
fn open_webview(
    app: AppHandle,
    url: String,
    profile_id: String,
    network_id: String,
    storage_origins: Option<Vec<String>>,
    _x: f64,
    _y: f64,
    _width: f64,
    _height: f64,
) -> Result<(), String> {
    let validated_url = validate_android_webview_url(&url, &network_id)?;
    let validated_storage_origins =
        validate_android_storage_origins(storage_origins, &network_id)?;

    // Use "profileId-networkId" as the session key for Android
    let session_key = format!("{}-{}", profile_id, network_id);
    app.android_webview()
        .open(
            validated_url.as_str(),
            &session_key,
            &network_id,
            validated_storage_origins,
        )
        .map_err(|e| e.to_string())
}

#[tauri::command]
#[cfg(target_os = "android")]
fn validate_android_oauth_callback(
    app: AppHandle,
    callback_url: String,
    expected_state: String,
    expected_nonce: Option<String>,
    started_at_ms: i64,
) -> Result<(), String> {
    let parsed = validate_android_oauth_callback_url(&callback_url)?;
    let now = now_millis();

    if started_at_ms <= 0 || now - started_at_ms > OAUTH_CALLBACK_TTL_MS {
        return Err("Android OAuth callback rejected: request expired".to_string());
    }

    let callback_state = parsed
        .query_pairs()
        .find_map(|(key, value)| (key == "state").then(|| value.into_owned()))
        .ok_or_else(|| "Android OAuth callback rejected: state is missing".to_string())?;

    if callback_state != expected_state {
        return Err("Android OAuth callback rejected: state mismatch".to_string());
    }

    if let Some(expected_nonce) = expected_nonce {
        let callback_nonce = parsed
            .query_pairs()
            .find_map(|(key, value)| (key == "nonce").then(|| value.into_owned()))
            .ok_or_else(|| "Android OAuth callback rejected: nonce is missing".to_string())?;
        if callback_nonce != expected_nonce {
            return Err("Android OAuth callback rejected: nonce mismatch".to_string());
        }
    }

    let replay_state = app.state::<AndroidOAuthReplayState>();
    let mut consumed_states = replay_state
        .0
        .lock()
        .map_err(|_| "Android OAuth callback rejected: replay state unavailable".to_string())?;
    prune_consumed_oauth_states(&mut consumed_states, now);
    if consumed_states.contains_key(&callback_state) {
        return Err("Android OAuth callback rejected: state replay detected".to_string());
    }
    consumed_states.insert(callback_state, now);

    Ok(())
}

#[tauri::command]
#[cfg(not(target_os = "android"))]
fn validate_android_oauth_callback(
    _app: AppHandle,
    _callback_url: String,
    _expected_state: String,
    _expected_nonce: Option<String>,
    _started_at_ms: i64,
) -> Result<(), String> {
    Err("Android OAuth callback validation is only available on Android".to_string())
}

#[tauri::command]
#[cfg(target_os = "android")]
fn resize_webview(
    _app: AppHandle,
    _profile_id: String,
    _network_id: String,
    _x: f64,
    _y: f64,
    _width: f64,
    _height: f64,
) -> Result<(), String> {
    // On Android the social webview is always full-screen; resize is a no-op
    Ok(())
}

#[tauri::command]
#[cfg(target_os = "android")]
fn close_webview(app: AppHandle, profile_id: String, network_id: String) -> Result<(), String> {
    let session_key = format!("{}-{}", profile_id, network_id);
    app.android_webview()
        .close(&session_key)
        .map_err(|e| e.to_string())
}

// Android: pooling not yet implemented — hide/show fall back to close/open behavior
#[tauri::command]
#[cfg(target_os = "android")]
fn hide_webview(app: AppHandle, profile_id: String, network_id: String) -> Result<(), String> {
    close_webview(app, profile_id, network_id)
}

#[tauri::command]
#[cfg(target_os = "android")]
fn show_webview(
    _app: AppHandle,
    _profile_id: String,
    _network_id: String,
    _x: f64,
    _y: f64,
    _width: f64,
    _height: f64,
) -> Result<bool, String> {
    // Always return false on Android — caller will fall through to open_webview
    Ok(false)
}

#[tauri::command]
#[cfg(target_os = "android")]
fn set_grayscale(app: AppHandle, enabled: bool) -> Result<(), String> {
    app.android_webview()
        .set_grayscale(enabled)
        .map_err(|e| e.to_string())
}

#[tauri::command]
#[cfg(not(target_os = "android"))]
fn set_grayscale(_app: AppHandle, _enabled: bool) -> Result<(), String> {
    Ok(()) // no-op on desktop — Vue applies the CSS filter directly
}

#[tauri::command]
#[cfg(target_os = "android")]
fn set_dark_mode(app: AppHandle, enabled: bool) -> Result<(), String> {
    app.android_webview()
        .set_dark_mode(enabled)
        .map_err(|e| e.to_string())
}

#[tauri::command]
#[cfg(not(target_os = "android"))]
fn set_dark_mode(_app: AppHandle, _enabled: bool) -> Result<(), String> {
    Ok(()) // no-op on desktop — Vue applies the CSS class directly
}

#[tauri::command]
#[cfg(target_os = "android")]
fn set_text_zoom(app: AppHandle, level: i32) -> Result<(), String> {
    app.android_webview()
        .set_text_zoom(level)
        .map_err(|e| e.to_string())
}

#[tauri::command]
#[cfg(not(target_os = "android"))]
fn set_text_zoom(_app: AppHandle, _level: i32) -> Result<(), String> {
    Ok(()) // no-op on desktop
}

/// Sync the bottom bar network icons with the profile's visible networks.
#[tauri::command]
#[cfg(target_os = "android")]
fn set_bar_networks(
    app: AppHandle,
    network_ids: Vec<String>,
    storage_origins_by_network: Option<HashMap<String, Vec<String>>>,
) -> Result<(), String> {
    let validated_storage_origins_by_network =
        validate_android_storage_origins_by_network(storage_origins_by_network, &network_ids)?;

    app.android_webview()
        .set_bar_networks(network_ids, validated_storage_origins_by_network)
        .map_err(|e| e.to_string())
}

#[tauri::command]
#[cfg(not(target_os = "android"))]
fn set_bar_networks(
    _app: AppHandle,
    _network_ids: Vec<String>,
    _storage_origins_by_network: Option<HashMap<String, Vec<String>>>,
) -> Result<(), String> {
    Ok(()) // no-op on desktop — no overlay bar
}

/// Send profile list to the Android popup menu for inline profile switching.
#[tauri::command]
#[cfg(target_os = "android")]
fn set_profiles(app: AppHandle, profiles_json: String, active_profile_id: String) -> Result<(), String> {
    app.android_webview()
        .set_profiles(profiles_json, active_profile_id)
        .map_err(|e| e.to_string())
}

#[tauri::command]
#[cfg(not(target_os = "android"))]
fn set_profiles(_app: AppHandle, _profiles_json: String, _active_profile_id: String) -> Result<(), String> {
    Ok(())
}

/// Sync the UI locale to the Android plugin for native string translations.
#[tauri::command]
#[cfg(target_os = "android")]
fn set_locale(app: AppHandle, locale: String) -> Result<(), String> {
    app.android_webview()
        .set_locale(locale)
        .map_err(|e| e.to_string())
}

#[tauri::command]
#[cfg(not(target_os = "android"))]
fn set_locale(_app: AppHandle, _locale: String) -> Result<(), String> {
    Ok(())
}

/// Injects a JavaScript string into a running social webview.
/// Used by the friends filter to hide posts from non-friends.
#[tauri::command]
#[cfg(not(target_os = "android"))]
fn inject_script(
    app: AppHandle,
    profile_id: String,
    network_id: String,
    script: String,
) -> Result<(), String> {
    let label = webview_label(&profile_id, &network_id);
    if let Some(wv) = app.get_webview(&label) {
        wv.eval(&script).map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
#[cfg(target_os = "android")]
fn inject_script(
    _app: AppHandle,
    _profile_id: String,
    _network_id: String,
    _script: String,
) -> Result<(), String> {
    // Android: script injection via Kotlin plugin not yet implemented
    Ok(())
}

// ── Session deletion ─────────────────────────────────────────────────────────

/// Wipe all session data for a profile (all networks).
/// Desktop: delete the filesystem data directory.
#[tauri::command]
#[cfg(not(target_os = "android"))]
fn delete_profile_session(app: AppHandle, profile_id: String) -> Result<(), String> {
    let data_dir = app
        .path()
        .app_data_dir()
        .map_err(|e| e.to_string())?
        .join("sessions")
        .join(&profile_id);

    if data_dir.exists() {
        std::fs::remove_dir_all(&data_dir).map_err(|e| e.to_string())?;
    }
    Ok(())
}

/// Android: delegate to Kotlin plugin to clear SharedPreferences cookies.
#[tauri::command]
#[cfg(target_os = "android")]
fn delete_profile_session(app: AppHandle, profile_id: String) -> Result<(), String> {
    app.android_webview()
        .delete_profile_session(&profile_id)
        .map_err(|e| e.to_string())
}

/// Wipe session data for a single (profile, network) pair.
/// Desktop: delete the filesystem data directory.
#[tauri::command]
#[cfg(not(target_os = "android"))]
fn delete_network_session(
    app: AppHandle,
    profile_id: String,
    network_id: String,
) -> Result<(), String> {
    let data_dir = app
        .path()
        .app_data_dir()
        .map_err(|e| e.to_string())?
        .join("sessions")
        .join(&profile_id)
        .join(&network_id);

    if data_dir.exists() {
        std::fs::remove_dir_all(&data_dir).map_err(|e| e.to_string())?;
    }
    Ok(())
}

/// Android: delegate to Kotlin plugin to clear SharedPreferences cookies.
#[tauri::command]
#[cfg(target_os = "android")]
fn delete_network_session(
    app: AppHandle,
    profile_id: String,
    network_id: String,
) -> Result<(), String> {
    app.android_webview()
        .delete_network_session(&profile_id, &network_id)
        .map_err(|e| e.to_string())
}

// ─── Backup / Restore ────────────────────────────────────────────────────────

/// Create an encrypted backup, save to disk, return the file path.
/// On Android the Tauri FS plugin is unreliable, so Rust handles all I/O directly.
#[tauri::command]
fn create_backup(
    app: AppHandle,
    store_data: String,
    password: String,
) -> Result<String, String> {
    use base64::{engine::general_purpose::STANDARD, Engine as _};

    let app_data = app
        .path()
        .app_data_dir()
        .map_err(|e| e.to_string())?;
    let sessions_dir = app_data.join("sessions");

    let zip_bytes = backup::create_backup_archive(&sessions_dir, &store_data)?;
    let blob = backup::encrypt_archive(&zip_bytes, &password)?;

    // Save encrypted blob to disk (backups/ dir inside app data)
    let backups_dir = app_data.join("backups");
    std::fs::create_dir_all(&backups_dir)
        .map_err(|e| format!("Failed to create backups dir: {e}"))?;

    let filename = format!("socialglowz-backup-{}.sfbak", chrono::Utc::now().timestamp_millis());
    let file_path = backups_dir.join(&filename);
    std::fs::write(&file_path, &blob)
        .map_err(|e| format!("Failed to write backup file: {e}"))?;

    // Also return base64 for desktop (file dialog flow)
    Ok(STANDARD.encode(&blob))
}

/// Restore from the most recent backup on disk, or from provided base64 data.
/// If encrypted_b64 is empty, auto-finds the latest .sfbak in the backups dir.
#[tauri::command]
fn restore_backup(
    app: AppHandle,
    encrypted_b64: String,
    password: String,
) -> Result<String, String> {
    use base64::{engine::general_purpose::STANDARD, Engine as _};

    let app_data = app
        .path()
        .app_data_dir()
        .map_err(|e| e.to_string())?;
    let sessions_dir = app_data.join("sessions");

    let blob = if encrypted_b64.is_empty() {
        // Auto-find latest backup on disk
        let backups_dir = app_data.join("backups");
        let mut backups: Vec<_> = std::fs::read_dir(&backups_dir)
            .map_err(|_| "Aucune sauvegarde trouvée. Exportez d'abord vos données.".to_string())?
            .filter_map(|e| e.ok())
            .filter(|e| e.path().extension().is_some_and(|ext| ext == "sfbak"))
            .collect();
        backups.sort_by(|a, b| b.file_name().cmp(&a.file_name()));
        let latest = backups.first()
            .ok_or_else(|| "Aucune sauvegarde trouvée. Exportez d'abord vos données.".to_string())?;
        std::fs::read(latest.path())
            .map_err(|e| format!("Failed to read backup file: {e}"))?
    } else {
        STANDARD
            .decode(&encrypted_b64)
            .map_err(|e| format!("Invalid backup data: {e}"))?
    };

    let zip_bytes = backup::decrypt_archive(&blob, &password)?;
    let store_data = backup::extract_backup_archive(&zip_bytes, &sessions_dir)?;

    Ok(store_data)
}

// ─── Entry point ─────────────────────────────────────────────────────────────

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .manage(AndroidOAuthReplayState::default())
        .plugin(tauri_plugin_android_webview::init())
        .plugin(tauri_plugin_deep_link::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .setup(|app| {
            if cfg!(debug_assertions) {
                app.handle().plugin(
                    tauri_plugin_log::Builder::default()
                        .level(log::LevelFilter::Info)
                        .build(),
                )?;
            }
            #[cfg(not(target_os = "android"))]
            if let Err(err) = build_tray(app.handle()) {
                eprintln!("tray initialization failed: {err}");
            }
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            open_webview,
            resize_webview,
            close_webview,
            validate_android_oauth_callback,
            hide_webview,
            show_webview,
            set_grayscale,
            set_dark_mode,
            set_text_zoom,
            set_bar_networks,
            set_profiles,
            set_locale,
            inject_script,
            delete_profile_session,
            delete_network_session,
            create_backup,
            restore_backup,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
