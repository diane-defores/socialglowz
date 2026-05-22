---
artifact: spec
metadata_schema_version: "1.0"
artifact_version: "0.3.0"
project: "socialglowz"
created: "2026-05-22"
created_at: "2026-05-22 15:55:00 UTC"
updated: "2026-05-22"
updated_at: "2026-05-22 16:20:00 UTC"
status: ready
source_skill: sf-spec
source_model: "gpt-5.5"
scope: security-hardening
owner: "Diane"
user_story: "En tant qu'utilisateur SocialGlowz Android avec plusieurs profils, je veux que les sessions WebView d'un réseau comme CinderReels soient isolées par profil, afin d'éviter toute fuite d'authentification entre profils."
risk_level: high
security_impact: yes
docs_impact: yes
linked_systems:
  - "src/ui/setup/pages/SocialGlowz"
  - "src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt"
  - "src-tauri/plugins/android-webview/android/build.gradle.kts"
  - "src-tauri/src/lib.rs"
  - "src/config/socialNetworks.ts"
depends_on:
  - artifact: "README.md"
    artifact_version: unknown
    required_status: unknown
  - artifact: "shipflow_data/technical/context.md"
    artifact_version: "1.0.0"
    required_status: reviewed
  - artifact: "shipflow_data/technical/context-function-tree.md"
    artifact_version: "1.0.0"
    required_status: reviewed
supersedes: []
evidence:
  - "User requested via $shipflow: fais le via une spec et subagents avec les modeles adaptes."
  - "CinderReels stores auth in localStorage; cookie-only Android isolation may leak sessions between profiles."
  - "Current Android isolation saves/restores cookies in SharedPreferences(\"sfz_cookies\") but not localStorage/sessionStorage."
  - "Existing Rust Android session key is `${profileId}-${networkId}`."
  - "Existing restoreCookiesForSession is async while current open/switch paths call loadUrl immediately after restore."
next_step: "Run Android generated-project build or CI Android build, then device QA for CinderReels profile isolation."
---

## Title

Android WebView Storage Isolation For CinderReels Sessions

## Status

Ready. This spec has passed the readiness gate for delegated sequential implementation.

## User Story

En tant qu'utilisateur SocialGlowz Android avec plusieurs profils, je veux que les sessions WebView d'un réseau comme CinderReels soient isolées par profil, afin d'éviter toute fuite d'authentification entre profils.

Primary actor: a SocialGlowz Android user switching between profiles for the same embedded network.

Trigger: the user opens, switches, deletes, backs up, or restores a native Android WebView session for a network whose web app persists auth in localStorage rather than cookies only.

Observable result: Profile A and Profile B do not share CinderReels authenticated state through cookies, localStorage, or restored session data.

## Minimal Behavior Contract

For each Android WebView network session, the native plugin must isolate persisted web storage by the existing canonical session key `${profileId}-${networkId}` and by origin. Before any site JavaScript runs for a navigation, the plugin restores the selected session's localStorage values for the target origin using an AndroidX document-start script when the feature is supported. During the session, page-origin storage changes are captured through a WebMessageListener bridge with origin and session validation, persisted under the same session key, and later restored only for that same session key. sessionStorage is treated as runtime-only or best-effort export data; it must not be advertised as a durable isolation guarantee. Cookie restore must complete before loadUrl is called. If document-start script support is unavailable, the plugin must warn and report degraded isolation rather than silently claiming success.

## Success Behavior

- Opening CinderReels for Profile A restores only Profile A cookies and Profile A localStorage for CinderReels origins before CinderReels page scripts execute.
- Opening or switching to CinderReels for Profile B restores only Profile B cookies and Profile B localStorage for CinderReels origins before page scripts execute.
- Switching A -> B -> A returns to each profile's own authenticated state without showing the other profile's account.
- localStorage changes made by CinderReels are persisted to Android SharedPreferences or an equivalent existing plugin persistence layer under `${profileId}-${networkId}` plus origin.
- Cookie restore is sequenced so loadUrl starts only after restoreCookiesForSession has completed its async restore callback.
- Unsupported WebView document-start support produces an explicit degraded-isolation warning and does not report full localStorage isolation as active.

