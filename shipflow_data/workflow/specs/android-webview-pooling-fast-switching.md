---
artifact: spec
metadata_schema_version: "1.0"
artifact_version: "1.1.0"
project: "socialglowz"
created: "2026-05-23"
created_at: "2026-05-23 20:10:00 UTC"
updated: "2026-05-23"
updated_at: "2026-05-23 20:00:27 UTC"
status: ready
source_skill: sf-build
source_model: "GPT-5 Codex"
scope: android-performance-architecture
owner: "Diane"
confidence: medium
user_story: "En tant qu'utilisateur SocialGlowz Android, je veux que plusieurs pages reseau restent chaudes et isolees par profil+reseau, afin que les allers-retours soient rapides sans fuite de session."
risk_level: high
security_impact: yes
docs_impact: yes
linked_systems:
  - "src/ui/setup/pages/SocialGlowz/composables/useNetworkWebview.ts"
  - "src-tauri/src/lib.rs"
  - "src-tauri/plugins/android-webview/src/mobile.rs"
  - "src-tauri/plugins/android-webview/android/build.gradle.kts"
  - "src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt"
  - "shipflow_data/technical/android-webview-session-isolation.md"
depends_on:
  - artifact: "shipflow_data/workflow/specs/android-webview-storage-isolation.md"
    artifact_version: "1.0.2"
    required_status: ready
  - artifact: "shipflow_data/technical/android-webview-session-isolation.md"
    artifact_version: "1.0.0"
    required_status: reviewed
supersedes: []
evidence:
  - "User clarified that caching is the priority because network pages are too slow to load and switch."
  - "User explicitly prefers the clean durable architecture over an easier keep-alive workaround."
  - "Frontend useNetworkWebview already expects hide_webview/show_webview pooling semantics."
  - "Current Android Rust hide_webview calls close_webview and show_webview always returns false."
  - "Current Android Kotlin plugin owns a single socialWebView; bottom-bar network switches reuse that WebView but force a new loadUrl after session restore."
  - "Official AndroidX WebKit docs: Profile represents one browsing session, multiple profiles hold separate data, and Profile exposes CookieManager/WebStorage/ServiceWorkerController."
  - "Official AndroidX WebKit docs: WebViewCompat.setProfile must be called before using the WebView and can create a profile by name when MULTI_PROFILE is supported."
  - "Official Android docs: ProcessGlobalConfig/WebView data directory suffix is per process and must be configured before WebView initialization, so it is a fallback rather than the best fit for the current Tauri activity plugin."
next_step: "Implement delegated sequentially, then verify with local checks and GitHub Actions / Blacksmith APK manual QA."
---

# Android WebKit Multi-Profile Pooling And Fast Switching

## Status

Implemented locally; pending APK verification through GitHub Actions / Blacksmith.

## User Story

En tant qu'utilisateur SocialGlowz Android, je veux que plusieurs pages reseau restent chaudes et isolees par profil+reseau, afin que les allers-retours soient rapides sans fuite de session.

Primary actor: SocialGlowz Android user switching between embedded networks from the sidebar, native bottom bar, or profile/network navigation.

Observable result: after a profile/network session has loaded once, switching away and back reuses a live WebView host when it is still in the Android pool. The reused page must keep its DOM/scroll/auth state without sharing cookies, localStorage, IndexedDB, service workers, or cache with another `${profileId}-${networkId}` session when Android WebKit `MULTI_PROFILE` is available.

## Minimal Behavior Contract

Android must prefer AndroidX WebKit multi-profile isolation. Each canonical session key `${profileId}-${networkId}` gets a deterministic WebKit profile name and a bounded live WebView host. The WebView profile must be assigned with `WebViewCompat.setProfile(webView, profileName)` immediately after WebView construction and before settings, clients, document-start scripts, cookie access, or navigation.

When `WebViewFeature.MULTI_PROFILE` is supported, cookies and WebStorage must use the profile-specific APIs exposed by `Profile` rather than the process-global `CookieManager.getInstance()` for active navigation/session restore decisions. The existing SharedPreferences cookie/localStorage snapshot path may remain for backup/export compatibility and fallback devices, but it must not be the primary isolation mechanism on capable devices.

If `MULTI_PROFILE` is not supported, Android must explicitly enter a degraded fallback mode. The first fallback is the existing single-WebView save/restore path with no multi-WebView pooling. Process/data-directory-suffix isolation remains a later fallback architecture, not part of this implementation, because the current Tauri activity WebView already initializes WebView in the main process.

