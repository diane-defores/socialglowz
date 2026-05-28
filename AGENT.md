---
artifact: technical_guidelines
metadata_schema_version: "1.0"
artifact_version: "1.0.1"
project: "socialglowz"
created: "2026-04-26"
updated: "2026-05-25"
status: reviewed
source_skill: sf-docs
scope: agent-entrypoint
owner: "Diane"
confidence: medium
risk_level: low
security_impact: none
docs_impact: yes
linked_systems:
  - "README.md"
  - "shipflow_data/technical/context.md"
  - "shipflow_data/technical/context-function-tree.md"
  - "src/ui/setup/pages/SocialGlowz/main.ts"
  - "src/ui/setup/pages/SocialGlowz/App.vue"
  - "src-tauri/src/lib.rs"
  - "convex/schema.ts"
  - "vite.config.ts"
  - "vite.chrome.config.ts"
  - "vite.firefox.config.ts"
  - "vite.tauri.config.ts"
depends_on: []
supersedes: []
evidence:
  - "README.md"
  - "package.json"
  - "src/ui/setup/pages/SocialGlowz/main.ts"
  - "src/ui/setup/pages/SocialGlowz/App.vue"
  - "src/ui/setup/pages/SocialGlowz/router/index.ts"
  - "convex/auth.config.ts"
  - "convex/schema.ts"
  - "src-tauri/src/lib.rs"
  - "src-tauri/tauri.conf.json"
  - "manifest.config.ts"
next_review: "2026-07-26"
next_step: "/sf-docs audit AGENT.md"
---

# AGENT

## Purpose

Ce fichier sert de point d'entrée rapide pour toute action dans `socialglowz`.
Il permet de lire les bons documents avant de parcourir le code.

## Read Order

1. Lire `README.md` pour la vue d'ensemble des plateformes.
2. Lire `shipflow_data/technical/context.md` pour la carte opérationnelle et les flux.
3. Lire `shipflow_data/technical/context-function-tree.md` avant toute tâche sur modules principaux.
4. Lire `shipflow_data/technical/context.md` quand la question touche aux frontières techniques.
5. Lire `shipflow_data/technical/context.md` encore une fois avant une tâche de maintenance transversale.
6. Considérer `archive/` comme matière historique uniquement; ne pas l'utiliser comme source de vérité d'implémentation.

## Archive Policy

- `archive/` est une zone de preuve historique (snapshots, expérimentations, backups de contenu).
- Les traces `socialflow` présentes dans `archive/` sont conservées tel quel.
- Le périmètre actif du projet est hors `archive/` (code, config, données, docs courantes).
- Avant un tri de contenu ou migration de marque, traiter `archive/` séparément si nécessaire.

## What This Repo Is

- Une seule base Vue 3 qui produit :
  - une extension navigateur (Chrome + Firefox)
  - un shell desktop Tauri 2 (Windows/Mac/Linux)
  - une build mobile Android via plugin Android WebView
- L'authentification et le sync principal passent par Convex.
- Le coeur métier social se trouve dans `src/` et `src/ui/setup/pages/SocialGlowz/`.

## Route by Task

- Si la tâche concerne l'expérience extension (manifest, background, content script, popup, side panel), ouvrir :
  - `manifest.config.ts`
  - `manifest.chrome.config.ts`
  - `manifest.firefox.config.ts`
  - `shipflow_data/technical/extension-parity-map.md`
  - `src/platform/capabilities.ts`
  - `src/platform/extensionNetworkLauncher.ts`
  - `src/background/index.ts`
  - `src/content-script/index.ts`
  - `src/ui/*/index.ts`
  - puis `shipflow_data/technical/context-function-tree.md`
- Si la tâche concerne la logique métier principale SocialGlowz :
  - `src/ui/setup/pages/SocialGlowz/main.ts`
  - `src/ui/setup/pages/SocialGlowz/App.vue`
  - `src/ui/setup/pages/SocialGlowz/router/index.ts`
  - `src/stores`
  - `src/lib`
- Si la tâche concerne un flow natif desktop/mobile :
  - `src-tauri/src/lib.rs`
  - `src-tauri/tauri.conf.json`
  - `src-tauri/plugins/android-webview`
  - puis `shipflow_data/technical/context.md`
- Si la tâche concerne le backend sync :
  - `convex/*`
  - `src/lib/convex*.ts`
  - `src/lib/cloudSync*`
  - puis `shipflow_data/technical/context.md`
- Si la tâche concerne build/déploiement :
  - `vite.config.ts`
  - `vite.chrome.config.ts`
  - `vite.firefox.config.ts`
  - `vite.tauri.config.ts`
  - `package.json`

## Invariants

- Le même store et utilitaires partagés restent la source de vérité métier.
- Les métadonnées des réseaux webview intégrés côté UI se gèrent depuis `src/config/socialNetworks.ts`.
- Pour ajouter un réseau intégré, utiliser la skill projet locale `.claude/skills/socialglowz-add-network/SKILL.md`.
- Les invocations natives Tauri sont encapsulées dans `@tauri-apps/api/core.invoke` depuis le front.
- Toute mutation du stockage de session passe par `convexAuth` / `cloudSync` ou les commandes Rust dédiées.
- `convex` est le contrat de données synchronisées quand VITE_CONVEX_URL est configuré.

## Boundaries & Responsibilities

- `src/ui/setup/pages/SocialGlowz/` : app principale SocialGlowz (routing, UI, vues réseaux).
- `src/` : services et stores partagés (état, auth, sync, utilitaires).
- `src-tauri/` : orchestration WebView, commandes natives, plugins et persistance sessions.
- `src/ui/*` (hors SocialGlowz) : shells historiques de l'extension.
- `convex/` : schema, queries/mutations et auth backend.

## Editing Order

- Changer un comportement d'app SocialGlowz → `src/ui/setup/pages/SocialGlowz/*` puis `src/stores/*`/`src/lib/*`.
- Changer des services partagés → `src/services/*`, `src/utils/*`, `src/composables/*` puis mettre à jour SocialGlowz.
- Modifier les commandes natives → `src-tauri/src/lib.rs` et tests manuels sur desktop/mobile après.
- Changer des règles de sync/auth → `convex/*` + `src/lib/cloud*` + `src/lib/convexAuth.ts`.
