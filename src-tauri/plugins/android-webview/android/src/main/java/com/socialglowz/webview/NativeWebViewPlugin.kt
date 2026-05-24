package com.socialglowz.webview

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.view.MotionEvent
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.PathParser
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.ScriptHandler
import androidx.webkit.ProfileStore
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import java.net.URL
import java.security.MessageDigest
import org.json.JSONObject

private const val TAG = "SFZ"
private const val TEXT_ZOOM_MIN = 50
private const val TEXT_ZOOM_MAX = 200
private const val TEXT_ZOOM_STEP = 5
private const val TEXT_ZOOM_DEFAULT = 100
private const val TEXT_ZOOM_RANGE_STEPS = (TEXT_ZOOM_MAX - TEXT_ZOOM_MIN) / TEXT_ZOOM_STEP
private const val DEFAULT_TAP_SOUND_VARIANT = "classic"
private const val STORAGE_BRIDGE_OBJECT = "sfzStorageBridge"
private const val LOCAL_STORAGE_PREFS_NAME = "sfz_local_storage"
private const val LOCAL_STORAGE_RESTORE_PENDING_PREFS_NAME = "sfz_local_storage_restore_pending"
private const val LOCAL_STORAGE_CAPTURE_MAX_BYTES = 262_144
private const val WEBKIT_PROFILE_PREFIX = "sgzp_"
private const val MAX_WARM_HOSTS = 3
private val TAP_SOUND_ASSETS = mapOf(
    "classic" to "sounds/click.wav",
    "soft" to "sounds/soft.wav",
    "pop" to "sounds/pop.wav",
)

@InvokeArg
class OpenWebViewArgs {
    var url: String = ""
    var accountId: String = ""
    var networkId: String = ""
    var storageOrigins: ArrayList<String> = arrayListOf()
}

@InvokeArg
class AccountArgs {
    var accountId: String = ""
}

@InvokeArg
class GrayscaleArgs {
    var enabled: Boolean = false
}

@InvokeArg
class DarkModeArgs {
    var enabled: Boolean = false
}

@InvokeArg
class TextZoomArgs {
    var level: Int = 100
}

@InvokeArg
class TapSoundVariantArgs {
    var variant: String = DEFAULT_TAP_SOUND_VARIANT
}

@InvokeArg
class NavigateArgs {
    var url: String = ""
    var networkId: String = ""
}

@InvokeArg
class BarNetworksArgs {
    var networkIds: ArrayList<String> = arrayListOf()
    var storageOriginsByNetworkJson: String = "{}"
}

@InvokeArg
class SetProfilesArgs {
    var profilesJson: String = "[]"
    var activeProfileId: String = ""
}

@InvokeArg
class SetLocaleArgs {
    var locale: String = "fr"
}

@InvokeArg
class DeleteSessionArgs {
    var profileId: String = ""
    var networkId: String = ""
}

@InvokeArg
class SaveBackupArgs {
    var base64Data: String = ""
    var fileName: String = ""
}

@InvokeArg
class ImportCookiesBackupArgs {
    var cookiesJson: String = ""
    var localStorageJson: String = ""
}

// Lightweight profile data for the popup menu
private data class ProfileMenuItem(
    val id: String,
    val name: String,
    val emoji: String,
    val avatar: String? = null
)

private data class SessionIdentity(
    val profileId: String,
    val networkId: String,
    val sessionKey: String,
)

private data class SessionWebViewHost(
    val session: SessionIdentity,
    val profileName: String,
    val root: FrameLayout,
    val webView: WebView,
    var bottomBar: LinearLayout,
    var localStorageScriptHandler: ScriptHandler? = null,
    var localStorageRestoreActive: Boolean = false,
    var localStorageCaptureActive: Boolean = false,
    var initialBackIndex: Int = -1,
    var isLoggedIn: Boolean = false,
    var pagesSinceOpen: Int = 0,
    var facebookDesktopOverride: Boolean = false,
    var facebookStoryNoticeShown: Boolean = false,
    var lastUsedAt: Long = 0L,
    var isVisible: Boolean = false,
)

// ── i18n ──────────────────────────────────────────────────────────────────────
private object Strings {
    private val translations = mapOf(
        // Popup menu
        "profiles" to mapOf("fr" to "Profils", "en" to "Profiles"),
        "mute_on" to mapOf("fr" to "Activer le son", "en" to "Sound on"),
        "mute_off" to mapOf("fr" to "Couper le son", "en" to "Mute"),
        "grayscale_on" to mapOf("fr" to "Désactiver niveaux de gris", "en" to "Disable grayscale"),
        "grayscale_off" to mapOf("fr" to "Niveaux de gris", "en" to "Grayscale"),
        "dark_mode_on" to mapOf("fr" to "Mode clair", "en" to "Light mode"),
        "dark_mode_off" to mapOf("fr" to "Mode sombre", "en" to "Dark mode"),
        "text_zoom" to mapOf("fr" to "Taille du texte des réseaux", "en" to "Network text size"),
        // Blocked page
        "blocked_title" to mapOf("fr" to "Accès bloqué par", "en" to "Access blocked by"),
        "blocked_message" to mapOf(
            "fr" to "Ce site bloque les navigateurs intégrés. Vous pouvez effacer les cookies et réessayer, ou ouvrir le site dans votre navigateur.",
            "en" to "This site blocks embedded browsers. You can clear cookies and retry, or open the site in your browser."
        ),
        "blocked_clear_retry" to mapOf("fr" to "Effacer les cookies et réessayer", "en" to "Clear cookies and retry"),
        "blocked_back" to mapOf("fr" to "← Retour", "en" to "← Back"),
        "blocked_open_browser" to mapOf("fr" to "Ouvrir dans le navigateur", "en" to "Open in browser"),
    )

    var locale: String = "fr"

    fun t(key: String): String {
        return translations[key]?.get(locale) ?: translations[key]?.get("fr") ?: key
    }
}

// Network metadata for the bottom bar switcher
// iconChar: PrimeIcons codepoint (same font as the Vue app, loaded from assets/primeicons.ttf)
// color:    brand color shown as button background when active
private data class NetworkInfo(val id: String, val iconChar: String, val color: Int, val url: String)

private val NETWORKS = listOf(
    NetworkInfo("twitter",   "\ue9b6", Color.parseColor("#000000"), "https://x.com"),
    NetworkInfo("facebook",  "\ue9b4", Color.parseColor("#1877F2"), "https://facebook.com"),
    NetworkInfo("instagram", "\ue9cc", Color.parseColor("#E4405F"), "https://instagram.com"),
    NetworkInfo("linkedin",  "\ue9cb", Color.parseColor("#0A66C2"), "https://linkedin.com"),
    NetworkInfo("tiktok",    "\uea21", Color.parseColor("#010101"), "https://tiktok.com"),
    NetworkInfo("threads",   "\ue9d8", Color.parseColor("#000000"), "https://threads.net"),
    NetworkInfo("discord",   "\ue9c0", Color.parseColor("#5865F2"), "https://discord.com/app"),
    NetworkInfo("reddit",    "\ue9e8", Color.parseColor("#FF4500"), "https://reddit.com"),
    NetworkInfo("snapchat",  "\ue96c", Color.parseColor("#FFFC00"), "https://www.snapchat.com/web/"),
    NetworkInfo("cinderreels", "\ue96c", Color.parseColor("#E11D48"), "https://cinderreels.com/"),
    NetworkInfo("quora",     "\ue959", Color.parseColor("#A82400"), "https://www.quora.com"),
    NetworkInfo("pinterest", "\uea09", Color.parseColor("#E60023"), "https://www.pinterest.com"),
    // NetworkInfo("whatsapp",  "\ue9d0", Color.parseColor("#25D366"), "https://web.whatsapp.com"), // disabled 2026-04-12 — see docs/whatsapp-web-integration.md
    NetworkInfo("telegram",  "\ue9d3", Color.parseColor("#0088CC"), "https://web.telegram.org"),
    NetworkInfo("nextdoor",  "\ue968", Color.parseColor("#8ED500"), "https://nextdoor.com"),
    NetworkInfo("patreon",   "\ue9da", Color.parseColor("#FF424D"), "https://www.patreon.com"),
    NetworkInfo("theresanaiforthat", "\ue9e7", Color.parseColor("#111827"), "https://theresanaiforthat.com"),
    NetworkInfo("industrysocial", "\ue9bb", Color.parseColor("#2563EB"), "https://industrysocial.net"),
    NetworkInfo("bluesky",   "\ue9b1", Color.parseColor("#1185FE"), "https://bsky.app"),
    NetworkInfo("mastodon",  "\ue9ad", Color.parseColor("#6364FF"), "https://mastodon.social"),
    NetworkInfo("substack",  "\ue977", Color.parseColor("#FF6719"), "https://substack.com"),
    NetworkInfo("ko-fi",     "\ue9da", Color.parseColor("#29ABE0"), "https://ko-fi.com"),
    NetworkInfo("buymeacoffee", "\ue9da", Color.parseColor("#FFDD00"), "https://www.buymeacoffee.com"),
    NetworkInfo("producthunt", "\ue99d", Color.parseColor("#DA552F"), "https://www.producthunt.com"),
    NetworkInfo("indiehackers", "\ue9ab", Color.parseColor("#0E2439"), "https://www.indiehackers.com"),
    NetworkInfo("hackernews", "\ue95e", Color.parseColor("#FF6600"), "https://news.ycombinator.com/show"),
    NetworkInfo("folloverse", "\ue9ab", Color.parseColor("#7C3AED"), "https://folloverse.com/?ref=betalist"),
    NetworkInfo("industrysocial-waitlist", "\uea18", Color.parseColor("#1D4ED8"), "https://industrysocial.net/waitlist"),
    NetworkInfo("koru", "\ue9cb", Color.parseColor("#16A34A"), "https://koru.now"),
    NetworkInfo("medium", "\uea18", Color.parseColor("#000000"), "https://medium.com"),
)

// Official SVG path data from Simple Icons (24x24 viewBox) for networks without PrimeIcons glyphs
private val SVG_ICONS = mapOf(
    "threads" to "M12.186 24h-.007c-3.581-.024-6.334-1.205-8.184-3.509C2.35 18.44 1.5 15.586 1.472 12.01v-.017c.03-3.579.879-6.43 2.525-8.482C5.845 1.205 8.6.024 12.18 0h.014c2.746.02 5.043.725 6.826 2.098 1.677 1.29 2.858 3.13 3.509 5.467l-2.04.569c-1.104-3.96-3.898-5.984-8.304-6.015-2.91.022-5.11.936-6.54 2.717C4.307 6.504 3.616 8.914 3.589 12c.027 3.086.718 5.496 2.057 7.164 1.43 1.783 3.631 2.698 6.54 2.717 2.623-.02 4.358-.631 5.8-2.045 1.647-1.613 1.618-3.593 1.09-4.798-.31-.71-.873-1.3-1.634-1.75-.192 1.352-.622 2.446-1.284 3.272-.886 1.102-2.14 1.704-3.73 1.79-1.202.065-2.361-.218-3.259-.801-1.063-.689-1.685-1.74-1.752-2.964-.065-1.19.408-2.285 1.33-3.082.88-.76 2.119-1.207 3.583-1.291a13.853 13.853 0 0 1 3.02.142c-.126-.742-.375-1.332-.75-1.757-.513-.586-1.308-.883-2.359-.89h-.029c-.844 0-1.992.232-2.721 1.32L7.734 7.847c.98-1.454 2.568-2.256 4.478-2.256h.044c3.194.02 5.097 1.975 5.287 5.388.108.046.216.094.321.142 1.49.7 2.58 1.761 3.154 3.07.797 1.82.871 4.79-1.548 7.158-1.85 1.81-4.094 2.628-7.277 2.65Zm1.003-11.69c-.242 0-.487.007-.739.021-1.836.103-2.98.946-2.916 2.143.067 1.256 1.452 1.839 2.784 1.767 1.224-.065 2.818-.543 3.086-3.71a10.5 10.5 0 0 0-2.215-.221z",
    "snapchat" to "M12.206.793c.99 0 4.347.276 5.93 3.821.529 1.193.403 3.219.299 4.847l-.003.06c-.012.18-.022.345-.03.51.075.045.203.09.401.09.3-.016.659-.12 1.033-.301.165-.088.344-.104.464-.104.182 0 .359.029.509.09.45.149.734.479.734.838.015.449-.39.839-1.213 1.168-.089.029-.209.075-.344.119-.45.135-1.139.36-1.333.81-.09.224-.061.524.12.868l.015.015c.06.136 1.526 3.475 4.791 4.014.255.044.435.27.42.509 0 .075-.015.149-.045.225-.24.569-1.273.988-3.146 1.271-.059.091-.12.375-.164.57-.029.179-.074.36-.134.553-.076.271-.27.405-.555.405h-.03c-.135 0-.313-.031-.538-.074-.36-.075-.765-.135-1.273-.135-.3 0-.599.015-.913.074-.6.104-1.123.464-1.723.884-.853.599-1.826 1.288-3.294 1.288-.06 0-.119-.015-.18-.015h-.149c-1.468 0-2.427-.675-3.279-1.288-.599-.42-1.107-.779-1.707-.884-.314-.045-.629-.074-.928-.074-.54 0-.958.089-1.272.149-.211.043-.391.074-.54.074-.374 0-.523-.224-.583-.42-.061-.192-.09-.389-.135-.567-.046-.181-.105-.494-.166-.57-1.918-.222-2.95-.642-3.189-1.226-.031-.063-.052-.15-.055-.225-.015-.243.165-.465.42-.509 3.264-.54 4.73-3.879 4.791-4.02l.016-.029c.18-.345.224-.645.119-.869-.195-.434-.884-.658-1.332-.809-.121-.029-.24-.074-.346-.119-1.107-.435-1.257-.93-1.197-1.273.09-.479.674-.793 1.168-.793.146 0 .27.029.383.074.42.194.789.3 1.104.3.234 0 .384-.06.465-.105l-.046-.569c-.098-1.626-.225-3.651.307-4.837C7.392 1.077 10.739.807 11.727.807l.419-.015h.06z",
    "quora" to "M7.3799.9483A11.9628 11.9628 0 0 1 21.248 19.5397l2.4096 2.4225c.7322.7362.21 1.9905-.8272 1.9905l-10.7105.01a12.52 12.52 0 0 1-.304 0h-.02A11.9628 11.9628 0 0 1 7.3818.9503Zm7.3217 4.428a7.1717 7.1717 0 1 0-5.4873 13.2512 7.1717 7.1717 0 0 0 5.4883-13.2511Z",
)

// Anti-fingerprint JS — patches WebView detection vectors used by Akamai, PerimeterX, etc.
private val STEALTH_SCRIPT = """
(function(){
  if (window.__sfzStealth) return;
  window.__sfzStealth = true;
  // navigator.webdriver — automation/WebView flag
  Object.defineProperty(navigator, 'webdriver', { get: () => false });
  // window.chrome — real Chrome exposes this, WebViews don't
  if (!window.chrome) {
    window.chrome = { runtime: {}, loadTimes: function(){}, csi: function(){}, app: { isInstalled: false } };
  }
  // navigator.plugins — WebViews report empty
  Object.defineProperty(navigator, 'plugins', {
    get: () => {
      var arr = [
        { name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer', description: 'Portable Document Format' },
        { name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai', description: '' },
        { name: 'Native Client', filename: 'internal-nacl-plugin', description: '' }
      ];
      arr.item = function(i) { return arr[i] || null; };
      arr.namedItem = function(n) { return arr.find(function(p) { return p.name === n; }) || null; };
      arr.refresh = function() {};
      return arr;
    }
  });
  // navigator.languages
  Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });
  // permissions.query — Notification permission detection
  var origQuery = window.Permissions && Permissions.prototype.query;
  if (origQuery) {
    Permissions.prototype.query = function(params) {
      return params.name === 'notifications'
        ? Promise.resolve({ state: Notification.permission })
        : origQuery.call(this, params);
    };
  }

  // ── Desktop device spoofing (Snapchat, etc.) ──────────────────────────────
  // Sites like Snapchat Web check multiple JS signals beyond UA to detect mobile.
  // Only spoof when we're already sending a desktop UA (DESKTOP_UA_NETWORKS).
  if (/Windows NT 10\.0.*Chrome\/136/.test(navigator.userAgent)) {
    // Touch — desktop has no touchscreen
    Object.defineProperty(navigator, 'maxTouchPoints', { get: () => 0 });
    delete window.ontouchstart;
    // Platform
    Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });
    // Screen dimensions — report standard desktop
    Object.defineProperty(window.screen, 'width', { get: () => 1920 });
    Object.defineProperty(window.screen, 'height', { get: () => 1080 });
    Object.defineProperty(window.screen, 'availWidth', { get: () => 1920 });
    Object.defineProperty(window.screen, 'availHeight', { get: () => 1040 });
    // User-Agent Client Hints JS API
    if (navigator.userAgentData) {
      Object.defineProperty(navigator, 'userAgentData', {
        get: () => ({
          brands: [
            { brand: 'Chromium', version: '136' },
            { brand: 'Google Chrome', version: '136' },
            { brand: 'Not-A.Brand', version: '99' }
          ],
          mobile: false,
          platform: 'Windows',
          getHighEntropyValues: function() {
            return Promise.resolve({
              architecture: 'x86', bitness: '64', mobile: false,
              model: '', platform: 'Windows', platformVersion: '15.0.0',
              uaFullVersion: '136.0.0.0',
              brands: [{ brand: 'Chromium', version: '136.0.0.0' }, { brand: 'Google Chrome', version: '136.0.0.0' }],
              fullVersionList: [{ brand: 'Chromium', version: '136.0.0.0' }, { brand: 'Google Chrome', version: '136.0.0.0' }]
            });
          },
          toJSON: function() {
            return { brands: this.brands, mobile: false, platform: 'Windows' };
          }
        })
      });
    }
    // Media queries — pointer: fine (mouse), hover: hover
    var origMatchMedia = window.matchMedia;
    window.matchMedia = function(q) {
      if (q === '(pointer: coarse)') return Object.assign(origMatchMedia.call(this, q), { matches: false });
      if (q === '(pointer: fine)') return Object.assign(origMatchMedia.call(this, q), { matches: true });
      if (q === '(hover: hover)') return Object.assign(origMatchMedia.call(this, q), { matches: true });
      if (q === '(hover: none)') return Object.assign(origMatchMedia.call(this, q), { matches: false });
      return origMatchMedia.call(this, q);
    };
  }
})();
""".trimIndent()

// For desktop-UA networks: force a wide viewport so the desktop layout fits on a mobile screen.
// Without this, sites with <meta viewport width=device-width> render desktop CSS at phone width
// (~360px), making everything appear 3-4x zoomed in. Setting width=980 lets loadWithOverviewMode
// zoom out the page to fit.
private val DESKTOP_VIEWPORT_SCRIPT = """
(function(){
  if (!/Windows NT 10\.0.*Chrome\/136/.test(navigator.userAgent)) return;
  var meta = document.querySelector('meta[name="viewport"]');
  if (!meta) {
    meta = document.createElement('meta');
    meta.name = 'viewport';
    (document.head || document.documentElement).appendChild(meta);
  }
  meta.setAttribute('content', 'width=980, shrink-to-fit=yes');
})();
""".trimIndent()