## Error Behavior

- If WebViewFeature.DOCUMENT_START_SCRIPT is unsupported, continue loading the page only after recording degraded isolation; do not inject late restore code and do not claim localStorage isolation is guaranteed.
- If WebViewFeature.WEB_MESSAGE_LISTENER is unsupported, continue with cookie isolation and document-start restore if available, but mark storage capture as unavailable and prevent backup/restore from claiming current localStorage durability.
- If origin validation fails for a storage bridge message, ignore the message and log a security-relevant warning without persisting the payload.
- If session key validation fails or profile/network input is missing, fail the native command with a clear error instead of falling back to a shared/global storage bucket.
- If persisted localStorage JSON is corrupt for one origin/session, skip that origin, log the failure, and keep other origins and cookies restoreable.
- If cookie restore callback fails or times out, the command must return or surface a degraded state before loadUrl rather than racing into navigation.

## Problem

CinderReels stores authentication in localStorage. The current Android WebView isolation only saves/restores cookies in SharedPreferences("sfz_cookies"). Because Android WebView localStorage is shared by WebView data directory/origin unless actively isolated, switching SocialGlowz profiles can expose Profile A's CinderReels auth state to Profile B even when cookies are separated.

There is also a sequencing bug risk: restoreCookiesForSession is async, but open/switch paths currently call loadUrl immediately after restore. That means the first network request or page script can run before the intended session cookies are restored.

## Solution

Extend the Android WebView plugin to isolate HTML5 localStorage per `${profileId}-${networkId}` and origin, using AndroidX WebViewCompat document-start injection for pre-page-JS restore and WebMessageListener for validated storage capture. Fix the open/switch navigation sequencing so loadUrl is called only after cookie and supported storage restore preparation is complete.

## Scope In

- Android native WebView plugin behavior in `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`.
- Android plugin dependency/version validation in `src-tauri/plugins/android-webview/android/build.gradle.kts` if needed to confirm `androidx.webkit:webkit:1.12.1`, compileSdk 35, and minSdk 24 remain compatible.
- Session key handling using the existing Rust Android command contract `${profileId}-${networkId}`.
- CinderReels-focused acceptance validation through the SocialGlowz Android embedded network flow.
- localStorage persistence, restore, delete, backup, and restore-from-backup per session key and origin.
- sessionStorage runtime-only or best-effort export behavior with explicit limits.
- Cookie restore callback sequencing before loadUrl.
- Degraded-mode reporting when AndroidX document-start or WebMessageListener features are unavailable.

## Scope Out

- No implementation in this spec creation run.
- No claims of isolation for IndexedDB, CacheStorage, service workers, HTTP cache, WebView global cache, Web SQL, or browser-managed credential stores.
- No migration of existing historical localStorage already present in the global WebView data directory unless it is captured after the new bridge is active.
- No change to Convex auth, web SPA auth, desktop Tauri WebView behavior, Chrome extension behavior, Firefox extension behavior, or non-Android storage contracts.
- No new network catalog work; the current CinderReels catalog/native addition is already present per intake context.
- No use of Android addJavascriptInterface for third-party web content.

## Constraints

- Use the existing session key from the Rust Android command: `${profileId}-${networkId}`.
- Avoid parsing profile IDs with `substringBeforeLast` or similar delimiter-sensitive heuristics when possible. Prefer passing profileId and networkId separately through the native bridge or validating a structured session descriptor before deriving storage keys.
- Restore scripts must be registered before loadUrl. AndroidX document-start scripts apply only to navigations after the call returns.
- WebMessageListener bridge messages must validate expected origin and active session before persistence.
- Do not call Android WebStorage.deleteAllData for session deletion because it clears Web SQL and HTML5 Web Storage globally.
- Treat WebStorage.deleteOrigin as insufficient for this feature because Android documentation frames it around origin storage/Web SQL and it is not a session-keyed localStorage isolation primitive.
- The implementation must be sequential. Do not use parallel subagent writes unless future orchestration defines non-overlapping ownership; this spec recommends delegated sequential execution.

## Dependencies

