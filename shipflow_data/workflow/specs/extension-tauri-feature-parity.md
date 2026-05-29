---
artifact: spec
metadata_schema_version: "1.0"
artifact_version: "1.0.0"
project: "socialglowz"
created: "2026-05-25"
created_at: "2026-05-25 17:59:45 UTC"
updated: "2026-05-27"
updated_at: "2026-05-28 18:18:00 UTC"
status: ready
source_skill: sf-spec
source_model: "GPT-5 Codex"
scope: "feature"
owner: "Diane"
user_story: "En tant qu'utilisatrice SocialGlowz qui installe l'extension navigateur, je veux retrouver les capacités utiles de l'application Tauri dans une expérience extension claire, afin de pouvoir piloter mes réseaux depuis Chrome ou Firefox sans tomber sur des écrans démo ou des promesses impossibles."
confidence: "high"
risk_level: "high"
security_impact: "yes"
docs_impact: "yes"
linked_systems:
  - "manifest.config.ts"
  - "manifest.chrome.config.ts"
  - "manifest.firefox.config.ts"
  - "vite.chrome.config.ts"
  - "vite.firefox.config.ts"
  - "src/background/index.ts"
  - "src/content-script/index.ts"
  - "src/ui/action-popup/"
  - "src/ui/side-panel/"
  - "src/ui/options-page/"
  - "src/ui/content-script-iframe/"
  - "src/ui/setup/pages/SocialGlowz/"
  - "src/stores/"
  - "src/lib/cloudSync.ts"
  - "src/lib/convexAuth.ts"
  - "src-tauri/src/lib.rs"
depends_on:
  - artifact: "README.md"
    artifact_version: "unknown"
    required_status: "active"
  - artifact: "AGENT.md"
    artifact_version: "1.0.1"
    required_status: "reviewed"
  - artifact: "CLAUDE.md"
    artifact_version: "1.0.1"
    required_status: "active"
  - artifact: "shipflow_data/technical/context.md"
    artifact_version: "1.1.1"
    required_status: "reviewed"
  - artifact: "shipflow_data/technical/context-function-tree.md"
    artifact_version: "1.0.0"
    required_status: "reviewed"
supersedes: []
evidence:
  - "manifest.config.ts declares MV3 action popup, content script, side_panel, devtools_page, permissions storage/tabs/background/sidePanel, and matches *://*/*."
  - "manifest.firefox.config.ts removes background and sidePanel permissions but leaves the shared side_panel and content script shape inherited unless build tooling drops unsupported fields."
  - "src/ui/action-popup/pages/index.vue still contains template/demo copy and links to original scaffold docs/support."
  - "src/ui/side-panel/pages/index.vue and src/ui/content-script-iframe/pages/index.vue are playground surfaces."
  - "src/ui/options-page/pages/index.vue is a scaffold options page using useOptionsStore/test settings, not SocialGlowz settings."
  - "src/background/index.ts only clears local storage on install and opens setup install/update pages."
  - "src/content-script/index.ts injects an iframe into every matched page."
  - "src/ui/setup/pages/SocialGlowz/App.vue is the actual Tauri application shell with onboarding, profiles, settings, sync, network launcher, and native webview orchestration."
  - "Chrome official docs: chrome.sidePanel requires sidePanel permission and is Chrome 114+ MV3+."
  - "MDN WebExtensions docs: Firefox WebExtensions differ from Chrome APIs and support browser/chrome namespace compatibility with browser-specific API differences."
next_step: "/sf-start extension-tauri-feature-parity"
---

# Spec: Extension Tauri Feature Parity

## Title

Extension Tauri Feature Parity

## Status

Ready. The browser extension surface has security and store-review implications, but the spec now defines explicit browser-mode fallbacks, permission constraints, degraded native-feature behavior, and validation gates before implementation.

## User Story

En tant qu'utilisatrice SocialGlowz qui installe l'extension navigateur, je veux retrouver les capacités utiles de l'application Tauri dans une expérience extension claire, afin de pouvoir piloter mes réseaux depuis Chrome ou Firefox sans tomber sur des écrans démo ou des promesses impossibles.