// Shared helper for LinkedIn's native dark-mode preference bridge.
// LinkedIn's public help says the preference is saved locally in the browser/device.
// We therefore patch both storage and cookie access so the site sees a consistent
// light/dark preference before its own JS finishes booting.
private val LINKEDIN_THEME_BRIDGE_HELPERS = """
function linkedInThemeStorageKeys() {
  return ['mobileWebTheme', 'theme', 'themeMode', 'displayTheme', 'display_mode', 'appearance', 'colorScheme', 'color_scheme', 'darkMode', 'dark_mode', 'isDarkMode'];
}
function linkedInThemeCookieKeys() {
  return ['li_theme', 'mobileWebTheme', 'theme', 'themeMode', 'displayTheme', 'display_mode', 'appearance', 'colorScheme', 'color_scheme', 'isDarkMode'];
}
function linkedInThemeValueForKey(key, enabled) {
  var normalized = String(key || '').toLowerCase();
  if (normalized === 'isdarkmode' || normalized === 'darkmode' || normalized === 'dark_mode') {
    return enabled ? 'true' : 'false';
  }
  return enabled ? 'dark' : 'light';
}
function isLinkedInThemeStorageKey(key) {
  var normalized = String(key || '').toLowerCase();
  return /^(mobilewebtheme|theme|thememode|displaytheme|display_mode|appearance|colorscheme|color_scheme|darkmode|dark_mode|isdarkmode)$/.test(normalized);
}
function isLinkedInThemeCookieKey(key) {
  var normalized = String(key || '').toLowerCase();
  return /^(li_theme|mobilewebtheme|theme|thememode|displaytheme|display_mode|appearance|colorscheme|color_scheme|isdarkmode)$/.test(normalized);
}
function patchLinkedInCookies(enabled) {
  if (!isLinkedIn()) return;
  try {
    window.__sfzLinkedInThemeEnabled = !!enabled;
  } catch (e) {}
  try {
    var proto = (window.Document && Document.prototype) ? Document.prototype : null;
    var descriptor = proto ? Object.getOwnPropertyDescriptor(proto, 'cookie') : null;
    if (descriptor && !window.__sfzLinkedInCookiePatched) {
      var originalGetter = descriptor.get;
      var originalSetter = descriptor.set;
      Object.defineProperty(proto, 'cookie', {
        configurable: true,
        enumerable: descriptor.enumerable,
        get: function() {
          var base = '';
          try { base = String(originalGetter.call(this) || ''); } catch (e) {}
          var extras = [];
          var keys = linkedInThemeCookieKeys();
          for (var i = 0; i < keys.length; i++) {
            var name = keys[i];
            var escaped = name.replace(/[.*+?^$()|[\]\\]/g, '\\$&');
            if (!(new RegExp('(?:^|;\\s*)' + escaped + '=')).test(base)) {
              extras.push(name + '=' + linkedInThemeValueForKey(name, window.__sfzLinkedInThemeEnabled));
            }
          }
          if (!extras.length) return base;
          return base ? base + '; ' + extras.join('; ') : extras.join('; ');
        },
        set: function(rawValue) {
          var nextValue = String(rawValue || '');
          try {
            var separatorIndex = nextValue.indexOf(';');
            var firstPart = separatorIndex === -1 ? nextValue : nextValue.slice(0, separatorIndex);
            var eqIndex = firstPart.indexOf('=');
            if (eqIndex !== -1) {
              var rawName = firstPart.slice(0, eqIndex).trim();
              var name = decodeURIComponent(rawName);
              if (isLinkedInThemeCookieKey(name)) {
                var normalized = encodeURIComponent(name) + '=' + encodeURIComponent(linkedInThemeValueForKey(name, window.__sfzLinkedInThemeEnabled));
                nextValue = normalized + (separatorIndex === -1 ? '' : nextValue.slice(separatorIndex));
              }
            }
          } catch (e) {}
          return originalSetter.call(this, nextValue);
        }
      });
      window.__sfzLinkedInCookiePatched = true;
    }
  } catch (e) {}
  try {
    var cookieKeys = linkedInThemeCookieKeys();
    for (var j = 0; j < cookieKeys.length; j++) {
      var cookieName = cookieKeys[j];
      var cookieValue = linkedInThemeValueForKey(cookieName, enabled);
      try { document.cookie = encodeURIComponent(cookieName) + '=' + encodeURIComponent(cookieValue) + '; path=/; domain=.linkedin.com; SameSite=Lax'; } catch (e) {}
      try { document.cookie = encodeURIComponent(cookieName) + '=' + encodeURIComponent(cookieValue) + '; path=/; SameSite=Lax'; } catch (e) {}
    }
  } catch (e) {}
}
function forceLinkedInTheme(enabled) {
  if (!isLinkedIn()) return;
  try {
    window.__sfzLinkedInThemeEnabled = !!enabled;
    window.__sfzLinkedInThemeValue = enabled ? 'dark' : 'light';
    function patchStorage(storage) {
      if (!storage) return;
      try {
        var proto = Object.getPrototypeOf(storage);
        if (proto && !proto.__sfzLinkedInThemePatched) {
          var originalGetItem = proto.getItem;
          var originalSetItem = proto.setItem;
          var originalRemoveItem = proto.removeItem;
          proto.getItem = function(key) {
            if (isLinkedInThemeStorageKey(key)) {
              return linkedInThemeValueForKey(key, window.__sfzLinkedInThemeEnabled);
            }
            return originalGetItem.apply(this, arguments);
          };
          proto.setItem = function(key, value) {
            if (isLinkedInThemeStorageKey(key)) {
              return originalSetItem.call(this, key, linkedInThemeValueForKey(key, window.__sfzLinkedInThemeEnabled));
            }
            return originalSetItem.apply(this, arguments);
          };
          proto.removeItem = function(key) {
            if (isLinkedInThemeStorageKey(key)) {
              return originalSetItem.call(this, key, linkedInThemeValueForKey(key, window.__sfzLinkedInThemeEnabled));
            }
            return originalRemoveItem.apply(this, arguments);
          };
          proto.__sfzLinkedInThemePatched = true;
        }
      } catch (e) {}
      try {
        var storageKeys = linkedInThemeStorageKeys();
        for (var i = 0; i < storageKeys.length; i++) {
          (function(propKey) {
            try {
              Object.defineProperty(storage, propKey, {
                configurable: true,
                get: function() {
                  return linkedInThemeValueForKey(propKey, window.__sfzLinkedInThemeEnabled);
                },
                set: function(_) {
                  try {
                    storage.setItem(propKey, linkedInThemeValueForKey(propKey, window.__sfzLinkedInThemeEnabled));
                  } catch (e) {}
                }
              });
            } catch (e) {}
            try {
              storage.setItem(propKey, linkedInThemeValueForKey(propKey, enabled));
            } catch (e) {}
          })(storageKeys[i]);
        }
      } catch (e) {}
    }
    patchStorage(window.localStorage);
    patchStorage(window.sessionStorage);
    patchLinkedInCookies(enabled);
  } catch (e) {}
}
""".trimIndent()

// Document-start dark-mode bridge. Reads the last preference we stored in page-local
// storage so new navigations pick the right theme before site JS finishes booting.
private val DARK_MODE_DOC_START_SCRIPT = """
(function() {
  try {
    function isFacebook() {
      return /(^|\.)facebook\.com$/i.test(location.hostname) && !/\/messages|\/reels?(\/|$)/.test(location.pathname);
    }
    function isLinkedIn() {
      return /(^|\.)linkedin\.com$/i.test(location.hostname);
    }
    function readDark() {
      try {
        var stored = localStorage.getItem('__sfzPreferredDark');
        if (stored === '1') return true;
        if (stored === '0') return false;
      } catch (e) {}
      return false;
    }
${LINKEDIN_THEME_BRIDGE_HELPERS}

    function install(dark) {
      try {
        window.__sfzPreferredDark = dark;
        if (isLinkedIn()) {
          forceLinkedInTheme(dark);
        }
        var originalMatchMedia = window.__sfzOriginalMatchMedia || window.matchMedia;
        if (!window.__sfzOriginalMatchMedia && originalMatchMedia) {
          window.__sfzOriginalMatchMedia = originalMatchMedia;
        }
        if (originalMatchMedia) {
          window.matchMedia = function(query) {
            if (query && query.indexOf('prefers-color-scheme') !== -1) {
              var base = originalMatchMedia.call(window, query);
              var isDarkQuery = query.indexOf('dark') !== -1;
              var isLightQuery = query.indexOf('light') !== -1;
              return {
                matches: isDarkQuery ? dark : (isLightQuery ? !dark : base.matches),
                media: query,
                onchange: null,
                addListener: function() {},
                removeListener: function() {},
                addEventListener: function() {},
                removeEventListener: function() {},
                dispatchEvent: function() { return false; }
              };
            }
            return originalMatchMedia.call(window, query);
          };
        }

        document.documentElement.style.colorScheme = dark ? 'dark' : 'light';

        var style = document.getElementById('__sfz-dark-mode-hint');
        if (!style) {
          style = document.createElement('style');
          style.id = '__sfz-dark-mode-hint';
          (document.head || document.documentElement).appendChild(style);
        }
        var fb = isFacebook();
        var linkedIn = isLinkedIn();
        style.textContent = dark
          ? (linkedIn
              ? ':root{color-scheme:dark !important;}'
              : ':root{color-scheme:dark !important;} html,body{background:#0f172a !important;}') +
            (fb
              ? 'html.__sfz-facebook-dark-fallback{background:#0f172a !important;filter:invert(1) hue-rotate(180deg) !important;}' +
                'html.__sfz-facebook-dark-fallback img,' +
                'html.__sfz-facebook-dark-fallback video,' +
                'html.__sfz-facebook-dark-fallback canvas,' +
                'html.__sfz-facebook-dark-fallback [role=\"img\"],' +
                'html.__sfz-facebook-dark-fallback image{filter:invert(1) hue-rotate(180deg) !important;}'
              : '')
          : ':root{color-scheme:light !important;}';

        document.documentElement.classList.toggle('__sfz-facebook-dark-fallback', !!(dark && fb));
      } catch (e) {}
    }

    install(readDark());
    var attempts = 0;
    var timer = setInterval(function() {
      install(readDark());
      attempts += 1;
      if (attempts >= 12) clearInterval(timer);
    }, 250);
  } catch (e) {}
})();
""".trimIndent()

// JavaScript injected after every page load to dismiss "open in app" / "get the app" prompts.
// Strategy: inject persistent CSS to hide known banner elements, then click "Not now" buttons.
private val DISMISS_APP_BANNERS_SCRIPT = """
(function() {
  'use strict';
  if (window.__sfzAppBannerWatcher) return;
  window.__sfzAppBannerWatcher = true;
  window.__sfzAppBannerLog = window.__sfzAppBannerLog || [];

  function L(msg) {
    try {
      window.__sfzAppBannerLog.push(msg);
      if (window.__sfzAppBannerLog.length > 80) window.__sfzAppBannerLog.shift();
    } catch (e) {}
  }

  // Persistent CSS — hides known app-banner elements even if re-inserted into the DOM
  var style = document.createElement('style');
  style.textContent =
    '#smart-banner, .smartbanner, .smart-banner, #smartbanner, .smartbanner-container,' +
    '[data-testid="BottomBar"],' +
    '#xpromo-banner, .xpromo, [data-testid="xpromo-interstitial"], #AppPromo, .AppPromo,' +
    '.IgCMI,' +
    '#app-download-guide,' +
    '[id*="app-banner" i], [id*="app-download" i], [id*="install-banner" i],' +
    '[class*="AppBanner"], [class*="app-install-prompt"],' +
    // Facebook / Instagram / Threads (Meta) — "Download" & "Open in app" banners
    '[data-sigil="mbasic_inline_feed_promo"], [data-sigil="app_banner"],' +
    '[id*="download-app" i], [class*="download-app" i],' +
    '[class*="MobileAppPromoBanner"], [class*="appBanner"],' +
    '#mobile-install-banner, [data-testid="mobile_app_banner"],' +
    '[class*="open-in-app" i], [class*="openInApp" i],' +
    '[data-testid*="open-in-app" i], [data-testid*="app_upsell" i]' +
    '{ display: none !important; }';
  (document.head || document.documentElement).appendChild(style);

  // Remove HTML smart-app-banner meta tags (prevents browser-native banners)
  function removeSmartBannerMeta() {
    document.querySelectorAll('meta[name="apple-itunes-app"], meta[name="google-play-app"]')
      .forEach(function(el) { el.parentNode && el.parentNode.removeChild(el); });
  }

  // Text patterns for "stay in browser / not now" dismiss buttons
  var DISMISS_RE = /^(not now|pas maintenant|no thanks|non merci|continue in browser|continuer dans le navigateur|stay in browser|rester sur le site|use web|utiliser le web|continue to site|maybe later|peut-être plus tard|dismiss|ignorer|skip|passer|×|✕|close|fermer|log in|se connecter)$/i;

  // Hide banners by text content (e.g. "Download Facebook for Android")
  var DOWNLOAD_RE = /t(é|e)l(é|e)charger.*(facebook|instagram|threads|android)|download.*(facebook|instagram|threads|android)|naviguer plus vite|browse faster|open in app|ouvrir.*(l.application|l.app)|get the app|installer l.app/i;
  function hideDownloadBanners() {
    var els = document.querySelectorAll('div, section, aside, header, [role="banner"]');
    for (var i = 0; i < els.length; i++) {
      var el = els[i];
      if (el.children.length > 10 || el.offsetHeight > 120) continue;
      var txt = (el.textContent || '').trim();
      if (txt.length < 200 && DOWNLOAD_RE.test(txt)) {
        L('HIDE banner: ' + txt.substring(0, 120));
        el.style.display = 'none';
      }
    }
  }

  function dismissAppPrompts() {
    removeSmartBannerMeta();
    hideDownloadBanners();

    var btns = document.querySelectorAll('button, a[role="button"], [role="button"], a');
    for (var i = 0; i < btns.length; i++) {
      var el = btns[i];
      if (!el.offsetParent) continue;
      var label = (el.textContent || el.getAttribute('aria-label') || el.getAttribute('title') || '').trim();
      if (!DISMISS_RE.test(label)) continue;
      // Only click if the button lives inside an app-promotion container
      var parent = el.closest(
        '[id*="app" i], [class*="app" i], [id*="banner" i], [class*="banner" i],' +
        '[id*="install" i], [class*="install" i], [id*="promo" i], [class*="promo" i]'
      );
      if (parent) { L('DISMISS CTA: ' + label); el.click(); return; }
    }
  }

  // Trace likely content-creation clicks so Kotlin logs can show what happened
  // right before an app-promo banner appears or the flow stalls.
  var STORY_RE = /(story|stories|create story|créer une story|ajouter à la story|your story|reel|camera|photo|create post|composer)/i;
  document.addEventListener('click', function(ev) {
    var el = ev.target && ev.target.closest ? ev.target.closest('button, a, [role="button"], div, span') : null;
    if (!el) return;
    var label = (el.textContent || el.getAttribute('aria-label') || el.getAttribute('title') || '').trim();
    if (!label || !STORY_RE.test(label)) return;
    L('CLICK candidate: ' + label.substring(0, 140));
  }, true);

  dismissAppPrompts();
  setTimeout(dismissAppPrompts, 800);
  setTimeout(dismissAppPrompts, 2500);

  var observer = new MutationObserver(function(mutations) {
    for (var i = 0; i < mutations.length; i++) {
      if (mutations[i].addedNodes.length) { setTimeout(dismissAppPrompts, 300); break; }
    }
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
})();
""".trimIndent()

// Lightweight cookie-accept script injected via addDocumentStartJavaScript into ALL frames.
// Only runs inside iframes (skips main frame) — handles cross-origin CMP dialogs
// like Google Funding Choices (Quora) that render consent UI in an iframe.
private val COOKIE_IFRAME_SCRIPT = """
(function() {
  'use strict';
  if (window === window.top) return;
  if (window.__sfzCookieIframe) return;
  window.__sfzCookieIframe = true;

  var RE = /^(accept( all( cookies?)?)?|i accept|allow( all( cookies?)?)?|i agree|agree|tout accepter|accepter( tout(es)?)?|autoriser( tous?( les cookies?)?)?|j'accepte|confirm all)$/i;

  function clickEl(el) {
    var r = el.getBoundingClientRect();
    if (r.width === 0 && r.height === 0 && !el.offsetParent) return false;
    var x = r.left + r.width / 2, y = r.top + r.height / 2;
    try {
      var opts = {bubbles:true, cancelable:true, clientX:x, clientY:y, pointerId:1, pointerType:'touch'};
      el.dispatchEvent(new PointerEvent('pointerdown', opts));
      el.dispatchEvent(new PointerEvent('pointerup', opts));
    } catch(e) {}
    el.click();
    return true;
  }
  function tryClick() {
    // Buttons first, then divs/spans as fallback
    var selectors = ['button, a, [role="button"]', 'div, span, p'];
    for (var s = 0; s < selectors.length; s++) {
      var els = document.querySelectorAll(selectors[s]);
      for (var i = 0; i < els.length; i++) {
        var label = (els[i].textContent || els[i].getAttribute('aria-label') || '').trim();
        if (RE.test(label) && clickEl(els[i])) return;
      }
    }
  }

  var attempts = 0;
  var interval = setInterval(function() {
    tryClick();
    if (++attempts >= 100) clearInterval(interval);
  }, 50);
  tryClick();
})();
""".trimIndent()

// JavaScript injected after every page load to auto-accept cookie consent dialogs.
// Uses MutationObserver so it also catches dialogs that appear after initial load.
private val COOKIE_ACCEPT_SCRIPT = """
(function() {
  'use strict';
  if (window.__sfzCookieWatcher) return; // already installed
  window.__sfzCookieWatcher = true;

  // Specific CMP selectors (OneTrust, CookieBot, Didomi, Axeptio, Quantcast, Meta/Instagram…)
  var SELECTORS = [
    '#onetrust-accept-btn-handler',
    '#accept-recommended-btn-handler',
    '.onetrust-accept-btn-handler',
    '#CybotCookiebotDialogBodyButtonAccept',
    '#CybotCookiebotDialogBodyLevelButtonAccept',
    '#didomi-notice-agree-button',
    '#didomi-notice-learn-more-button ~ button',
    '[data-consent="accept"]',
    '[id="axeptio_btn_acceptAll"]',
    '.qc-cmp2-summary-buttons button[mode="primary"]',
    '.qc-cmp2-summary-buttons button:first-child',
    '.sp_choice_type_11',
    '[data-testid="GDPR-accept"]',
    '[data-testid="cookie-policy-manage-dialog-accept-button"]',
    '[data-cookiebanner="accept_button"]',
    '#L2AGLb',
    '.tOjcNe',
    '[aria-label="Accept all"]',
    '[aria-label="Tout accepter"]',
    '[aria-label="Allow all cookies"]',
    '[aria-label="Autoriser tous les cookies"]',
    // TikTok
    '[data-e2e="cookie-banner-accept"]',
    '.tiktok-cookie-banner button:last-child',
    '[class*="CookieBanner"] button:last-child',
    '[class*="cookie-banner"] button:last-child'
  ];

  // Text patterns matched case-insensitively against button innerText / aria-label
  var ACCEPT_RE = /^(accept( all( cookies?)?)?|accept cookies on this browser|accepter( tout(es)?( les cookies?)?)?|tout accepter|tout autoriser|autoriser( tous?( les cookies?)?)?|autoriser les cookies.*|allow( all( cookies?)?)?|allow.*cookies|i agree|j'accepte|ok|got it|i accept|confirm all|agree)$/i;

  // Robust click: dispatch pointer events (for React/Vue onPointerDown handlers)
  // then native click. Covers all frameworks.
  function robustClick(el) {
    if (!el) return false;
    var r = el.getBoundingClientRect();
    if (r.width === 0 && r.height === 0 && !el.offsetParent) return false;
    var x = r.left + r.width / 2, y = r.top + r.height / 2;
    try {
      var opts = {bubbles:true, cancelable:true, clientX:x, clientY:y, pointerId:1, pointerType:'touch'};
      el.dispatchEvent(new PointerEvent('pointerdown', opts));
      el.dispatchEvent(new PointerEvent('pointerup', opts));
    } catch(e) {}
    el.click();
    return true;
  }

  // TikTok uses <tiktok-cookie-banner> custom element with Shadow DOM.
  // Normal selectors can't reach inside — we must access shadowRoot directly.
  function tryTikTokShadowBanner() {
    try {
      var banner = document.querySelector('tiktok-cookie-banner');
      if (!banner || !banner.shadowRoot) return false;
      var btns = banner.shadowRoot.querySelectorAll('button');
      for (var i = 0; i < btns.length; i++) {
        var label = (btns[i].textContent || '').trim();
        if (ACCEPT_RE.test(label)) { robustClick(btns[i]); return true; }
      }
      // Fallback: click the last button (typically "Allow all" / "Accept")
      if (btns.length > 0) { robustClick(btns[btns.length - 1]); return true; }
    } catch(e) {}
    return false;
  }

  // Diagnostic log — collected and sent to Kotlin debug logs after 6s
  var log = [];
  function L(msg) { log.push(msg); }

  function tryAccept() {
    // 0. TikTok shadow DOM banner (must be checked before normal selectors)
    if (tryTikTokShadowBanner()) { L('CLICKED via TikTok shadow DOM'); return true; }

    // 1. Try known CMP selectors — only click interactive elements
    //    (aria-label selectors can match container divs, not buttons)
    for (var i = 0; i < SELECTORS.length; i++) {
      try {
        var el = document.querySelector(SELECTORS[i]);
        if (!el) continue;
        var tag = el.tagName;
        if (tag !== 'BUTTON' && tag !== 'A' && !el.getAttribute('role')) {
          L('CMP skip non-interactive: ' + SELECTORS[i] + ' → <' + tag + '> "' + (el.textContent||'').trim().substring(0,40) + '"');
          continue;
        }
        if (robustClick(el)) { L('CLICKED via CMP selector: ' + SELECTORS[i]); return true; }
      } catch(e) {}
    }

    // 2. Scan elements in two passes: interactive first (button/a), then any element.
    //    This prevents clicking a parent <div> when the real <button> is inside it.
    function scanDoc(doc) {
      // Pass 1: buttons and links only (the actual clickable elements)
      var btns = doc.querySelectorAll('button, a, [role="button"]');
      for (var b = 0; b < btns.length; b++) {
        var label = (btns[b].textContent || btns[b].getAttribute('aria-label') || '').trim();
        if (ACCEPT_RE.test(label) && robustClick(btns[b])) {
          L('CLICKED via btn scan: <' + btns[b].tagName + '> "' + label + '"');
          return true;
        }
      }
      // Pass 2: any element (div, span, p) — fallback for non-standard CMPs
      var els = doc.querySelectorAll('div, span, p');
      for (var b = 0; b < els.length; b++) {
        var label = (els[b].textContent || els[b].getAttribute('aria-label') || '').trim();
        if (ACCEPT_RE.test(label) && robustClick(els[b])) {
          L('CLICKED via div scan: <' + els[b].tagName + '> "' + label + '"');
          return true;
        }
      }
      return false;
    }
    if (scanDoc(document)) return true;

    // 3. Scan same-origin iframes (some CMPs like Quantcast render in an iframe)
    var iframes = document.querySelectorAll('iframe');
    for (var f = 0; f < iframes.length; f++) {
      try {
        var doc = iframes[f].contentDocument;
        if (doc && scanDoc(doc)) { L('CLICKED via iframe #' + f); return true; }
      } catch(e) { L('iframe #' + f + ' cross-origin (skipped)'); }
    }
    return false;
  }

  // Retry every 50ms for 5 seconds, then stop.
  var clicked = false;
  var attempts = 0;
  var interval = setInterval(function() {
    if (!clicked) clicked = tryAccept();
    if (clicked || ++attempts >= 100) clearInterval(interval);
  }, 50);
  clicked = tryAccept();

  // After 6s, expose diagnostic log for Kotlin to retrieve
  setTimeout(function() {
    window.__sfzCookieLog = (clicked ? 'OK' : 'FAIL') + ' (' + attempts + ' attempts)\\n' + log.join('\\n');
  }, 6000);
})();
""".trimIndent()

@TauriPlugin
class NativeWebViewPlugin(private val activity: Activity) : Plugin(activity) {

    // Main Tauri WebView (the one running Vue) — used to dispatch CustomEvents to Vue
    // via evaluateJavascript(). This is the reliable Kotlin→Vue communication channel.
    // (Plugin trigger() + addPluginListener was unreliable in testing.)
    private var mainWebView: WebView? = null

    private var socialRoot: FrameLayout? = null
    private var socialWebView: WebView? = null
    private var currentAccountId: String? = null
    private var currentNetworkId: String? = null
    private var activeSessionIdentity: SessionIdentity? = null
    private var localStorageScriptHandler: ScriptHandler? = null
    private var localStorageRestoreActive = false
    private var localStorageCaptureActive = false
    private val sessionHosts = linkedMapOf<String, SessionWebViewHost>()
    private var activeHostSessionKey: String? = null
    private var multiProfileModeEnabled = false
    private var multiProfileInitTried = false
    private var disableMultiProfilePooling = false
    private val declaredStorageOriginsByNetwork = mutableMapOf<String, Set<String>>()
    private val declaredStorageOriginsBySession = mutableMapOf<String, Set<String>>()

