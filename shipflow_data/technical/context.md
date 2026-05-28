---
artifact: documentation
metadata_schema_version: "1.0"
artifact_version: "1.1.1"
project: "socialglowz"
created: "2026-04-26"
updated: "2026-05-25"
status: reviewed
source_skill: sf-docs
scope: context
owner: "Diane"
confidence: medium
risk_level: medium
security_impact: none
docs_impact: yes
evidence:
  - "README.md"
  - "package.json"
  - "vite.config.ts"
  - "vite.chrome.config.ts"
  - "vite.firefox.config.ts"
  - "vite.tauri.config.ts"
  - "src/ui/setup/pages/SocialGlowz/main.ts"
  - "src/ui/setup/pages/SocialGlowz/App.vue"
  - "src-tauri/src/lib.rs"
  - "convex/schema.ts"
  - "manifest.config.ts"
depends_on:
  - "README.md"
  - "AGENT.md"
supersedes: []
linked_systems:
  - "README.md"
  - "AGENT.md"
  - "shipflow_data/technical/context-function-tree.md"
  - "shipflow_data/technical/android-webview-session-isolation.md"
  - "shipflow_data/technical/architecture.md"
  - "package.json"
  - "vite.config.ts"
  - "vite.tauri.config.ts"
  - "src-tauri/tauri.conf.json"
  - "convex/schema.ts"
next_step: "/sf-docs update shipflow_data/technical/context.md"
---

# CONTEXT

## What SocialGlowz Is

SocialGlowz est une application social multi-canaux avec une base Vue 3 commune et des cibles de distribution extension navigateur, desktop Tauri et mobile Tauri.

## Product/Platform Matrix

- Extension Chrome/Firefox
  - Build via `vite.chrome.config.ts` et `vite.firefox.config.ts`
  - Entrées HTML dans `src/ui/*`
  - Manifest CRX v3 dans `manifest.config.ts`
- Desktop/Tauri
  - Build via `vite.tauri.config.ts` + `pnpm tauri:bundle`
  - Entrée principale `src-tauri/src/lib.rs` + `src-tauri/tauri.conf.json`
- Android
  - Cible Tauri mobile avec plugin `src-tauri/plugins/android-webview`
## Repo Map

- `src/` : logique partagée, stores, composables, services, utilitaires.
- `src/ui/setup/pages/SocialGlowz/` : application principale.
- `src/ui/setup/pages/SocialGlowz/main.ts` : bootstrap front (Vue + Convex + services).
- `src/ui/setup/pages/SocialGlowz/App.vue` : shell desktop/mobile principal et orchestration.
- `src/ui/*` : pages de shell navigateur (setup popup panel options).
- `convex/` : backend serverless auth + données persistées.
- `src-tauri/src/` : host natif, commandes IPC, création/gestion webviews.
- `src-tauri/plugins/android-webview/` : API native Android.

## Core Runtime Flows

### 1) Front boot + auth

1. Vite démarre une entrée UI.
2. `src/ui/setup/pages/SocialGlowz/main.ts` initialise Pinia, i18n, PrimeVue, router.
3. Si `VITE_CONVEX_URL` est présent, `getConvexClient()` et `setupConvexAuth()` initient Convex Auth.
4. App bootstrap puis montage de l'application.

#### Android OAuth callback hardening (mobile)

1. `main.ts` écoute les événements `deep-link://new-url` du plugin deep-link et lit aussi `plugin:deep-link|get_current` au démarrage.
2. Lorsqu'une requête OAuth démarre, l'app enregistre un `state`/`nonce` pending local via `socialglowz:android-oauth-request-started`.
3. Chaque URL candidate OAuth est validée côté Rust via `validate_android_oauth_callback` contre cette requête pending (host/schéma allowlist, `state`, `nonce`, TTL 5 min, anti-rejeu).
4. Un callback rejeté ne doit pas muter l'état auth/session et déclenche un signal Sentry anonymisé si le SDK est disponible.
5. Le lock session n'autorise pas de création PIN depuis l'écran verrouillé: si aucun PIN préenregistré, l'utilisateur retourne au login.

### 2) Navigation SocialGlowz

1. `src/ui/setup/pages/SocialGlowz/router/index.ts` route selon hash.
2. `AuthGuard` protège les vues réseau.
3. Vue réseau utilise `webviewStore` et store profils pour ouvrir le bon WebView.
4. Sur desktop/mobile, le front appelle des commandes natives Tauri via IPC.