## Minimal Behavior Contract

When the user opens the SocialGlowz extension through the toolbar popup, side panel, or install/update page, the extension must show a coherent SocialGlowz control surface based on the same catalog, profiles, settings, auth, cloud sync, onboarding, and network-launching concepts as the Tauri app. When a Tauri-native feature cannot exist inside a browser extension, the extension must provide the closest browser-native behavior or clearly label the feature as unavailable, without silently pretending that isolated native WebViews, Android bottom bars, haptics, or per-profile native session storage exist. The easiest edge case to miss is session isolation: extension UI can reuse SocialGlowz state, but Chrome/Firefox network tabs cannot safely guarantee the same per-profile cookie isolation as Tauri native WebViews.

## Success Behavior

- Opening the extension toolbar action shows SocialGlowz, not scaffold copy.
- Opening the Chrome side panel shows the same SocialGlowz control surface adapted to side-panel width.
- Firefox build does not expose unsupported Chrome-only side panel behavior as if it were available.
- Install/update flows route users into the SocialGlowz extension onboarding or dashboard, not generic "Installed" scaffold text.
- Network tiles use `src/config/socialNetworks.ts` and open a deterministic browser-native target for each network.
- Auth, Convex sync, profiles, hidden networks, custom links, friends filter settings, theme, language, text zoom, backup/restore availability, and onboarding are either functional or explicitly marked as unavailable in extension mode.
- Tauri-only features are guarded by capability detection and never call `@tauri-apps/api/core.invoke` from extension contexts.
- Content script injection is disabled, narrowed, or explicitly gated so SocialGlowz does not inject a playground iframe into every website by default.
- Chrome and Firefox builds pass their target build commands and manifest lint where available.

## Error Behavior

- If Convex is not configured, extension UI must continue in local/offline mode with the same graceful fallback expected in Tauri.
- If a network cannot be opened because the URL is invalid, blocked, or unsupported, show a visible error state and preserve the current profile/network state.
- If a browser API is unavailable, the UI must disable the related control and show an extension-mode explanation.
- If the extension cannot provide per-profile session isolation, it must not expose destructive "clear this profile/network session" controls as if they were equivalent to Tauri native storage deletion.
- If Chrome side panel is unavailable, the extension must fall back to popup/setup-page behavior.
- If Firefox lacks a parity capability, Firefox must receive a Firefox-specific fallback instead of a broken inherited Chrome manifest field.

## Problem

The repository still builds Chrome and Firefox extensions, but the active extension surfaces are largely scaffold-era UI:

- toolbar popup: generic "Hello there" demo with old documentation/support links;
- side panel: "Side Panel Playground";
- options page: generic options/test store, not SocialGlowz settings;
- content iframe: "Content Script UI Playround";
- devtools panel: "My Panel" demo;
- background: install/update tab opening only;
- content script: iframe injection on `*://*/*`.

Meanwhile, the actual SocialGlowz product experience lives in `src/ui/setup/pages/SocialGlowz/` and Tauri native commands/plugins. The extension can share much of the Vue/Pinia/Convex UI, but it cannot replicate native child WebViews, Android WebKit profile isolation, Android bottom navigation, haptics, native session deletion, or Tauri backup file behavior one-to-one.

## Solution

Create a browser-extension mode for SocialGlowz that reuses the main SocialGlowz app shell and stores where possible, replaces Tauri-native WebView/session operations with browser-extension adapters, and removes or quarantines demo/scaffold extension surfaces. Define feature parity as "same user intent and state model where browser APIs allow, explicit degraded behavior where they do not."

## Scope In

- Audit and classify every extension entrypoint:
  - `action` popup;
  - Chrome side panel;
  - Firefox extension build;
  - options page;
  - setup install/update pages;
  - content script and iframe;
  - background service worker/script;
  - devtools panel.