- AndroidX WebKit local dependency: `androidx.webkit:webkit:1.12.1`.
- Android build targets from intake: compileSdk 35, minSdk 24.
- Official docs checked by the main thread:
  - AndroidX WebViewCompat `addDocumentStartJavaScript`: requires `WebViewFeature.DOCUMENT_START_SCRIPT`, runs before page JavaScript, applies only to navigations after the call returns, should be called before `loadUrl`, and returns `ScriptHandler` for removal.
  - AndroidX WebViewCompat `addWebMessageListener`: injects a JavaScript object at page load for matching origins and is available when the page begins loading; requires `WebViewFeature.WEB_MESSAGE_LISTENER`.
  - Android `WebStorage.deleteAllData`: clears Web SQL and HTML5 Web Storage globally.
  - Android `WebStorage.deleteOrigin`: per-origin API documented around Web SQL/origin storage, not a session-keyed isolation primitive.
  - Android `addJavascriptInterface`: risky for third-party content; use WebMessageListener with origin/session validation instead.
- Documentation freshness verdict: fresh-docs checked by the main thread and incorporated into this spec.

## Invariants

- The active Vue 3 SocialGlowz app remains under `src/ui/setup/pages/SocialGlowz`.
- Android native storage isolation lives in the Android plugin, not in shared web app state.
- Cookies, localStorage snapshots, and backup payloads are keyed by session and origin; no shared default bucket is allowed.
- The user-visible selected profile and the native active session key must refer to the same profile/network pair before any restore or capture occurs.
- A degraded isolation mode is acceptable only when visible to the native caller/logs and testable; silent fallback is not acceptable.
- WebView global storage clearing must not be used as a normal profile switch mechanism because it can destroy other networks/profiles.

## Links & Consequences

- Security: closes a profile isolation gap for third-party web apps that persist auth in localStorage.
- Data retention: introduces per-session localStorage backup data that must be deleted when the network session is deleted.
- Backup/restore: backup payload shape must include localStorage snapshots by session key and origin, plus clear flags for best-effort sessionStorage if exported.
- Performance: document-start scripts and bridge messages should be scoped to the active WebView/session and removed/replaced on session switch to avoid accumulating handlers.
- Compatibility: devices without DOCUMENT_START_SCRIPT or WEB_MESSAGE_LISTENER support must remain functional but flagged as degraded.
- Regression surface: Android open, switch, delete session, backup, restore, and CinderReels profile validation flows.

## Documentation Coherence

- Update the relevant technical context after implementation to state that Android cookie isolation is complemented by per-session localStorage isolation.
- Document the explicit non-coverage for IndexedDB, CacheStorage, service workers, HTTP cache, WebView global cache, and credential stores.
- If user-facing troubleshooting exists for Android profile switching, add a note that unsupported WebView document-start features can degrade localStorage isolation.
- Keep README platform overview unchanged unless implementation changes the public feature matrix.

## Edge Cases

- CinderReels redirects across multiple same-site origins; localStorage must be stored by exact origin and restored only to that origin.
- Profile ID or network ID contains hyphens; delimiter parsing must not route storage to the wrong profile.
- User switches profiles while a WebView is still loading; pending restore/capture operations must be tied to the active session and ignored if stale.
- Existing global localStorage may already contain a previous auth session before this feature ships; the implementation must not import it into another profile without explicit capture under the active session.
- Backup restored onto a new device before a WebView for that origin has loaded; restore must apply at the first supported document-start navigation after registration.
- Deleting a session while another profile for the same network exists; only the deleted session key and its origin snapshots are removed.
- WebMessageListener receives a large localStorage payload; enforce reasonable size limits or failure behavior before persisting.

## Implementation Tasks

