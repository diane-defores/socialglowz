# SocialGlowz

Dashboard unifié pour gérer tous vos réseaux sociaux depuis une seule interface. Disponible en extension Chrome/Firefox, application desktop et application mobile.

## Plateformes

| Plateforme | Technologie | Build | Statut |
|---|---|---|---|
| Chrome Extension | CRXJS + Vite | `pnpm build:chrome` | Production |
| Firefox Extension | CRXJS + Vite | `pnpm build:firefox` | Production |
| Desktop (Win/Mac/Linux) | Tauri 2 | `pnpm tauri:bundle` | Production |
| Android | Tauri 2 Mobile | CI GitHub Actions | Production |
| iOS | Tauri 2 Mobile | CI GitHub Actions (macOS) | Planned |

## Architecture

```
Une seule codebase Vue.js → extension navigateur, desktop et mobile

src/ui/setup/pages/SocialGlowz/    # App principale (Vue 3 + PrimeVue)
├── main.ts                        # Entry point standalone
├── App.vue                        # Layout responsive (mobile/desktop)
├── router/                        # Vue Router (createWebHashHistory)
├── components/
│   ├── networks/                  # Vues par réseau social
│   ├── kanban/                    # Tableau Kanban
│   └── feed/                      # Feed unifié
├── stores/                        # Pinia stores
├── composables/                   # Hooks Vue
└── services/                      # Services API (Gmail, etc.)
```

### Configs Vite par plateforme

| Fichier | Cible | Sortie |
|---|---|---|
| `vite.chrome.config.ts` | Extension Chrome | `dist/chrome/` |
| `vite.firefox.config.ts` | Extension Firefox | `dist/firefox/` |
| `vite.tauri.config.ts` | Desktop/Mobile Tauri | `dist/tauri/` |

## Pourquoi Tauri et pas Flutter ou Expo

> **ADR-001** — Choix du framework cross-platform (2025-01)

### Contexte

SocialGlowz affiche des réseaux sociaux dans des WebViews natives et injecte des scripts dans ces WebViews (grayscale, protection copie, session par profil). L'UI est écrite en Vue.js car le projet a démarré comme une extension Chrome.

### Décision : Tauri 2

### Alternatives rejetées

**Flutter (Dart)**
- Réécriture complète de l'UI en Dart — double codebase à maintenir
- WebView support limité : pas d'injection de scripts JS, pas de contrôle cookie granulaire
- Impossible de partager du code avec l'extension Chrome (Dart ≠ JS)
- Pas de support extension navigateur

**Expo / React Native (React)**
- Réécriture complète en React — double codebase à maintenir
- WebView : le package `react-native-webview` ne supporte pas l'injection `document_start`, ni le contrôle cookie natif
- Pas de support desktop natif sans Electron (lourd, ~200 MB)
- Pas de support extension navigateur

**Electron**
- Supporte le web mais embarque un Chromium complet (~200 MB par app)
- Pas de support mobile
- Mémoire excessive pour une app qui affiche déjà des WebViews

### Pourquoi Tauri gagne

| Critère | Tauri | Flutter | Expo/RN | Electron |
|---|---|---|---|---|
| Réutilise le code Vue.js existant | Oui | Non (Dart) | Non (React) | Oui |
| Extension navigateur | Oui (même code) | Non | Non | Non |
| WebView native + injection JS | Oui | Limité | Limité | Oui |
| Taille binaire | ~5 MB | ~15 MB | ~30 MB | ~200 MB |
| Support mobile | Tauri 2 | Natif | Natif | Non |
| Support desktop | Natif | Natif | Limité | Natif |
| Contrôle cookies WebView | Natif (Kotlin/Swift) | Non | Non | Oui |

**En résumé** : Tauri est le framework qui permet de garder une codebase Vue.js unique pour les cibles desktop et mobile, avec un accès bas-niveau au WebView natif pour l'injection de scripts et la gestion des cookies — ce qui est le coeur fonctionnel de SocialGlowz.

## Stack technique

- **Frontend** : Vue 3, PrimeVue, Tailwind CSS, DaisyUI, Pinia
- **Auth** : Convex Auth (`@auth/core`, `@convex-dev/auth`)
- **Backend** : Convex (serverless)
- **i18n** : vue-i18n
- **Build** : Vite 6, pnpm
- **Desktop/Mobile** : Tauri 2 (Rust + Kotlin/Swift)

## Contrat de parité extension (Chrome/Firefox)