- Create an extension capability map that distinguishes:
  - shared Vue app features;
  - browser-extension equivalents;
  - Chrome-only features;
  - Firefox fallback behavior;
  - Tauri-only features.
- Reuse `src/ui/setup/pages/SocialGlowz/` or extract shared SocialGlowz extension-safe components instead of rebuilding a separate app.
- Add an extension adapter for network launching:
  - browser tab/window opening for social networks;
  - optional side panel control surface;
  - no false native WebView isolation claims.
- Replace scaffold UI in popup, side panel, options, and setup pages.
- Remove, narrow, or gate the global content-script iframe injection.
- Align manifests and permissions with the actual feature set.
- Add focused build and manifest validation for Chrome and Firefox.
- Update README/AGENT/technical docs if the extension parity contract changes platform status.

## Scope Out

- Exact native WebView parity for browser extension tabs.
- Android WebKit `MULTI_PROFILE` parity inside Chrome/Firefox extensions.
- Browser-profile-level cookie isolation automation in Chrome.
- Shipping to Chrome Web Store or Firefox Add-ons in this spec.
- Rewriting the Tauri native host.
- Implementing new social-network automation that violates network terms, browser store policy, or user consent boundaries.
- Reintroducing a public Vercel/web SPA target.

## Constraints

- `src/ui/setup/pages/SocialGlowz/` remains the source of truth for product UX when a feature is shared.
- Extension mode must not import or execute Tauri IPC unless `__TAURI_INTERNALS__` exists.
- Extension mode must not inject UI into arbitrary websites by default.
- Permissions must be minimum necessary for the chosen extension behavior.
- Chrome and Firefox differences must be represented explicitly; Firefox must not inherit broken Chrome-only assumptions.
- Session isolation must be described honestly. If a feature cannot isolate cookies/localStorage per SocialGlowz profile in the browser, the UI must say so or avoid the claim.
- Convex Auth and sync must remain optional and must not crash offline/local-only mode.
- Existing Tauri behavior must not regress.
- Custom-link URLs are untrusted input. The extension launcher must accept only normalized `https://` URLs unless a later spec explicitly authorizes another scheme with a security review.
- Extension logs and user-visible errors must not include auth tokens, cookies, profile backup payloads, Convex credentials, or full custom URLs when those URLs may contain sensitive query parameters.

## Dependencies

- Local dependencies:
  - Vue 3, Pinia, PrimeVue, vue-router, vue-i18n.
  - CRXJS Vite plugin for extension builds.
  - Convex Auth and sync wrappers in `src/lib/convexAuth.ts` and `src/lib/cloudSync.ts`.
  - Social network catalog in `src/config/socialNetworks.ts`.
- External documentation freshness:
  - `fresh-docs checked`: Chrome official extension API reference and Side Panel API docs were consulted. Chrome Side Panel API is MV3 and Chrome 114+ and requires `"sidePanel"` permission.
  - `fresh-docs checked`: MDN WebExtensions API and manifest docs were consulted for Firefox/WebExtensions compatibility and browser/Chrome namespace differences.
- Official source anchors:
  - Chrome Extensions API reference: https://developer.chrome.com/docs/extensions/reference/api
  - Chrome Side Panel API: https://developer.chrome.com/docs/extensions/reference/api/sidePanel
  - MDN WebExtensions API: https://developer.mozilla.org/en-US/Add-ons/WebExtensions/API
  - MDN manifest.json: https://developer.mozilla.org/en-US/Add-ons/WebExtensions/manifest.json

## Invariants

- The social network catalog stays centralized in `src/config/socialNetworks.ts`.
- `profiles`, `customLinks`, `friendsFilter`, `theme`, onboarding, and auth/sync state keep their existing stores unless a spec-ready task explicitly extracts adapters.
- Tauri WebView commands remain encapsulated in Tauri-only paths.
- Browser-extension state writes use extension-safe storage or shared persisted Pinia state intentionally; accidental split-brain between `chrome.storage` scaffold stores and SocialGlowz stores is not allowed.
- Extension builds must be deterministic for Chrome and Firefox.
- User-facing copy must not promise native session isolation in extension mode.