    // Back-stack baseline — set after the initial page+redirects settle.
    // canGoBack() returns true for redirect-created entries too, so we only treat
    // it as real navigable history if currentIndex > initialBackIndex.
    private var initialBackIndex = -1

    // Mute state — survives network switches within a session
    private var isMuted = false

    // Grayscale state — survives network switches within a session
    private var isGrayscale = false

    // Haptic feedback — controlled from Vue settings, defaults to on
    private var hapticEnabled = true

    // Tap sound — controlled from Vue settings, defaults to off.
    // We use SoundPool with bundled assets rather than view.playSoundEffect()
    // because the latter respects the system "Touch sounds" setting, which is
    // off by default on most Android devices.
    private var tapSoundEnabled = false
    private var tapSoundVariant = DEFAULT_TAP_SOUND_VARIANT
    private var soundPool: SoundPool? = null
    private val tapSoundIds = mutableMapOf<String, Int>()
    private val loadedTapSoundIds = mutableSetOf<Int>()

    // Text zoom level — percentage, 100 = default
    private var textZoomLevel: Int = TEXT_ZOOM_DEFAULT

    // Incremented on navigation so delayed dark-mode reapplications from older pages
    // don't keep fighting the current Facebook page and cause flashes.
    private var darkModeReapplyGeneration = 0

    // Facebook stays mobile by default; switch to desktop only for story-specific flows.
    private var facebookDesktopOverride = false
    private var facebookStoryNoticeShown = false

    // SAF file picker — pending invoke for backup restore
    private var pendingBackupInvoke: Invoke? = null
    private var pickBackupLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>? = null
    private var pendingFilePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>? = null
    private var pickFileLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>? = null

    private fun haptic(view: View, type: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
        if (hapticEnabled) view.performHapticFeedback(type)
        playTapSound()
    }

