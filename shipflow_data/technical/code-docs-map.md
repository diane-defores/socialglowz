---
artifact: documentation
metadata_schema_version: "1.0"
artifact_version: "1.0.0"
project: "socialglowz"
created: "2026-05-14"
updated: "2026-05-22"
status: active
source_skill: sf-docs
scope: code_docs_map
owner: "socialglowz-team"
confidence: medium
risk_level: medium
security_impact: yes
docs_impact: yes
linked_systems:
  - "README.md"
  - "shipflow_data/technical/context.md"
  - "src/ui/setup/pages/SocialGlowz/main.ts"
  - "src/ui/setup/pages/SocialGlowz/views/SessionLockView.vue"
  - "src/lib/convexAuth.ts"
  - "src/lib/convexAuth.test.ts"
  - "src-tauri/src/lib.rs"
  - "src-tauri/Cargo.toml"
  - "src-tauri/capabilities/default.json"
  - "src-tauri/tauri.conf.json"
depends_on:
  - "shipflow_data/technical/context.md"
supersedes: []
next_step: "/sf-docs maintain shipflow_data/technical/code-docs-map.md"
---

# CODE DOCS MAP

## Auth/session hardening (Android)

- Code:
  - `src/ui/setup/pages/SocialGlowz/views/SessionLockView.vue`
  - `src/lib/convexAuth.ts`
  - `src/lib/convexAuth.test.ts`
- Behavior:
  - Lock screen only unlocks with pre-enrolled session PIN.
  - If no PIN exists for a locked session, relogin is required.
  - Session restore requires both JWT + refresh token in namespaced storage.
  - Legacy auth keys are purged during bootstrap.
- Docs:
  - `README.md` (section "Sécurité auth Android (hardening)")
  - `shipflow_data/technical/context.md` (Android OAuth callback hardening flow)

## Android deep-link OAuth callback validation

- Code:
  - `src/ui/setup/pages/SocialGlowz/main.ts`
  - `src-tauri/src/lib.rs`
  - `src-tauri/Cargo.toml`
  - `src-tauri/capabilities/default.json`
  - `src-tauri/tauri.conf.json`
- Behavior:
  - Tauri deep-link plugin registered at native runtime.
  - Frontend records pending OAuth requests through `socialglowz:android-oauth-request-started`.
  - Frontend listens to deep-link URLs and forwards callback validation to Rust command `validate_android_oauth_callback` only when the callback `state` matches a pending request.
  - Validation enforces callback allowlist, state/nonce checks against the pending request, TTL and replay protection.
  - Rejected callbacks emit an anonymized Sentry message when a runtime Sentry SDK is available.
- Docs:
  - `README.md`
  - `shipflow_data/technical/context.md`

## Security rendering guard

- Code:
  - `src/ui/setup/pages/SocialGlowz/main.ts`
- Behavior:
  - Auth bootstrap error screen message is rendered with `textContent`, not interpolated `innerHTML`.
- Docs:
  - `README.md`

## Android WebView storage isolation matrix

- Code:
  - `src/config/socialNetworks.ts`
  - `src/ui/setup/pages/SocialGlowz/composables/useNetworkWebview.ts`
  - `src/ui/setup/pages/SocialGlowz/composables/useWebviewPreload.ts`
  - `src-tauri/src/lib.rs`
  - `src-tauri/plugins/android-webview/src/mobile.rs`
  - `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
- Behavior:
  - Une matrice déclarative définit la politique d'isolation (par défaut `cookies` + `localStorage`, non-couverture `sessionStorage`/`IndexedDB`/`CacheStorage`/`serviceWorker`) et les origins additionnelles par réseau.
  - Le front passe `storageOrigins` à `open_webview` pour ouverture normale/preload et `storageOriginsByNetwork` à `set_bar_networks` pour les switches de la bottom bar native.
  - Rust Android valide/normalise ces origins (HTTPS + host autorisé par réseau + réseau visible pour la bottom bar) puis les transmet au plugin mobile.
  - Le plugin Kotlin élargit `allowedOrigins` des hooks globaux d'isolation stockage sans branche réseau spécifique, y compris lors d'un changement de réseau piloté uniquement côté natif.
- Docs:
  - `shipflow_data/technical/context.md`
  - `shipflow_data/workflow/specs/android-webview-storage-isolation.md`