## Links & Consequences

- Product: the extension becomes a real SocialGlowz distribution surface again instead of a scaffold artifact.
- Security/privacy: broad content-script injection and excessive permissions can create review and user-trust risk; narrowing is required.
- UX: popup and side panel need compact layouts, while setup page can host a larger dashboard.
- Architecture: likely needs an adapter layer such as `src/platform/extension/*` or `src/ui/setup/pages/SocialGlowz/platform/*` to keep browser-extension behavior separate from Tauri IPC.
- Documentation: platform matrix must distinguish exact Tauri features from extension equivalents.
- QA: Chrome and Firefox must be tested separately because side panel and manifest compatibility differ.
- Store policy: permission changes and content-script behavior affect review posture.

## Documentation Coherence

Update these docs when implementation begins or changes platform status:

- `README.md`: extension capabilities, commands, and known limitations.
- `AGENT.md` / `AGENTS.md`: route-by-task instructions for extension parity work.
- `CLAUDE.md`: runtime targets and command guidance.
- `shipflow_data/technical/context.md`: platform matrix and extension flow.
- `shipflow_data/technical/context-function-tree.md`: extension entrypoint tree if the app shell changes.
- `CHANGELOG.md`: user-facing extension parity changes.

## Edge Cases

- Chrome popup closes when focus changes; long workflows must move to side panel or setup tab.
- Chrome side panel is not available in Firefox; Firefox needs popup/setup fallback.
- Social networks commonly block iframe embedding; extension parity must not rely on iframe-rendering third-party networks.
- Opening a network tab uses the user's browser session, not SocialGlowz per-profile native WebView storage.
- Multiple SocialGlowz profiles may point to the same browser cookies in extension mode.
- Content scripts on every website create performance, privacy, and review risk.
- Firefox manifest may reject Chrome-specific keys or permissions depending on build output.
- `chrome` namespace and `browser` namespace behavior differs across browsers; use `webextension-polyfill` or a project wrapper if shared logic needs promises/cross-browser consistency.
- Extension pages have CSP and extension-origin constraints that may affect Convex/Auth flows.
- Backup/restore file handling differs between browser extension downloads/upload and Tauri native filesystem dialogs.
- A malformed or hostile custom link such as `javascript:`, `data:`, `file:`, `chrome:`, `moz-extension:`, or a URL containing embedded credentials must be rejected before any browser API call.

## Implementation Tasks

- [ ] Task 1: Produce an extension capability matrix.
  - File: `shipflow_data/technical/extension-parity-map.md`
  - Action: Create a technical note listing Tauri feature, current extension status, browser-equivalent behavior, unsupported/degraded status, Chrome support, Firefox support, permissions, and proof path.
  - User story link: Defines honest parity before implementation.
  - Depends on: None.
  - Validate with: `python3 /home/claude/shipflow/tools/shipflow_metadata_lint.py shipflow_data/technical/extension-parity-map.md`.
  - Notes: Include native WebView/session isolation as degraded/unsupported for browser extension mode.

- [ ] Task 2: Add platform capability helpers.
  - File: `src/platform/capabilities.ts`
  - Action: Create shared capability detection for `isTauri`, `isExtension`, `isChromeExtension`, `isFirefoxExtension`, `supportsSidePanel`, `supportsNativeWebview`, `supportsNativeSessionIsolation`, `supportsHaptics`, and `supportsNativeBackup`.
  - User story link: Prevents Tauri-only calls from leaking into extension mode.
  - Depends on: Task 1.
  - Validate with: Unit tests for capability detection under mocked `window`, `chrome`, and Tauri globals.
  - Notes: Do not import `@tauri-apps/api` from this helper.