## Success Behavior

- Android detects and logs/report-selects `multi-profile` mode when `WebViewFeature.MULTI_PROFILE` is supported.
- `open_webview(profileId, networkId)` creates or reuses a host keyed by `${profileId}-${networkId}` and assigned to a matching WebKit profile.
- `hide_webview(profileId, networkId)` hides/pauses the matching host without destroying it.
- `show_webview(profileId, networkId, ...)` returns `true` when the matching host exists and is shown without `loadUrl`.
- Switching A -> B -> A through Vue or the native bottom bar reuses warm hosts when still in the LRU pool.
- LRU eviction is bounded from the first implementation to protect memory; target default is 3 warm social WebViews.
- Eviction saves observable session data, resets WebView hooks for that host, removes the root, destroys the WebView, and keeps other hosts isolated.
- Profile A / network X never shares WebKit profile data with Profile B / network X or Profile A / network Y.

## Error Behavior

- If `WebViewCompat.setProfile` is unsupported or fails, do not create multiple warm hosts with the process-global cookie jar; degrade to single-WebView behavior and report degraded performance/isolation mode.
- If `show_webview` receives an invalid or missing session, return `false` rather than showing another host.
- If host reattachment fails, destroy only that host and return `false` so the caller can recreate it.
- If the current session identity cannot be resolved during a native bottom-bar switch, block the switch as today.
- If profile deletion is requested while a live WebView still exists, destroy/evict that host before deleting profile data or fallback snapshots.

## Scope In

- Android native WebView host manager in `NativeWebViewPlugin.kt`.
- Android Rust command behavior in `src-tauri/src/lib.rs` for `hide_webview` and `show_webview`.
- Android mobile bridge response types in `src-tauri/plugins/android-webview/src/mobile.rs`.
- AndroidX WebKit dependency validation or upgrade in `android/build.gradle.kts` if needed.
- Technical docs describing multi-profile pooling, fallback mode, and APK-only validation.

## Scope Out

- No process-per-session implementation in this milestone.
- No aggressive startup preload of all networks.
- No desktop Tauri behavior change.
- No Convex auth, extension, Vercel web app, pricing, or public marketing claim change.
- No promise that fallback devices without `MULTI_PROFILE` receive full multi-WebView isolation.

## Constraints

- Pool key is exactly the existing canonical session key `${profileId}-${networkId}`.
- WebKit profile names must be deterministic, sanitized, and non-sensitive; use a prefix plus a hash of the session key rather than raw profile IDs.
- `WebViewCompat.setProfile` must be called before any WebView use except construction/attachment.
- `ProfileStore.deleteProfile` may throw when live WebViews are associated with the profile; destroy hosts first.
- Hidden WebViews should call `onPause()`; visible WebViews should call `onResume()`.
- Do not use `WebView.pauseTimers()` for host-level hide because Android documents it as global for all WebViews.
- Bottom bar remains a single native overlay, but its actions target the active host/session manager rather than one global WebView.
- Logs must not print cookies, localStorage values, tokens, raw profile IDs, or account identifiers.

## Invariants

- The isolation boundary remains `${profileId}-${networkId}` for all networks, not a CinderReels-only path.
- The existing declared storage origins matrix remains valid for document-start localStorage bridge/fallback behavior.
- A visible host and the plugin's active session metadata must always refer to the same session key.
- Hidden hosts must not receive storage bridge writes for the active visible host.
- Android manual QA remains APK-based via GitHub Actions / Blacksmith artifact installation.

## Implementation Tasks

- [x] Task 1: Add Android WebKit profile capability and host model.
  - File: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Action: Add capability probe for `WebViewFeature.MULTI_PROFILE`, deterministic profile-name helper, `SessionWebViewHost` state, and a session-host map with LRU metadata.
  - Validate with: `rg -n "MULTI_PROFILE|ProfileStore|setProfile|SessionWebViewHost|profileName|lastUsed" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`

- [x] Task 2: Refactor WebView creation around per-session profiles.
  - File: `NativeWebViewPlugin.kt`
  - Action: Change `createWebView()` into a session-aware factory that assigns `WebViewCompat.setProfile` before settings/scripts/clients/navigation. Move global per-host fields like script handler/back baseline/pages-since-open where necessary.
  - Validate with: `rg -n "createWebView\\(|setProfile|localStorageScriptHandler|initialBackIndex|pagesSinceOpen|activeHost" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`

