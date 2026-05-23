---
artifact: technical_module_context
metadata_schema_version: "1.0"
artifact_version: "1.1.0"
project: "socialglowz"
created: "2026-05-23"
updated: "2026-05-23"
status: reviewed
source_skill: sf-docs
scope: android-webview-session-isolation
owner: "Diane"
confidence: medium
risk_level: high
security_impact: yes
docs_impact: yes
linked_systems:
  - "README.md"
  - "shipflow_data/technical/context.md"
  - "shipflow_data/workflow/tauri-mobile.md"
  - "shipflow_data/workflow/specs/android-webview-storage-isolation.md"
  - "shipflow_data/workflow/specs/android-webview-pooling-fast-switching.md"
  - "src/config/socialNetworks.ts"
  - "src/ui/setup/pages/SocialGlowz/composables/useNetworkWebview.ts"
  - "src/ui/setup/pages/SocialGlowz/composables/useWebviewPreload.ts"
  - "src-tauri/src/lib.rs"
  - "src-tauri/plugins/android-webview/src/mobile.rs"
  - "src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt"
depends_on:
  - "shipflow_data/technical/context.md"
  - "shipflow_data/technical/code-docs-map.md"
supersedes: []
evidence:
  - "shipflow_data/workflow/specs/android-webview-storage-isolation.md"
  - "src/config/socialNetworks.ts"
next_review: "2026-07-23"
next_step: "/sf-docs audit shipflow_data/technical/android-webview-session-isolation.md"
---

# Android WebView Session Isolation

## Purpose

Ce document décrit le contrat actif d'isolation et de pooling des sessions WebView Android pour SocialGlowz. Il couvre les sessions de réseaux intégrés affichés dans le plugin Android WebView, pas l'auth Convex de l'application hôte.

## Owned Files

- `src/config/socialNetworks.ts` déclare la politique d'isolation par réseau, les origins additionnelles, et les limites non couvertes.
- `src/ui/setup/pages/SocialGlowz/composables/useNetworkWebview.ts` transmet les origins d'isolation lors de l'ouverture d'une WebView.
- `src/ui/setup/pages/SocialGlowz/composables/useWebviewPreload.ts` transmet les mêmes origins lors du preload.
- `src-tauri/src/lib.rs` valide et relaie les paramètres Android vers le plugin natif.
- `src-tauri/plugins/android-webview/src/mobile.rs` expose le pont mobile Tauri.
- `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt` applique l'isolation cookies/localStorage côté Android.

## Entrypoints

- `open_webview` ouvre une session réseau en utilisant la clé canonique `${profileId}-${networkId}`.
- `hide_webview` garde l'hôte de session chaud quand le mode multi-profile est actif.
- `show_webview` réaffiche une session chaude existante et renvoie si elle a été trouvée.
- `set_bar_networks` configure les réseaux disponibles dans la bottom bar Android et transmet `storageOriginsByNetwork` pour les switches natifs.
- `delete_network_session` doit supprimer les données persistées associées à la session ciblée.
- Les flows backup/restore doivent conserver les snapshots `localStorage` sous la même clé session + origin.

## Isolation Contract

SocialGlowz isole par défaut les sessions Android WebView par profil et réseau avec la clé `${profileId}-${networkId}`. Cette clé ne doit pas être remplacée par un bucket global, même en mode dégradé.

Quand Android WebKit expose `WebViewFeature.MULTI_PROFILE`, cette clé est convertie en nom de profil WebKit déterministe et non sensible. La WebView de session reçoit ce profil via `WebViewCompat.setProfile` immédiatement après sa construction, avant toute configuration ou navigation. Ce mode permet de garder plusieurs WebViews chaudes dans un pool LRU borné sans partager le `CookieManager` global.

L'isolation couvre:

- en mode multi-profile: les données WebKit natives associées au profil de session, notamment cookies et WebStorage exposés par AndroidX WebKit,
- en mode fallback: les cookies persistés par session et les snapshots `localStorage` persistés par session et par origin exacte.

Les origins d'isolation viennent de `src/config/socialNetworks.ts`:

- l'origine principale est dérivée de l'URL du réseau,
- une matrice d'origins additionnelles complète ce socle pour les réseaux qui utilisent plusieurs domaines d'auth ou d'app,
- CinderReels déclare explicitement `https://cinderreels.com` car son authentification utilise `localStorage`.

Les autres réseaux bénéficient du même mécanisme via leur URL principale. Un nouveau réseau ne doit ajouter une origin additionnelle que si son parcours réel sort de son domaine principal.

## Invariants

- La frontière de session Android WebView reste `${profileId}-${networkId}`.
- En mode multi-profile, une WebView de session doit recevoir son profil WebKit avant tout accès WebView autre que la construction.
- Les cookies et snapshots fallback `localStorage` sont toujours associés à une origin exacte.
- Le pool de WebViews chaudes reste borné; les hôtes evincés sont détruits avant suppression du profil WebKit.
- Les origins additionnelles doivent rester déclaratives dans `src/config/socialNetworks.ts`.
- Le fallback single-WebView ne doit pas promettre la couverture d'IndexedDB, CacheStorage, service workers, cache HTTP global WebView ou credential store système.
- Les modes dégradés doivent être observables sans exposer de données sensibles.

## Degraded Modes

Le plugin Android doit signaler un mode dégradé quand une WebView ne supporte pas les primitives nécessaires à l'isolation complète:

- sans `MULTI_PROFILE`, le multi-WebView chaud est désactivé et le plugin revient au mode single-WebView avec snapshots,
- sans `DOCUMENT_START_SCRIPT`, le restore `localStorage` ne peut pas être garanti avant le JavaScript de la page,
- sans `WEB_MESSAGE_LISTENER`, la capture durable des changements `localStorage` peut être indisponible.

Le mode dégradé ne doit pas être présenté comme une isolation complète. Les logs et statuts doivent rester exempts de tokens, cookies, valeurs `localStorage`, identifiants de comptes et secrets.

## Non-Coverage

Le fallback par snapshots ne couvre pas:

- IndexedDB,
- CacheStorage,
- service workers,
- cache HTTP global de la WebView Android,
- credential store système.

`sessionStorage` n'est pas une garantie durable d'isolation. S'il apparaît dans un export, il doit être traité comme runtime-only ou best-effort.

## Validation

- Vérifier la matrice côté front:
  `rg -n "NETWORK_ISOLATION|storageOrigins|getNetworkIsolationOrigins" src/config/socialNetworks.ts`
- Vérifier le passage des origins:
  `rg -n "storageOrigins|storageOriginsByNetwork" src/ui/setup/pages/SocialGlowz src-tauri/src/lib.rs`
- Vérifier les hooks natifs:
  `rg -n "MULTI_PROFILE|ProfileStore|setProfile|DOCUMENT_START_SCRIPT|WEB_MESSAGE_LISTENER|localStorage|restoreCookiesForSession|loadUrl|degraded" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`
- Vérifier le pooling Android:
  `rg -n "SessionWebViewHost|MAX_WARM|showWebView|hideWebView|destroyHost|shown" src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt src-tauri/src/lib.rs src-tauri/plugins/android-webview/src/mobile.rs`
- Tester Android depuis l'APK CI GitHub Actions / Blacksmith, artifact `socialglowz-android-debug`.

## Reader Checklist

- Si une tâche change `src/config/socialNetworks.ts`, vérifier que les origins restent HTTPS et minimales.
- Si une tâche change `open_webview`, `hide_webview`, `show_webview`, `set_bar_networks`, backup, restore ou delete session, vérifier que la clé `${profileId}-${networkId}` reste la frontière de session.
- Si une tâche touche le pooling Android, vérifier que `MULTI_PROFILE` est le seul mode autorisant plusieurs WebViews chaudes.
- Si une tâche ajoute un réseau dont l'auth traverse plusieurs domaines, ajouter uniquement les origins nécessaires à la matrice.
- Si une tâche touche le plugin Android WebView, vérifier que les limites de non-couverture restent documentées.

## Maintenance Rule

Mettre à jour ce document avec `shipflow_data/technical/context.md` quand le contrat Android WebView change, notamment pour les origins, la clé de session, les modes dégradés, backup/restore, ou les limites d'isolation.