- [ ] Task 3: Extract an extension-safe SocialGlowz shell.
  - File: `src/ui/setup/pages/SocialGlowz/App.vue`
  - Action: Separate product UI from Tauri host behavior so popup/side panel/setup extension pages can render a compact SocialGlowz control surface without native WebView commands.
  - User story link: Replaces scaffold UI with real SocialGlowz UI.
  - Depends on: Task 2.
  - Validate with: `pnpm typecheck` and a focused Vue test or smoke build for `build:chrome`.
  - Notes: The extracted shell may live under `src/ui/setup/pages/SocialGlowz/components/ExtensionShell.vue` if cleaner.

- [ ] Task 4: Implement browser network launcher adapter.
  - File: `src/platform/extensionNetworkLauncher.ts`
  - Action: Add a browser-extension launcher that opens network URLs via `chrome.tabs.create` / WebExtensions-compatible wrapper and records active network intent without claiming native WebView state.
  - User story link: Lets extension users open the same network catalog.
  - Depends on: Tasks 1-2.
  - Validate with: Mocked unit tests for URL validation and tab-open calls; Chrome extension smoke.
  - Notes: Validate URLs from built-in catalog and custom links before opening.

- [ ] Task 5: Replace toolbar popup scaffold.
  - File: `src/ui/action-popup/pages/index.vue`
  - Action: Replace demo hero with a compact SocialGlowz launcher: active profile, top networks grid, settings shortcut, open full dashboard/setup page, and account/sync status.
  - User story link: Toolbar action becomes useful immediately.
  - Depends on: Tasks 2-4.
  - Validate with: Chrome popup smoke and Firefox popup smoke.
  - Notes: Keep popup flows short because browser popups close easily.

- [ ] Task 6: Replace Chrome side panel scaffold.
  - File: `src/ui/side-panel/pages/index.vue`
  - Action: Render the SocialGlowz control surface optimized for persistent side-panel width; include network launcher, active profile switcher, settings, and sync status.
  - User story link: Provides persistent browser-native control surface closest to the Tauri sidebar/mobile home.
  - Depends on: Tasks 2-4.
  - Validate with: Chrome side panel smoke; Firefox build must not expose broken side panel entry.
  - Notes: Side panel is Chrome-specific per current official docs; Firefox must use fallback.

- [ ] Task 7: Replace options page scaffold with SocialGlowz settings.
  - File: `src/ui/options-page/pages/index.vue`
  - Action: Remove `useOptionsStore` demo settings and expose extension-safe SocialGlowz settings: language, theme, account/sync, profile management link, backup/export limitations, privacy/session notes.
  - User story link: Extension settings match the product.
  - Depends on: Tasks 1-3.
  - Validate with: Chrome and Firefox options page smoke.
  - Notes: Do not expose Tauri-only haptic/native WebView controls unless disabled with explanation.

- [ ] Task 8: Replace install/update pages.
  - File: `src/ui/setup/pages/install.vue`, `src/ui/setup/pages/update.vue`
  - Action: Replace generic install/update copy with SocialGlowz onboarding entry, extension limitations, and calls to open popup/side panel/setup dashboard.
  - User story link: First-run path becomes product-specific.
  - Depends on: Tasks 3-6.
  - Validate with: Simulated install/update routes in built extension.
  - Notes: Preserve background install/update routing unless a better route is implemented.

- [ ] Task 9: Gate or remove global content-script iframe injection.
  - File: `manifest.config.ts`, `src/content-script/index.ts`, `src/ui/content-script-iframe/pages/index.vue`
  - Action: Disable the default `*://*/*` iframe injection, or gate it behind explicit host permissions and user action if a future overlay feature is intentionally scoped.
  - User story link: Protects user trust and store-review posture.
  - Depends on: Task 1.
  - Validate with: Manifest diff review and `pnpm build:chrome` / `pnpm build:firefox`.
  - Notes: If no concrete overlay feature exists, remove the content script from active manifest.

- [ ] Task 10: Remove or quarantine devtools/offscreen scaffold surfaces.
  - File: `manifest.config.ts`, `src/devtools/index.ts`, `src/ui/devtools-panel/pages/index.vue`, `src/offscreen/index.ts`
  - Action: Remove demo devtools/offscreen entries from production manifests unless a real SocialGlowz support/debug workflow is specified.
  - User story link: Extension no longer ships unrelated demo surfaces.
  - Depends on: Task 1.
  - Validate with: Chrome/Firefox manifest builds and manual inspection of generated manifests.
  - Notes: If kept for internal builds, guard by mode and document the mode.