- [x] Task 1: Inspect and map current Android WebView session flows.
  - File: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Action: Identify open, switch, delete, backup, restore, cookie save, cookie restore, WebView creation, and loadUrl call sites. Record where session key, profileId, networkId, URL, and active WebView are available.
  - User story link: Ensures all profile-switching entrypoints are covered.
  - Depends on: None
  - Validate with: `rg -n "restoreCookiesForSession|loadUrl|SharedPreferences|sfz_cookies|backup|delete|sessionKey|profileId|networkId" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Notes: This is read-only discovery before editing. Future implementation remains sequential.

- [x] Task 2: Introduce structured Android session identity.
  - File: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Action: Create or adapt a small internal session descriptor carrying profileId, networkId, sessionKey, and active URL/origin. Prefer explicit profileId/networkId inputs; if only `${profileId}-${networkId}` is available, validate against known network IDs rather than using delimiter heuristics like `substringBeforeLast`.
  - User story link: Prevents storage from being persisted under the wrong profile.
  - Depends on: Task 1
  - Validate with: `rg -n "substringBeforeLast|sessionKey|profileId|networkId" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Notes: Keep the external Rust command contract compatible with the existing `${profileId}-${networkId}` key unless changing the bridge is explicitly chosen during implementation.

- [x] Task 3: Add per-session localStorage persistence model.
  - File: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Action: Add storage helpers for localStorage snapshots keyed by sessionKey and origin, using SharedPreferences or the existing plugin persistence pattern. Store JSON maps safely and handle corrupt entries per origin.
  - User story link: Makes CinderReels auth durable per profile instead of global per WebView origin.
  - Depends on: Task 2
  - Validate with: `rg -n "localStorage|SharedPreferences|origin|JSONObject|JSONArray" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Notes: Do not claim IndexedDB, CacheStorage, service worker, or HTTP cache coverage.

- [x] Task 4: Register document-start localStorage restore before navigation.
  - File: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Action: Use `WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)` and `WebViewCompat.addDocumentStartJavaScript` to restore saved localStorage for the target origin before page scripts run. Keep and remove/replace the returned ScriptHandler when sessions change.
  - User story link: Prevents CinderReels page JavaScript from reading another profile's existing localStorage before restore.
  - Depends on: Task 3
  - Validate with: `rg -n "DOCUMENT_START_SCRIPT|addDocumentStartJavaScript|ScriptHandler|remove" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Notes: The script must be registered before `loadUrl`; it applies only to navigations after registration returns.

- [x] Task 5: Add WebMessageListener capture bridge.
  - File: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Action: Use `WebViewFeature.WEB_MESSAGE_LISTENER` and `WebViewCompat.addWebMessageListener` to expose a narrowly named bridge object for matching allowed origins. Validate origin and active session before persisting localStorage snapshots.
  - User story link: Captures CinderReels auth changes into the correct profile bucket.
  - Depends on: Task 4
  - Validate with: `rg -n "WEB_MESSAGE_LISTENER|addWebMessageListener|WebMessageCompat|JavaScriptReplyProxy|origin" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Notes: Do not use `addJavascriptInterface` for third-party pages.

- [x] Task 6: Fix cookie restore and storage restore sequencing.
  - File: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Action: Refactor open/switch paths so `loadUrl` is called only after async `restoreCookiesForSession` completion and after supported document-start restore/listener registration has completed. Return degraded or failed state when restore cannot complete.
  - User story link: Ensures the first CinderReels request and first page script use the intended profile state.
  - Depends on: Tasks 4 and 5
  - Validate with: `rg -n "restoreCookiesForSession|loadUrl|onReceiveValue|CookieManager|flush" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Notes: This task is mandatory even if localStorage isolation is implemented.

- [x] Task 7: Extend delete session behavior.
  - File: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Action: Delete localStorage snapshots and best-effort sessionStorage export data for the specific sessionKey and origins when deleting a network session, alongside existing cookie cleanup. Avoid `WebStorage.deleteAllData`.
  - User story link: Prevents deleted Profile A CinderReels auth from returning after future opens or backup restores.
  - Depends on: Task 3
  - Validate with: `rg -n "deleteAllData|deleteOrigin|delete.*session|remove.*session|sfz_cookies|localStorage" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Notes: If deleting currently loaded WebView state is needed, navigate to a neutral page before clearing active runtime state for that WebView only.

- [x] Task 8: Extend backup and restore payloads.
  - File: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Action: Include localStorage snapshots by sessionKey and origin in backup data, restore them into the same session/origin buckets, and label sessionStorage as runtime-only or best-effort if any export exists.
  - User story link: Keeps profile isolation intact across backup/restore.
  - Depends on: Tasks 3 and 7
  - Validate with: `rg -n "backup|restore|localStorage|sessionStorage|sessionKey|origin" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Notes: Backup restore must not merge Profile A storage into Profile B even for the same network.

