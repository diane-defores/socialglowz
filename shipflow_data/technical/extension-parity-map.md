---
artifact: documentation
metadata_schema_version: "1.0"
artifact_version: "1.0.0"
project: "socialglowz"
created: "2026-05-25"
updated: "2026-05-25"
status: active
source_skill: sf-start
scope: extension-parity
owner: "Diane"
confidence: medium
risk_level: high
security_impact: yes
docs_impact: yes
depends_on:
  - "shipflow_data/workflow/specs/extension-tauri-feature-parity.md"
supersedes: []
linked_systems:
  - "manifest.config.ts"
  - "manifest.chrome.config.ts"
  - "manifest.firefox.config.ts"
  - "src/platform/capabilities.ts"
  - "src/platform/extensionNetworkLauncher.ts"
  - "src/ui/action-popup/pages/index.vue"
  - "src/ui/side-panel/pages/index.vue"
  - "src/ui/options-page/pages/index.vue"
  - "src/ui/setup/pages/install.vue"
  - "src/ui/setup/pages/update.vue"
  - "src/ui/setup/pages/SocialFlow.vue"
  - "src/content-script/index.ts"
  - "src/devtools/index.ts"
  - "src/offscreen/index.ts"
evidence:
  - "manifest.config.ts no longer injects content scripts or devtools by default."
  - "manifest.chrome.config.ts adds side_panel + sidePanel permission only for Chrome."
  - "manifest.firefox.config.ts keeps Firefox-compatible baseline without side panel fields."
  - "src/platform/extensionNetworkLauncher.ts enforces normalized HTTPS URLs and blocks embedded credentials."
  - "Extension UI surfaces now render ExtensionParitySurface instead of scaffold/demo views."
next_step: "/sf-verify extension-tauri-feature-parity"
---

# Extension Parity Map

## Contract

Cette carte décrit la parité réelle entre la cible extension (Chrome/Firefox) et la cible native Tauri.

## Capability Matrix

| Capability | Tauri | Extension Chrome | Extension Firefox | Permissions / API | Notes |
|---|---|---|---|---|---|
| Social catalog launcher (`src/config/socialNetworks.ts`) | Native WebView open | Browser tab open (`tabs.create`) | Browser tab open (`tabs.create`) | `tabs` | URL validée/normalisée en HTTPS avant ouverture. |
| Custom links launcher | Native open via app shell | HTTPS-only + credentials blocked | HTTPS-only + credentials blocked | `tabs` | Rejette `javascript:`, `data:`, `file:`, `chrome:`, `moz-extension:` et `user:pass@`. |
| Toolbar launcher | N/A | Oui (`action` popup) | Oui (`action` popup) | `action` | Plus de scaffold UI. |
| Side panel launcher | N/A | Oui (Chrome uniquement) | Non | `sidePanel` (Chrome) | Firefox n’expose pas de promesse side panel. |
| Options/settings surface | N/A | Oui | Oui | `options_page`, `storage` | Contrôle profil, langue, thème, liens, limitations. |
| Install/update setup flows | N/A | Oui (`/setup/install`, `/setup/update`, `/setup/social-flow`) | Oui | `tabs` + background install/update tab | Routes orientées produit, pas de texte démo. |
| Native WebView orchestration | Oui | Non | Non | N/A | Dégradé explicite: onglets navigateur classiques seulement. |
| Per-profile native session isolation | Oui (Android/Tauri) | Non | Non | N/A | Dégradé explicite: pas d’isolation cookies/localStorage par profil en extension. |
| Native haptics + Android bottom bar | Oui | Non | Non | N/A | Dégradé explicite. |
| Native backup/restore (.sfbak, filesystem natif) | Oui | Non | Non | N/A | Dégradé explicite dans UI extension. |
| Global injected iframe on every site | N/A | Désactivé par défaut | Désactivé par défaut | Aucune `content_scripts` active | Le content script est neutre; pas d’injection globale active. |
| Devtools/offscreen scaffold | N/A | Non exposé en production | Non exposé en production | Pas de `devtools_page` active | Entrées conservées mais no-op/quarantaine. |

## Security Notes

- Le launcher extension est un boundary de sécurité: il n’ouvre que des URLs HTTPS validées.
- Les erreurs UI n’exposent pas de token/cookie/payload backup ni URL sensible complète.
- Le manifeste baseline retire les permissions non nécessaires (`background`, `sidePanel` hors Chrome).

## Proof Path

- Tests ciblés:
  - `src/platform/capabilities.test.ts`
  - `src/platform/extensionNetworkLauncher.test.ts`
  - `src/platform/manifest.test.ts`
- Build/lint:
  - `pnpm test:once`
  - `pnpm typecheck`
  - `pnpm build:chrome`
  - `pnpm build:firefox`
  - `pnpm lint:manifest`
  - `python3 /home/claude/shipflow/tools/shipflow_metadata_lint.py shipflow_data/technical/extension-parity-map.md`