- [ ] Task 11: Align Chrome and Firefox manifests.
  - File: `manifest.config.ts`, `manifest.chrome.config.ts`, `manifest.firefox.config.ts`
  - Action: Split Chrome-only side panel fields from Firefox, remove unsupported permissions, keep minimum permissions, and use `webextension-polyfill` or a wrapper for shared browser APIs where needed.
  - User story link: Users get reliable installs on both browsers.
  - Depends on: Tasks 4, 6, 9, 10.
  - Validate with: `pnpm build:chrome`, `pnpm build:firefox`, `pnpm lint:manifest` for Firefox after build.
  - Notes: Chrome Side Panel needs `"sidePanel"` permission; Firefox fallback should not contain a broken side panel promise.

- [ ] Task 12: Harmonize state persistence.
  - File: `src/utils/pinia.ts`, `src/stores/options.store.ts`, `src/composables/useBrowserStorage.ts`, `src/stores/*`
  - Action: Decide whether extension mode uses existing persisted Pinia/localStorage, `chrome.storage`, or a narrow adapter; remove scaffold `options.store.ts` from product paths if no longer used.
  - User story link: Profiles/settings behave predictably across extension surfaces.
  - Depends on: Tasks 2-7.
  - Validate with: Unit tests for profile/settings persistence in extension mode.
  - Notes: Avoid split-brain between `chrome.storage` demo state and SocialGlowz product state.

- [ ] Task 13: Add extension-mode copy for unsupported Tauri-only features.
  - File: `src/locales/**`
  - Action: Add localized strings explaining extension limitations for native WebView isolation, haptics, Android bottom bar, per-network session deletion, and native backup behavior.
  - User story link: Users understand why extension behavior differs from Tauri.
  - Depends on: Task 1.
  - Validate with: i18n key scan and UI smoke.
  - Notes: Keep copy concise and honest.

- [ ] Task 14: Add test and build proof.
  - File: `package.json`, `vitest.config.ts`, focused tests under `src/**/*.test.ts`
  - Action: Add/extend tests for capability detection, URL launcher, manifest shape, and extension-safe rendering. Ensure Chrome and Firefox builds run in CI/local checks.
  - User story link: Prevents future extension drift.
  - Depends on: Tasks 2-13.
  - Validate with: `pnpm test:once`, `pnpm typecheck`, `pnpm build:chrome`, `pnpm build:firefox`, `pnpm lint:manifest`.
  - Notes: Browser manual smoke remains required for popup/side panel behavior.

- [ ] Task 15: Update project docs and changelog.
  - File: `README.md`, `AGENT.md`, `CLAUDE.md`, `shipflow_data/technical/context.md`, `shipflow_data/technical/context-function-tree.md`, `CHANGELOG.md`
  - Action: Document the extension parity contract, active commands, Chrome/Firefox limitations, and proof path.
  - User story link: Operators and agents know how to maintain the extension surface.
  - Depends on: Tasks 1-14.
  - Validate with: Metadata lint for frontmatter docs and grep for stale scaffold claims.
  - Notes: Do not reintroduce the removed web/Vercel target.

## Acceptance Criteria

- Chrome extension builds successfully and exposes no scaffold/demo UI in toolbar popup, side panel, options page, setup install/update page, content iframe, or devtools surfaces.
- Firefox extension builds successfully and exposes no unsupported Chrome-only side panel promise.
- Generated manifests use minimum permissions for implemented behavior.
- Content script is absent by default or restricted to an explicitly documented, user-enabled feature.
- Popup and side panel share SocialGlowz catalog/profile/settings state instead of standalone demo/test stores.
- Network launcher opens every built-in network from `src/config/socialNetworks.ts` through browser-native tabs/windows and handles custom links safely.
- Extension mode never calls Tauri IPC and never shows Tauri-only controls as enabled.
- Extension mode clearly labels degraded features where native Tauri parity is impossible.
- Convex Auth/sync startup remains optional and does not crash when environment variables are absent.
- `pnpm test:once`, `pnpm typecheck`, `pnpm build:chrome`, `pnpm build:firefox`, and Firefox manifest lint pass.
- README and technical docs accurately describe extension parity and limitations.

