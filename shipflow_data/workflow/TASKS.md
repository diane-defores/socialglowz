# Tasks — socialglowz

> **Priority:** 🔴 P0 blocker · 🟠 P1 high · 🟡 P2 normal · 🟢 P3 low · ⚪ deferred
> **Status:** 📋 todo · 🔄 in progress · ✅ done · ⛔ blocked · 💤 deferred

---

## Active

🟢 [socialglowz] task: Compact mobile network list into four-column square tiles | status: done | area: mobile-ui | id: sg-mobile-network-grid-compact
🟢 [socialglowz] task: Remove stale web/Vercel target references from project docs and scripts | status: done | area: docs | id: sg-docs-remove-stale-web-target
🟢 [socialglowz] task: Fix extension dependency audit findings: remove unused vite-plugin-pwa path or patch its transitive Babel/brace-expansion advisories, upgrade Convex/ws safely, and re-run extension builds | status: done | area: deps | id: sg-extension-deps-audit-fixes
🟡 [socialglowz] task: Clean unused extension/dev dependencies and package metadata gaps: vuefire, webext-bridge, unplugin-imagemin, get-installed-browsers, unused icon packs, license, engines, and documented overrides | status: todo | area: deps | id: sg-extension-deps-hygiene
🟠 [socialglowz] task: Run sf-verify for extension parity: Chrome popup/side panel/options/install/update, Firefox popup/options, web-ext innerHTML warnings, and targeted Tauri regression proof | status: todo | area: extension-qa | id: sg-extension-parity-verify
🟡 [socialglowz] task: Validate compact mobile network grid on narrow and standard device widths | status: todo | area: mobile-ui | id: sg-mobile-network-grid-qa

---

## Setup

| Pri | Task | Status |
|-----|------|--------|
| 🟠 | [Task backlog was migrated; validate project tracker entries] | ✅ done |
| 🟡 | [Quand un vrai flow OAuth existe, tester le callback positif Android: connexion lancée depuis l'app, state/nonce attendus, callback accepté, faux deep links toujours rejetés] | 💤 deferred |

---

## Historical completed work

(Keep existing project history entries here if available from legacy tracker.)

- 2026-05-23: Completed Android WebView storage isolation for CinderReels profile sessions. Includes per-session cookies/localStorage snapshots, declarative network storage origins, bottom bar propagation, backup/delete wiring, docs, and user-reported Android APK A/B/A QA pass.

## Audit Findings

- 2026-05-11: ShipFlow layout aligned. Legacy root docs were removed or moved into `shipflow_data/`; specs, bugs, research, competitors/inspirations, and audit log now use canonical paths.
- 2026-05-11: Task notes were updated after full layout migration; site assets and legacy root web artifacts were consolidated under shipflow_data/ and site split finalized.