- [x] Task 9: Verify AndroidX WebKit dependency and feature gates.
  - File: `src-tauri/plugins/android-webview/android/build.gradle.kts`
  - Action: Confirm the Android plugin remains on `androidx.webkit:webkit:1.12.1`, compileSdk 35, and minSdk 24, or adjust only if implementation proves a version mismatch. Ensure code gates features at runtime rather than assuming support from dependency version alone.
  - User story link: Prevents false success on unsupported device WebView implementations.
  - Depends on: Tasks 4 and 5
  - Validate with: `sed -n '1,220p' src-tauri/plugins/android-webview/android/build.gradle.kts`
  - Notes: Dependency version alone does not guarantee runtime WebView feature support.

- [x] Task 10: Add focused Android validation support.
  - File: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Action: Add logs/status fields sufficient for manual or automated validation to distinguish full isolation, degraded document-start support, degraded capture support, corrupt snapshot skip, and cookie restore sequencing failure.
  - User story link: Makes the security behavior observable and testable.
  - Depends on: Tasks 4, 5, and 6
  - Validate with: `rg -n "degraded|isolation|DOCUMENT_START_SCRIPT|WEB_MESSAGE_LISTENER|restoreCookiesForSession|loadUrl" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - Notes: Keep logs free of tokens, cookies, localStorage values, and account identifiers.

## Acceptance Criteria

- CinderReels Profile A/B isolation: after logging into different CinderReels accounts under Profile A and Profile B, opening Profile B never shows Profile A's authenticated account through localStorage or cookies.
- Switch A/B/A: after switching Profile A -> Profile B -> Profile A, each profile returns to its own CinderReels authenticated state without manual logout/login and without cross-profile leakage.
- Delete network session: deleting CinderReels for Profile A removes Profile A cookies and localStorage snapshots for `${profileId}-${networkId}` while leaving Profile B's CinderReels session intact.
- Backup/restore: backup includes per-session localStorage snapshots by origin, restore writes them back to the same session/origin buckets, and restored Profile A/Profile B sessions remain isolated.
- Unsupported document-start feature: when `WebViewFeature.DOCUMENT_START_SCRIPT` is unavailable, the plugin reports degraded isolation and does not claim localStorage isolation success.
- Unsupported bridge feature: when `WebViewFeature.WEB_MESSAGE_LISTENER` is unavailable, the plugin reports degraded capture and does not claim durable localStorage backup freshness.
- Cookie restore sequencing: every open/switch path invokes `loadUrl` only after async cookie restore callback completion and document-start setup, verified by code review and runtime logs.
- Security bridge: no `addJavascriptInterface` is introduced for third-party CinderReels content; WebMessageListener messages are accepted only for the expected origin and active session.
- Non-coverage is explicit: implementation and docs do not claim isolation for IndexedDB, CacheStorage, service workers, HTTP cache, WebView global cache, or credential stores.

## Test Strategy

- Static code validation:
  - `rg -n "restoreCookiesForSession|loadUrl|DOCUMENT_START_SCRIPT|WEB_MESSAGE_LISTENER|addJavascriptInterface|deleteAllData|deleteOrigin" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
  - `sed -n '1,220p' src-tauri/plugins/android-webview/android/build.gradle.kts`
- Build validation after implementation:
  - `pnpm typecheck`
  - `pnpm tauri:android:build` for Android plugin/package validation when the local Android toolchain is available.
  - `pnpm test:once` when the implementation also changes TypeScript backup/profile UI behavior.
- Manual Android validation:
  - Install an Android build with CinderReels enabled.
  - Profile A: open CinderReels, log into Account A, close or switch away.
  - Profile B: open CinderReels, confirm Account A is not visible, log into Account B.
  - Switch Profile B -> Profile A and confirm Account A returns.
  - Switch Profile A -> Profile B and confirm Account B returns.
  - Delete Profile A CinderReels session and confirm Profile A is logged out/reset while Profile B remains logged in.
  - Backup, restore to a clean install/device, and repeat A/B/A checks.
  - Test or simulate unsupported DOCUMENT_START_SCRIPT and WEB_MESSAGE_LISTENER paths; confirm degraded warnings and no false success.