    private fun playTapSound(ignoreEnabled: Boolean = false) {
        if (!ignoreEnabled && !tapSoundEnabled) return
        val pool = soundPool ?: return
        val normalizedVariant = normalizeTapSoundVariant(tapSoundVariant)
        val soundId = tapSoundIds[normalizedVariant] ?: tapSoundIds[DEFAULT_TAP_SOUND_VARIANT] ?: return
        if (!loadedTapSoundIds.contains(soundId)) return
        pool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    private fun initSoundPool() {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedTapSoundIds.add(sampleId)
            } else {
                Log.w(TAG, "tap sound failed to load (sampleId=$sampleId status=$status)")
            }
        }
        for ((variant, assetPath) in TAP_SOUND_ASSETS) {
            try {
                val afd = activity.assets.openFd(assetPath)
                tapSoundIds[variant] = pool.load(afd, 1)
                afd.close()
            } catch (e: Exception) {
                Log.w(TAG, "Could not load tap sound asset: $assetPath", e)
            }
        }
        soundPool = pool
    }
    private var bottomBarView: LinearLayout? = null

    // Dark mode state — synced from Vue settings toggle
    private var isDarkMode = false

    // Cookie consent: only inject when not logged in.
    // Detected by checking for auth cookies specific to each network.
    // pagesSinceOpen ensures the first 3 pages always get the script
    // (consent can appear after redirects, not just on page 1).
    private var isLoggedIn = false
    private var pagesSinceOpen = 0

    // Auth cookie names per network — these cookies only exist when logged in.
    private val AUTH_COOKIES = mapOf(
        "instagram" to listOf("sessionid", "ds_user_id"),
        "facebook"  to listOf("c_user"),
        "twitter"   to listOf("auth_token", "ct0"),
        "tiktok"    to listOf("sessionid", "sid_tt"),
        "pinterest"  to listOf("_auth", "_pinterest_sess"),
        "linkedin"  to listOf("li_at"),
        "reddit"    to listOf("reddit_session", "token_v2"),
        "threads"   to listOf("sessionid", "ds_user_id"),
        "discord"   to listOf("__dcfduid", "__sdcfduid"),
        "snapchat"  to listOf("sc-a-session"),
        "quora"     to listOf("m-login"),  // m-b is set for all visitors, m-login only after auth
        "whatsapp"  to listOf("wa_lang_pref"),  // minimal signal — WhatsApp Web is auth-gated
        "telegram"  to listOf("stel_ssid"),
        "nextdoor"  to listOf("ndsid"),
        "patreon"   to listOf("patreon_device_id", "session_id"),
        "theresanaiforthat" to listOf("authjs.session-token", "__Secure-authjs.session-token"),
        "industrysocial" to listOf("sessionid", "session"),
        "bluesky"   to listOf("did", "sid"),
        "mastodon"  to listOf("_mastodon_session"),
        "substack"  to listOf("substack.sid", "substack.lli"),
        "ko-fi"     to listOf("__stripe_mid", "kofi_session"),
        "buymeacoffee" to listOf("connect.sid", "remember_web"),
        "producthunt" to listOf("_producthunt_session"),
        "indiehackers" to listOf("_indie_hackers_session"),
        "hackernews" to listOf("user"),
        "folloverse" to listOf("sessionid", "session"),
        "industrysocial-waitlist" to listOf("sessionid", "session"),
        "koru" to listOf("sessionid", "session"),
        "medium" to listOf("uid", "sid"),
    )

    /** Check if the current network has auth cookies → user is logged in. */
    private fun checkLoggedIn(): Boolean {
        val networkId = currentNetworkId ?: return false
        val sessionKey = currentAccountId ?: return false
        val authNames = AUTH_COOKIES[networkId] ?: return false
        val cm = profileCookieManagerForSession(sessionKey) ?: CookieManager.getInstance()
        val net = NETWORKS.find { it.id == networkId } ?: return false
        val cookies = cm.getCookie(net.url) ?: return false
        return authNames.any { name -> cookies.contains("$name=") }
    }


    // Visible network IDs — synced from Vue profile visibility (null = show all)
    private var visibleNetworkIds: Set<String>? = null

    // Profile list for the popup menu — synced from Vue whenever profiles change
    private var menuProfiles: List<ProfileMenuItem> = emptyList()
    private var activeProfileId: String = ""

    // Hardware back button intercept — registered when webview opens, removed when closed
    private var backCallback: OnBackPressedCallback? = null

    // PrimeIcons typeface — loaded once from assets/primeicons.ttf
    private val primeIconsTypeface: Typeface by lazy {
        Typeface.createFromAsset(activity.assets, "primeicons.ttf")
    }

    init {
        Log.i(TAG, "NativeWebViewPlugin instantiated")
    }

    /**
     * Walk the view hierarchy to find the FIRST WebView that is NOT our social webview.
     * This is the Tauri/WRY main WebView running the Vue app.
     */
    private fun findMainWebViewInHierarchy(): WebView? {
        val root = activity.window?.decorView as? ViewGroup ?: return null
        return findWebViewRecursive(root)
    }

    private fun findWebViewRecursive(parent: ViewGroup): WebView? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is WebView && !isManagedHostWebView(child)) {
                return child
            }
            if (child is ViewGroup) {
                val found = findWebViewRecursive(child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun isManagedHostWebView(candidate: WebView): Boolean {
        if (candidate === socialWebView) return true
        return sessionHosts.values.any { it.webView === candidate }
    }

    private fun hostForWebView(candidate: WebView): SessionWebViewHost? {
        return sessionHosts.values.firstOrNull { it.webView === candidate }
    }

    private fun isInactiveManagedCallback(view: WebView): Boolean {
        val host = hostForWebView(view) ?: return false
        if (host.session.sessionKey == activeHostSessionKey) {
            syncGlobalsFromHost(host)
            return false
        }
        return true
    }

    /**
     * Get the main Tauri WebView — uses load() reference, with view-hierarchy fallback.
     */
    private fun getMainWebView(): WebView? {
        mainWebView?.let { return it }
        // Fallback: walk the view hierarchy
        val found = findMainWebViewInHierarchy()
        if (found != null) {
            mainWebView = found
            Log.i(TAG, "mainWebView found via view hierarchy fallback: ${found.hashCode()}")
        } else {
            Log.e(TAG, "mainWebView not found — load() was not called and hierarchy search failed")
        }
        return found
    }

    // ── Debug log buffer — last 200 lines, copyable from popup menu ─────────
    private val debugLog = mutableListOf<String>()
    private fun dbg(msg: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        val line = "$ts $msg"
        Log.i(TAG, msg)
        synchronized(debugLog) {
            debugLog.add(line)
            if (debugLog.size > 400) debugLog.removeAt(0)
        }
    }

    // Usage counters — persisted across app restarts via SharedPreferences
    private val prefs by lazy {
        activity.getSharedPreferences("sfz_network_usage", Context.MODE_PRIVATE)
    }

    // Cookie isolation — save/restore cookies per (profile, network) pair.
    // Android CookieManager is a singleton shared by all WebViews, so we manually
    // save cookies before closing and restore them before opening.
    private val cookiePrefs by lazy {
        activity.getSharedPreferences("sfz_cookies", Context.MODE_PRIVATE)
    }
    private val localStoragePrefs by lazy {
        activity.getSharedPreferences(LOCAL_STORAGE_PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val localStorageRestorePendingPrefs by lazy {
        activity.getSharedPreferences(LOCAL_STORAGE_RESTORE_PENDING_PREFS_NAME, Context.MODE_PRIVATE)
    }

    // All base URLs we need to save/restore cookies for
    private val COOKIE_URLS = NETWORKS.map { it.url } + listOf(
        "https://www.facebook.com", "https://m.facebook.com",
        "https://www.instagram.com", "https://m.instagram.com",
        "https://www.threads.net", "https://mobile.twitter.com",
        "https://www.tiktok.com", "https://www.reddit.com",
        "https://www.linkedin.com",
        "https://www.snapchat.com",
        "https://accounts.snapchat.com",
    )

    private val knownNetworkIdsByLength = NETWORKS
        .map { it.id }
        .sortedByDescending { it.length }

    private fun isKnownNetworkId(networkId: String): Boolean {
        return knownNetworkIdsByLength.contains(networkId)
    }

    private fun buildSessionIdentity(profileId: String, networkId: String): SessionIdentity? {
        val normalizedProfileId = profileId.trim()
        val normalizedNetworkId = networkId.trim()
        if (normalizedProfileId.isEmpty() || normalizedNetworkId.isEmpty()) return null
        if (!isKnownNetworkId(normalizedNetworkId)) return null
        return SessionIdentity(
            profileId = normalizedProfileId,
            networkId = normalizedNetworkId,
            sessionKey = "$normalizedProfileId-$normalizedNetworkId",
        )
    }

    private fun parseSessionIdentity(sessionKey: String, expectedNetworkId: String? = null): SessionIdentity? {
        val normalizedSessionKey = sessionKey.trim()
        if (normalizedSessionKey.isEmpty()) return null

        val expected = expectedNetworkId?.trim()?.takeIf { it.isNotEmpty() }
        if (expected != null) {
            if (!isKnownNetworkId(expected)) return null
            val suffix = "-$expected"
            if (!normalizedSessionKey.endsWith(suffix)) return null
            val profileId = normalizedSessionKey.removeSuffix(suffix)
            if (profileId.isEmpty()) return null
            return SessionIdentity(profileId = profileId, networkId = expected, sessionKey = normalizedSessionKey)
        }

        for (networkId in knownNetworkIdsByLength) {
            val suffix = "-$networkId"
            if (!normalizedSessionKey.endsWith(suffix)) continue
            val profileId = normalizedSessionKey.removeSuffix(suffix)
            if (profileId.isEmpty()) continue
            return SessionIdentity(profileId = profileId, networkId = networkId, sessionKey = normalizedSessionKey)
        }
        return null
    }

    private fun nowElapsedMs(): Long = android.os.SystemClock.elapsedRealtime()

    private fun webkitProfileNameForSession(sessionKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sessionKey.toByteArray(Charsets.UTF_8))
        val hex = buildString(digest.size * 2) {
            for (b in digest) append(String.format("%02x", b))
        }
        return WEBKIT_PROFILE_PREFIX + hex.take(24)
    }

    private fun ensureMultiProfileModeInitialized() {
        if (multiProfileInitTried) return
        multiProfileInitTried = true
        multiProfileModeEnabled = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
        if (multiProfileModeEnabled) {
            dbg("android-webview mode=multi-profile")
        } else {
            disableMultiProfilePooling = true
            dbg("android-webview mode=fallback-single-webview (MULTI_PROFILE unsupported)")
        }
    }

    private fun isPoolingEnabled(): Boolean {
        ensureMultiProfileModeInitialized()
        return multiProfileModeEnabled && !disableMultiProfilePooling
    }

    private fun activeHost(): SessionWebViewHost? {
        val activeKey = activeHostSessionKey ?: return null
        return sessionHosts[activeKey]
    }

    private fun markHostUsed(host: SessionWebViewHost) {
        host.lastUsedAt = nowElapsedMs()
    }

    private fun syncGlobalsFromHost(host: SessionWebViewHost) {
        socialRoot = host.root
        socialWebView = host.webView
        bottomBarView = host.bottomBar
        currentAccountId = host.session.sessionKey
        currentNetworkId = host.session.networkId
        activeSessionIdentity = host.session
        localStorageScriptHandler = host.localStorageScriptHandler
        localStorageRestoreActive = host.localStorageRestoreActive
        localStorageCaptureActive = host.localStorageCaptureActive
        initialBackIndex = host.initialBackIndex
        isLoggedIn = host.isLoggedIn
        pagesSinceOpen = host.pagesSinceOpen
        facebookDesktopOverride = host.facebookDesktopOverride
        facebookStoryNoticeShown = host.facebookStoryNoticeShown
    }

    private fun syncHostFromGlobals(host: SessionWebViewHost) {
        host.localStorageScriptHandler = localStorageScriptHandler
        host.localStorageRestoreActive = localStorageRestoreActive
        host.localStorageCaptureActive = localStorageCaptureActive
        host.initialBackIndex = initialBackIndex
        host.isLoggedIn = isLoggedIn
        host.pagesSinceOpen = pagesSinceOpen
        host.facebookDesktopOverride = facebookDesktopOverride
        host.facebookStoryNoticeShown = facebookStoryNoticeShown
    }

    private fun attachHostAsActive(host: SessionWebViewHost) {
        syncGlobalsFromHost(host)
        host.isVisible = true
        markHostUsed(host)
        activeHostSessionKey = host.session.sessionKey
    }

    private fun clearGlobalActivePointers() {
        socialRoot = null
        socialWebView = null
        bottomBarView = null
        currentAccountId = null
        currentNetworkId = null
        activeSessionIdentity = null
        localStorageScriptHandler = null
        localStorageRestoreActive = false
        localStorageCaptureActive = false
        initialBackIndex = -1
        isLoggedIn = false
        pagesSinceOpen = 0
        facebookDesktopOverride = false
        facebookStoryNoticeShown = false
    }

    private fun originFromUrl(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase() ?: return null
            val host = uri.host?.lowercase() ?: return null
            if (scheme != "http" && scheme != "https") return null
            val isDefaultPort = (scheme == "http" && uri.port == 80) || (scheme == "https" && uri.port == 443)
            if (uri.port > 0 && !isDefaultPort) "$scheme://$host:${uri.port}" else "$scheme://$host"
        } catch (_: Exception) {
            null
        }
    }

    private fun originFromSourceUri(sourceOrigin: Uri): String? {
        val raw = sourceOrigin.toString()
        if (raw == "null") return null
        return originFromUrl(raw)
    }

    private fun normalizeDeclaredStorageOrigins(origins: Collection<String>?): Set<String> {
        if (origins.isNullOrEmpty()) return emptySet()
        val normalized = linkedSetOf<String>()
        for (rawOrigin in origins) {
            val origin = originFromUrl(rawOrigin) ?: continue
            if (!origin.startsWith("https://")) continue
            normalized.add(origin)
        }
        return normalized
    }

    private fun getDeclaredStorageOrigins(session: SessionIdentity): Set<String> {
        return declaredStorageOriginsBySession[session.sessionKey]
            ?: declaredStorageOriginsByNetwork[session.networkId]
            ?: emptySet()
    }

    private fun setDeclaredStorageOrigins(session: SessionIdentity, origins: Collection<String>?) {
        val normalized = normalizeDeclaredStorageOrigins(origins)
        if (normalized.isEmpty()) {
            declaredStorageOriginsBySession.remove(session.sessionKey)
            return
        }
        declaredStorageOriginsBySession[session.sessionKey] = normalized
        declaredStorageOriginsByNetwork[session.networkId] = normalized
    }

    private fun setDeclaredStorageOriginsByNetworkJson(rawJson: String) {
        val next = mutableMapOf<String, Set<String>>()
        try {
            val payload = JSONObject(rawJson.ifBlank { "{}" })
            val keys = payload.keys()
            while (keys.hasNext()) {
                val networkId = keys.next()
                if (!isKnownNetworkId(networkId)) continue
                val rawOrigins = payload.optJSONArray(networkId) ?: continue
                val origins = mutableListOf<String>()
                for (i in 0 until rawOrigins.length()) {
                    rawOrigins.optString(i, "").takeIf { it.isNotBlank() }?.let { origins.add(it) }
                }
                val normalized = normalizeDeclaredStorageOrigins(origins)
                if (normalized.isNotEmpty()) {
                    next[networkId] = normalized
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ignored invalid storage origin matrix for bottom bar", e)
            return
        }

        declaredStorageOriginsByNetwork.clear()
        declaredStorageOriginsByNetwork.putAll(next)
    }

    private fun removeDeclaredStorageOriginsForSession(sessionKey: String) {
        declaredStorageOriginsBySession.remove(sessionKey)
    }

    private fun removeDeclaredStorageOriginsForProfile(profileId: String) {
        val keysToRemove = declaredStorageOriginsBySession.keys.filter { key ->
            parseSessionIdentity(key)?.profileId == profileId
        }
        for (key in keysToRemove) {
            declaredStorageOriginsBySession.remove(key)
        }
    }

    private fun localStoragePrefKey(sessionKey: String, origin: String): String {
        return "$sessionKey|$origin"
    }

    private fun sessionKeyFromPrefKey(key: String): String {
        return key.substringBefore("|", "")
    }

    private fun collectValidSessionKeysFromPrefKeys(keys: Collection<String>): Set<String> {
        return keys
            .asSequence()
            .map { sessionKeyFromPrefKey(it) }
            .filter { it.isNotBlank() && parseSessionIdentity(it) != null }
            .toSet()
    }

    private fun hasPendingLocalStorageRestore(sessionKey: String): Boolean {
        return localStorageRestorePendingPrefs.getBoolean(sessionKey, false)
    }

    private fun clearPendingLocalStorageRestore(sessionKey: String) {
        localStorageRestorePendingPrefs.edit().remove(sessionKey).apply()
    }

    private fun replacePendingLocalStorageRestores(sessionKeys: Set<String>) {
        val editor = localStorageRestorePendingPrefs.edit().clear()
        for (sessionKey in sessionKeys) {
            editor.putBoolean(sessionKey, true)
        }
        editor.apply()
    }

    private fun prefKeyBelongsToProfile(key: String, profileId: String): Boolean {
        val sessionKey = sessionKeyFromPrefKey(key)
        if (sessionKey.isBlank()) return false
        return parseSessionIdentity(sessionKey)?.profileId == profileId
    }

    private fun loadStoredOriginsForSession(sessionKey: String): Set<String> {
        val prefix = "$sessionKey|"
        return localStoragePrefs.all.keys
            .asSequence()
            .filter { it.startsWith(prefix) }
            .mapNotNull { key ->
                val origin = key.removePrefix(prefix).trim()
                origin.takeIf { it.isNotEmpty() }
            }
            .toSet()
    }

    private fun loadLocalStorageSnapshot(sessionKey: String, origin: String): JSONObject? {
        val raw = localStoragePrefs.getString(localStoragePrefKey(sessionKey, origin), null) ?: return null
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            Log.w(TAG, "Skipped corrupt localStorage snapshot for one origin")
            null
        }
    }

    private fun saveLocalStorageSnapshot(sessionKey: String, origin: String, snapshot: JSONObject) {
        val encoded = snapshot.toString()
        if (encoded.length > LOCAL_STORAGE_CAPTURE_MAX_BYTES) {
            Log.w(TAG, "Skipped oversized localStorage snapshot")
            return
        }
        localStoragePrefs.edit()
            .putString(localStoragePrefKey(sessionKey, origin), encoded)
            .apply()
    }

    private fun removeLocalStorageSnapshotsForSession(sessionKey: String) {
        val prefix = "$sessionKey|"
        val keys = localStoragePrefs.all.keys.filter { it.startsWith(prefix) }
        if (keys.isEmpty()) return
        val editor = localStoragePrefs.edit()
        for (key in keys) {
            editor.remove(key)
        }
        editor.apply()
    }

    private fun removeLocalStorageSnapshotsForProfile(profileId: String) {
        val keys = localStoragePrefs.all.keys.filter { prefKeyBelongsToProfile(it, profileId) }
        if (keys.isEmpty()) return
        val editor = localStoragePrefs.edit()
        for (key in keys) {
            editor.remove(key)
        }
        editor.apply()
    }

    private fun buildSessionLocalStoragePayload(sessionKey: String, targetOrigin: String): JSONObject {
        val payload = JSONObject()
        val allOrigins = mutableSetOf<String>()
        allOrigins.add(targetOrigin)
        for (origin in loadStoredOriginsForSession(sessionKey)) {
            originFromUrl(origin)?.let { allOrigins.add(it) }
        }
        for (origin in allOrigins) {
            loadLocalStorageSnapshot(sessionKey, origin)?.let { payload.put(origin, it) }
        }
        return payload
    }

    private fun buildLocalStorageDocumentStartScript(sessionKey: String, snapshotsByOrigin: JSONObject): String {
        val quotedSessionKey = JSONObject.quote(sessionKey)
        val quotedBridgeName = JSONObject.quote(STORAGE_BRIDGE_OBJECT)
        val quotedSnapshots = JSONObject.quote(snapshotsByOrigin.toString())
        return """
            (function(){
              if (window.__sfzLocalStorageBridgeSession === $quotedSessionKey) return;
              window.__sfzLocalStorageBridgeSession = $quotedSessionKey;
              var snapshotsByOrigin = {};
              try {
                snapshotsByOrigin = JSON.parse($quotedSnapshots) || {};
              } catch (e) {
                snapshotsByOrigin = {};
              }
              var origin = location.origin || '';
              var snapshot = snapshotsByOrigin[origin];
              try {
                localStorage.clear();
                if (snapshot && typeof snapshot === 'object') {
                  Object.keys(snapshot).forEach(function(key) {
                    var value = snapshot[key];
                    if (value === null || value === undefined) return;
                    localStorage.setItem(key, String(value));
                  });
                }
              } catch (e) {}
              var bridgeName = $quotedBridgeName;
              var bridge = window[bridgeName];
              if (!bridge || typeof bridge.postMessage !== 'function') return;
              var sendSnapshot = function(reason) {
                try {
                  var data = {};
                  for (var i = 0; i < localStorage.length; i++) {
                    var key = localStorage.key(i);
                    if (key === null) continue;
                    data[key] = localStorage.getItem(key);
                  }
                  bridge.postMessage(JSON.stringify({
                    type: 'localStorageSnapshot',
                    sessionKey: $quotedSessionKey,
                    origin: location.origin || '',
                    reason: reason || 'change',
                    data: data
                  }));
                } catch (e) {}
              };
              if (!window.__sfzLocalStoragePatched) {
                window.__sfzLocalStoragePatched = true;
                ['setItem','removeItem','clear'].forEach(function(methodName) {
                  try {
                    var original = localStorage[methodName];
                    if (typeof original !== 'function') return;
                    localStorage[methodName] = function() {
                      var result = original.apply(localStorage, arguments);
                      sendSnapshot(methodName);
                      return result;
                    };
                  } catch (e) {}
                });
              }
              sendSnapshot('init');
            })();
        """.trimIndent()
    }

    private fun buildLocalStorageCaptureDocumentStartScript(sessionKey: String): String {
        val quotedSessionKey = JSONObject.quote(sessionKey)
        val quotedBridgeName = JSONObject.quote(STORAGE_BRIDGE_OBJECT)
        return """
            (function(){
              if (window.__sfzLocalStorageBridgeSession === $quotedSessionKey) return;
              window.__sfzLocalStorageBridgeSession = $quotedSessionKey;
              var bridgeName = $quotedBridgeName;
              var bridge = window[bridgeName];
              if (!bridge || typeof bridge.postMessage !== 'function') return;
              var sendSnapshot = function(reason) {
                try {
                  var data = {};
                  for (var i = 0; i < localStorage.length; i++) {
                    var key = localStorage.key(i);
                    if (key === null) continue;
                    data[key] = localStorage.getItem(key);
                  }
                  bridge.postMessage(JSON.stringify({
                    type: 'localStorageSnapshot',
                    sessionKey: $quotedSessionKey,
                    origin: location.origin || '',
                    reason: reason || 'change',
                    data: data
                  }));
                } catch (e) {}
              };
              if (!window.__sfzLocalStoragePatched) {
                window.__sfzLocalStoragePatched = true;
                ['setItem','removeItem','clear'].forEach(function(methodName) {
                  try {
                    var original = localStorage[methodName];
                    if (typeof original !== 'function') return;
                    localStorage[methodName] = function() {
                      var result = original.apply(localStorage, arguments);
                      sendSnapshot(methodName);
                      return result;
                    };
                  } catch (e) {}
                });
              }
              sendSnapshot('init');
            })();
        """.trimIndent()
    }

    /** Save all cookies for the current profile session key. */
    private fun saveCookiesForSession(sessionKey: String) {
        val cm = profileCookieManagerForSession(sessionKey) ?: CookieManager.getInstance()
        val editor = cookiePrefs.edit()
        var totalSaved = 0
        for (url in COOKIE_URLS) {
            val cookies = cm.getCookie(url)
            if (cookies != null) {
                val names = cookies.split(";").map { it.trim().substringBefore("=") }
                editor.putString("$sessionKey|$url", cookies)
                totalSaved += names.size
            } else {
                editor.remove("$sessionKey|$url")
            }
        }
        editor.apply()
        dbg("cookies saved for session (${totalSaved} entries)")
    }

    /** Clear all cookies, then restore saved cookies for the target session. */
    /** Extract base domain from URL host (e.g. www.snapchat.com → .snapchat.com).
     *  Restored cookies are set as domain-wide so all subdomains can access them.
     *  Without this, cookies originally set on .snapchat.com get restored only for
     *  www.snapchat.com, and API subdomains lose the session. */
    private fun baseDomainOf(url: String): String? {
        val host = android.net.Uri.parse(url).host ?: return null
        val parts = host.split(".")
        return if (parts.size >= 2) ".${parts.takeLast(2).joinToString(".")}" else null
    }

    private fun restoreCookiesForSession(sessionKey: String, onComplete: (Boolean) -> Unit) {
        if (isPoolingEnabled()) {
            val cm = profileCookieManagerForSession(sessionKey)
            if (cm == null) {
                Log.w(TAG, "Session isolation degraded: WebKit profile cookie manager unavailable")
                onComplete(false)
                return
            }
            try {
                var restoredCookies = 0
                for (url in COOKIE_URLS) {
                    val cookies = cookiePrefs.getString("$sessionKey|$url", null) ?: continue
                    val domain = baseDomainOf(url)
                    val parts = cookies.split(";")
                    for (cookie in parts) {
                        val trimmed = cookie.trim()
                        if (trimmed.isEmpty()) continue
                        val name = trimmed.substringBefore("=")
                        val cookieWithAttrs = if (name.startsWith("__Host-")) {
                            "$trimmed; Path=/; Secure"
                        } else if (domain != null) {
                            "$trimmed; Domain=$domain; Path=/; Secure"
                        } else {
                            trimmed
                        }
                        cm.setCookie(url, cookieWithAttrs)
                        restoredCookies++
                    }
                }
                cm.flush()
                if (restoredCookies > 0) {
                    dbg("cookies restored into WebKit profile (${restoredCookies} entries)")
                }
                onComplete(true)
            } catch (e: Exception) {
                Log.w(TAG, "Session isolation degraded: profile cookie restore error", e)
                onComplete(false)
            }
            return
        }
        val cm = CookieManager.getInstance()
        var done = false
        fun finish(ok: Boolean) {
            if (done) return
            done = true
            onComplete(ok)
        }

        activity.window.decorView.postDelayed({
            if (!done) {
                Log.w(TAG, "Session isolation degraded: cookie restore callback timeout")
                finish(false)
            }
        }, 4000)

        try {
            cm.removeAllCookies {
                try {
                    var restoredCookies = 0
                    for (url in COOKIE_URLS) {
                        val cookies = cookiePrefs.getString("$sessionKey|$url", null) ?: continue
                        val domain = baseDomainOf(url)
                        val parts = cookies.split(";")
                        // setCookie expects one cookie at a time; the stored string may have multiple
                        for (cookie in parts) {
                            val trimmed = cookie.trim()
                            if (trimmed.isNotEmpty()) {
                                // __Host- cookies MUST NOT have a Domain attribute (RFC 6265bis).
                                // Setting Domain= on them causes silent rejection by the browser.
                                val name = trimmed.substringBefore("=")
                                val cookieWithAttrs = if (name.startsWith("__Host-")) {
                                    "$trimmed; Path=/; Secure"
                                } else if (domain != null) {
                                    "$trimmed; Domain=$domain; Path=/; Secure"
                                } else {
                                    trimmed
                                }
                                cm.setCookie(url, cookieWithAttrs)
                                restoredCookies++
                            }
                        }
                    }
                    cm.flush()
                    dbg("cookies restored for session (${restoredCookies} entries)")
                    finish(true)
                } catch (e: Exception) {
                    Log.w(TAG, "Session isolation degraded: cookie restore error", e)
                    finish(false)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Session isolation degraded: cookie restore setup error", e)
            finish(false)
        }
    }

    private fun profileCookieManagerForSession(sessionKey: String): CookieManager? {
        if (!isPoolingEnabled()) return null
        val host = sessionHosts[sessionKey] ?: return null
        return try {
            WebViewCompat.getProfile(host.webView).cookieManager
        } catch (_: Exception) {
            null
        }
    }

    private fun resetStorageIsolationHooks(webView: WebView, host: SessionWebViewHost? = activeHost()) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                host?.localStorageScriptHandler?.remove()
            } catch (e: Exception) {
                Log.w(TAG, "Could not remove previous localStorage restore script", e)
            }
        }
        if (host != null) {
            host.localStorageScriptHandler = null
            host.localStorageRestoreActive = false
            host.localStorageCaptureActive = false
        }
        if (host == activeHost()) {
            localStorageScriptHandler = null
            localStorageRestoreActive = false
            localStorageCaptureActive = false
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            try {
                WebViewCompat.removeWebMessageListener(webView, STORAGE_BRIDGE_OBJECT)
            } catch (e: Exception) {
                Log.w(TAG, "Could not remove previous localStorage listener", e)
            }
        }
    }

    private fun onLocalStorageBridgeMessage(
        messageJson: String?,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        expectedSession: SessionIdentity,
    ) {
        if (!isMainFrame || messageJson.isNullOrBlank()) return

        val payload = try {
            JSONObject(messageJson)
        } catch (_: Exception) {
            Log.w(TAG, "Ignored invalid localStorage bridge payload")
            return
        }

        if (payload.optString("type") != "localStorageSnapshot") return

        val payloadSessionKey = payload.optString("sessionKey", "")
        if (payloadSessionKey != expectedSession.sessionKey) {
            Log.w(TAG, "Ignored localStorage bridge message for unexpected session")
            return
        }

        val payloadOrigin = originFromUrl(payload.optString("origin", ""))
        val source = originFromSourceUri(sourceOrigin)
        if (payloadOrigin == null || source == null || payloadOrigin != source) {
            Log.w(TAG, "Ignored localStorage bridge message with invalid origin")
            return
        }

        val storageData = payload.optJSONObject("data") ?: JSONObject()
        saveLocalStorageSnapshot(expectedSession.sessionKey, payloadOrigin, storageData)
    }

    private fun configureStorageIsolationForNavigation(
        webView: WebView,
        session: SessionIdentity,
        targetUrl: String,
        declaredStorageOrigins: Collection<String> = emptyList(),
        host: SessionWebViewHost? = activeHost(),
    ) {
        val targetOrigin = originFromUrl(targetUrl)
        resetStorageIsolationHooks(webView, host)

        if (targetOrigin == null) {
            Log.w(TAG, "Session isolation degraded: target origin unavailable")
            return
        }

        val allowedOrigins = mutableSetOf<String>()
        allowedOrigins.add(targetOrigin)
        for (origin in loadStoredOriginsForSession(session.sessionKey)) {
            originFromUrl(origin)?.let { allowedOrigins.add(it) }
        }
        for (origin in declaredStorageOrigins) {
            originFromUrl(origin)?.let { allowedOrigins.add(it) }
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            try {
                WebViewCompat.addWebMessageListener(
                    webView,
                    STORAGE_BRIDGE_OBJECT,
                    allowedOrigins
                ) { _, message, sourceOrigin, isMainFrame, _ ->
                    onLocalStorageBridgeMessage(message.data, sourceOrigin, isMainFrame, session)
                }
                host?.localStorageCaptureActive = true
            } catch (e: Exception) {
                Log.w(TAG, "Session isolation degraded: localStorage capture unavailable", e)
            }
        } else {
            Log.w(TAG, "Session isolation degraded: WEB_MESSAGE_LISTENER unsupported")
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                val restoreLocalStorage = !isPoolingEnabled() || hasPendingLocalStorageRestore(session.sessionKey)
                val script = if (restoreLocalStorage) {
                    val snapshotsByOrigin = buildSessionLocalStoragePayload(session.sessionKey, targetOrigin)
                    buildLocalStorageDocumentStartScript(session.sessionKey, snapshotsByOrigin)
                } else {
                    buildLocalStorageCaptureDocumentStartScript(session.sessionKey)
                }
                val handler = WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    script,
                    allowedOrigins
                )
                host?.localStorageScriptHandler = handler
                host?.localStorageRestoreActive = restoreLocalStorage
                if (restoreLocalStorage && isPoolingEnabled()) {
                    clearPendingLocalStorageRestore(session.sessionKey)
                    dbg("localStorage restore armed for imported session")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Session isolation degraded: localStorage restore unavailable", e)
            }
        } else {
            Log.w(TAG, "Session isolation degraded: DOCUMENT_START_SCRIPT unsupported")
        }

        if (host == activeHost()) {
            localStorageScriptHandler = host?.localStorageScriptHandler
            localStorageRestoreActive = host?.localStorageRestoreActive ?: false
            localStorageCaptureActive = host?.localStorageCaptureActive ?: false
        }
    }

    private fun prepareSessionBeforeLoad(
        webView: WebView,
        session: SessionIdentity,
        targetUrl: String,
        declaredStorageOrigins: Collection<String> = emptyList(),
        host: SessionWebViewHost? = activeHost(),
        onReady: (Boolean) -> Unit,
    ) {
        activeSessionIdentity = session
        restoreCookiesForSession(session.sessionKey) { cookiesRestored ->
            if (!cookiesRestored) {
                Log.w(TAG, "Session isolation degraded: cookie restore did not complete cleanly")
            }
            configureStorageIsolationForNavigation(
                webView,
                session,
                targetUrl,
                declaredStorageOrigins,
                host,
            )
            onReady(cookiesRestored)
        }
    }

    private fun hideHostInternal(
        host: SessionWebViewHost,
        persistFallbackSnapshot: Boolean = true,
    ) {
        syncGlobalsFromHost(host)
        if (persistFallbackSnapshot) {
            saveCookiesForSession(host.session.sessionKey)
        }
        syncHostFromGlobals(host)
        host.isVisible = false
        host.root.visibility = View.GONE
        host.webView.onPause()
        markHostUsed(host)
    }

    private fun showHostInternal(host: SessionWebViewHost) {
        attachHostAsActive(host)
        host.root.visibility = View.VISIBLE
        host.webView.onResume()
        applyVisibleHostPreferences(host)
        updateBottomBarActiveNetwork(host.session.networkId)
        registerBackCallback()
    }

    private fun applyVisibleHostPreferences(host: SessionWebViewHost) {
        applyTextZoomToWebView(host.webView)
        applyDarkModeToWebView(host.webView)
        applyGrayscaleToWebView(host.webView)
        applyMuteToWebView(host.webView)
        applyGrayscaleToBottomBar(host.bottomBar)
        applyDarkModeToBottomBar(host.bottomBar)
    }

    private fun hideCurrentHostIfAny(persistFallbackSnapshot: Boolean = true) {
        val host = activeHost() ?: return
        hideHostInternal(host, persistFallbackSnapshot)
    }

    private fun switchToSession(
        targetSession: SessionIdentity,
        targetUrl: String,
        loadIfMissing: Boolean,
        declaredStorageOrigins: Collection<String> = emptyList(),
        onResult: (shown: Boolean, cookiesRestored: Boolean) -> Unit,
    ) {
        ensureMultiProfileModeInitialized()

        if (!isPoolingEnabled()) {
            val keysToDestroy = sessionHosts.keys
                .filter { it != targetSession.sessionKey }
                .toList()
            for (key in keysToDestroy) {
                destroyHost(key, "fallback-single")
            }
        }

        val current = activeHost()
        if (current != null && current.session.sessionKey != targetSession.sessionKey) {
            hideHostInternal(current)
        }

        val existing = sessionHosts[targetSession.sessionKey]
        if (existing != null) {
            showHostInternal(existing)
            onResult(true, true)
            return
        }

        if (!loadIfMissing) {
            onResult(false, false)
            return
        }

        val created = createHostForSession(targetSession, targetUrl)
        if (created == null) {
            onResult(false, false)
            return
        }

        showHostInternal(created)
        if (targetSession.networkId == "facebook") {
            created.facebookDesktopOverride = isFacebookDesktopFlow(targetUrl)
            created.facebookStoryNoticeShown = false
        }
        syncGlobalsFromHost(created)
        created.initialBackIndex = -1
        created.isLoggedIn = false
        created.pagesSinceOpen = 0
        prepareSessionBeforeLoad(
            created.webView,
            targetSession,
            targetUrl,
            declaredStorageOrigins,
            created,
        ) { cookiesRestored ->
            created.initialBackIndex = -1
            created.isLoggedIn = false
            created.pagesSinceOpen = 0
            syncGlobalsFromHost(created)
            applyUaForNetwork(targetSession.networkId, targetUrl)
            created.webView.loadUrl(targetUrl)
            updateBottomBarActiveNetwork(targetSession.networkId)
            incrementUsage(targetSession.networkId)
            syncHostFromGlobals(created)
            enforceWarmHostBound()
            onResult(true, cookiesRestored)
        }
    }

    private fun createHostForSession(session: SessionIdentity, initialUrl: String): SessionWebViewHost? {
        val density = activity.resources.displayMetrics.density
        val windowInsets = activity.window.decorView.rootWindowInsets
        val statusBarHeight = windowInsets?.systemWindowInsetTop ?: 0
        val navBarHeight = windowInsets?.systemWindowInsetBottom ?: 0
        val barHeight = (52 * density).toInt()

        val root = FrameLayout(activity)
        val webView = createWebView(webkitProfileNameForSession(session.sessionKey))

        val wvParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        wvParams.topMargin = statusBarHeight
        wvParams.bottomMargin = navBarHeight + barHeight
        webView.layoutParams = wvParams
        webView.setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.view.WindowInsets.CONSUMED
            } else {
                @Suppress("DEPRECATION")
                insets.replaceSystemWindowInsets(0, 0, 0, 0)
            }
        }

        val bottomBar = buildBottomBar(density, navBarHeight, session.networkId, sortedNetworks())
        val bottomBarParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            barHeight + navBarHeight
        )
        bottomBarParams.gravity = Gravity.BOTTOM
        bottomBar.layoutParams = bottomBarParams

        root.addView(webView)
        root.addView(bottomBar)
        root.visibility = View.GONE

        activity.addContentView(
            root,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val host = SessionWebViewHost(
            session = session,
            profileName = webkitProfileNameForSession(session.sessionKey),
            root = root,
            webView = webView,
            bottomBar = bottomBar,
            lastUsedAt = nowElapsedMs(),
        )

        sessionHosts[session.sessionKey] = host
        markHostUsed(host)
        applyMuteToWebView(webView)
        if (isGrayscale) applyGrayscaleToWebView(webView)
        applyTextZoomToWebView(webView)
        if (session.networkId == "facebook") {
            host.facebookDesktopOverride = isFacebookDesktopFlow(initialUrl)
            host.facebookStoryNoticeShown = false
        }
        return host
    }

    private fun destroyHost(sessionKey: String, reason: String) {
        val host = sessionHosts[sessionKey] ?: return
        if (activeHostSessionKey == sessionKey) {
            activeHostSessionKey = null
        }
        syncGlobalsFromHost(host)
        hideHostInternal(host)
        dismissPopupMenu()
        resetStorageIsolationHooks(host.webView, host)
        runCatching { host.webView.stopLoading() }
        runCatching { host.webView.onPause() }
        sessionHosts.remove(sessionKey)
        host.webView.destroy()
        (host.root.parent as? ViewGroup)?.removeView(host.root)
        dbg("host destroyed reason=$reason")

        if (activeHostSessionKey == null) {
            clearGlobalActivePointers()
            backCallback?.remove()
            backCallback = null
        } else {
            val stillActive = activeHost()
            if (stillActive != null) {
                syncGlobalsFromHost(stillActive)
            }
        }
    }

    private fun enforceWarmHostBound() {
        if (!isPoolingEnabled()) return
        if (sessionHosts.size <= MAX_WARM_HOSTS) return

        val candidates = sessionHosts.values
            .filter { !it.isVisible }
            .sortedBy { it.lastUsedAt }

        for (host in candidates) {
            if (sessionHosts.size <= MAX_WARM_HOSTS) break
            destroyHost(host.session.sessionKey, "lru-evict")
        }
    }

    private fun disablePoolingAndDestroyInactiveHosts() {
        disableMultiProfilePooling = true
        val keys = sessionHosts.values
            .filter { !it.isVisible }
            .map { it.session.sessionKey }
        for (key in keys) {
            destroyHost(key, "disable-pooling")
        }
    }

    private fun deleteWebkitProfileByName(profileName: String) {
        if (!isPoolingEnabled()) return
        try {
            val deleted = ProfileStore.getInstance().deleteProfile(profileName)
            dbg("webkit profile delete requested=$deleted")
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Could not delete WebKit profile (still in use)")
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete WebKit profile")
        }
    }

    /** Capture the main Tauri WebView on plugin load — this is the Vue app webview. */
    override fun load(webView: WebView) {
        mainWebView = webView
        Log.i(TAG, "load() called — mainWebView captured: ${webView.hashCode()}")
        ensureMultiProfileModeInitialized()
        // Init SoundPool for tap sound (uses bundled asset, independent of system "Touch sounds" setting)
        initSoundPool()
        // Register SAF file pickers using activityResultRegistry directly
        // (works regardless of lifecycle state, unlike registerForActivityResult on ComponentActivity)
        try {
            (activity as? androidx.activity.ComponentActivity)?.let { componentActivity ->
                pickBackupLauncher = componentActivity.activityResultRegistry.register(
                    "sfz_backup_picker",
                    androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val invoke = pendingBackupInvoke ?: return@register
                    pendingBackupInvoke = null

                    if (result.resultCode != Activity.RESULT_OK || result.data?.data == null) {
                        invoke.reject("Aucun fichier sélectionné")
                        return@register
                    }

                    try {
                        val uri = result.data!!.data!!
                        val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw Exception("Could not read backup file")
                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val jsResult = JSObject()
                        jsResult.put("base64", b64)
                        invoke.resolve(jsResult)
                    } catch (e: Exception) {
                        invoke.reject(e.message ?: "Load backup failed")
                    }
                }
                Log.i(TAG, "Backup file picker registered via activityResultRegistry")

                pickFileLauncher = componentActivity.activityResultRegistry.register(
                    "sfz_web_file_picker",
                    androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val callback = pendingFilePathCallback ?: return@register
                    pendingFilePathCallback = null
                    dbg("[file] picker resultCode=${result.resultCode}")

                    if (result.resultCode != Activity.RESULT_OK) {
                        dbg("[file] picker cancelled")
                        callback.onReceiveValue(null)
                        return@register
                    }

                    val intent = result.data
                    val uris = mutableListOf<android.net.Uri>()

                    try {
                        val clipData = intent?.clipData
                        if (clipData != null) {
                            for (i in 0 until clipData.itemCount) {
                                clipData.getItemAt(i)?.uri?.let { uri ->
                                    try {
                                        activity.contentResolver.takePersistableUriPermission(
                                            uri,
                                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    } catch (_: Exception) {}
                                    uris.add(uri)
                                }
                            }
                        }

                        intent?.data?.let { uri ->
                            try {
                                activity.contentResolver.takePersistableUriPermission(
                                    uri,
                                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (_: Exception) {}
                            if (!uris.contains(uri)) uris.add(uri)
                        }

                        callback.onReceiveValue(
                            if (uris.isEmpty()) null else uris.toTypedArray()
                        )
                        dbg("[file] picker returned ${uris.size} uri(s): ${uris.joinToString(", ")}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Web file picker result handling failed: ${e.message}")
                        dbg("[file] picker handling failed: ${e.message}")
                        callback.onReceiveValue(null)
                    }
                }
                Log.i(TAG, "Web file picker registered via activityResultRegistry")
            } ?: Log.w(TAG, "Activity is not a ComponentActivity — file picker unavailable")
        } catch (e: Exception) {
            Log.w(TAG, "Could not register file pickers: ${e.message}")
        }
    }

    /**
     * Dispatch a CustomEvent on the main Tauri WebView (the Vue app).
     * This is the reliable Kotlin→Vue communication channel — same mechanism
     * as evaluateJavascript() used for grayscale/mute on the social webview.
     */
    private fun dispatchToVue(eventName: String, detailJson: String = "{}") {
        val wv = getMainWebView() ?: return
        val js = "window.dispatchEvent(new CustomEvent('$eventName', { detail: $detailJson }))"
        wv.evaluateJavascript(js, null)
    }

    private fun showFacebookStoryUnavailableNotice() {
        if (facebookStoryNoticeShown) return
        facebookStoryNoticeShown = true
        val message = if (Strings.locale == "fr") {
            "Les stories Facebook ne sont pas disponibles dans la version mobile web. Utilisez les posts ou messages avec photos à la place."
        } else {
            "Facebook Stories are not available in the mobile web version. Use posts or messages with photos instead."
        }
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
        dbg("[fb-ui] story unavailable notice shown")
    }

    private fun incrementUsage(networkId: String) {
        val count = prefs.getInt(networkId, 0)
        prefs.edit().putInt(networkId, count + 1).apply()
    }

    /** Returns NETWORKS sorted by usage count descending (most used → first),
     *  filtered by the visible set from the active profile. */
    private fun sortedNetworks(): List<NetworkInfo> {
        val visible = visibleNetworkIds
        val base = if (visible != null) NETWORKS.filter { it.id in visible } else NETWORKS
        return base.sortedByDescending { prefs.getInt(it.id, 0) }
    }

    // ── Display setup (edge-to-edge) ─────────────────────────────────────────

    @Command
    fun setupDisplay(invoke: Invoke) {
        activity.runOnUiThread { setupEdgeToEdge() }
        invoke.resolve(JSObject())
    }

    /**
     * Edge-to-edge mode: content extends behind the status bar.
     * The status bar becomes a transparent overlay showing only time & battery icons.
     * Matches what Instagram, TikTok, and other social apps do.
     */
    private fun setupEdgeToEdge() {
        val window = activity.window

        // Allow app content to draw behind the status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        // Transparent status + nav bars — content draws behind both
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        applyStatusBarIconColor()
    }

    /** Light mode → dark status bar icons; dark mode → white status bar icons. */
    private fun applyStatusBarIconColor() {
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val flag = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            if (isDarkMode) {
                // White icons for dark backgrounds
                window.insetsController?.setSystemBarsAppearance(0, flag)
            } else {
                // Dark icons for light backgrounds
                window.insetsController?.setSystemBarsAppearance(flag, flag)
            }
        } else {
            @Suppress("DEPRECATION")
            if (isDarkMode) {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    /**
     * WebView maps prefers-color-scheme to the host app theme on recent Android/WebView
     * versions. Facebook appears to trust that native signal more than our JS override, so
     * keep the host activity's night mode aligned with the in-app theme when possible.
     */
    private fun applyNativeNightMode() {
        val appCompatActivity = activity as? AppCompatActivity ?: return
        val desiredMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        if (appCompatActivity.delegate.localNightMode != desiredMode) {
            appCompatActivity.delegate.localNightMode = desiredMode
        }
    }

    private fun cookieManagerForWebView(view: WebView?): CookieManager {
        if (view != null && isPoolingEnabled()) {
            return runCatching { WebViewCompat.getProfile(view).cookieManager }
                .getOrElse { CookieManager.getInstance() }
        }
        return CookieManager.getInstance()
    }

    private fun seedLinkedInThemeCookies(view: WebView?) {
        val cookieManager = cookieManagerForWebView(view)
        val themeValue = if (isDarkMode) "dark" else "light"
        val booleanThemeValue = if (isDarkMode) "true" else "false"
        val cookieValues = linkedMapOf(
            "li_theme" to themeValue,
            "mobileWebTheme" to themeValue,
            "theme" to themeValue,
            "themeMode" to themeValue,
            "displayTheme" to themeValue,
            "display_mode" to themeValue,
            "appearance" to themeValue,
            "colorScheme" to themeValue,
            "color_scheme" to themeValue,
            "isDarkMode" to booleanThemeValue,
        )
        val urls = listOf("https://www.linkedin.com", "https://linkedin.com")

        try {
            for (url in urls) {
                for ((name, value) in cookieValues) {
                    cookieManager.setCookie(url, "$name=$value; Path=/; Domain=.linkedin.com; SameSite=Lax")
                    cookieManager.setCookie(url, "$name=$value; Path=/; SameSite=Lax")
                }
            }
            cookieManager.flush()
        } catch (_: Exception) {
            // Some vendor WebViews reject individual cookie shapes; JS bridge will still run.
        }
    }

    /**
     * Best-effort dark mode for social WebViews.
     *
     * Read this as a 3-tier stack, from best to worst:
     *
     * 1. Site-native dark theme
     *    The site itself switches to its own dark design system. This is the target state,
     *    because the result uses the network's real tokens, assets, and contrast rules.
     *    We try to encourage this with:
     *    - host activity night mode (`applyNativeNightMode()`)
     *    - WebView strategy "prefer web theme over UA darkening"
     *    - JS hints for `prefers-color-scheme` and `color-scheme`
     *
     * 2. WebView / Android darkening
     *    If the site ignores our theme signals, Android System WebView may still recolor
     *    the page via FORCE_DARK / algorithmic darkening. This is generic and often less
     *    accurate than the site's real dark mode.
     *
     * 3. Network-specific custom fallback
     *    Only used when (1) and (2) are not sufficient for a specific network. Today this
     *    exists only for Facebook via `__sfz-facebook-dark-fallback`.
     *
     * Current implication for debugging:
     * - Facebook may end up in tier 3.
     * - LinkedIn now gets an explicit storage/cookie preference bridge, and we avoid
     *   WebView algorithmic darkening there so the site can render its own native dark theme.
     */
    private fun applyDarkModeToWebView(view: WebView?) {
        if (view == null) return

        val settings = view.settings
        val isFacebookView = currentNetworkId == "facebook" ||
            (view.url?.contains("facebook.com", ignoreCase = true) == true)
        val isLinkedInView = currentNetworkId == "linkedin" ||
            (view.url?.contains("linkedin.com", ignoreCase = true) == true)

        if (isLinkedInView) {
            seedLinkedInThemeCookies(view)
        }

        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(
                    settings,
                    if (isDarkMode && !isLinkedInView) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                )
            }

            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                val preferredStrategy = if (isFacebookView) {
                    try {
                        WebSettingsCompat::class.java
                            .getField("DARK_STRATEGY_USER_AGENT_DARKENING_ONLY")
                            .getInt(null)
                    } catch (_: Exception) {
                        WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                    }
                } else {
                    WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                }
                WebSettingsCompat.setForceDarkStrategy(
                    settings,
                    preferredStrategy
                )
            }

            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDarkMode && !isLinkedInView)
            }
        } catch (_: Exception) {
            // Older / vendor WebViews may expose a feature flag but still fail at runtime.
        }

        val darkModeScript = """
            (function() {
              try {
                var dark = ${if (isDarkMode) "true" else "false"};
                function isFacebook() {
                  return /(^|\.)facebook\.com$/i.test(location.hostname) && !/\/messages|\/reels?(\/|$)/.test(location.pathname);
                }
                function isLinkedIn() {
                  return /(^|\.)linkedin\.com$/i.test(location.hostname);
                }
${LINKEDIN_THEME_BRIDGE_HELPERS}
                try {
                  localStorage.setItem('__sfzPreferredDark', dark ? '1' : '0');
                } catch (e) {}
                if (isLinkedIn()) {
                  forceLinkedInTheme(dark);
                }
                window.__sfzPreferredDark = dark;
                var originalMatchMedia = window.__sfzOriginalMatchMedia || window.matchMedia;
                if (!window.__sfzOriginalMatchMedia && originalMatchMedia) {
                  window.__sfzOriginalMatchMedia = originalMatchMedia;
                }
                if (originalMatchMedia) {
                  window.matchMedia = function(query) {
                    if (query && query.indexOf('prefers-color-scheme') !== -1) {
                      var base = originalMatchMedia.call(window, query);
                      var isDarkQuery = query.indexOf('dark') !== -1;
                      var isLightQuery = query.indexOf('light') !== -1;
                      return {
                        matches: isDarkQuery ? dark : (isLightQuery ? !dark : base.matches),
                        media: query,
                        onchange: null,
                        addListener: function() {},
                        removeListener: function() {},
                        addEventListener: function() {},
                        removeEventListener: function() {},
                        dispatchEvent: function() { return false; }
                      };
                    }
                    return originalMatchMedia.call(window, query);
                  };
                }

                document.documentElement.style.colorScheme = dark ? 'dark' : 'light';

                var style = document.getElementById('__sfz-dark-mode-hint');
                if (!style) {
                  style = document.createElement('style');
                  style.id = '__sfz-dark-mode-hint';
                  (document.head || document.documentElement).appendChild(style);
                }
                function baseCss(enabled) {
                  if (!enabled) return ':root{color-scheme:light !important;}';
                  if (isLinkedIn()) return ':root{color-scheme:dark !important;}';
                  return ':root{color-scheme:dark !important;} html,body{background:#0f172a !important;}';
                }
                function fallbackCss(enabled) {
                  if (!enabled) return '';
                  if (!isFacebook()) return '';
                  return [
                    'html.__sfz-facebook-dark-fallback{background:#0f172a !important;filter:invert(1) hue-rotate(180deg) !important;}',
                    'html.__sfz-facebook-dark-fallback img,',
                    'html.__sfz-facebook-dark-fallback video,',
                    'html.__sfz-facebook-dark-fallback canvas,',
                    'html.__sfz-facebook-dark-fallback [role=\"img\"],',
                    'html.__sfz-facebook-dark-fallback image{filter:invert(1) hue-rotate(180deg) !important;}'
                  ].join('');
                }

                function applyFacebookFallback(enabled) {
                  if (!isFacebook()) {
                    document.documentElement.classList.remove('__sfz-facebook-dark-fallback');
                    return;
                  }
                  document.documentElement.classList.toggle('__sfz-facebook-dark-fallback', !!enabled);
                }

                style.textContent = baseCss(dark) + fallbackCss(dark);

                applyFacebookFallback(dark);

                var attempts = 0;
                var timer = setInterval(function() {
                  try {
                    document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
                    if (isLinkedIn()) {
                      forceLinkedInTheme(dark);
                    }
                    if (style) {
                      style.textContent = baseCss(dark) + fallbackCss(dark);
                    }
                    applyFacebookFallback(dark);
                  } catch (e) {}
                  attempts += 1;
                  if (attempts >= ${if (isFacebookView) "12" else "4"}) clearInterval(timer);
                }, 250);
              } catch (e) {}
            })();
        """.trimIndent()

        view.evaluateJavascript(darkModeScript, null)
    }

    private fun scheduleDarkModeReapply(view: WebView) {
        val generation = darkModeReapplyGeneration
        val delays = if (currentNetworkId == "facebook") {
            longArrayOf(900L)
        } else if (currentNetworkId == "linkedin") {
            longArrayOf(350L, 1200L)
        } else {
            longArrayOf(350L)
        }

        for (delay in delays) {
            view.postDelayed({
                if (socialWebView == view && generation == darkModeReapplyGeneration) {
                    applyDarkModeToWebView(view)
                    logFacebookDarkState(view, "reapply-${delay}ms")
                    logLinkedInDarkState(view, "reapply-${delay}ms")
                }
            }, delay)
        }
    }

    private fun logFacebookDarkState(view: WebView?, phase: String) {
        if (view == null) return
        val url = view.url ?: ""
        if (!url.contains("facebook.com", ignoreCase = true) || url.contains("/messages")) return

        dbg("[fb-dark] phase=$phase nativeWanted=${if (isDarkMode) "dark" else "light"} url=$url")

        val js = """
            (function() {
              try {
                var html = document.documentElement;
                var body = document.body || html;
                var htmlStyle = getComputedStyle(html);
                var bodyStyle = getComputedStyle(body);
                var mmDark = false;
                var mmLight = false;
                try {
                  mmDark = !!window.matchMedia('(prefers-color-scheme: dark)').matches;
                  mmLight = !!window.matchMedia('(prefers-color-scheme: light)').matches;
                } catch (e) {}
                var payload = {
                  href: location.href,
                  preferredDark: window.__sfzPreferredDark,
                  storedDark: (function() {
                    try { return localStorage.getItem('__sfzPreferredDark'); } catch (e) { return 'err'; }
                  })(),
                  mmDark: mmDark,
                  mmLight: mmLight,
                  htmlColorScheme: html.style.colorScheme || '',
                  cssColorScheme: htmlStyle.colorScheme || '',
                  htmlBg: htmlStyle.backgroundColor || '',
                  bodyBg: bodyStyle.backgroundColor || '',
                  bodyColor: bodyStyle.color || '',
                  fallbackClass: html.classList.contains('__sfz-facebook-dark-fallback'),
                  darkHintPresent: !!document.getElementById('__sfz-dark-mode-hint'),
                  rootClasses: html.className || ''
                };
                return JSON.stringify(payload);
              } catch (e) {
                return JSON.stringify({ error: String(e) });
              }
            })();
        """.trimIndent()

        view.evaluateJavascript(js) { result ->
            val raw = result?.trim()
            if (raw.isNullOrEmpty() || raw == "null") {
                dbg("[fb-dark] phase=$phase snapshot=null")
                return@evaluateJavascript
            }
            val decoded = try {
                org.json.JSONTokener(raw).nextValue()?.toString() ?: raw
            } catch (_: Exception) {
                raw.trim('"')
            }
            dbg("[fb-dark] phase=$phase snapshot=$decoded")
        }
    }

    private fun logLinkedInDarkState(view: WebView?, phase: String) {
        if (view == null) return
        val url = view.url ?: ""
        val isLinkedInView = url.contains("linkedin.com", ignoreCase = true)
        if (!isLinkedInView) return

        dbg("[li-dark] phase=$phase nativeWanted=${if (isDarkMode) "dark" else "light"} url=$url")

        val js = """
            (function() {
              try {
                var html = document.documentElement;
                var body = document.body || html;
                var htmlStyle = getComputedStyle(html);
                var bodyStyle = getComputedStyle(body);
                var mmDark = false;
                var mmLight = false;
                try {
                  mmDark = !!window.matchMedia('(prefers-color-scheme: dark)').matches;
                  mmLight = !!window.matchMedia('(prefers-color-scheme: light)').matches;
                } catch (e) {}

                var storageMatches = [];
                try {
                  for (var i = 0; i < localStorage.length; i++) {
                    var key = localStorage.key(i);
                    if (!key || !/(theme|dark|appearance|color)/i.test(key)) continue;
                    var value = '';
                    try {
                      value = String(localStorage.getItem(key) || '');
                    } catch (e) {
                      value = '[read-error]';
                    }
                    storageMatches.push({
                      key: key,
                      value: value.slice(0, 180)
                    });
                  }
                } catch (e) {
                  storageMatches = [{ error: String(e) }];
                }

                var cookieMatches = [];
                try {
                  var cookies = document.cookie ? document.cookie.split(';') : [];
                  for (var j = 0; j < cookies.length; j++) {
                    var cookie = cookies[j].trim();
                    if (/(theme|dark|appearance|color)/i.test(cookie)) {
                      cookieMatches.push(cookie.slice(0, 180));
                    }
                  }
                } catch (e) {
                  cookieMatches = ['[read-error] ' + String(e)];
                }

                var payload = {
                  href: location.href,
                  preferredDark: window.__sfzPreferredDark,
                  storedDark: (function() {
                    try { return localStorage.getItem('__sfzPreferredDark'); } catch (e) { return 'err'; }
                  })(),
                  mmDark: mmDark,
                  mmLight: mmLight,
                  htmlColorScheme: html.style.colorScheme || '',
                  cssColorScheme: htmlStyle.colorScheme || '',
                  htmlBg: htmlStyle.backgroundColor || '',
                  bodyBg: bodyStyle.backgroundColor || '',
                  bodyColor: bodyStyle.color || '',
                  htmlClasses: html.className || '',
                  bodyClasses: body.className || '',
                  hasHtmlDarkClass: html.classList.contains('dark'),
                  hasBodyDarkClass: body.classList.contains('dark'),
                  darkNodeCount: document.querySelectorAll('.dark, [data-theme="dark"], [data-color-scheme="dark"]').length,
                  darkHintPresent: !!document.getElementById('__sfz-dark-mode-hint'),
                  storageMatches: storageMatches,
                  cookieMatches: cookieMatches
                };
                return JSON.stringify(payload);
              } catch (e) {
                return JSON.stringify({ error: String(e) });
              }
            })();
        """.trimIndent()

        view.evaluateJavascript(js) { result ->
            val raw = result?.trim()
            if (raw.isNullOrEmpty() || raw == "null") {
                dbg("[li-dark] phase=$phase snapshot=null")
                return@evaluateJavascript
            }
            val decoded = try {
                org.json.JSONTokener(raw).nextValue()?.toString() ?: raw
            } catch (_: Exception) {
                raw.trim('"')
            }
            dbg("[li-dark] phase=$phase snapshot=$decoded")
        }
    }

    // ── Open / navigate ──────────────────────────────────────────────────────

    @Command
    fun openWebView(invoke: Invoke) {
        val args = invoke.parseArgs(OpenWebViewArgs::class.java)
        val session = parseSessionIdentity(args.accountId, args.networkId)
        if (session == null) {
            invoke.reject("Invalid Android session key")
            return
        }
        setDeclaredStorageOrigins(session, args.storageOrigins)
        val declaredStorageOrigins = getDeclaredStorageOrigins(session)

        dbg("▶ OPEN webview: network=${session.networkId} url=${args.url}")

        activity.runOnUiThread {
            switchToSession(
                targetSession = session,
                targetUrl = args.url,
                loadIfMissing = true,
                declaredStorageOrigins = declaredStorageOrigins,
            ) { shown, cookiesRestored ->
                if (!shown) {
                    invoke.reject("Android session host unavailable")
                    return@switchToSession
                }
                val host = activeHost()
                val result = JSObject()
                result.put("cookiesRestored", cookiesRestored)
                result.put("localStorageRestoreActive", host?.localStorageRestoreActive ?: false)
                result.put("localStorageCaptureActive", host?.localStorageCaptureActive ?: false)
                invoke.resolve(result)
            }
        }
    }

    // ── Navigate (reuse existing webview, no destroy/recreate) ───────────────

    @Command
    fun navigateWebView(invoke: Invoke) {
        val args = invoke.parseArgs(NavigateArgs::class.java)
        activity.runOnUiThread {
            val host = activeHost()
            val webView = host?.webView ?: socialWebView
            if (webView == null) {
                invoke.reject("Android WebView unavailable")
                return@runOnUiThread
            }
            if (args.networkId == "facebook") {
                facebookDesktopOverride = isFacebookDesktopFlow(args.url)
                facebookStoryNoticeShown = false
            }
            initialBackIndex = -1  // Reset baseline for new network URL
            isLoggedIn = false; pagesSinceOpen = 0
            applyUaForNetwork(args.networkId, args.url)
            webView.loadUrl(args.url)
            currentNetworkId = args.networkId
            host?.let {
                syncHostFromGlobals(it)
                markHostUsed(it)
            }
            updateBottomBarActiveNetwork(args.networkId)
            invoke.resolve(JSObject())
        }
    }

    // ── Close (destroy) — blocks until UI thread completes ───────────────────

    @Command
    fun closeWebView(invoke: Invoke) {
        val args = invoke.parseArgs(AccountArgs::class.java)
        val latch = java.util.concurrent.CountDownLatch(1)
        activity.runOnUiThread {
            val session = parseSessionIdentity(args.accountId)
            if (session != null) {
                destroyHost(session.sessionKey, "close-command")
            } else {
                destroySocialView()
            }
            latch.countDown()
        }
        latch.await()
        invoke.resolve(JSObject())
    }

    // ── Show ─────────────────────────────────────────────────────────────────

    @Command
    fun showWebView(invoke: Invoke) {
        val args = invoke.parseArgs(AccountArgs::class.java)
        activity.runOnUiThread {
            val session = parseSessionIdentity(args.accountId)
            val shown = if (session == null) {
                false
            } else {
                val host = sessionHosts[session.sessionKey]
                if (host == null) {
                    false
                } else {
                    val current = activeHost()
                    if (current != null && current.session.sessionKey != host.session.sessionKey) {
                        hideHostInternal(current)
                    }
                    showHostInternal(host)
                    true
                }
            }
            val result = JSObject()
            result.put("shown", shown)
            invoke.resolve(result)
        }
    }

    // ── Hide ─────────────────────────────────────────────────────────────────

    @Command
    fun hideWebView(invoke: Invoke) {
        val args = invoke.parseArgs(AccountArgs::class.java)
        activity.runOnUiThread {
            val requested = parseSessionIdentity(args.accountId)
            val active = activeHost()
            val hostToHide = when {
                requested != null && active?.session?.sessionKey == requested.sessionKey -> active
                requested != null -> sessionHosts[requested.sessionKey]
                else -> active
            }
            if (hostToHide != null) {
                hideHostInternal(hostToHide)
            } else {
                hideSocialView()
            }
            invoke.resolve(JSObject())
        }
    }

    // ── Set grayscale (called from Vue settings toggle) ───────────────────────

    @Command
    fun setGrayscale(invoke: Invoke) {
        val args = invoke.parseArgs(GrayscaleArgs::class.java)
        activity.runOnUiThread {
            isGrayscale = args.enabled
            if (sessionHosts.isEmpty()) {
                applyGrayscaleToWebView(socialWebView)
                applyGrayscaleToBottomBar(bottomBarView)
            } else {
                sessionHosts.values.forEach { host ->
                    applyGrayscaleToWebView(host.webView)
                    applyGrayscaleToBottomBar(host.bottomBar)
                }
            }
        }
        invoke.resolve(JSObject())
    }

    // ── Set dark mode (called from Vue settings toggle) ────────────────────

    @Command
    fun setDarkMode(invoke: Invoke) {
        val args = invoke.parseArgs(DarkModeArgs::class.java)
        activity.runOnUiThread {
            isDarkMode = args.enabled
            dbg("[dark] toggle requested=${if (isDarkMode) "dark" else "light"} net=${currentNetworkId ?: "?"}")
            applyNativeNightMode()
            if (sessionHosts.isEmpty()) {
                applyDarkModeToWebView(socialWebView)
            } else {
                sessionHosts.values.forEach { host ->
                    applyDarkModeToWebView(host.webView)
                }
            }
            logFacebookDarkState(socialWebView, "toggle")
            logLinkedInDarkState(socialWebView, "toggle")
            applyDarkModeToBottomBar(bottomBarView)
            applyStatusBarIconColor()
        }
        invoke.resolve(JSObject())
    }

    // ── Set locale (called from Vue when language changes) ─────────────────

    @Command
    fun setLocale(invoke: Invoke) {
        val args = invoke.parseArgs(SetLocaleArgs::class.java)
        Strings.locale = args.locale
        invoke.resolve(JSObject())
    }

    // ── Set haptic feedback preference ────────────────────────────────────────

    @Command
    fun setHaptic(invoke: Invoke) {
        val args = invoke.parseArgs(GrayscaleArgs::class.java) // reuse — same shape { enabled: bool }
        hapticEnabled = args.enabled
        invoke.resolve(JSObject())
    }

    @Command
    fun setTapSound(invoke: Invoke) {
        val args = invoke.parseArgs(GrayscaleArgs::class.java) // reuse — same shape { enabled: bool }
        tapSoundEnabled = args.enabled
        invoke.resolve(JSObject())
    }

    @Command
    fun setTapSoundVariant(invoke: Invoke) {
        val args = invoke.parseArgs(TapSoundVariantArgs::class.java)
        tapSoundVariant = normalizeTapSoundVariant(args.variant)
        invoke.resolve(JSObject())
    }

    @Command
    fun previewTapSound(invoke: Invoke) {
        activity.runOnUiThread {
            playTapSound(ignoreEnabled = true)
        }
        invoke.resolve(JSObject())
    }

    // Trigger haptic + tap sound from Vue-side buttons.
    // Gating respects the same hapticEnabled / tapSoundEnabled flags as the native bottom bar.
    @Command
    fun triggerHaptic(invoke: Invoke) {
        activity.runOnUiThread {
            val view = bottomBarView ?: socialWebView ?: activity.window.decorView
            haptic(view)
        }
        invoke.resolve(JSObject())
    }

    @Command
    fun setTextZoom(invoke: Invoke) {
        val args = invoke.parseArgs(TextZoomArgs::class.java)
        activity.runOnUiThread {
            textZoomLevel = normalizeTextZoomLevel(args.level)
            if (sessionHosts.isEmpty()) {
                socialWebView?.settings?.textZoom = textZoomLevel
                applyTextZoomToWebView(socialWebView)
            } else {
                sessionHosts.values.forEach { host ->
                    host.webView.settings.textZoom = textZoomLevel
                    applyTextZoomToWebView(host.webView)
                }
            }
        }
        invoke.resolve(JSObject())
    }

    // ── Backup: save/load to Downloads via MediaStore ────────────────────────

    @Command
    fun saveBackupToDownloads(invoke: Invoke) {
        val args = invoke.parseArgs(SaveBackupArgs::class.java)
        try {
            val bytes = android.util.Base64.decode(args.base64Data, android.util.Base64.DEFAULT)
            val resolver = activity.contentResolver
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, args.fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/SocialGlowz")
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("MediaStore insert failed")
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw Exception("Could not open output stream")
            values.clear()
            values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            val result = JSObject()
            result.put("path", "Download/SocialGlowz/${args.fileName}")
            invoke.resolve(result)
        } catch (e: Exception) {
            invoke.reject("Backup save failed: ${e.message}")
        }
    }

    @Command
    fun loadBackupFromDownloads(invoke: Invoke) {
        try {
            val launcher = pickBackupLauncher
                ?: throw Exception("File picker not available")
            // Use SAF file picker — works even after uninstall/reinstall
            // (MediaStore scoped storage hides files from reinstalled apps)
            pendingBackupInvoke = invoke
            val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            launcher.launch(intent)
        } catch (e: Exception) {
            pendingBackupInvoke = null
            invoke.reject(e.message ?: "Load backup failed")
        }
    }

    @Command
    fun exportCookiesForBackup(invoke: Invoke) {
        try {
            currentAccountId?.let { saveCookiesForSession(it) }

            val cookies = org.json.JSONObject()
            for ((key, value) in cookiePrefs.all) {
                if (value is String) {
                    cookies.put(key, value)
                }
            }

            val localStorage = org.json.JSONObject()
            for ((key, value) in localStoragePrefs.all) {
                if (value is String) {
                    localStorage.put(key, value)
                }
            }

            val result = JSObject()
            result.put("cookiesJson", cookies.toString())
            result.put("localStorageJson", localStorage.toString())
            invoke.resolve(result)
        } catch (e: Exception) {
            invoke.reject("Cookie export failed: ${e.message}")
        }
    }

    @Command
    fun importCookiesFromBackup(invoke: Invoke) {
        val args = invoke.parseArgs(ImportCookiesBackupArgs::class.java)
        try {
            val sessionKeysToReset = mutableSetOf<String>()
            sessionKeysToReset.addAll(collectValidSessionKeysFromPrefKeys(cookiePrefs.all.keys))
            sessionKeysToReset.addAll(collectValidSessionKeysFromPrefKeys(localStoragePrefs.all.keys))

            val importedCookiePrefs = mutableMapOf<String, String>()
            val importedLocalStoragePrefs = mutableMapOf<String, String>()
            val pendingLocalStorageRestoreSessions = mutableSetOf<String>()

            val json = args.cookiesJson.trim()
            if (json.isNotEmpty()) {
                val cookies = org.json.JSONObject(json)
                val keys = cookies.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val sessionKey = sessionKeyFromPrefKey(key)
                    if (parseSessionIdentity(sessionKey) != null) {
                        sessionKeysToReset.add(sessionKey)
                    }
                    importedCookiePrefs[key] = cookies.optString(key, "")
                }
            }

            val localStorageJson = args.localStorageJson.trim()
            if (localStorageJson.isNotEmpty()) {
                val snapshots = org.json.JSONObject(localStorageJson)
                val keys = snapshots.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val sessionKey = sessionKeyFromPrefKey(key)
                    if (parseSessionIdentity(sessionKey) != null) {
                        sessionKeysToReset.add(sessionKey)
                    }
                    val snapshotRaw = snapshots.optString(key, "")
                    if (snapshotRaw.isBlank()) continue
                    try {
                        JSONObject(snapshotRaw)
                        importedLocalStoragePrefs[key] = snapshotRaw
                        if (parseSessionIdentity(sessionKey) != null) {
                            pendingLocalStorageRestoreSessions.add(sessionKey)
                        }
                    } catch (_: Exception) {
                        Log.w(TAG, "Skipped corrupt localStorage snapshot during import")
                    }
                }
            }

            val latch = java.util.concurrent.CountDownLatch(1)
            activity.runOnUiThread {
                try {
                    val keys = (sessionKeysToReset + sessionHosts.keys).toSet()
                    for (sessionKey in keys) {
                        destroyHost(sessionKey, "backup-import")
                    }
                    if (isPoolingEnabled()) {
                        for (sessionKey in keys) {
                            deleteWebkitProfileByName(webkitProfileNameForSession(sessionKey))
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
            latch.await()

            val cookieEditor = cookiePrefs.edit()
            cookieEditor.clear()
            for ((key, value) in importedCookiePrefs) {
                cookieEditor.putString(key, value)
            }
            cookieEditor.apply()

            val localStorageEditor = localStoragePrefs.edit()
            localStorageEditor.clear()
            for ((key, value) in importedLocalStoragePrefs) {
                localStorageEditor.putString(key, value)
            }
            localStorageEditor.apply()
            replacePendingLocalStorageRestores(pendingLocalStorageRestoreSessions)

            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()
            isLoggedIn = false
            pagesSinceOpen = 0

            invoke.resolve(JSObject())
        } catch (e: Exception) {
            invoke.reject("Cookie import failed: ${e.message}")
        }
    }

    /** Update which networks appear in the bottom bar (synced from per-profile visibility). */
    @Command
    fun setBarNetworks(invoke: Invoke) {
        val args = invoke.parseArgs(BarNetworksArgs::class.java)
        activity.runOnUiThread {
            visibleNetworkIds = if (args.networkIds.isEmpty()) null else args.networkIds.toSet()
            setDeclaredStorageOriginsByNetworkJson(args.storageOriginsByNetworkJson)
            rebuildBottomBar()
        }
        invoke.resolve(JSObject())
    }

    @Command
    fun setProfiles(invoke: Invoke) {
        val args = invoke.parseArgs(SetProfilesArgs::class.java)
        try {
            val arr = org.json.JSONArray(args.profilesJson)
            val list = mutableListOf<ProfileMenuItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(ProfileMenuItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    emoji = obj.optString("emoji", ""),
                    avatar = obj.optString("avatar", "").takeIf { it.isNotBlank() }
                ))
            }
            activity.runOnUiThread {
                menuProfiles = list
                activeProfileId = args.activeProfileId
            }
        } catch (e: Exception) {
            Log.e(TAG, "setProfiles parse error: ${e.message}")
        }
        invoke.resolve(JSObject())
    }

    // ── Delete session cookies (called from Vue profile management) ─────────

    @Command
    fun deleteNetworkSession(invoke: Invoke) {
        val args = invoke.parseArgs(DeleteSessionArgs::class.java)
        val session = buildSessionIdentity(args.profileId, args.networkId)
        if (session == null) {
            invoke.reject("Invalid Android session key")
            return
        }
        val sessionKey = session.sessionKey
        val editor = cookiePrefs.edit()
        for (url in COOKIE_URLS) {
            editor.remove("$sessionKey|$url")
        }
        editor.apply()
        removeLocalStorageSnapshotsForSession(sessionKey)
        removeDeclaredStorageOriginsForSession(sessionKey)
        activity.runOnUiThread {
            destroyHost(sessionKey, "delete-network-session")
            if (isPoolingEnabled()) {
                deleteWebkitProfileByName(webkitProfileNameForSession(sessionKey))
            }
        }
        // Re-arm cookie consent if deleting the currently active session
        if (currentAccountId == sessionKey) {
            isLoggedIn = false
            pagesSinceOpen = 0
        }
        Log.i(TAG, "Session data deleted for one profile/network pair")
        invoke.resolve(JSObject())
    }

    @Command
    fun deleteProfileSession(invoke: Invoke) {
        val args = invoke.parseArgs(DeleteSessionArgs::class.java)
        val profileId = args.profileId.trim()
        if (profileId.isEmpty()) {
            invoke.reject("Profile ID is required")
            return
        }
        val editor = cookiePrefs.edit()
        // Remove cookies for all networks under this profile
        val allPrefs = cookiePrefs.all
        for (key in allPrefs.keys) {
            if (prefKeyBelongsToProfile(key, profileId)) {
                editor.remove(key)
            }
        }
        editor.apply()
        removeLocalStorageSnapshotsForProfile(profileId)
        removeDeclaredStorageOriginsForProfile(profileId)
        activity.runOnUiThread {
            val keysToDestroy = sessionHosts.values
                .filter { it.session.profileId == profileId }
                .map { it.session.sessionKey }
                .toList()
            for (sessionKey in keysToDestroy) {
                destroyHost(sessionKey, "delete-profile-session")
                if (isPoolingEnabled()) {
                    deleteWebkitProfileByName(webkitProfileNameForSession(sessionKey))
                }
            }
        }
        // Re-arm cookie consent if deleting the currently active profile
        if (currentAccountId?.let { parseSessionIdentity(it)?.profileId == profileId } == true) {
            isLoggedIn = false
            pagesSinceOpen = 0
        }
        Log.i(TAG, "Session data deleted for profile")
        invoke.resolve(JSObject())
    }

    /** Tear down and rebuild the bottom bar with the current filtered/sorted networks. */
    private fun rebuildBottomBar() {
        if (sessionHosts.isNotEmpty()) {
            for (host in sessionHosts.values.toList()) {
                rebuildBottomBarForHost(host)
            }
            return
        }
        val root = socialRoot ?: return
        val oldBar = bottomBarView ?: return
        root.removeView(oldBar)

        val density = activity.resources.displayMetrics.density
        val windowInsets = root.rootWindowInsets
        val navBarHeight = windowInsets?.systemWindowInsetBottom ?: 0
        val activeId = currentNetworkId ?: ""

        val newBar = buildBottomBar(density, navBarHeight, activeId, sortedNetworks())
        bottomBarView = newBar

        val barHeight = (52 * density).toInt()
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            barHeight + navBarHeight
        )
        params.gravity = Gravity.BOTTOM
        newBar.layoutParams = params
        root.addView(newBar)

        if (isGrayscale) applyGrayscaleToBottomBar(newBar)
    }

    private fun rebuildBottomBarForHost(host: SessionWebViewHost) {
        val root = host.root
        val oldBar = host.bottomBar
        root.removeView(oldBar)

        val density = activity.resources.displayMetrics.density
        val windowInsets = root.rootWindowInsets
        val navBarHeight = windowInsets?.systemWindowInsetBottom ?: 0
        val activeId = host.session.networkId
        val newBar = buildBottomBar(density, navBarHeight, activeId, sortedNetworks())
        host.bottomBar = newBar

        val barHeight = (52 * density).toInt()
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            barHeight + navBarHeight
        )
        params.gravity = Gravity.BOTTOM
        newBar.layoutParams = params
        root.addView(newBar)

        if (isGrayscale) applyGrayscaleToBottomBar(newBar)
        if (activeHostSessionKey == host.session.sessionKey) {
            bottomBarView = newBar
            socialRoot = root
        }
    }

    // ── Build bottom bar ─────────────────────────────────────────────────────

    private fun buildBottomBar(
        density: Float,
        navBarHeight: Int,
        activeNetworkId: String,
        networks: List<NetworkInfo> = NETWORKS,
    ): LinearLayout {
        val bar = LinearLayout(activity)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.TOP or Gravity.CENTER_VERTICAL
        bar.setBackgroundColor(if (isDarkMode) Color.parseColor("#09090B") else Color.parseColor("#FFFFFF"))
        bar.setPadding(0, 0, 0, navBarHeight)

        // Inner row sits at the top of the bar (nav bar padding is below)
        val innerRow = LinearLayout(activity)
        innerRow.orientation = LinearLayout.HORIZONTAL
        innerRow.gravity = Gravity.CENTER_VERTICAL
        val innerHeight = (52 * density).toInt()
        innerRow.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            innerHeight
        )

        // 🏠 Home button — returns to dashboard
        val homeBtn = buildHomeButton(density)
        innerRow.addView(homeBtn)

        // Thin divider
        innerRow.addView(buildDivider(density))

        // Scrollable network switcher (takes all remaining space)
        // Override dispatchTouchEvent to see ALL touch events, even those going to child buttons.
        var isTouching = false
        val scrollView = object : HorizontalScrollView(activity) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isTouching = true
                        val row = getChildAt(0) as? LinearLayout ?: return super.dispatchTouchEvent(ev)
                        for (i in 0 until row.childCount) {
                            val child = row.getChildAt(i)
                            child.animate().cancel()
                            child.animate().alpha(1f).setDuration(600).start()
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isTouching = false
                        val row = getChildAt(0) as? LinearLayout ?: return super.dispatchTouchEvent(ev)
                        for (i in 0 until row.childCount) {
                            val child = row.getChildAt(i)
                            val netId = child.tag as? String
                            if (netId != null) {
                                val targetAlpha = if (netId == currentNetworkId) 1f else 0.45f
                                child.animate().cancel()
                                child.animate().alpha(targetAlpha).setDuration(800).start()
                            }
                        }
                    }
                }
                return super.dispatchTouchEvent(ev)
            }
        }
        scrollView.isHorizontalScrollBarEnabled = false
        scrollView.isSmoothScrollingEnabled = true
        scrollView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)

        val networkRow = LinearLayout(activity)
        networkRow.orientation = LinearLayout.HORIZONTAL
        networkRow.gravity = Gravity.CENTER_VERTICAL

        for (net in networks) {
            val btn = buildNetworkButton(density, net, net.id == activeNetworkId)
            btn.tag = net.id
            networkRow.addView(btn)
        }

        scrollView.addView(networkRow)

        innerRow.addView(scrollView)

        // (mute + grayscale are now in the home button popup menu)

        bar.addView(innerRow)
        return bar
    }

    private fun buildDivider(density: Float): View {
        val divider = View(activity)
        divider.setBackgroundColor(if (isDarkMode) Color.parseColor("#27272A") else Color.parseColor("#DEE2E6"))
        val params = LinearLayout.LayoutParams((1 * density).toInt(), (24 * density).toInt())
        params.setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
        divider.layoutParams = params
        return divider
    }

    private var popupMenuOverlayView: FrameLayout? = null
    private var popupMenuView: LinearLayout? = null

    private fun buildHomeButton(density: Float): TextView {
        val btn = TextView(activity)
        btn.text = "\ue941"  // pi-home — PrimeIcons home icon
        btn.typeface = primeIconsTypeface
        btn.textSize = 18f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#495057"))
        btn.background = null
        val size = (48 * density).toInt()
        btn.layoutParams = LinearLayout.LayoutParams(size, size)
        btn.isClickable = true
        btn.isFocusable = true
        btn.isLongClickable = true
        btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> playTapSound()
            }
            false
        }

        // Single tap → show/hide popup menu
        btn.setOnClickListener {
            if (hapticEnabled) btn.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            togglePopupMenu(density)
        }

        // Long press → go home (dashboard)
        btn.setOnLongClickListener {
            haptic(btn, HapticFeedbackConstants.LONG_PRESS)
            dismissPopupMenu()
            goHome()
            true
        }

        return btn
    }

    private fun togglePopupMenu(density: Float) {
        if (popupMenuView != null) {
            dismissPopupMenu()
            return
        }
        showPopupMenu(density)
    }

    private fun dismissPopupMenu() {
        popupMenuOverlayView?.let { overlay ->
            (overlay.parent as? ViewGroup)?.removeView(overlay)
        }
        popupMenuOverlayView = null
        popupMenuView = null
    }

    private fun showPopupMenu(density: Float) {
        val root = socialRoot ?: return
        val bar = bottomBarView ?: return

        val overlay = FrameLayout(activity)
        overlay.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        overlay.isClickable = true
        overlay.isFocusable = true
        overlay.setOnClickListener {
            dismissPopupMenu()
        }

        val menu = LinearLayout(activity)
        menu.orientation = LinearLayout.VERTICAL
        val bgColor = if (isDarkMode) Color.parseColor("#1C1C1E") else Color.parseColor("#FFFFFF")
        val menuBg = GradientDrawable()
        menuBg.setColor(bgColor)
        menuBg.cornerRadius = 16 * density
        menu.background = menuBg
        menu.elevation = 8 * density
        val pad = (8 * density).toInt()
        menu.setPadding(pad, pad, pad, pad)

        val menuWidth = (220 * density).toInt()
        val menuParams = FrameLayout.LayoutParams(menuWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        menuParams.gravity = Gravity.BOTTOM or Gravity.START
        menuParams.leftMargin = (8 * density).toInt()
        menuParams.bottomMargin = bar.layoutParams.height + (8 * density).toInt()
        menu.layoutParams = menuParams
        menu.isClickable = true
        menu.isFocusable = true
        menu.setOnClickListener { }

        // ── Menu items ──

        // 1. Profile list — inline switcher
        if (menuProfiles.isNotEmpty()) {
            val sectionColor = if (isDarkMode) Color.parseColor("#9A9AB0") else Color.parseColor("#ADB5BD")
            val sectionLabel = TextView(activity)
            sectionLabel.text = Strings.t("profiles")
            sectionLabel.textSize = 11f
            sectionLabel.setTextColor(sectionColor)
            sectionLabel.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            val slPad = (12 * density).toInt()
            sectionLabel.setPadding(slPad, (4 * density).toInt(), slPad, (2 * density).toInt())
            menu.addView(sectionLabel)

            for (profile in menuProfiles) {
                val isActive = profile.id == activeProfileId
                menu.addView(buildProfilePopupMenuItem(density, profile, dimmed = !isActive) {
                    dismissPopupMenu()
                    dispatchToVue("sfz-switch-profile", """{"profileId": "${profile.id}"}""")
                })
            }

            // Divider
            val divider = View(activity)
            val divColor = if (isDarkMode) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
            divider.setBackgroundColor(divColor)
            val divParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (1 * density).toInt())
            val divMargin = (8 * density).toInt()
            divParams.setMargins(divMargin, (4 * density).toInt(), divMargin, (4 * density).toInt())
            divider.layoutParams = divParams
            menu.addView(divider)
        }

        // 2. Mute toggle
        val muteLabel = if (isMuted) Strings.t("mute_on") else Strings.t("mute_off")
        val muteIcon = if (isMuted) "\ue978" else "\ue977"
        menu.addView(buildPopupMenuItem(density, muteIcon, muteLabel) {
            isMuted = !isMuted
            tapSoundEnabled = !isMuted
            if (sessionHosts.isEmpty()) {
                applyMuteToWebView(socialWebView)
            } else {
                sessionHosts.values.forEach { host -> applyMuteToWebView(host.webView) }
            }
            dispatchToVue("sfz-tap-sound-changed", """{"enabled": $tapSoundEnabled}""")
            dismissPopupMenu()
        })

        // 3. Grayscale toggle
        val grayLabel = if (isGrayscale) Strings.t("grayscale_on") else Strings.t("grayscale_off")
        menu.addView(buildPopupMenuItem(density, "\ue9dd", grayLabel, dimmed = isGrayscale) {
            isGrayscale = !isGrayscale
            if (sessionHosts.isEmpty()) {
                applyGrayscaleToWebView(socialWebView)
                applyGrayscaleToBottomBar(bottomBarView)
            } else {
                sessionHosts.values.forEach { host ->
                    applyGrayscaleToWebView(host.webView)
                    applyGrayscaleToBottomBar(host.bottomBar)
                }
            }
            dispatchToVue("sfz-grayscale-changed", """{"enabled": $isGrayscale}""")
            dismissPopupMenu()
        })

        // 4. Dark mode toggle
        val darkLabel = if (isDarkMode) Strings.t("dark_mode_on") else Strings.t("dark_mode_off")
        val darkIcon = if (isDarkMode) "\ue9c8" else "\ue9c7"  // pi-sun / pi-moon
        menu.addView(buildPopupMenuItem(density, darkIcon, darkLabel) {
            dismissPopupMenu()
            dispatchToVue("sfz-toggle-dark-mode")
        })

        // 5. Text zoom quick control
        menu.addView(buildTextZoomControl(density))

        // 6. Copy debug logs
        menu.addView(buildPopupMenuItem(density, "\ue957", "Copy debug logs") {  // pi-copy
            val logText = synchronized(debugLog) { debugLog.joinToString("\n") }
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("SFZ Debug Logs", logText))
            dbg("Logs copied to clipboard (${debugLog.size} lines)")
            dismissPopupMenu()
        })

        overlay.addView(menu)
        root.addView(overlay)
        popupMenuOverlayView = overlay
        popupMenuView = menu
    }

    private fun buildTextZoomControl(density: Float): LinearLayout {
        val wrap = LinearLayout(activity)
        wrap.orientation = LinearLayout.VERTICAL
        val padH = (12 * density).toInt()
        val padTop = (8 * density).toInt()
        val padBottom = (10 * density).toInt()
        wrap.setPadding(padH, padTop, padH, padBottom)

        val textColor = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#1C1C1E")
        val secondaryColor = if (isDarkMode) Color.parseColor("#9A9AB0") else Color.parseColor("#6C757D")

        val topRow = LinearLayout(activity)
        topRow.orientation = LinearLayout.HORIZONTAL
        topRow.gravity = Gravity.CENTER_VERTICAL

        val label = TextView(activity)
        label.text = Strings.t("text_zoom")
        label.textSize = 14f
        label.setTextColor(textColor)
        label.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        label.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        val value = TextView(activity)
        value.text = "${textZoomLevel}%"
        value.textSize = 12f
        value.setTextColor(secondaryColor)
        value.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        topRow.addView(label)
        topRow.addView(value)
        wrap.addView(topRow)

        val slider = SeekBar(activity)
        slider.max = TEXT_ZOOM_RANGE_STEPS
        slider.progress = ((normalizeTextZoomLevel(textZoomLevel) - TEXT_ZOOM_MIN) / TEXT_ZOOM_STEP)
            .coerceIn(0, TEXT_ZOOM_RANGE_STEPS)
        slider.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val level = normalizeTextZoomLevel(TEXT_ZOOM_MIN + (progress * TEXT_ZOOM_STEP))
                value.text = "$level%"
                if (!fromUser || level == textZoomLevel) return
                textZoomLevel = level
                if (sessionHosts.isEmpty()) {
                    socialWebView?.settings?.textZoom = textZoomLevel
                    applyTextZoomToWebView(socialWebView)
                } else {
                    sessionHosts.values.forEach { host ->
                        host.webView.settings.textZoom = textZoomLevel
                        applyTextZoomToWebView(host.webView)
                    }
                }
                dispatchToVue("sfz-text-zoom-changed", """{"level": $textZoomLevel}""")
                dbg("[zoom] level=$textZoomLevel net=${currentNetworkId ?: "?"}")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        wrap.addView(slider)

        return wrap
    }

    private fun buildPopupMenuItem(
        density: Float,
        iconChar: String,
        label: String,
        dimmed: Boolean = false,
        onClick: () -> Unit
    ): LinearLayout {
        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val rowPadH = (12 * density).toInt()
        val rowPadV = (11 * density).toInt()
        row.setPadding(rowPadH, rowPadV, rowPadH, rowPadV)
        row.isClickable = true
        row.isFocusable = true

        // Rounded hover/press background
        val rippleBg = GradientDrawable()
        rippleBg.cornerRadius = 10 * density
        rippleBg.setColor(Color.TRANSPARENT)
        row.background = rippleBg
        row.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (hapticEnabled) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    playTapSound()
                    rippleBg.setColor(if (isDarkMode) Color.parseColor("#2C2C2E") else Color.parseColor("#F2F2F7"))
                    v.invalidate()
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    rippleBg.setColor(Color.TRANSPARENT)
                    v.invalidate()
                }
            }
            false
        }

        val textColor = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#1C1C1E")
        val dimColor = if (isDarkMode) Color.parseColor("#9A9AB0") else Color.parseColor("#ADB5BD")

        // Icon
        val icon = TextView(activity)
        icon.text = iconChar
        icon.typeface = primeIconsTypeface
        icon.textSize = 16f
        icon.gravity = Gravity.CENTER
        icon.setTextColor(if (dimmed) dimColor else textColor)
        val iconSize = (28 * density).toInt()
        icon.layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
        row.addView(icon)

        // Spacing
        val spacer = View(activity)
        spacer.layoutParams = LinearLayout.LayoutParams((10 * density).toInt(), 1)
        row.addView(spacer)

        // Label
        val text = TextView(activity)
        text.text = label
        text.textSize = 14f
        text.setTextColor(if (dimmed) dimColor else textColor)
        text.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        text.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        row.addView(text)

        row.setOnClickListener {
            onClick()
        }

        return row
    }

    private fun buildProfilePopupMenuItem(
        density: Float,
        profile: ProfileMenuItem,
        dimmed: Boolean = false,
        onClick: () -> Unit
    ): LinearLayout {
        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val rowPadH = (12 * density).toInt()
        val rowPadV = (11 * density).toInt()
        row.setPadding(rowPadH, rowPadV, rowPadH, rowPadV)
        row.isClickable = true
        row.isFocusable = true

        val rippleBg = GradientDrawable()
        rippleBg.cornerRadius = 10 * density
        rippleBg.setColor(Color.TRANSPARENT)
        row.background = rippleBg
        row.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (hapticEnabled) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    playTapSound()
                    rippleBg.setColor(if (isDarkMode) Color.parseColor("#2C2C2E") else Color.parseColor("#F2F2F7"))
                    v.invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    rippleBg.setColor(Color.TRANSPARENT)
                    v.invalidate()
                }
            }
            false
        }

        val textColor = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#1C1C1E")
        val dimColor = if (isDarkMode) Color.parseColor("#9A9AB0") else Color.parseColor("#ADB5BD")
        val avatarSize = (28 * density).toInt()

        val avatarFrame = FrameLayout(activity)
        avatarFrame.layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize)
        avatarFrame.alpha = if (dimmed) 0.72f else 1f
        avatarFrame.clipToOutline = true
        avatarFrame.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }

        val avatarBg = GradientDrawable()
        avatarBg.shape = GradientDrawable.OVAL
        avatarBg.setColor(if (isDarkMode) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA"))
        avatarFrame.background = avatarBg

        val avatarImage = ImageView(activity)
        avatarImage.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        avatarImage.scaleType = ImageView.ScaleType.CENTER_CROP
        avatarImage.visibility = View.GONE
        avatarFrame.addView(avatarImage)

        val avatarText = TextView(activity)
        avatarText.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        avatarText.gravity = Gravity.CENTER
        avatarText.textSize = 13f
        avatarText.setTextColor(if (dimmed) dimColor else textColor)
        avatarText.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        avatarText.text = profile.emoji.ifBlank { "👤" }
        avatarFrame.addView(avatarText)

        bindProfileAvatar(profile, avatarImage, avatarText)
        row.addView(avatarFrame)

        val spacer = View(activity)
        spacer.layoutParams = LinearLayout.LayoutParams((10 * density).toInt(), 1)
        row.addView(spacer)

        val text = TextView(activity)
        text.text = profile.name
        text.textSize = 14f
        text.setTextColor(if (dimmed) dimColor else textColor)
        text.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        text.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        row.addView(text)

        row.setOnClickListener {
            onClick()
        }

        return row
    }

    private fun bindProfileAvatar(profile: ProfileMenuItem, avatarImage: ImageView, avatarText: TextView) {
        val avatar = profile.avatar ?: return
        avatarImage.tag = avatar

        if (avatar.startsWith("data:")) {
            decodeAvatarBitmap(avatar)?.let { bitmap ->
                avatarImage.setImageBitmap(bitmap)
                avatarImage.visibility = View.VISIBLE
                avatarText.visibility = View.GONE
            }
            return
        }

        if (avatar.startsWith("http://") || avatar.startsWith("https://")) {
            Thread {
                val bitmap = decodeAvatarBitmap(avatar)
                if (bitmap != null) {
                    activity.runOnUiThread {
                        if (avatarImage.tag == avatar) {
                            avatarImage.setImageBitmap(bitmap)
                            avatarImage.visibility = View.VISIBLE
                            avatarText.visibility = View.GONE
                        }
                    }
                }
            }.start()
        }
    }

    private fun decodeAvatarBitmap(avatar: String): Bitmap? {
        return try {
            when {
                avatar.startsWith("data:") -> {
                    val base64 = avatar.substringAfter("base64,", "")
                    if (base64.isBlank()) null else {
                        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
                avatar.startsWith("http://") || avatar.startsWith("https://") -> {
                    URL(avatar).openStream().use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun applyTextZoomToWebView(view: WebView?) {
        val wv = view ?: return
        val level = normalizeTextZoomLevel(textZoomLevel)
        wv.settings.textZoom = level
        val js = """
            (function() {
              var level = $level;
              var percent = level + '%';
              var isFacebook = /facebook\.com/.test(location.host);
              var textAdjustPercent = isFacebook ? '100%' : percent;
              document.documentElement.style.setProperty('-webkit-text-size-adjust', textAdjustPercent);
              if (document.body) {
                document.body.style.setProperty('-webkit-text-size-adjust', textAdjustPercent);
              }
              if (isFacebook) {
                var nodes = document.querySelectorAll('body, div, span, a, p, h1, h2, h3, h4, h5, h6');
                for (var i = 0; i < nodes.length; i++) {
                  var el = nodes[i];
                  if (el.dataset.sfzZoomInlineFontSize === undefined) {
                    el.dataset.sfzZoomInlineFontSize = el.style.getPropertyValue('font-size') || '';
                    el.dataset.sfzZoomInlineFontPriority = el.style.getPropertyPriority('font-size') || '';
                  }
                  var inlineFontSize = el.dataset.sfzZoomInlineFontSize || '';
                  var inlinePriority = el.dataset.sfzZoomInlineFontPriority || '';
                  if (inlineFontSize) {
                    el.style.setProperty('font-size', inlineFontSize, inlinePriority);
                  } else {
                    el.style.removeProperty('font-size');
                  }
                }
                for (var j = 0; j < nodes.length; j++) {
                  var baseNode = nodes[j];
                  var baseStyle = window.getComputedStyle(baseNode);
                  var baseSize = parseFloat(baseStyle.fontSize || '0');
                  if (!baseSize || baseSize < 10 || baseSize > 40) continue;
                  baseNode.dataset.sfzZoomBase = String(baseSize);
                }
                if (level === 100) {
                  return;
                }
                for (var k = 0; k < nodes.length; k++) {
                  var node = nodes[k];
                  var base = parseFloat(node.dataset.sfzZoomBase || '0');
                  if (!base) continue;
                  node.style.fontSize = (base * level / 100) + 'px';
                }
              }
            })();
        """.trimIndent()
        wv.evaluateJavascript(js, null)
    }

    private fun normalizeTextZoomLevel(level: Int): Int {
        val clamped = level.coerceIn(TEXT_ZOOM_MIN, TEXT_ZOOM_MAX)
        val offset = clamped - TEXT_ZOOM_MIN
        val snapped = ((offset + (TEXT_ZOOM_STEP / 2)) / TEXT_ZOOM_STEP) * TEXT_ZOOM_STEP
        return (TEXT_ZOOM_MIN + snapped).coerceIn(TEXT_ZOOM_MIN, TEXT_ZOOM_MAX)
    }

    private fun normalizeTapSoundVariant(variant: String?): String {
        return if (variant != null && TAP_SOUND_ASSETS.containsKey(variant)) {
            variant
        } else {
            DEFAULT_TAP_SOUND_VARIANT
        }
    }

    /**
     * Show a user-friendly error page when a site blocks WebView access (Akamai, etc.).
     * Replaces the blank/cryptic "Access Denied" page with an actionable message.
     */
    private fun showBlockedPage(view: WebView, blockedUrl: String) {
        val siteName = try { android.net.Uri.parse(blockedUrl).host?.removePrefix("www.") ?: blockedUrl } catch (_: Exception) { blockedUrl }
        val encodedUrl = android.net.Uri.encode(blockedUrl)
        val html = """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: -apple-system, system-ui, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 2rem; background: #f8f9fa; color: #333; }
                    .card { text-align: center; max-width: 360px; }
                    .icon { font-size: 3.5rem; margin-bottom: 1rem; }
                    h1 { font-size: 1.2rem; margin-bottom: 0.5rem; font-weight: 600; }
                    p { font-size: 0.9rem; color: #666; line-height: 1.5; margin-bottom: 1.25rem; }
                    .actions { display: flex; flex-direction: column; gap: 0.6rem; align-items: center; }
                    .btn { display: inline-block; padding: 0.6rem 1.5rem; border-radius: 8px; background: #3b82f6; color: #fff; text-decoration: none; font-size: 0.9rem; font-weight: 500; }
                    .btn.outline { background: none; border: 1px solid #3b82f6; color: #3b82f6; }
                    .btn.ghost { background: none; color: #3b82f6; font-size: 0.8rem; padding: 0.4rem 1rem; }
                    @media (prefers-color-scheme: dark) {
                        body { background: #09090b; color: #e4e4e7; }
                        p { color: #a1a1aa; }
                        .btn.outline { border-color: #5BA8F5; color: #5BA8F5; }
                        .btn.ghost { color: #5BA8F5; }
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="icon">🚫</div>
                    <h1>${Strings.t("blocked_title")} $siteName</h1>
                    <p>${Strings.t("blocked_message")}</p>
                    <div class="actions">
                        <a class="btn" href="sfz://clear-cookies?retry=$encodedUrl">${Strings.t("blocked_clear_retry")}</a>
                        <a class="btn outline" href="javascript:history.back()">${Strings.t("blocked_back")}</a>
                        <a class="btn ghost" href="$blockedUrl" target="_blank">${Strings.t("blocked_open_browser")}</a>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
        view.loadDataWithBaseURL(blockedUrl, html, "text/html", "UTF-8", blockedUrl)
    }

    /**
     * Clear all cookies for the current session and reload the URL.
     * Used from the blocked page to give the user a fresh start.
     */
    private fun clearCookiesAndRetry(view: WebView, retryUrl: String) {
        val sessionKey = currentAccountId
        val cm = sessionKey?.let { profileCookieManagerForSession(it) } ?: CookieManager.getInstance()
        // Wipe the saved cookie data for this session key so stale Akamai cookies don't persist
        sessionKey?.let { key ->
            val editor = cookiePrefs.edit()
            for (url in COOKIE_URLS) {
                editor.remove("${key}|$url")
            }
            editor.apply()
        }
        // Clear all in-memory cookies and reload
        isLoggedIn = false; pagesSinceOpen = 0
        cm.removeAllCookies {
            view.post { view.loadUrl(retryUrl) }
        }
        cm.flush()
    }

    /** Destroy the social webview and return to the Vue dashboard. */
    private fun goHome() {
        destroySocialView()
        dispatchToVue("sfz-webview-back")
    }

    /**
     * Shared back navigation logic used by both the UI back button and the hardware back button.
     * - If the webview has history → go back one page.
     * - Otherwise → hide overlay immediately + tell Vue to clear store (which triggers close_webview IPC).
     */
    private fun navigateBackOrClose() {
        val list = socialWebView?.copyBackForwardList()
        val currentIndex = list?.currentIndex ?: 0
        val baseline = initialBackIndex.coerceAtLeast(0)
        if (currentIndex > baseline) {
            socialWebView?.goBack()
        } else {
            destroySocialView()
            // Tell Vue to clear its store state (close_webview IPC will be a no-op since already destroyed)
            dispatchToVue("sfz-webview-back")
        }
    }

    /**
     * Register an Android back-press callback so the hardware back button is intercepted
     * while the social webview is visible. Removes itself when the webview is destroyed.
     */
    private fun registerBackCallback() {
        backCallback?.remove()
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackOrClose()
            }
        }
        (activity as? androidx.activity.ComponentActivity)
            ?.onBackPressedDispatcher
            ?.addCallback(callback)
        backCallback = callback
    }

    private fun applyMuteToWebView(webView: WebView?) {
        val js = """
        (function(){
          var muted = $isMuted;
          // Mute all existing media
          document.querySelectorAll('video,audio').forEach(function(el){ el.muted = muted; });
          // Persistent: watch for new media elements via MutationObserver
          if (muted && !window.__sfzMuteObserver) {
            window.__sfzMuteObserver = new MutationObserver(function(mutations) {
              for (var i = 0; i < mutations.length; i++) {
                var nodes = mutations[i].addedNodes;
                for (var j = 0; j < nodes.length; j++) {
                  var n = nodes[j];
                  if (n.tagName === 'VIDEO' || n.tagName === 'AUDIO') n.muted = true;
                  if (n.querySelectorAll) n.querySelectorAll('video,audio').forEach(function(el){ el.muted = true; });
                }
              }
            });
            window.__sfzMuteObserver.observe(document.documentElement, { childList: true, subtree: true });
            // Override AudioContext to silence Web Audio API
            if (!window.__sfzOrigAudioCtx) {
              window.__sfzOrigAudioCtx = window.AudioContext;
              window.__sfzOrigWebkitCtx = window.webkitAudioContext;
              var SilentCtx = function() {
                var ctx = new window.__sfzOrigAudioCtx();
                ctx.suspend();
                return ctx;
              };
              SilentCtx.prototype = (window.__sfzOrigAudioCtx || function(){}).prototype;
              window.AudioContext = SilentCtx;
              if (window.webkitAudioContext) window.webkitAudioContext = SilentCtx;
            }
          }
          // Unmute: disconnect observer and restore AudioContext
          if (!muted && window.__sfzMuteObserver) {
            window.__sfzMuteObserver.disconnect();
            delete window.__sfzMuteObserver;
            if (window.__sfzOrigAudioCtx) {
              window.AudioContext = window.__sfzOrigAudioCtx;
              delete window.__sfzOrigAudioCtx;
              if (window.__sfzOrigWebkitCtx) { window.webkitAudioContext = window.__sfzOrigWebkitCtx; delete window.__sfzOrigWebkitCtx; }
            }
          }
        })();
        """.trimIndent()
        webView?.evaluateJavascript(js, null)
    }

    private fun applyGrayscaleToWebView(view: WebView?) {
        val js = if (isGrayscale)
            "document.documentElement.style.filter='grayscale(1)';"
        else
            "document.documentElement.style.filter='';"
        view?.evaluateJavascript(js, null)
    }

    private fun applyGrayscaleToBottomBar(bar: LinearLayout?) {
        bar ?: return
        if (isGrayscale) {
            val cm = android.graphics.ColorMatrix()
            cm.setSaturation(0f)
            val paint = android.graphics.Paint()
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            bar.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, paint)
        } else {
            bar.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
        }
    }

    /** Re-apply dark/light colors to an existing bottom bar without rebuilding it. */
    private fun applyDarkModeToBottomBar(bar: LinearLayout?) {
        bar ?: return
        bar.setBackgroundColor(if (isDarkMode) Color.parseColor("#09090B") else Color.parseColor("#FFFFFF"))
        val iconColor = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#495057")
        // Walk the inner row and update dividers + utility button colors
        val innerRow = bar.getChildAt(0) as? LinearLayout ?: return
        for (i in 0 until innerRow.childCount) {
            val child = innerRow.getChildAt(i)
            // Dividers are plain Views (not TextView, not ImageButton, not HorizontalScrollView)
            if (child is View && child !is ViewGroup && child !is TextView && child !is ImageButton) {
                child.setBackgroundColor(if (isDarkMode) Color.parseColor("#27272A") else Color.parseColor("#DEE2E6"))
            }
        }
        // Update home button (first TextView in inner row, before the scroll view)
        (innerRow.getChildAt(0) as? TextView)?.setTextColor(iconColor)
        // Re-apply network button backgrounds (blend base changes between dark/light)
        updateBottomBarActiveNetwork(currentNetworkId ?: "")
    }

    private fun buildNetworkButton(density: Float, net: NetworkInfo, isActive: Boolean): View {
        SVG_ICONS[net.id]?.let { return buildSvgButton(density, net, isActive, it) }

        val btn = TextView(activity)
        btn.text = net.iconChar
        btn.typeface = primeIconsTypeface
        btn.textSize = 15f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)

        val size = (36 * density).toInt()
        val margin = (2 * density).toInt()
        val params = LinearLayout.LayoutParams(size, size)
        params.setMargins(margin, margin, margin, margin)
        btn.layoutParams = params

        applyNetworkButtonBackground(btn, net, isActive, size)
        btn.tag = net.id  // used by updateBottomBarActiveNetwork

        btn.isClickable = true
        btn.isFocusable = true
        btn.setOnClickListener {
            haptic(btn)
            dismissPopupMenu()
            if (net.id != currentNetworkId) {
                dbg("⇄ SWITCH ${currentNetworkId} → ${net.id}")
                val oldKey = currentAccountId
                if (oldKey.isNullOrBlank()) {
                    Log.w(TAG, "Blocked network switch: active session unavailable")
                    return@setOnClickListener
                }

                val oldSession = parseSessionIdentity(oldKey, currentNetworkId)
                if (oldSession == null) {
                    Log.w(TAG, "Blocked network switch: could not resolve active session")
                    return@setOnClickListener
                }
                val newSession = buildSessionIdentity(oldSession.profileId, net.id)
                if (newSession == null) {
                    Log.w(TAG, "Blocked network switch: invalid target session")
                    return@setOnClickListener
                }

                switchToSession(
                    targetSession = newSession,
                    targetUrl = net.url,
                    loadIfMissing = true,
                    declaredStorageOrigins = getDeclaredStorageOrigins(newSession),
                ) { shown, _ ->
                    if (!shown) {
                        Log.w(TAG, "Blocked network switch: target host unavailable")
                    }
                }
            }
        }

        return btn
    }

    private fun applyNetworkButtonBackground(btn: View, net: NetworkInfo, isActive: Boolean, size: Int) {
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.RECTANGLE
        bg.cornerRadius = size / 2f  // fully circular
        if (isActive) {
            bg.setColor(net.color)
        } else {
            // Blend brand color with bar background — different base for light vs dark
            val baseR = if (isDarkMode) 0x09 else 0xE8
            val baseG = if (isDarkMode) 0x09 else 0xE8
            val baseB = if (isDarkMode) 0x0B else 0xF0
            val brandWeight = if (isDarkMode) 0.25f else 0.3f
            val baseWeight = 1f - brandWeight
            val r = ((Color.red(net.color) * brandWeight) + (baseR * baseWeight)).toInt()
            val g = ((Color.green(net.color) * brandWeight) + (baseG * baseWeight)).toInt()
            val b = ((Color.blue(net.color) * brandWeight) + (baseB * baseWeight)).toInt()
            bg.setColor(Color.rgb(r, g, b))
        }
        btn.background = bg
        // Icon color: white on dark, darker on light (for inactive with light bg)
        val iconColor = if (isActive || isDarkMode) Color.WHITE else Color.parseColor("#495057")
        if (btn is TextView) {
            btn.setTextColor(iconColor)
        } else if (btn is FrameLayout && btn.childCount > 0) {
            // Threads custom icon — pass color via tag and redraw
            val child = btn.getChildAt(0)
            child.tag = iconColor
            child.invalidate()
        }

        // Active icon: full opacity + slight scale-up; inactive: dimmed — smooth transition
        val targetAlpha = if (isActive) 1f else 0.45f
        val targetScale = if (isActive) 1.12f else 1f
        btn.animate().cancel()
        btn.animate()
            .alpha(targetAlpha)
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(300)
            .start()
    }

    /** Build a button using official SVG path data (from Simple Icons, 24x24 viewBox). */
    private fun buildSvgButton(
        density: Float,
        net: NetworkInfo,
        isActive: Boolean,
        svgPathData: String,
    ): View {
        val size = (36 * density).toInt()
        val margin = (2 * density).toInt()
        val hasStroke = net.id == "snapchat"

        val iconView = object : View(activity) {
            private val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            private val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                color = Color.BLACK
                strokeWidth = 2.5f * density
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                val w = (width - paddingLeft - paddingRight).toFloat()
                val h = (height - paddingTop - paddingBottom).toFloat()

                val srcPath = PathParser.createPathFromPathData(svgPathData)
                val scaled = android.graphics.Path()
                val m = android.graphics.Matrix()
                m.setScale(w / 24f, h / 24f)
                srcPath.transform(m, scaled)

                val color = (tag as? Int) ?: if (isDarkMode) Color.WHITE else Color.parseColor("#495057")
                fillPaint.color = color
                fillPaint.style = android.graphics.Paint.Style.FILL

                canvas.save()
                canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
                if (hasStroke) canvas.drawPath(scaled, strokePaint)
                canvas.drawPath(scaled, fillPaint)
                canvas.restore()
            }
        }

        val wrapper = FrameLayout(activity)
        val wrapperParams = LinearLayout.LayoutParams(size, size)
        wrapperParams.setMargins(margin, margin, margin, margin)
        wrapper.layoutParams = wrapperParams

        val iconPad = (size * 0.22f).toInt()
        val iconParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        iconView.layoutParams = iconParams
        iconView.setPadding(iconPad, iconPad, iconPad, iconPad)

        wrapper.addView(iconView)
        applyNetworkButtonBackground(wrapper, net, isActive, size)
        wrapper.tag = net.id
        wrapper.isClickable = true
        wrapper.isFocusable = true
        wrapper.setOnClickListener {
            haptic(wrapper)
            dismissPopupMenu()
            if (net.id != currentNetworkId) {
                dbg("⇄ SWITCH ${currentNetworkId} → ${net.id} (threads btn)")
                val oldKey = currentAccountId
                if (oldKey.isNullOrBlank()) {
                    Log.w(TAG, "Blocked network switch: active session unavailable")
                    return@setOnClickListener
                }

                val oldSession = parseSessionIdentity(oldKey, currentNetworkId)
                if (oldSession == null) {
                    Log.w(TAG, "Blocked network switch: could not resolve active session")
                    return@setOnClickListener
                }
                val newSession = buildSessionIdentity(oldSession.profileId, net.id)
                if (newSession == null) {
                    Log.w(TAG, "Blocked network switch: invalid target session")
                    return@setOnClickListener
                }

                switchToSession(
                    targetSession = newSession,
                    targetUrl = net.url,
                    loadIfMissing = true,
                    declaredStorageOrigins = getDeclaredStorageOrigins(newSession),
                ) { shown, _ ->
                    if (!shown) {
                        Log.w(TAG, "Blocked network switch: target host unavailable")
                    }
                }
            }
        }

        return wrapper
    }

    private fun updateBottomBarActiveNetwork(activeNetworkId: String) {
        val root = socialRoot ?: return
        val density = activity.resources.displayMetrics.density
        // root → child 1 = bottomBar → child 0 = innerRow → child 2 = scrollView → child 0 = networkRow
        val bottomBar = root.getChildAt(1) as? LinearLayout ?: return
        val innerRow = bottomBar.getChildAt(0) as? LinearLayout ?: return
        val scrollView = innerRow.getChildAt(2) as? HorizontalScrollView ?: return
        val networkRow = scrollView.getChildAt(0) as? LinearLayout ?: return

        for (i in 0 until networkRow.childCount) {
            val btn = networkRow.getChildAt(i)
            val netId = btn.tag as? String ?: continue
            val net = NETWORKS.find { it.id == netId } ?: continue
            applyNetworkButtonBackground(btn, net, netId == activeNetworkId, btn.layoutParams.width)
        }
    }

    // Web login URLs for networks that redirect to app stores instead of showing a login page.
    private val NETWORK_LOGIN_URLS = mapOf(
        "tiktok" to "https://www.tiktok.com/login",
        "instagram" to "https://www.instagram.com/accounts/login/",
        "twitter" to "https://x.com/i/flow/login",
        "facebook" to "https://www.facebook.com/login",
        "snapchat" to "https://accounts.snapchat.com/accounts/v2/login",
        "discord" to "https://discord.com/login",
        "reddit" to "https://www.reddit.com/login/",
        "pinterest" to "https://www.pinterest.com/login/",
    )

    // Networks that require a desktop UA (their web app blocks mobile browsers).
    private val DESKTOP_UA_NETWORKS = setOf("whatsapp", "telegram", "discord", "snapchat")
    private val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
    private lateinit var mobileUa: String

    private fun isFacebookDesktopFlow(url: String?): Boolean {
        val value = url?.lowercase() ?: return false
        return value.contains("/stories/create") ||
            value.contains("/stories?") ||
            value.contains("/stories/") ||
            value.contains("story.php") ||
            value.contains("story_bucket")
    }

    private fun shouldUseDesktopUa(networkId: String?, url: String? = null): Boolean {
        if (networkId in DESKTOP_UA_NETWORKS) return true
        if (networkId == "facebook") {
            return facebookDesktopOverride || isFacebookDesktopFlow(url)
        }
        return false
    }

    /** Set the appropriate UA before loading a URL — desktop only where required. */
    private fun applyUaForNetwork(networkId: String?, url: String? = null) {
        val wv = socialWebView ?: return
        wv.settings.userAgentString = if (shouldUseDesktopUa(networkId, url)) DESKTOP_UA else mobileUa
    }

    private fun maybeHandleFacebookUaSwitch(view: WebView, url: String): Boolean {
        if (currentNetworkId != "facebook") return false

        val targetDesktop = isFacebookDesktopFlow(url)
        if (targetDesktop == facebookDesktopOverride) return false

        facebookDesktopOverride = targetDesktop
        applyUaForNetwork("facebook", url)
        dbg("[ua] facebook -> ${if (targetDesktop) "desktop" else "mobile"} for $url")
        view.loadUrl(url)
        return true
    }

    // ── WebView factory ───────────────────────────────────────────────────────

    private fun createWebView(profileName: String): WebView {
        val webView = WebView(activity)
        if (isPoolingEnabled()) {
            try {
                WebViewCompat.setProfile(webView, profileName)
            } catch (e: Exception) {
                disableMultiProfilePooling = true
                disablePoolingAndDestroyInactiveHosts()
                Log.w(TAG, "Session isolation degraded: MULTI_PROFILE setProfile failed")
                dbg("android-webview mode=fallback-single-webview (setProfile failed)")
            }
        }
        val settings = webView.settings
        applyNativeNightMode()
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.textZoom = textZoomLevel
        // Use the real WebView UA but strip the "; wv" token that flags us as a WebView.
        // This keeps the Chrome version in sync with the actual engine (no fingerprint mismatch).
        val defaultUa = WebSettings.getDefaultUserAgent(activity)
        mobileUa = defaultUa.replace("; wv", "")
        settings.userAgentString = mobileUa

        val cookieManager = if (isPoolingEnabled()) {
            runCatching { WebViewCompat.getProfile(webView).cookieManager }
                .getOrElse { CookieManager.getInstance() }
        } else {
            CookieManager.getInstance()
        }
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Inject stealth/cookie/banner scripts at document start (before page JS runs).
        // This is critical for anti-bot bypass — onPageFinished is too late.
        val useDocStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        if (useDocStart) {
            WebViewCompat.addDocumentStartJavaScript(webView, STEALTH_SCRIPT, setOf("*"))
            WebViewCompat.addDocumentStartJavaScript(webView, DESKTOP_VIEWPORT_SCRIPT, setOf("*"))
            WebViewCompat.addDocumentStartJavaScript(webView, DARK_MODE_DOC_START_SCRIPT, setOf("*"))
            // COOKIE_ACCEPT_SCRIPT is injected conditionally in onPageFinished (main frame).
            // COOKIE_IFRAME_SCRIPT runs in ALL frames but skips main frame — handles
            // cross-origin CMP iframes (Google Funding Choices, etc.).
            WebViewCompat.addDocumentStartJavaScript(webView, COOKIE_IFRAME_SCRIPT, setOf("*"))
            WebViewCompat.addDocumentStartJavaScript(webView, DISMISS_APP_BANNERS_SCRIPT, setOf("*"))
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (isInactiveManagedCallback(view)) return
                darkModeReapplyGeneration += 1
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                val url = request.url.toString()
                val scheme = request.url.scheme ?: ""
                val host = request.url.host ?: ""
                val path = request.url.path ?: ""
                if (isInactiveManagedCallback(view)) {
                    return scheme != "http" && scheme != "https"
                }
                dbg("[nav] net=${currentNetworkId ?: "?"} scheme=$scheme host=$host path=$path url=$url")

                // Allow normal web navigation — but intercept app store redirects
                if (scheme == "http" || scheme == "https") {
                    if (maybeHandleFacebookUaSwitch(view, url)) {
                        return true
                    }
                    // Intercept Play Store / App Store redirects → send to web login instead
                    if (host.contains("play.google.com") || host.contains("apps.apple.com") || host.contains("itunes.apple.com")) {
                        val loginUrl = NETWORK_LOGIN_URLS[currentNetworkId]
                        if (loginUrl != null) {
                            Log.i(TAG, "App store redirect intercepted ($host) → $loginUrl")
                            dbg("[nav] app-store redirect blocked → $loginUrl")
                            view.loadUrl(loginUrl)
                            return true
                        }
                        dbg("[nav] app-store redirect blocked with no fallback")
                        return true  // block even if no login URL known
                    }
                    return false
                }

                // Handle our custom "clear cookies and retry" action from the blocked page
                if (url.startsWith("sfz://clear-cookies")) {
                    val retryUrl = android.net.Uri.parse(url).getQueryParameter("retry") ?: return true
                    dbg("[nav] clear-cookies action retry=$retryUrl")
                    clearCookiesAndRetry(view, retryUrl)
                    return true
                }

                // Block all other custom URL schemes (intent://, market://, fb://,
                // instagram://, twitter://, whatsapp://, tg://, etc.)
                val loginUrl = NETWORK_LOGIN_URLS[currentNetworkId]
                if (loginUrl != null) {
                    Log.i(TAG, "Blocked custom scheme ($scheme) → redirecting to $loginUrl")
                    dbg("[nav] custom scheme blocked ($scheme) → $loginUrl")
                    view.loadUrl(loginUrl)
                } else {
                    Log.i(TAG, "Blocked custom scheme: $url")
                    dbg("[nav] custom scheme blocked without fallback: $url")
                }
                return true
            }
            override fun onReceivedHttpError(view: WebView, request: android.webkit.WebResourceRequest, errorResponse: android.webkit.WebResourceResponse) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (isInactiveManagedCallback(view)) return
                // Only handle main frame navigation (not sub-resources like images/scripts)
                if (request.isForMainFrame && errorResponse.statusCode == 403) {
                    showBlockedPage(view, request.url.toString())
                }
            }
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (isInactiveManagedCallback(view)) {
                    hostForWebView(view)?.let { host ->
                        if (host.initialBackIndex < 0) {
                            host.initialBackIndex = view.copyBackForwardList().currentIndex
                        }
                    }
                    return
                }
                dbg("[page] finished net=${currentNetworkId ?: "?"} ua=${if (shouldUseDesktopUa(currentNetworkId, url)) "desktop" else "mobile"} url=$url")
                applyTextZoomToWebView(view)
                // Fallback: inject scripts here only if addDocumentStartJavaScript wasn't available
                if (!useDocStart) {
                    view.evaluateJavascript(STEALTH_SCRIPT, null)
                    view.evaluateJavascript(DISMISS_APP_BANNERS_SCRIPT, null)
                }
                // Cookie consent: always inject for the first 3 pages after opening
                // a network (consent can appear after redirects). After that, check
                // auth cookies and stop injecting once logged in.
                if (!isLoggedIn) {
                    pagesSinceOpen++
                    view.evaluateJavascript(COOKIE_ACCEPT_SCRIPT, null)
                    // Retrieve cookie consent diagnostic log after 7s
                    val netId = currentNetworkId ?: "?"
                    view.postDelayed({
                        view.evaluateJavascript("window.__sfzCookieLog || ''") { result ->
                            val log = result?.trim('"') ?: ""
                            if (log.isNotEmpty()) {
                                for (line in log.split("\\n")) {
                                    dbg("[cookie:$netId] $line")
                                }
                            }
                        }
                    }, 7000)
                    if (pagesSinceOpen > 3 && checkLoggedIn()) {
                        isLoggedIn = true
                        dbg("[cookie:$netId] Auth cookies detected — disabled")
                    }
                }
                if (currentNetworkId == "facebook") {
                    view.postDelayed({
                        view.evaluateJavascript("(window.__sfzAppBannerLog || []).join('\\n')") { result ->
                            val raw = result?.trim()
                            if (raw.isNullOrEmpty() || raw == "null" || raw == "\"\"") return@evaluateJavascript
                            val decoded = try {
                                org.json.JSONTokener(raw).nextValue()?.toString() ?: ""
                            } catch (_: Exception) {
                                raw.trim('"')
                            }
                            if (decoded.isBlank()) return@evaluateJavascript

                            var sawStoryClick = false
                            var sawOpenAppBanner = false
                            decoded.split("\n").forEach { line ->
                                val entry = line.trim()
                                if (entry.isBlank()) return@forEach
                                dbg("[fb-ui] $entry")
                                if (entry.contains("CLICK candidate:", ignoreCase = true) &&
                                    entry.contains("story", ignoreCase = true)) {
                                    sawStoryClick = true
                                }
                                if (entry.contains("HIDE banner:", ignoreCase = true) &&
                                    entry.contains("ouvrir l’application", ignoreCase = true)) {
                                    sawOpenAppBanner = true
                                }
                            }
                            if (sawStoryClick && sawOpenAppBanner && !shouldUseDesktopUa(currentNetworkId, url)) {
                                showFacebookStoryUnavailableNotice()
                            }
                            view.evaluateJavascript("window.__sfzAppBannerLog = []", null)
                        }
                    }, 1200)
                }
                // Always re-inject desktop viewport override in onPageFinished (backup —
                // the page may have set its own viewport meta after our document-start script)
                if (shouldUseDesktopUa(currentNetworkId, url)) {
                    view.evaluateJavascript(DESKTOP_VIEWPORT_SCRIPT, null)
                }
                applyDarkModeToWebView(view)
                logFacebookDarkState(view, "page-finished")
                logLinkedInDarkState(view, "page-finished")
                scheduleDarkModeReapply(view)
                if (isGrayscale) applyGrayscaleToWebView(view)
                if (isMuted) applyMuteToWebView(view)
                // Detect Akamai/CDN block pages that return 200 but show "Access Denied"
                view.evaluateJavascript("""
                    (function() {
                        var body = document.body ? document.body.innerText : '';
                        if (body.length < 500 && /access\s*denied/i.test(body) && /reference\s*#/i.test(body)) {
                            return 'blocked';
                        }
                        return 'ok';
                    })();
                """.trimIndent()) { result ->
                    if (result?.contains("blocked") == true) {
                        showBlockedPage(view, url)
                    }
                }
                // Record back-stack depth after the initial page+redirects settle.
                // We only consider deeper entries as real user navigation.
                if (initialBackIndex < 0) {
                    initialBackIndex = view.copyBackForwardList().currentIndex
                }
                hostForWebView(view)?.let { syncHostFromGlobals(it) }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                val launcher = pickFileLauncher
                if (launcher == null || filePathCallback == null) {
                    dbg("[file] chooser unavailable launcher=${launcher != null} callback=${filePathCallback != null}")
                    filePathCallback?.onReceiveValue(null)
                    return false
                }

                pendingFilePathCallback?.onReceiveValue(null)
                pendingFilePathCallback = filePathCallback

                return try {
                    val rawAcceptTypes = (fileChooserParams?.acceptTypes ?: emptyArray())
                        .mapNotNull { it?.trim() }
                        .filter { it.isNotEmpty() }
                        .flatMap { value -> value.split(",").map(String::trim) }
                        .filter { it.isNotEmpty() }
                        .distinct()

                    val allowMultiple =
                        fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                    val filenameHint = fileChooserParams?.filenameHint ?: ""
                    val title = fileChooserParams?.title ?: ""
                    val isCaptureEnabled = fileChooserParams?.isCaptureEnabled ?: false
                    dbg(
                        "[file] chooser opened net=${currentNetworkId ?: "?"} " +
                            "accept=${if (rawAcceptTypes.isEmpty()) "*/*" else rawAcceptTypes.joinToString("|")} " +
                            "multiple=$allowMultiple capture=$isCaptureEnabled title=$title hint=$filenameHint"
                    )

                    val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)

                        if (rawAcceptTypes.size == 1) {
                            type = rawAcceptTypes.first()
                        } else {
                            type = "*/*"
                            if (rawAcceptTypes.isNotEmpty()) {
                                putExtra(android.content.Intent.EXTRA_MIME_TYPES, rawAcceptTypes.toTypedArray())
                            }
                        }
                    }

                    launcher.launch(intent)
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "Could not open web file picker: ${e.message}")
                    dbg("[file] chooser launch failed: ${e.message}")
                    pendingFilePathCallback = null
                    filePathCallback.onReceiveValue(null)
                    false
                }
            }

            // reCAPTCHA (and other verification services) open a hidden child window
            // to communicate with Google's servers. Without this, reCAPTCHA fails with
            // "impossible d'établir une connexion avec le service Recaptcha".
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message): Boolean {
                val childWebView = WebView(activity)
                if (isPoolingEnabled()) {
                    try {
                        val parentProfile = WebViewCompat.getProfile(view)
                        WebViewCompat.setProfile(childWebView, parentProfile.name)
                    } catch (e: Exception) {
                        Log.w(TAG, "Session isolation degraded: child WebView profile unavailable")
                    }
                }
                childWebView.settings.javaScriptEnabled = true
                childWebView.settings.domStorageEnabled = true
                childWebView.settings.userAgentString = view.settings.userAgentString
                val childCookieManager = if (isPoolingEnabled()) {
                    runCatching { WebViewCompat.getProfile(childWebView).cookieManager }
                        .getOrElse { CookieManager.getInstance() }
                } else {
                    CookieManager.getInstance()
                }
                childCookieManager.setAcceptThirdPartyCookies(childWebView, true)
                childWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, request: android.webkit.WebResourceRequest): Boolean {
                        // reCAPTCHA callback — load result in the parent WebView
                        val url = request.url.toString()
                        view.loadUrl(url)
                        return true
                    }
                }
                childWebView.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView) {
                        (window.parent as? ViewGroup)?.removeView(window)
                        window.destroy()
                    }
                }
                val transport = resultMsg.obj as android.webkit.WebView.WebViewTransport
                transport.webView = childWebView
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView) {
                (window.parent as? ViewGroup)?.removeView(window)
                window.destroy()
            }
        }
        return webView
    }

    // ── Visibility helpers ────────────────────────────────────────────────────

    private fun showSocialView() {
        val host = activeHost()
        if (host != null) {
            showHostInternal(host)
            return
        }
        socialRoot?.visibility = View.VISIBLE
    }

    private fun hideSocialView() {
        val host = activeHost()
        if (host != null) {
            hideHostInternal(host)
            return
        }
        socialRoot?.visibility = View.GONE
    }

    private fun destroySocialView() {
        val activeKey = activeHostSessionKey
        if (activeKey != null) {
            destroyHost(activeKey, "destroy-social-view")
            return
        }

        val allKeys = sessionHosts.keys.toList()
        for (key in allKeys) {
            destroyHost(key, "destroy-social-view-all")
        }
        clearGlobalActivePointers()
    }
}