## Test Strategy

- Proof path: evidence-first plus focused automated tests.
- Static checks:
  - `pnpm typecheck`
  - `pnpm test:once`
  - `pnpm build:chrome`
  - `pnpm build:firefox`
  - `pnpm lint:manifest`
  - `python3 /home/claude/shipflow/tools/shipflow_metadata_lint.py <changed-docs>`
- Unit tests:
  - platform capability detection with mocked globals;
  - extension launcher URL validation and browser API calls;
  - manifest shape/permission assertions where practical;
  - persistence adapter behavior if storage changes.
- Manual smoke:
  - Chrome: load unpacked, open popup, open side panel, open options, click built-in network, click custom link, verify unsupported native features are disabled/labeled.
  - Firefox: load temporary add-on, open popup/options/setup, verify no broken side-panel UI and no unsupported manifest behavior.
- Regression checks:
  - `pnpm tauri:build` or targeted Tauri smoke after shared SocialGlowz extraction to ensure Tauri app behavior remains intact.

## Risks

- High: exact Tauri session isolation cannot be reproduced in ordinary browser tabs; the product must accept a degraded extension contract.
- High: broad content script injection can trigger privacy/store review issues if left as-is.
- Medium: sharing the full SocialGlowz app shell inside popup may be too heavy or ergonomically poor; popup may need a compact launcher while setup page hosts fuller UI.
- Medium: Chrome side panel and Firefox parity differ; treating both as one surface will break expectations.
- Medium: auth flows may behave differently inside extension origins and browser popup lifetimes.
- Medium: changing shared stores can regress Tauri unless adapters are explicit.
- Low: removing demo devtools/offscreen surfaces may affect only scaffold-era development paths.
- Medium: custom links and browser API calls become an extension security boundary; strict URL allowlisting, minimal permissions, and redacted errors are required.

## Execution Notes

- Audit summary:
  - The extension currently has one browser-extension product with multiple surfaces: toolbar popup, side panel, options page, setup install/update pages, content-script iframe, devtools panel, and background service worker/script.
  - Most UI surfaces are scaffold/demo and not product parity.
  - The SocialGlowz product surface exists under `src/ui/setup/pages/SocialGlowz/`.
  - Tauri-specific behavior is mostly guarded by `isTauri`, but the extension needs a first-class browser adapter rather than relying on dev placeholders.
- Official docs verdict:
  - `fresh-docs checked` for Chrome extension API and Side Panel API.
  - `fresh-docs checked` for MDN WebExtensions API and manifest documentation.
- Implementation recommendation:
  - Start with capability map and adapter layer before replacing UI surfaces.
  - Treat extension parity as a staged migration, not a direct copy of Tauri native WebViews.
  - Preserve Tauri as the authoritative native session-isolation implementation.
- Security implementation notes:
  - Authentication: extension pages may initiate SocialGlowz auth only through existing `convexAuth` wrappers; no new token storage path is allowed without a separate review.
  - Authorization: browser-extension UI must not treat local profile selection as a security boundary; server-side Convex permissions remain authoritative for synced data.
  - Input validation: custom-link and network-launch URLs must be normalized and allowlisted before tab/window creation.
  - Workflow integrity: install/update/onboarding routes must not bypass auth or session-lock expectations already present in the SocialGlowz app.
  - Data exposure: do not log tokens, cookies, backup payloads, profile avatars, or full URLs with sensitive query strings.
  - Abuse prevention: remove default all-sites iframe injection unless a future explicit overlay feature defines host permissions and user consent.

## Open Questions