- Les surfaces extension actives (popup, side panel Chrome, options, setup install/update/dashboard) exposent un lanceur SocialGlowz réel basé sur le même catalogue réseaux et les mêmes profils.
- Le mode extension ouvre les réseaux dans des onglets navigateur (`tabs.create`) au lieu de WebViews natives Tauri.
- Les liens personnalisés sont validés strictement:
  - uniquement `https://`;
  - URL normalisée;
  - rejet de `javascript:`, `data:`, `file:`, `chrome:`, `moz-extension:`;
  - rejet des credentials intégrés (`user:pass@host`).
- L’extension n’injecte plus d’iframe global sur `*://*/*` par défaut.
- Le side panel est activé uniquement pour Chrome; Firefox reçoit un fallback popup/options/setup sans promesse side panel.
- Limites explicites en extension:
  - pas d’isolation de session native par profil (cookies/localStorage partagés du navigateur),
  - pas de haptics/barre Android native,
  - pas de backup natif Tauri.

## Sécurité auth Android (hardening)

- Les callbacks OAuth Android sont traités uniquement via deep links autorisés (`socialglowz://auth-callback/oauth` et `https://socialglowz.com/auth/callback`).
- Chaque callback passe par une validation native Rust (`validate_android_oauth_callback`) avant d'être accepté:
  - schéma + host allowlist,
  - `state` obligatoire et présent dans une requête OAuth pending créée par l'app,
  - `nonce` vérifié contre cette requête pending quand présent,
  - TTL max 5 minutes,
  - anti-rejeu (`state` consommé une seule fois).
- Les callbacks rejetés déclenchent un signal Sentry anonymisé si le SDK Sentry est exposé au runtime.
- Le lock de session n'autorise plus la création d'un PIN depuis l'écran verrouillé:
  - PIN préconfiguré requis pour déverrouiller,
  - sinon retour login obligatoire.
- L'écran d'erreur de bootstrap auth n'injecte plus le message via `innerHTML` (rendu DOM via `textContent`).
- Sur Android, les WebViews natives isolent les sessions par profil et réseau avec la clé `${profileId}-${networkId}`:
  - quand Android WebKit `MULTI_PROFILE` est disponible, chaque session utilise un profil WebKit natif distinct et peut rester chaude dans un pool borné,
  - cookies et snapshots `localStorage` sont persistés par origine,
  - CinderReels déclare explicitement `https://cinderreels.com` car son auth utilise `localStorage`,
  - les autres réseaux utilisent le même mécanisme via l'origine de leur URL principale,
  - les origins additionnelles servent aux réseaux dont l'auth ou l'app passe par plusieurs domaines.
- Fallback Android: si `MULTI_PROFILE` n'est pas supporté par le WebView du device, l'app revient au mode single-WebView avec snapshots cookies/localStorage; le multi-WebView chaud n'est pas activé.
- Limites connues du fallback: IndexedDB, CacheStorage, service workers, cache HTTP global WebView et credential store système ne sont pas isolés par les snapshots.
- Détails: [shipflow_data/technical/android-webview-session-isolation.md](shipflow_data/technical/android-webview-session-isolation.md).

## Variables d'environnement

```env
VITE_CONVEX_URL=              # URL Convex (obligatoire en runtime front)
VITE_GMAIL_CLIENT_ID=         # Google API (optionnel, Gmail)
VITE_GMAIL_API_KEY=           # Google API (optionnel, Gmail)
CONVEX_DEPLOYMENT=            # Déploiement Convex (local/dev), si utilisé
VITE_CONVEX_SITE_URL=         # Site URL Convex (optionnel)
SOCIALGLOWZ_SUITE_BRIDGE_URL= # Endpoint suite bridge (suite entitlement ledger)
SOCIALGLOWZ_SUITE_BRIDGE_SECRET= # Secret serveur->serveur pour bridge suite
```

## Scripts

```bash
# Développement
pnpm dev:chrome              # Dev extension Chrome
pnpm dev:firefox             # Dev extension Firefox
pnpm tauri:dev               # Dev desktop Tauri

# Build
pnpm build:chrome            # Build extension Chrome
pnpm build:firefox           # Build extension Firefox
pnpm tauri:bundle            # Build desktop

# Qualité
pnpm test:once                # Vitest (mode CI)
pnpm test                     # Vitest (watch)
pnpm lint                    # ESLint
pnpm format                  # Prettier
pnpm typecheck               # TypeScript
pnpm exec tsc -p convex/tsconfig.json --noEmit  # Typecheck Convex
```

## Déploiement

### Mobile (CI)
Voir [shipflow_data/workflow/tauri-mobile.md](shipflow_data/workflow/tauri-mobile.md) pour le workflow GitHub Actions.

### Extensions
Les fichiers `.zip` sont générés dans `dist/` pour upload sur le Chrome Web Store et Firefox Add-ons.
