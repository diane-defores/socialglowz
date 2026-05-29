---
artifact: documentation
metadata_schema_version: "1.0"
artifact_version: "1.0.1"
project: "socialglowz"
created: "2026-05-14"
updated: "2026-05-29"
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
  - "convex/billing.ts"
  - "convex/billing.test.ts"
  - "src-tauri/src/lib.rs"
  - "src-tauri/Cargo.toml"
  - "src-tauri/capabilities/default.json"
  - "src-tauri/tauri.conf.json"
depends_on:
  - "shipflow_data/technical/context.md"
supersedes: []
evidence:
  - "src-tauri/src/lib.rs"
  - "src-tauri/plugins/android-webview/src/mobile.rs"
  - "src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt"
  - "shipflow_data/technical/android-webview-session-isolation.md"
next_step: "/sf-docs maintain shipflow_data/technical/code-docs-map.md"
---

# CODE DOCS MAP

## Processor-agnostic product access

- Code:
  - `convex/schema.ts`
  - `convex/billing.ts`
  - `convex/billing.test.ts`
- Behavior:
  - SocialGlowz product access is owned by internal `entitlements`, not by a payment processor.
  - `billing.redeemCode` lets an authenticated user redeem AppSumo/manual codes into active `socialglowz/founder_ltd` access.
  - `billing.adminUpsertRedemptionCode` is protected by `SOCIALGLOWZ_BILLING_ADMIN_SECRET` and is intended for server/operator imports only.
  - `billing.getProductAccess` returns active entitlement access and keeps a temporary fallback to legacy `subscriptions`.
  - `billingEvents` records redemption/admin events for auditability without coupling the app to AppSumo, Lemon Squeezy, Polar, Stripe, Paddle, or another provider.
- Docs:
  - `shipflow_data/workflow/specs/socialglowz-billing-entitlements-foundation.md`
  - `shipflow_data/technical/context.md`

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

## Android WebView storage isolation and pooling

- Code:
  - `src/config/socialNetworks.ts`
  - `src/ui/setup/pages/SocialGlowz/composables/useNetworkWebview.ts`
  - `src/ui/setup/pages/SocialGlowz/composables/useWebviewPreload.ts`
  - `src-tauri/src/lib.rs`
  - `src-tauri/plugins/android-webview/src/mobile.rs`
  - `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
- Behavior:
  - Quand Android WebKit `MULTI_PROFILE` est disponible, le plugin rattache chaque WebView de session `${profileId}-${networkId}` à un profil WebKit natif distinct via `WebViewCompat.setProfile`, avant toute configuration ou navigation.
  - Les hôtes WebView de session sont conservés dans un pool LRU borné pour accélérer les switches; `hide_webview`/`show_webview` Android reflètent maintenant ce contrat au lieu de détruire systématiquement la WebView.
  - Si `MULTI_PROFILE` est indisponible ou échoue, le plugin annonce un fallback single-WebView et désactive le pooling multi-WebView pour éviter un partage du `CookieManager` global.
  - Une matrice déclarative définit la politique d'isolation (par défaut `cookies` + `localStorage`, non-couverture `sessionStorage`/`IndexedDB`/`CacheStorage`/`serviceWorker`) et les origins additionnelles par réseau.
  - Le front passe `storageOrigins` à `open_webview` pour ouverture normale/preload et `storageOriginsByNetwork` à `set_bar_networks` pour les switches de la bottom bar native.
  - Rust Android valide/normalise ces origins (HTTPS + host autorisé par réseau + réseau visible pour la bottom bar) puis les transmet au plugin mobile.
  - Le plugin Kotlin élargit `allowedOrigins` des hooks d'isolation/capture stockage sans branche réseau spécifique, y compris lors d'un changement de réseau piloté uniquement côté natif.
- Docs:
  - `shipflow_data/technical/context.md`
  - `shipflow_data/workflow/specs/android-webview-storage-isolation.md`