### 3) Sync et persistance

- État local : Pinia + localStorage via stores.
- Sync cloud : `src/lib/cloudSyncQueue.ts`, `src/lib/cloudSettings.ts`, `src/lib/cloudSync.ts`.
- Backend : tables Convex (`users`, `socialAccounts`, `activeAccounts`, `settings`, `profiles`, `customLinks`, `friendsFilters`, `subscriptions`).
- Android WebView (plugin natif) : quand Android WebKit `MULTI_PROFILE` est disponible, chaque session `${profileId}-${networkId}` utilise un profil WebKit natif distinct et un hôte WebView chaud dans un pool LRU borné. En fallback, le plugin revient au mode single-WebView avec cookies + snapshots `localStorage` persistés par session et par origin exacte. CinderReels déclare une origin explicite car son auth utilise `localStorage`; les autres réseaux utilisent le même mécanisme via leur URL principale.
- Les origins additionnelles où l'isolation scriptée s'applique sont déclarées côté front dans `src/config/socialNetworks.ts` puis transmises à `open_webview` et `set_bar_networks` (validation HTTPS/allowlist côté Rust Android), afin de couvrir les réseaux dont l'auth/app traverse plusieurs domaines et les switches natifs de la bottom bar Android.
- Mode dégradé explicite si `DOCUMENT_START_SCRIPT` ou `WEB_MESSAGE_LISTENER` ne sont pas disponibles.
- Mode dégradé explicite aussi si `MULTI_PROFILE` est indisponible : le multi-WebView chaud est désactivé pour éviter un partage du `CookieManager` global. Les snapshots fallback ne couvrent pas IndexedDB, CacheStorage, service workers, HTTP cache WebView global, credential stores système. `sessionStorage` n'est pas une garantie durable.
- Détail du contrat : `shipflow_data/technical/android-webview-session-isolation.md`.

### 4) Extension surfaces

- `src/background/index.ts` gère install/update et ouvre la page setup.
- `src/content-script/index.ts` est neutre par défaut (pas d'injection globale active).
- `src/ui/action-popup`, `src/ui/options-page`, `src/ui/side-panel` exposent des pages dédiées au navigateur.
- `src/platform/capabilities.ts` centralise la détection extension/Tauri.
- `src/platform/extensionNetworkLauncher.ts` ouvre les réseaux en onglets navigateur avec validation stricte HTTPS.
- Le side panel est activé uniquement sur Chrome via `manifest.chrome.config.ts`; Firefox conserve un fallback popup/options/setup.
- Le détail de parité est dans `shipflow_data/technical/extension-parity-map.md`.

## Technical Decisions

- Tauri est retenu pour la couche desktop/mobile pour partager la même base JS tout en gardant contrôle WebView natif.
- L'application SocialGlowz reste dans `src/ui/setup/pages/SocialGlowz` avec réutilisation contrôlée des modules partagés de `src/`.
- La stratégie auth-connexion privilégie Convex Auth avec fallback offline.

## Hotspots

- `src/ui/setup/pages/SocialGlowz/App.vue` : flux global, sync, gestion évènements natifs.
- `src/stores/webviewState.ts` : state réseau actif, ouverture/fermeture et profils.
- `src/lib/cloudSync.ts` : sync de settings et données entre local et Convex.
- `src-tauri/src/lib.rs` : commandes natives critiques (webview, session, commande Android).
- `convex/socialAccounts.ts`, `convex/settings.ts`, `convex/profiles.ts` : tables cœur métier.

## Read by Task

- Changer UI/UX : lire d'abord `src/ui/setup/pages/SocialGlowz/*` puis `src/stores/*` et `src/components/*`.
- Changer logique métier : lire `src/` puis la vue SocialGlowz correspondante.
- Changer extension shell : lire `src/ui/*`, manifest et `shipflow_data/technical/context-function-tree.md`.
- Changer backend : lire `convex/*`, `src/lib/convex.ts`, `src/lib/convexAuth.ts`, puis mise à jour docs.
- Changer build : lire scripts dans `package.json` puis configs Vite correspondantes.

## Update Rule

Mettre à jour `shipflow_data/technical/context.md` si les flux ci-dessus changent (nouveaux entry points, nouveaux stores critiques, nouveau comportement de sync ou de commande native).