- [x] Task 3: Implement real Android show/hide/open pooling.
  - Files: `NativeWebViewPlugin.kt`, `src-tauri/plugins/android-webview/src/mobile.rs`, `src-tauri/src/lib.rs`
  - Action: Make Android hide keep a host warm, show return a real boolean, open reuse an existing host, and close destroy only the targeted host/session.
  - Validate with: `rg -n "hide_webview|show_webview|showWebView|hideWebView|closeWebView|shown|destroyHost" src-tauri/src/lib.rs src-tauri/plugins/android-webview/src/mobile.rs src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`

- [x] Task 4: Route native bottom bar through the session manager.
  - File: `NativeWebViewPlugin.kt`
  - Action: Replace single-WebView `loadUrl` switches with a helper that saves/hides the current host, shows warm target host when present, or creates/loads a target host when absent.
  - Validate with: `rg -n "switchToSession|SWITCH|loadUrl|updateBottomBarActiveNetwork|activeHost" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`

- [x] Task 5: Add LRU eviction and deletion semantics.
  - File: `NativeWebViewPlugin.kt`
  - Action: Bound warm hosts, evict least-recently-used hidden hosts, destroy WebViews before profile deletion, and remove fallback snapshots for targeted sessions/profiles.
  - Validate with: `rg -n "evict|LRU|MAX_WARM|deleteNetworkSession|deleteProfileSession|ProfileStore|deleteProfile" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`

- [x] Task 6: Update technical docs and Android QA workflow.
  - Files: `shipflow_data/technical/android-webview-session-isolation.md`, `shipflow_data/technical/context.md`, `shipflow_data/workflow/tauri-mobile.md`
  - Action: Document multi-profile mode, fallback mode, LRU bound, non-coverage changes, and the APK manual QA route.
  - Validate with: `rg -n "MULTI_PROFILE|multi-profile|ProfileStore|APK|Blacksmith|WebView" shipflow_data/technical shipflow_data/workflow/tauri-mobile.md`

## Acceptance Criteria

- Android Rust `hide_webview` no longer calls `close_webview`.
- Android Rust `show_webview` returns `true` when Kotlin reuses a warm host.
- Kotlin uses `WebViewCompat.setProfile` before any session WebView navigation when `MULTI_PROFILE` is supported.
- In multi-profile mode, A -> B -> A after both sessions loaded once does not call `loadUrl` for A unless A was evicted.
- Profile A / CinderReels and Profile B / CinderReels can both stay warm without showing the wrong account.
- Deleting one network session destroys only that host/profile data and leaves other sessions intact.
- Fallback mode is explicit when `MULTI_PROFILE` is unavailable and does not pretend to support multi-WebView isolation.
- Local checks pass where available.
- Android APK manual QA covers fast switching, A/B profile isolation, delete session, app restart, and fallback visibility.

## Test Strategy

- Static validation:
  - `pnpm typecheck`
  - `pnpm lint:check`
  - `cargo check --manifest-path src-tauri/Cargo.toml` when local system dependencies allow it.
  - `rg` checks listed in implementation tasks.
- Android build validation:
  - Local `pnpm tauri:android:build` only when `src-tauri/gen/android` and Android toolchain are available.
  - Otherwise push through the GitHub Actions / Blacksmith APK workflow and install artifact `socialglowz-android-debug`.
- Manual APK validation:
  - Install a clean GitHub Actions / Blacksmith APK.
  - Open Profile A -> Instagram, log in, switch to Facebook, then back to Instagram; verify warm return without visible full reload.
  - Open Profile B -> Instagram and verify Profile A's Instagram account is not visible.
  - Repeat with CinderReels as the localStorage-heavy reference.
  - Use native bottom bar A -> B -> A and verify warm host reuse.
  - Open more than the pool limit and verify LRU eviction reloads only evicted sessions.
  - Delete Profile A / CinderReels and verify Profile B / CinderReels remains intact.
  - Kill/relaunch app and verify persisted profile separation.
  - Inspect logcat for selected isolation mode and host create/show/hide/destroy without sensitive values.

## Risks

- Android WebKit `MULTI_PROFILE` runtime support depends on the device WebView provider; fallback must be explicit.
- Current Kotlin state is globally shaped around one `socialWebView`; refactor risk is high.
- Backup/export currently reads SharedPreferences snapshots and may not fully represent WebKit profile data; keep that limitation explicit until export semantics are redesigned.
- Warm WebViews consume memory; LRU eviction is mandatory.
- Device-level performance/security proof cannot be completed inside this repo; the final user outcome requires APK manual QA.
- Process/data-directory-suffix fallback is heavier because WebView must be configured before process WebView initialization and cannot be applied to the already-initialized Tauri main WebView process.