- No project tests were run during spec creation by instruction.

## Risks

- High security risk if localStorage auth remains global because profile switching can expose another user's CinderReels account.
- Android WebView feature support depends on the runtime WebView provider, not only the AndroidX dependency; feature gates must be runtime checks.
- Document-start restore may not cover already-loaded pages; implementation must force new navigations after setup or clearly handle active runtime state.
- Backup payloads can contain sensitive auth-adjacent localStorage values; logs and diagnostics must not print values.
- Exact-origin storage may miss auth split across related origins; validation must enumerate observed CinderReels origins during manual tests.
- Existing global localStorage contamination before rollout may need separate user support guidance if users already experienced cross-profile state.

## Execution Notes

- User context: the request came through `$shipflow` after discussion that CinderReels stores auth in localStorage and cookie-only Android isolation may leak sessions between profiles.
- Intake context says the current CinderReels catalog/native addition is already present and the worktree is clean. This spec does not verify git state because git operations are forbidden for this run.
- Active app: Vue 3 SocialGlowz under `src/ui/setup/pages/SocialGlowz`.
- Android plugin target: `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`.
- Existing Android isolation target: SharedPreferences("sfz_cookies") for cookies only.
- Implementation should be delegated sequentially:
  1. Android native storage/cookie sequencing implementer.
  2. Android backup/delete implementer.
  3. Android QA/security verifier.
- Subagents must not write overlapping files at the same time. If multiple agents are used, hand off after each step with current diffs and validation output.
- Model application status for this spec: operator requested `source_model: gpt-5.5`; metadata records that override for downstream traceability if runtime accepts it.

## Open Questions

None.

## Skill Run History

| Date UTC | Skill | Model | Action | Result | Next step |
|----------|-------|-------|--------|--------|-----------|
| 2026-05-22 15:55:00 UTC | sf-spec | gpt-5.5 | Created ready-to-implement Android WebView storage isolation spec. | reviewed | /sf-ready shipflow_data/workflow/specs/android-webview-storage-isolation.md |
| 2026-05-22 16:02:00 UTC | sf-ready | GPT-5 Codex | Reviewed structure, metadata, security scope, docs freshness, Android dependency paths, validation commands, and acceptance criteria. | ready | /sf-start shipflow_data/workflow/specs/android-webview-storage-isolation.md |
| 2026-05-22 16:10:00 UTC | sf-start | gpt-5.3-codex | Implemented Android cookie sequencing, per-session localStorage isolation, backup/delete wiring, labels, and technical docs. | implemented | /sf-verify shipflow_data/workflow/specs/android-webview-storage-isolation.md |
| 2026-05-22 16:20:00 UTC | sf-verify | GPT-5 Codex | Ran local checks and reviewed security/session diff; Android generated-project build and device CinderReels QA unavailable in this workspace. | partial | Run Android build from generated project or CI, then device QA for CinderReels A/B/A, delete, and backup/restore. |

## Current Chantier Flow

| Step | Status | Notes |
|------|--------|-------|
| sf-spec | reviewed | Spec created as `shipflow_data/workflow/specs/android-webview-storage-isolation.md`. |
| sf-ready | ready | Spec corrected and accepted for delegated sequential implementation. |
| sf-start | implemented | Android native/session implementation, backup wiring, labels, and technical doc update are complete in the working tree. |
| sf-verify | partial | `pnpm typecheck`, `pnpm test:once`, `pnpm tauri:build`, `git diff --check`, and targeted static checks passed; `pnpm tauri:android:build` is blocked because `src-tauri/gen/android` is absent, and device/emulator CinderReels QA was not run. |
| sf-end | blocked | Do not close until Android build/device proof is collected or risk is explicitly accepted. |
| sf-ship | blocked | Do not ship until Android build/device proof is collected or risk is explicitly accepted. |