- None blocking for spec creation. Product decisions encoded here:
  - extension parity means browser-native functional parity, not identical native WebView/session isolation;
  - Chrome side panel is a Chrome capability with Firefox fallback;
  - global content-script iframe injection should be removed unless a real feature is specified.

## Skill Run History

| Date UTC | Skill | Model | Action | Result | Next step |
|----------|-------|-------|--------|--------|-----------|
| 2026-05-25 17:59:45 | sf-spec | GPT-5 Codex | Audited extension surfaces and created parity spec | Draft spec created | /sf-ready extension-tauri-feature-parity |
| 2026-05-25 19:53:05 | sf-ready | GPT-5 Codex | Validated readiness and tightened language/security requirements | ready | /sf-start extension-tauri-feature-parity |
| 2026-05-25 20:43:21 | sf-start | GPT-5.3 Codex sub-agent + GPT-5 Codex verification | Implemented extension launcher parity surfaces, capability/launcher helpers, manifest hardening, and targeted tests/docs updates | partial | /sf-verify extension-tauri-feature-parity |
| 2026-05-26 07:28:53 | sf-deps | GPT-5 Codex | Audited extension dependency security, drift, licenses, and config | findings | /sf-start sg-extension-deps-audit-fixes or /sf-verify extension-tauri-feature-parity after dependency cleanup |
| 2026-05-26 19:41:25 | sf-verify | GPT-5 Codex | Verified extension parity implementation, dependency cleanup, generated manifests, builds, audits, and Tauri frontend proof | partial | Run manual Chrome/Firefox extension smoke, then rerun /sf-verify |
| 2026-05-26 19:53:40 | sf-start | GPT-5.3 Codex sub-agent + GPT-5 Codex verification | Ran automatable extension verification gap pass: rebuilt Chrome/Firefox, re-linted Firefox manifest, inspected generated manifests/entrypoints/routes, executed Playwright screenshot smoke on Chrome/Firefox dist surfaces, and root-caused remaining UNSAFE_VAR_ASSIGNMENT warnings to vendor/runtime bundles | implemented | /sf-verify extension-tauri-feature-parity |
| 2026-05-27 21:46:53 | sf-verify | GPT-5 Codex | Re-verified checks, manifests, dependency posture, Tauri frontend build, and automated extension evidence | partial | Fix or explicitly accept web-ext tmp advisory, run real Chrome/Firefox extension smoke, then rerun /sf-verify |
| 2026-05-28 18:18:00 | sf-start | GPT-5 Codex | Bumped web-ext to ^10.3.0 and forced `tmp` to ^0.2.6 via pnpm overrides to remove the dev-only high-risk tmp advisory; updated lockfile and validated `pnpm audit` is clean at audit-level low | implemented | /sf-verify extension-tauri-feature-parity |
| 2026-05-29 21:59:07 UTC | sf-build | gpt-5.3-codex | Ran autonomous sf-build verification wave: typecheck, test:once, build:chrome, build:firefox, lint:manifest, manifest shape checks, and targeted scaffold/deprecation scan | partial | Execute manual Chrome/Firefox extension smoke and then rerun sf-verify |

## Current Chantier Flow

| Step | Status | Notes |
|------|--------|-------|
| sf-spec | done | Draft spec created from local extension audit and official extension API freshness check. |
| sf-ready | done | Ready after explicit URL validation, permission, redaction, and browser/Tauri boundary requirements were added. |
| sf-start | done | Automatable extension verification gaps were executed with agent-run proof (builds, lint, manifests/entrypoints/routes inspection, Playwright smoke), and web-ext tooling posture was then hardened (`web-ext` ^10.3.0, `tmp` -> 0.2.7 via override), leaving manual browser smoke as the remaining verification gate. |
| sf-verify | partial | Automated proof and parity surfaces remain in place; remaining gate is manual Chrome/Firefox extension smoke validation in real browsers. |
| sf-end | pending | Close docs/tasks/changelog after implementation proof. |
| sf-ship | pending | Commit/push/release only after verification. |