## Execution Notes

- Execution mode: delegated sequential.
- Model routing: architecture review used `gpt-5.5`; implementation-heavy native code should use `gpt-5.3-codex`.
- No safe parallel write batches are defined because the Kotlin WebView lifecycle owns the critical state.
- Official docs consulted on 2026-05-23:
  - AndroidX `Profile`
  - AndroidX `ProfileStore`
  - AndroidX `WebViewCompat.setProfile`
  - AndroidX `ProcessGlobalConfig.setDataDirectorySuffix`
  - Android `WebView.pauseTimers`

## Open Questions

None for implementation. If `MULTI_PROFILE` does not compile or is unavailable on the target APK/device, the chantier should stop and report fallback evidence before attempting a process-isolated architecture.

## Skill Run History

| Date UTC | Skill | Model | Action | Result | Next step |
|----------|-------|-------|--------|--------|-----------|
| 2026-05-23 20:10:00 UTC | sf-spec | GPT-5 Codex | Created Android WebView pooling and fast switching spec from user caching request. | ready | sf-ready |
| 2026-05-23 20:10:00 UTC | sf-ready | GPT-5 Codex | Checked user story, scope, isolation invariants, acceptance criteria, docs impact, and Android APK proof path. | ready | sf-start |
| 2026-05-23 20:17:00 UTC | sf-build | gpt-5.5 medium | Integrated read-only architecture finding that single-WebView keep-alive is safer than process-global multi-WebView pooling. | rerouted | Re-evaluate clean architecture after user opted for durable performance. |
| 2026-05-23 20:00:27 UTC | shipflow | GPT-5 Codex | Routed user preference for clean durable architecture to sf-build. | rerouted | sf-build architecture correction. |
| 2026-05-23 20:00:27 UTC | sf-build | gpt-5.5/gpt-5.4-mini scouts | Rewrote spec around AndroidX WebKit multi-profile pooling with process/data-directory-suffix as fallback. | ready | sf-start |
| 2026-05-23 20:19:35 UTC | sf-start | GPT-5 Codex + delegated implementation context | Implemented AndroidX WebKit multi-profile host pooling, Rust/mobile bridge show/hide semantics, and docs updates. | implemented | sf-verify |
| 2026-05-23 20:19:35 UTC | sf-verify | GPT-5 Codex | Ran local typecheck, lint, tests, metadata lint, diff checks, and static Android WebKit contract checks; Android build/manual APK proof remains CI/APK-only. | partial | Ship/push to GitHub Actions / Blacksmith for APK QA after explicit risk acceptance. |
| 2026-05-24 13:39:23 UTC | sf-test | GPT-5 Codex | Captured Android APK manual Test 1 failure: same-profile network return takes about 4 seconds and visibly reloads every time. | failed | sf-fix BUG-2026-05-24-001 |
| 2026-05-24 13:42:45 UTC | sf-test | GPT-5 Codex | Captured Android APK manual Tests 2 and 3 pass for profile isolation and return-to-profile session behavior. | partial | collect SFZ logs for Test 1 reload path |
| 2026-05-24 13:53:11 UTC | sf-test | GPT-5 Codex | Analyzed copied SFZ logs; installed APK logs show old switch path with explicit `loadUrl` and a stale debug string absent from current source. | blocked | ship current code to Blacksmith APK, then retest Test 1 |

## Current Chantier Flow

| Step | Status | Notes |
|------|--------|-------|
| sf-spec | ready | Spec corrected for AndroidX WebKit multi-profile pooling and process-suffix fallback only if needed. |
| sf-ready | ready | Ready for sequential implementation; no parallel write batches. |
| sf-start | implemented | Android multi-profile host pool, show/hide bridge, LRU eviction, profile deletion handling, and docs implemented locally. |
| sf-verify | blocked | Installed APK logs do not match current source; they show the old switch path (`loadUrl` on every switch) and a stale debug string. Need a new APK from the current code before judging the latest pooling implementation. |
| sf-end | pending | Not closed. |
| sf-ship | pending | Ship/push current code to generate a fresh Blacksmith APK, then retest `BUG-2026-05-24-001`. |
