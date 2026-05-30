---
artifact: spec
metadata_schema_version: "1.0"
artifact_version: "1.0.0"
project: "socialglowz"
created: "2026-04-30"
created_at: "2026-04-30 10:25:48 UTC"
updated: "2026-04-30"
updated_at: "2026-04-30 16:20:48 UTC"
status: ready
source_skill: sf-spec
source_model: "GPT-5 Codex"
scope: "migration / audit-fix"
owner: "Diane"
confidence: high
user_story: "As the SocialGlowz maintainer, I want dependency risk reduced through staged hygiene fixes and explicit major-line migration decisions, so web, extension, desktop, and Android releases remain secure and predictable without dependency churn breaking core workflows."
risk_level: "medium"
security_impact: "yes"
docs_impact: "yes"
linked_systems:
  - "pnpm dependency graph"
  - "Vite build configs"
  - "PrimeVue UI theme/runtime"
  - "Tailwind/PostCSS styling pipeline"
  - "Vue Router typed routes"
  - "Tauri Rust/Cargo dependency graph"
  - "GitHub Actions CI"
  - "Dependabot"
depends_on:
  - artifact: "shipflow_data/business/business.md"
    artifact_version: "1.0.0"
    required_status: "reviewed"
  - artifact: "shipflow_data/business/branding.md"
    artifact_version: "1.0.0"
    required_status: "active"
  - artifact: "shipflow_data/technical/guidelines.md"
    artifact_version: "1.0.0"
    required_status: "reviewed"
  - artifact: "shipflow_data/business/product.md"
    artifact_version: "1.0.1"
    required_status: "reviewed"
  - artifact: "shipflow_data/technical/architecture.md"
    artifact_version: "1.0.2"
    required_status: "reviewed"
  - artifact: "docs/repo-architecture-audit.md"
    artifact_version: "unknown"
    required_status: "active"
  - artifact: "PrimeVue official docs via Context7 /primefaces/primevue"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "unplugin-vue-router official docs via Context7 /posva/unplugin-vue-router"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "Tailwind CSS official docs via Context7 /tailwindlabs/tailwindcss.com"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "Vite official docs via Context7 /vitejs/vite/v8.0.0 and vite.dev migration guide"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "Tauri official docs via Context7 /websites/v2_tauri_app and tauri.app distribute guide"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "ESLint official v10 migration guide"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "TypeScript official 6.0 release notes"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "Pinia official v2 to v3 migration guide"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "Vue Router official v4 to v5 migration guide"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "Convex official auth docs via Context7 /websites/convex_dev"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "Mozilla web-ext official repository docs"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "CRXJS official Vite plugin docs"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "VueUse official repository docs"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
  - artifact: "Marked official release metadata"
    artifact_version: "current as of 2026-04-30"
    required_status: "official"
supersedes: []
evidence:
  - "sf-deps 2026-04-30 scored dependency health B-: 0 critical, 0 high, 1 moderate dev/build advisory, 10 patch, 14 minor, 28 major outdated, 3 unknown licenses, 31 undocumented overrides."
  - "pnpm audit --prod returns 0 known vulnerabilities across production dependencies."
  - "pnpm audit reports GHSA-w5hq-g745-h8pq: uuid@8.3.2 via web-ext@10.1.0 -> node-notifier@10.0.1."
  - "package.json has 19 dependencies, 57 devDependencies, 1 root override, and 30 pnpm overrides."
  - "package.json lists deprecated or stale tracks: @primevue/themes, @types/eslint__js, unplugin-vue-router, PrimeVue 3, Tailwind 3, Vite 6, Pinia 2, TypeScript 5, ESLint 9."
  - "src/ui/setup/pages/SocialGlowz/main.ts imports primevue/resources/themes/lara-light-blue/theme.css and primevue/resources/primevue.css, which PrimeVue 4 docs say are no longer used."
  - "tailwind.config.cjs and postcss.config.cjs use Tailwind v3-era config; Tailwind v4 docs recommend the official upgrade tool and @tailwindcss/vite for Vite projects."
  - "src/utils/router/index.ts and vite.config.ts depend on unplugin-vue-router generated routes; pnpm view says unplugin-vue-router has been merged into vuejs/router."
  - "scripts/vue-tsc-fixed.cjs requires semver, but semver is not declared as a direct package.json dependency."
  - "src-tauri/Cargo.toml has license = \"\" and cargo-audit is not installed locally."
  - "shipflow_data/workflow/TASKS.md Audit: Deps 2026-04-30 lists six follow-up tasks for uuid, major migrations, unused deps, semver, licenses, overrides, and RustSec scanning."
  - "sf-ready 2026-04-30 found the draft not ready because `pnpm tauri:build` is only a Vite build, release RustSec coverage was ambiguous, fresh-docs coverage was partial, and Convex/Auth security checks were underspecified."
  - "package.json defines `tauri:build` as `vite build -c vite.tauri.config.ts`; native desktop packaging requires `pnpm exec tauri build` or the existing `tauri:bundle` script."
  - "Current Vite 8 and ESLint 10 docs require Node 20.19+; `.nvmrc` currently says `20` and must be treated as a Node-major pin, not proof of the patch-level floor."
next_step: "/sf-start SocialGlowz Dependency Hygiene and Major-Line Migration"
---

# Title

SocialGlowz Dependency Hygiene and Major-Line Migration

# Status

Ready. This spec defines a staged dependency chantier. It does not implement package changes directly. The 2026-04-30 readiness gate confirms the validation, fresh-docs, release RustSec, Node floor, and Convex/Auth security gaps from the prior draft have been closed enough for staged implementation.

# User Story

As the SocialGlowz maintainer, I want dependency risk reduced through staged hygiene fixes and explicit major-line migration decisions, so web, extension, desktop, and Android releases remain secure and predictable without dependency churn breaking core workflows.

Actor: SocialGlowz maintainer/operator.

Trigger: a dependency audit, Dependabot PR, security advisory, or intentional framework upgrade touches the package graph, Vite build configs, PrimeVue styling, Tailwind/PostCSS styling, Vue Router routing, Tauri/Cargo dependencies, or CI dependency checks.

Expected observable result: production audit remains clean, the known dev/build advisory is either upstream-tracked or safely mitigated, unused and undocumented dependencies are removed or justified, project license posture is explicit, RustSec scanning is reproducible, and major-line migrations are executed only in validated stages.

Value: safer dependency maintenance across the product's five release surfaces without breaking session, profile, auth, webview, or build workflows.

# Minimal Behavior Contract

When a dependency-health run starts, the maintainer can apply low-risk hygiene fixes first, then handle each major-line migration as a separate validated stage; the system produces an updated lockfile, documented override/risk rationale, explicit license posture, and passing checks for production, extension, web, Tauri, Convex, and Rust where applicable. If a package update introduces a build, type, audit, runtime, or UI regression, the stage stops without weakening audit, install, or peer-dependency controls. The easy-to-miss edge case is that a dev-only build tool can still break release packaging or extension linting even when `pnpm audit --prod` is clean.

# Success Behavior

- Starting from the 2026-04-30 dependency state, `pnpm audit --prod` remains clean and `pnpm audit` has no untriaged critical/high findings.
- The remaining `uuid@8.3.2` advisory through `web-ext -> node-notifier` is handled by a documented upstream-risk entry or a proven safe package update; no forced transitive major override is used unless Firefox extension lint/build passes and the dependent package supports it.
- Direct unused/stale dependencies are removed or justified with evidence, and `pnpm install --frozen-lockfile --ignore-scripts` remains reproducible.
- `scripts/vue-tsc-fixed.cjs` either has `semver` declared directly or is removed after proving no command, doc, or CI path uses it.
- The project license posture is explicit for npm and Cargo metadata without accidentally claiming an open-source license.
- Rust dependency scanning is reproducible before both Android/dev artifacts and desktop release artifacts are produced; if scanning cannot run, the release or migration stage is blocked rather than silently accepted.
- PrimeVue, Tailwind, routing, Vite, TypeScript, ESLint, Pinia, Convex/Auth, VueUse, Marked, and related major-line updates are done in isolated migration stages with fresh official docs recorded for each stage.
- Tauri validation distinguishes the frontend web assets built by `tauri:build` from native packaging produced by `pnpm exec tauri build`, `tauri:bundle`, `tauri-action`, or `pnpm exec tauri android build`.
- Any Vite 7+/8 or ESLint 10 stage proves the local and CI Node runtime meet the current `20.19+` floor while staying on the Node 20 major line unless a separate runtime decision is approved.
- Evidence of success is visible through updated files, lockfile diffs, audit output, build/test output, and dependency docs.

# Error Behavior

- If a patch/minor update fails install, typecheck, lint, unit tests, web build, extension build, Tauri build, or Convex typecheck, revert only that stage's changes and leave the documented risk entry in place.
- If an advisory fix requires a transitive major override with unknown runtime compatibility, do not apply it; keep the risk accepted as dev/build-only until an upstream package path exists.
- If PrimeVue 4 theme migration breaks dark mode, surface tokens, component rendering, or mobile layout, stop the PrimeVue stage before starting Tailwind or router upgrades.
- If Tailwind 4 migration changes generated CSS enough to alter key SocialGlowz screens, stop and re-scope styling fixes before continuing.
- If route generation changes break hash routing, typed routes, extension shells, or Tauri Android compatibility, stop and keep Vue Router/unplugin changes isolated.
- If cargo-audit or cargo-deny reports RustSec issues that need incompatible Rust/Tauri upgrades, document them and route the Rust migration through a dedicated `/sf-migrate` step.
- If native packaging fails after a frontend-only `tauri:build` succeeds, the stage is failed; do not treat a Vite-only Tauri build as proof that desktop or Android release artifacts are safe.
- If a current migration guide requires Node above the allowed Node 20 line, stop and ask for a separate runtime upgrade decision before changing CI or `.nvmrc`.
- If a Convex/Auth upgrade changes session, token refresh, anonymous sign-in, password provider, or authorization behavior, stop and route through a dedicated auth/security spec unless the behavior change is fully covered by this spec and tests.
- Never disable audits, remove lockfile integrity, relax install commands, commit registry tokens, or hide failing checks to make dependency output quieter.

# Problem

The sf-deps audit on 2026-04-30 found no production vulnerabilities, but dependency health is not yet stable enough for routine shipping. The repo still has one moderate dev/build advisory, a large backlog of major-line upgrades, deprecated packages, unused direct dependencies, an undeclared script dependency, unclear project license metadata, many undocumented overrides, and no reproducible RustSec scan across both dev and release native pipelines.

This crosses the chantier threshold because the work spans package metadata, frontend build systems, UI theming, routing, CSS generation, native Rust dependencies, CI, and release validation. It cannot be safely handled as one blind package update.

Source intake from sf-deps:

- Titre propose: SocialGlowz Dependency Hygiene and Major-Line Migration
- Raison: multiple dependency, license, override, and migration decisions cross several build/runtime surfaces.
- Severite: P2
- Scope: package.json, pnpm-lock.yaml, Vite configs, PrimeVue/Tailwind/router setup, Rust/Tauri manifests, GitHub Actions, dependency docs, release checks.
- Spec recommandee: `/sf-spec SocialGlowz Dependency Hygiene and Major-Line Migration`
- Prochaine etape from intake: `/sf-ready SocialGlowz Dependency Hygiene and Major-Line Migration`

# Solution

Execute dependency maintenance in two tracks. First, apply narrow hygiene and documentation fixes that should not alter runtime behavior. Second, run explicit major-line migration stages, one framework family at a time, with a fresh-docs preflight, an implementation subplan, and cross-surface validation after every stage.

The implementation should prefer removing stale packages over adding new abstractions, and it should preserve the existing cross-platform architecture: shared Vue code, platform-specific Vite configs, Convex fail-soft auth/sync, and Tauri WebView/session behavior.

# Scope In

- Node/pnpm package graph in `package.json` and `pnpm-lock.yaml`.
- Direct stale or unused dependencies identified by `depcheck`, `rg`, and manual config review.
- The `uuid@8` dev/build advisory through `web-ext`.
- Semver dependency correctness for `scripts/vue-tsc-fixed.cjs`.
- Project license metadata for npm and Tauri/Cargo.
- Override and accepted-risk documentation.
- RustSec scanning with `cargo-audit` or `cargo-deny`.
- PrimeVue 3 to 4 migration planning and implementation, including theme imports and dark-mode/surface token compatibility.
- Tailwind CSS 3 to 4 migration planning and implementation, including Vite/PostCSS config and DaisyUI/plugin compatibility.
- Vue Router / unplugin-vue-router migration planning and implementation for generated typed routes and manual SocialGlowz router compatibility.
- Follow-up major-line migrations for Vite, TypeScript, ESLint, Pinia, VueUse, Marked, Convex/Auth, and related build/test dependencies when supported by official docs and local validation.
- CI validation for dependency install, audit, typecheck, tests, lint, web build, extension builds, Convex typecheck, native packaging, Android packaging, and native dependency scan.
- Release workflow hardening so `.github/workflows/build.yml` cannot produce desktop artifacts without RustSec coverage or an explicitly documented blocker.
- Documentation updates in dependency/risk docs and release notes.

# Scope Out

- No auth, Convex, profile, session, or WebView behavior redesign unless a dependency migration directly requires a compatibility fix.
- No visual redesign beyond restoring equivalent PrimeVue/Tailwind output after migration.
- No automatic major-version updates bundled with unrelated features.
- No replacement of Tauri, Convex, Vue, PrimeVue, Tailwind, or pnpm as the core stack.
- No weakening of `.npmrc`, audit, lockfile, or CI controls to bypass dependency failures.
- No legal licensing decision beyond declaring the current product as private/proprietary unless Diane explicitly chooses an open-source license.
- No publication of packages to npm or crates.io.
- No move from Convex Auth to another auth provider, no token-storage redesign, and no permission-model redesign unless a dependency upgrade makes the current flow incompatible and a dedicated auth/security spec approves the change.

# Constraints

- Use `pnpm` and the pinned `packageManager: pnpm@8.11.0`.
- Preserve the Node 20 major line because `.nvmrc` and GitHub Actions use Node 20, but allow raising the patch floor to `20.19.0` or later when Vite 7+/8 or ESLint 10 requires it.
- Keep `.npmrc` compatibility flags unless the migration proves installs pass without them.
- Keep `VITE_CONVEX_URL` optional and auth startup fail-soft.
- Preserve hash routing where it supports Tauri Android and extension behavior.
- Keep extension, web, desktop, and Android build surfaces in scope for validation. `corepack pnpm tauri:build` is only a Tauri frontend asset build; native desktop validation requires `corepack pnpm tauri:bundle`, `corepack pnpm exec tauri build --ci`, or a passing `tauri-action` release workflow.
- Never force major upgrades through transitive `pnpm.overrides` unless the direct package officially supports the new major and all impacted builds pass.
- Read fresh official migration docs again at implementation time for every major-line stage.
- Treat client-side UI checks as non-security controls. Any Convex function or auth-sensitive flow touched by dependency changes must keep identity and authorization checks server-side.

# Dependencies

Local dependencies and versions from `package.json` as of 2026-04-30:

- Package manager: `pnpm@8.11.0`
- Runtime pin: Node 20 in `.nvmrc`; active shell during audit was Node 22.22.2. Vite 8 and ESLint 10 currently require Node 20.19+ within the Node 20 line.
- Core frontend: `vue@3.5.13`, `vite@6.4.2`, `@vitejs/plugin-vue@5.2.4`, `typescript@5.7.3`
- UI/styling: `primevue@3.53.1`, `@primevue/themes@4.2.5`, `primeicons@7.0.0`, `primeflex@3.3.1`, `tailwindcss@3.4.19`, `daisyui@4.12.23`
- Routing: `vue-router@4.5.0`, `unplugin-vue-router@0.10.9`
- State/sync/auth: `pinia@2.3.1`, `pinia-plugin-persistedstate@4.2.0`, `convex@1.32.0`, `@convex-dev/auth@0.0.91`, `@auth/core@0.37.4`
- Native: `@tauri-apps/api@2.10.1`, `@tauri-apps/cli@2.10.0`, Rust `tauri@2.10.0`
- Extension tooling: `@crxjs/vite-plugin@2.4.0`, `web-ext@10.1.0`, `webextension-polyfill@0.12.0`

Fresh external docs:

- PrimeVue official docs via Context7 `/primefaces/primevue`: PrimeVue 4 styled mode uses `@primeuix/themes` presets and no longer uses the old `theme.css` or `primevue/resources` directory. Verdict: `fresh-docs checked`.
- unplugin-vue-router official docs via Context7 `/posva/unplugin-vue-router`: current plugin supports generated typed routes and requires Vue Router 4.4+; npm metadata reports it has merged into `vuejs/router`. Verdict: `fresh-docs checked` with npm registry metadata supplement.
- Vue Router official v4 to v5 migration guide: Vue Router 5 merges file-based routing into core; migration from `unplugin-vue-router` is mostly import path and generated type location changes. Verdict: `fresh-docs checked`.
- Tailwind CSS official docs via Context7 `/tailwindlabs/tailwindcss.com`: v3 to v4 migration should use the official upgrade tool on Node 20+ and Vite projects should move toward `@tailwindcss/vite`. Verdict: `fresh-docs checked`.
- Vite official docs via Context7 `/vitejs/vite/v8.0.0` and `vite.dev/guide/migration.html`: current major requires Node 20.19+ / 22.12+ and the current migration path includes Rolldown/Oxc behavior changes that can affect dependency optimization and output semantics. Verdict: `fresh-docs checked`.
- Tauri official docs via Context7 `/websites/v2_tauri_app` and `tauri.app/distribute`: native artifacts are produced by Tauri CLI `build`, `android build`, and `ios build`; `tauri-action` runs `tauri build` to generate release artifacts. Verdict: `fresh-docs checked`.
- ESLint official v10 migration guide: ESLint 10 requires Node 20.19+ and changes flat-config lookup, recommended rules, comment handling, formatter behavior, and integration APIs. Verdict: `fresh-docs checked`.
- TypeScript official 6.0 release notes: TypeScript 6.0 introduces breaking/deprecation behavior around defaults, `rootDir`, explicit `types`, DOM libs, and stricter defaults that can affect this repo's multi-tsconfig checks. Verdict: `fresh-docs checked`.
- Pinia official v2 to v3 guide: Pinia 3 supports Vue 3 only, requires Vue 3.5.11+ and TypeScript 5+, and removes deprecated APIs. Verdict: `fresh-docs checked`.
- Convex official auth docs via Context7 `/websites/convex_dev` and `docs.convex.dev/auth`: Convex authenticates requests with JWT identity and expects authorization checks at the start of public functions; Convex Auth is beta and can change. Verdict: `fresh-docs checked`.
- Mozilla `web-ext` official repository docs and npm metadata: `web-ext@10.1.0` is current, requires Node 20+, remains primarily a CLI, and depends on `node-notifier@10.0.1`. Verdict: `fresh-docs checked`.
- CRXJS official Vite plugin docs: CRXJS integrates through `@crxjs/vite-plugin` in Vite config and extension packaging still depends on Vite builds plus manifest output. Verdict: `fresh-docs checked`.
- VueUse official repository docs and npm metadata: current `@vueuse/core@14.2.1` requires Vue 3.5+. Verdict: `fresh-docs checked`.
- Marked official release metadata: current `marked@18.0.2` requires Node 20+ and v18 includes breaking changes around token trimming and TypeScript 6. Verdict: `fresh-docs checked`.

The docs above are current planning evidence for this spec. Each major-line stage must re-check the same official source immediately before editing if package versions or release notes have changed.

# Invariants

- Production dependency audit must stay clean or improve.
- Lockfile integrity must remain committed and installable with frozen lockfile.
- Build outputs must remain platform-specific: Chrome, Firefox, web, Tauri desktop/mobile.
- Native release checks must prove actual Tauri desktop/mobile packaging, not only Vite output for the Tauri frontend.
- PrimeVue migration must preserve existing app-level dark-mode behavior and surface token expectations.
- Tailwind migration must preserve current content scanning coverage for root HTML, FR/EN HTML, demo, and Vue/TS sources.
- Router migration must preserve `createWebHashHistory` for SocialGlowz and not break generated typed routes used by extension shells.
- Native Tauri dependency scanning must not require publishing crates or changing the product license.
- RustSec scanning must run before native artifacts are uploaded in both dev-build and release workflows, or the workflow must fail with a documented blocker.
- CI must continue to install dependencies with frozen lockfiles.
- No secrets, tokens, or registry credentials may be added to repo config.
- Convex/Auth-sensitive changes must preserve anonymous sign-in fallback, password account linking, local token namespace isolation, sign-out token clearing, and server-side authorization checks. Client route guards or UI visibility are not sufficient security controls.

# Links & Consequences

- `package.json` and `pnpm-lock.yaml`: every package change affects install reproducibility and Dependabot diffs.
- `.npmrc`: broad peer resolution and hoisting are currently documented compatibility concessions; removing them requires proving all surfaces still install/build.
- `src/ui/setup/pages/SocialGlowz/main.ts`: PrimeVue 4 changes old CSS imports and `app.use(PrimeVue)` theme config.
- `src/ui/setup/pages/SocialGlowz/App.vue` and `src/ui/setup/pages/SocialGlowz/assets/main.css`: dark-mode and surface token overrides may need adjustment after PrimeVue theme migration.
- `vite.config.ts`, `vite.web.config.ts`, `vite.tauri.config.ts`, `vite.chrome.config.ts`, `vite.firefox.config.ts`: Tailwind/Vite/router/plugin changes can affect different surfaces differently.
- `src/utils/router/index.ts`, `src/types/typed-router.d.ts`, `src/types/router-meta.d.ts`, `src/types/auto-imports.d.ts`: typed route generation and imports can break typecheck if migration order is wrong.
- `tailwind.config.cjs`, `postcss.config.cjs`, `src/assets/base.scss`, `src/landing/main.css`: Tailwind 4 migration can change CSS entrypoint semantics.
- `.github/workflows/dev-builds.yml`, `.github/workflows/build.yml`, `.github/dependabot.yml`: dependency scanning and native build checks need CI coverage.
- `src-tauri/Cargo.toml`, `src-tauri/plugins/android-webview/Cargo.toml`, `src-tauri/Cargo.lock`: RustSec and license posture apply to desktop/Android packaging.
- `package.json` scripts: `tauri:build` is a Vite-only build and must not be used as the only proof for native packaging success.
- `src/lib/convex.ts`, `src/lib/convexAuth.ts`, `src/lib/cloudSyncQueue.ts`, `convex/auth.ts`, `convex/http.ts`, and Convex public functions: runtime/auth dependency changes can affect JWT refresh, anonymous sessions, server-side ownership checks, and cloud sync side effects.
- `README.md`, `CHANGELOG.md`, and dependency docs: dependency policy and migration notes need durable documentation.

# Documentation Coherence

Documentation to update:

- Create or update `docs/dependency-risk-register.md` for accepted advisories, unknown licenses, and RustSec proof gaps.
- Create or update `docs/dependency-overrides.md` for each root and pnpm override, including reason, source advisory if any, affected package path, owner, and removal criteria.
- Update `README.md` or `CLAUDE.md` only if commands or dependency policy change.
- Update `CHANGELOG.md` after implementation stages land.
- Keep `shipflow_data/workflow/TASKS.md` and `shipflow_data/workflow/AUDIT_LOG.md` updates for audit/end/ship skills, not for this spec.

No user-facing marketing, pricing, onboarding, or FAQ copy change is required unless a migration changes visible behavior or platform support.

# Edge Cases

- `web-ext@10.1.0` is already latest, but still depends on `node-notifier@10.0.1`, which depends on `uuid@8.3.2`; forcing `uuid@14` could break CommonJS consumers.
- `@primevue/themes@4.2.5` is listed but the app currently imports PrimeVue 3 resource CSS instead, so removing or replacing it must be coordinated with the PrimeVue stage.
- `@tailwindcss/forms` is present but commented out in `tailwind.config.cjs`; removing it is low risk only if no templates rely on its plugin-generated styles.
- `unplugin-icons` has `autoInstall: true`; removing unused icon JSON packages could make builds mutate dependencies if missing icon collections are used later. Disable auto-install or keep only proven collections.
- Generated type files may mask dependency usage, so unused-dependency decisions must combine `depcheck`, `rg`, config reads, and build validation.
- Tailwind 4 can change CSS output enough to affect layouts even when TypeScript and tests pass.
- PrimeVue 4 can change component DOM/CSS classes, affecting custom dark-mode overrides and screenshots.
- RustSec scan may report advisories in transitive crates that require Tauri or plugin upgrades, which should be staged separately.
- `corepack pnpm tauri:build` can pass while native desktop bundling fails, because the script only builds the Tauri frontend with Vite.
- Current Vite and ESLint majors require Node 20.19+; `.nvmrc` value `20` is not precise enough as a validation artifact unless CI and local commands print an actual compatible version.
- Convex Auth is beta and the app uses a custom Vue client wrapper around Convex Auth internals; dependency updates can break token refresh or anonymous sign-in without changing visible UI routes.
- GitHub release workflow `.github/workflows/build.yml` currently uploads native desktop artifacts, so putting RustSec only in the Android/dev workflow would leave release artifacts uncovered.

# Implementation Tasks

- [ ] Task 1: Add dependency risk and override documentation scaffolding
  - File: `docs/dependency-risk-register.md`
  - Action: Create a concise register with the current `uuid@8` dev/build advisory, unknown-license packages, RustSec proof gap, and accepted-risk format.
  - User story link: Gives the maintainer an observable dependency-risk baseline before changing packages.
  - Depends on: None.
  - Validate with: `test -f docs/dependency-risk-register.md`.
  - Notes: Include status, affected path, production reachability, decision, next review date, and removal criteria.

- [ ] Task 2: Document root and pnpm overrides
  - File: `docs/dependency-overrides.md`
  - Action: Document `overrides["@crxjs/vite-plugin"]` and all 30 `pnpm.overrides` from `package.json` with reason, affected dependency path where known, safety rationale, and removal condition.
  - User story link: Makes future dependency upgrades reviewable instead of tribal knowledge.
  - Depends on: Task 1.
  - Validate with: Every key under `package.json` `overrides` and `pnpm.overrides` appears in the doc exactly once.
  - Notes: If a reason is no longer valid, remove the override in a later task only after `pnpm install --frozen-lockfile` and full checks pass.

- [ ] Task 3: Handle the `uuid@8` advisory without unsafe transitive forcing
  - File: `docs/dependency-risk-register.md`
  - Action: Record `GHSA-w5hq-g745-h8pq` as dev/build-only through `web-ext@10.1.0 -> node-notifier@10.0.1 -> uuid@8.3.2`, with `pnpm audit --prod` clean as evidence and a policy not to force `uuid@14` until upstream supports it.
  - User story link: Keeps release security posture explicit without breaking Firefox extension tooling.
  - Depends on: Task 1.
  - Validate with: `corepack pnpm audit --prod --json` reports zero vulnerabilities; `corepack pnpm why uuid` shows only the known dev/build path.
  - Notes: If a newer `web-ext` or `node-notifier` release removes the advisory, update normally and close the risk entry.

- [ ] Task 4: Remove or justify low-risk unused/stale direct dependencies
  - File: `package.json`
  - Action: Remove direct packages proven unused by source/config search, starting with `@tailwindcss/forms`, `@iconify-json/carbon`, `@iconify-json/lucide`, `@iconify-json/mdi`, `@iconify-json/svg-spinners`, `get-installed-browsers`, `prettier-plugin-tailwindcss`, `unplugin-imagemin`, `vuefire`, `webext-bridge`, and `@types/eslint__js`; keep `@iconify-json/ph` because `<i-ph-*>` components are used.
  - User story link: Reduces install surface and license/audit exposure without changing product behavior.
  - Depends on: Tasks 1-3.
  - Validate with: `corepack pnpm install --frozen-lockfile`, `corepack pnpm lint:check`, `corepack pnpm typecheck`, `corepack pnpm test:once`, `corepack pnpm build:web`, `corepack pnpm build:chrome`, `corepack pnpm build:firefox`.
  - Notes: Do not remove `@primevue/themes` in this task; handle it in the PrimeVue migration stage.

- [ ] Task 5: Fix or remove the undeclared `semver` script dependency
  - File: `scripts/vue-tsc-fixed.cjs`
  - Action: If no package script, docs, or CI path uses this script, remove it. If the script is intentionally retained, add `semver` as a direct `devDependency`.
  - User story link: Prevents hidden reliance on hoisting.
  - Depends on: Task 4.
  - Validate with: `rg -n "vue-tsc-fixed|semver" . --glob '!node_modules/**' --glob '!pnpm-lock.yaml'` and `corepack pnpm typecheck:full`.
  - Notes: Prefer removal if the script is dead; prefer direct dependency only if the script remains operationally useful.

- [ ] Task 6: Make project license posture explicit
  - File: `package.json`
  - Action: Add `"license": "UNLICENSED"` because the product is private and not positioned as open source.
  - User story link: Resolves npm package metadata ambiguity without changing product rights.
  - Depends on: Tasks 1-5.
  - Validate with: `node -e "console.log(require('./package.json').license)"`.
  - Notes: Do not use `Unlicense`; that is an open-source/public-domain style license and does not match this product.

- [ ] Task 7: Make Cargo publish/license posture explicit
  - File: `src-tauri/Cargo.toml`
  - Action: Replace `license = ""` with `publish = false` and add repository metadata if known from `package.json`; keep the crate private instead of inventing an open-source license.
  - User story link: Removes empty native metadata while preserving proprietary product assumptions.
  - Depends on: Task 6.
  - Validate with: `cargo metadata --manifest-path src-tauri/Cargo.toml --format-version 1`.
  - Notes: If a proprietary `LICENSE` file is later added, a follow-up can add `license-file`.

- [ ] Task 8: Add reproducible RustSec scanning to dev and release workflows
  - File: `.github/workflows/dev-builds.yml`
  - Action: Add a CI step after Rust toolchain setup to install and run `cargo audit` against `src-tauri/Cargo.lock` before the Android debug APK build.
  - User story link: Gives native dependency risk operational visibility before mobile artifacts are produced.
  - Depends on: Task 7.
  - Validate with: `cargo audit --version` and `(cd src-tauri && cargo audit)` locally if installed, or a passing `dev-builds` workflow.
  - Notes: If CI time becomes unacceptable, switch this task to `cargo-deny` with an explicit config file and the same RustSec coverage goal.

- [ ] Task 9: Gate desktop release artifacts on RustSec scanning
  - File: `.github/workflows/build.yml`
  - Action: Add equivalent RustSec scanning after Rust setup and before each `tauri-apps/tauri-action@v0` release build so Linux and Windows artifacts cannot upload without the native dependency scan.
  - User story link: Prevents a clean Android/dev pipeline from hiding a vulnerable desktop release path.
  - Depends on: Task 8.
  - Validate with: workflow YAML review plus a passing manual release workflow, or a documented blocker in `docs/dependency-risk-register.md`.
  - Notes: If `cargo-audit` cannot run on Windows, run it once in a dedicated Linux preflight job that gates both desktop release jobs.

- [ ] Task 10: Add major-stage preflight and Node floor documentation
  - File: `docs/dependency-risk-register.md`
  - Action: Add a reusable preflight checklist for every major migration stage: print `node --version`, confirm Node `20.19.0` or later for Vite 7+/8 and ESLint 10, record official docs source and package versions, and define the stage's rollback boundary before editing packages.
  - User story link: Keeps staged migrations predictable and prevents implementation from relying on stale docs or an underspecified Node 20 pin.
  - Depends on: Tasks 1-9.
  - Validate with: the risk register contains the preflight checklist and names Vite, Tauri, ESLint, TypeScript, Pinia, Vue Router, Convex/Auth, web-ext, VueUse, and Marked official docs.
  - Notes: Updating `.nvmrc` from `20` to `20.19.0` or a later Node 20 patch is allowed in the relevant major stage if the docs require it.

- [ ] Task 11: PrimeVue 3 to 4 migration stage
  - File: `src/ui/setup/pages/SocialGlowz/main.ts`
  - Action: Replace old `primevue/resources` CSS imports with PrimeVue 4 styled-mode configuration using `primevue` and `@primeuix/themes`; update package dependencies accordingly.
  - User story link: Moves the UI stack off deprecated theme architecture while preserving the app surface.
  - Depends on: Task 10.
  - Validate with: `corepack pnpm typecheck`, `corepack pnpm test:once`, `corepack pnpm build:web`, `corepack pnpm build:chrome`, `corepack pnpm build:firefox`, `corepack pnpm tauri:build`, plus visual smoke of login, settings, signup nudge, sidebar, mobile layout, and dark mode.
  - Notes: Re-check official PrimeVue 4 migration docs immediately before implementation. Update `src/ui/setup/pages/SocialGlowz/App.vue` and `src/ui/setup/pages/SocialGlowz/assets/main.css` only as needed to preserve existing tokens.

- [ ] Task 12: Tailwind CSS 3 to 4 migration stage
  - File: `tailwind.config.cjs`
  - Action: Run the official Tailwind upgrade path on Node 20+, migrate PostCSS/Vite configuration as required, and preserve current content coverage and DaisyUI/typography behavior.
  - User story link: Keeps styling dependencies current without visual regressions.
  - Depends on: Task 11.
  - Validate with: `corepack pnpm build:web`, `corepack pnpm build:chrome`, `corepack pnpm build:firefox`, `corepack pnpm tauri:build`, and visual smoke of representative pages.
  - Notes: If DaisyUI compatibility blocks Tailwind 4, stop and document the blocker instead of forcing CSS changes.

- [ ] Task 13: Vue Router / unplugin-vue-router migration stage
  - File: `vite.config.ts`
  - Action: Migrate from deprecated `unplugin-vue-router` to the official Vue Router 5 file-based routing path; preserve generated route types for extension shells and manual SocialGlowz hash router behavior.
  - User story link: Keeps routing safe across extension and Tauri surfaces.
  - Depends on: Task 12.
  - Validate with: `corepack pnpm typecheck`, `corepack pnpm build:chrome`, `corepack pnpm build:firefox`, `corepack pnpm build:web`, and a route smoke for setup, action popup, and SocialGlowz app entry.
  - Notes: Do not migrate `src/ui/setup/pages/SocialGlowz/router/index.ts` away from `createWebHashHistory` unless a separate routing spec approves it.

- [ ] Task 14: Vite and build-plugin major stage
  - File: `vite.config.ts`
  - Action: Migrate Vite, `@vitejs/plugin-vue`, Vite PWA, Vite devtools, CRXJS, and related build plugins according to current official migration docs, after proving local and CI Node versions satisfy the required patch floor.
  - User story link: Keeps build tooling current without sacrificing Chrome/Firefox/web/Tauri outputs.
  - Depends on: Task 13.
  - Validate with: `node --version`, `corepack pnpm build:web`, `corepack pnpm build:chrome`, `corepack pnpm build:firefox`, `corepack pnpm tauri:build`, `corepack pnpm exec tauri build --ci --no-bundle`, and `corepack pnpm lint:manifest` after Firefox build.
  - Notes: If CRXJS compatibility blocks a Vite major, stop and document the pin rather than upgrading Vite alone. `tauri:build` validates only Tauri frontend assets; the native `tauri build` command validates Rust/Tauri packaging.

- [ ] Task 15: TypeScript, Vue compiler, vue-tsc, ESLint major stage
  - File: `tsconfig.json`
  - Action: Migrate TypeScript, `@vue/compiler-sfc`, `vue-tsc`, ESLint, `typescript-eslint`, and ESLint Vue packages in a single tooling stage after checking TypeScript 6 and ESLint 10 official notes.
  - User story link: Keeps static checks aligned with the Vue compiler and avoids hidden type drift.
  - Depends on: Task 14.
  - Validate with: `node --version`, `corepack pnpm typecheck`, `corepack pnpm typecheck:full`, `corepack pnpm exec tsc -p convex/tsconfig.json --noEmit`, `corepack pnpm lint:check`, and `corepack pnpm test:once`.
  - Notes: Do not keep monkey-patch scripts for vue-tsc unless the current toolchain still requires them and they are documented. Watch TypeScript `rootDir`, explicit `types`, and ESLint flat-config lookup changes.

- [ ] Task 16: Runtime and auth-sensitive library major stage
  - File: `package.json`
  - Action: Migrate runtime libraries that affect app behavior, including Pinia 3, VueUse 14, Vue Router 5 if not already handled, Marked 18, and any Convex/Auth patch updates supported by official docs.
  - User story link: Keeps runtime dependencies current while preserving auth, sync, routing, and markdown behavior.
  - Depends on: Task 15.
  - Validate with: `corepack pnpm test:once`, `corepack pnpm typecheck`, `corepack pnpm build:web`, `corepack pnpm exec tsc -p convex/tsconfig.json --noEmit`, and manual smoke of auth startup, anonymous sign-in, password signup/sign-in, sign-out token clearing, settings, backup, cloud sync queue, route navigation, and changelog rendering.
  - Notes: Treat Convex/Auth upgrades as security-sensitive. Re-check official docs, verify every touched public Convex function authorizes with server-side identity checks, and do not trust client-provided user IDs or route guards.

- [ ] Task 17: Close dependency posture and update release docs
  - File: `CHANGELOG.md`
  - Action: Record completed dependency stages, accepted risks, remaining pins, validation commands, and any release notes.
  - User story link: Gives future maintainers a clear post-migration baseline.
  - Depends on: Tasks 1-16, or on the subset intentionally completed in this chantier pass.
  - Validate with: `corepack pnpm audit --prod`, `corepack pnpm audit`, `corepack pnpm outdated`, and the release check command set from `CLAUDE.md`.
  - Notes: If major stages are intentionally deferred, the changelog and risk register must say which ones remain and why.

# Acceptance Criteria

- [ ] CA 1: Given the current dependency graph, when hygiene tasks complete, then production audit remains clean and no new critical/high npm advisories are introduced.
- [ ] CA 2: Given `web-ext@10.1.0` still pulls `uuid@8.3.2`, when no safe upstream fix exists, then the advisory is documented as dev/build-only with a next review and no unsafe transitive major override.
- [ ] CA 3: Given a direct dependency is removed, when the full validation set runs, then install, typecheck, tests, lint, web build, Chrome build, Firefox build, and Tauri frontend build still pass.
- [ ] CA 4: Given `scripts/vue-tsc-fixed.cjs` is retained, when dependencies are installed without hoisting assumptions, then `semver` resolves from a direct devDependency.
- [ ] CA 5: Given the product is private, when metadata is reviewed, then npm and Cargo no longer expose empty or misleading license metadata.
- [ ] CA 6: Given Rust dependencies are part of release packaging, when Android/dev or desktop release CI runs, then RustSec scanning is executed before artifacts are built or the workflow is explicitly blocked with a documented replacement plan.
- [ ] CA 7: Given PrimeVue 4 removes old resource CSS, when the PrimeVue stage completes, then the app uses the new theme config and dark/light surfaces still render correctly.
- [ ] CA 8: Given Tailwind 4 changes Vite/PostCSS integration, when the Tailwind stage completes, then generated CSS covers the same HTML/Vue source surfaces and key UI screens remain visually usable.
- [ ] CA 9: Given routing is split between generated extension routes and manual SocialGlowz routes, when router tooling changes, then both route systems still typecheck and use hash history where required.
- [ ] CA 10: Given Vite/build tooling changes, when build validation runs, then Chrome, Firefox, web, Tauri frontend output, and at least one native Tauri packaging path still succeed or the stage is blocked.
- [ ] CA 11: Given Vite 7+/8 or ESLint 10 is adopted, when local and CI checks run, then `node --version` proves Node `20.19.0` or later without jumping to a new Node major unless separately approved.
- [ ] CA 12: Given TypeScript/ESLint majors are changed, when static checks run, then they fail only for real code issues and no temporary bypasses are committed.
- [ ] CA 13: Given runtime libraries change, when manual smoke runs, then auth startup remains optional, cloud sync remains fail-soft, and profile/webview state is not reset unexpectedly.
- [ ] CA 14: Given Convex/Auth packages change, when auth and cloud-sync paths are tested, then anonymous sign-in, password sign-up/sign-in, token refresh, sign-out clearing, and server-side authorization assumptions remain valid.
- [ ] CA 15: Given a major migration stage starts, when implementation begins, then the stage has recorded current official docs, package versions, rollback boundary, validation commands, and stop conditions in dependency docs or the implementation notes.

# Test Strategy

Automated checks:

- `corepack pnpm install --frozen-lockfile`
- `corepack pnpm install --frozen-lockfile --ignore-scripts` for supply-chain sanity after package changes
- `corepack pnpm audit --prod`
- `corepack pnpm audit`
- `corepack pnpm outdated`
- `corepack pnpm test:once`
- `corepack pnpm typecheck`
- `corepack pnpm typecheck:full`
- `corepack pnpm exec tsc -p convex/tsconfig.json --noEmit`
- `corepack pnpm lint:check`
- `corepack pnpm build:web`
- `corepack pnpm build:chrome`
- `corepack pnpm build:firefox`
- `corepack pnpm lint:manifest`
- `corepack pnpm tauri:build` for Tauri frontend assets only
- `corepack pnpm exec tauri build --ci --no-bundle` or `corepack pnpm tauri:bundle` for native desktop packaging when local native prerequisites are available
- `corepack pnpm exec tauri android build --ci --debug --apk -t aarch64` in CI or a documented equivalent Android workflow
- `cargo metadata --manifest-path src-tauri/Cargo.toml --format-version 1`
- `(cd src-tauri && cargo audit)` once cargo-audit is available
- `.github/workflows/dev-builds.yml` and `.github/workflows/build.yml` workflow review to confirm RustSec runs before native artifact upload

Manual smoke:

- SocialGlowz startup with no `VITE_CONVEX_URL`.
- Login/signup/settings drawer.
- Signup nudge dialog/bottom sheet.
- Sidebar and mobile network navigation.
- Dark/light theme toggle.
- Backup/restore entrypoints.
- Web build landing pages in FR/EN.
- Chrome and Firefox extension shell entrypoints.
- Tauri dev window if GUI is available.
- Native release artifact smoke in CI logs or local bundle output when a migration touches Tauri, Vite, build plugins, Rust, or packaging workflows.

# Risks

- PrimeVue 4 can break CSS variables and component styling in ways tests will not catch.
- Tailwind 4 can change generated CSS, especially when using Sass entrypoints and DaisyUI.
- Vite major upgrades can break CRXJS or extension manifest builds before web build notices.
- Vite/ESLint current majors require Node 20.19+, so a vague Node 20 pin can hide local/CI mismatch.
- `tauri:build` can create false confidence because it does not build native desktop installers.
- Vue Router migration can break generated typed routes while leaving manual routes intact, or the reverse.
- RustSec findings can require native upgrades that are larger than this dependency hygiene pass.
- RustSec in only one workflow can leave release artifacts uncovered.
- Convex Auth is beta and this app uses custom Vue auth wiring, so package updates can break token refresh, anonymous sign-in, or authorization assumptions without obvious compile errors.
- Removing seemingly unused packages can break auto-import or icon generation if generated files hide usage.
- License metadata changes can be legally sensitive; this spec assumes private/proprietary posture from project docs and package privacy.

# Execution Notes

Read these files first before implementation:

- `package.json`
- `src/ui/setup/pages/SocialGlowz/main.ts`
- `vite.config.ts`
- `vite.web.config.ts`
- `vite.tauri.config.ts`
- `tailwind.config.cjs`
- `postcss.config.cjs`
- `src/utils/router/index.ts`
- `src/ui/setup/pages/SocialGlowz/router/index.ts`
- `src-tauri/Cargo.toml`
- `.github/workflows/dev-builds.yml`
- `.github/workflows/build.yml`
- `.nvmrc`
- `src/lib/convexAuth.ts`
- `src/lib/cloudSyncQueue.ts`
- `convex/auth.ts`
- representative Convex public functions touched by package changes

Implementation approach:

1. Complete documentation/risk-register tasks before mutating packages.
2. Add RustSec gates to both native CI paths before changing native dependencies.
3. Add a major-stage preflight checklist that records official docs, package versions, Node floor, rollback boundary, and validation commands.
4. Apply narrow hygiene changes and validate.
5. Run one major-line migration stage at a time.
6. After each stage, run the validation commands for that stage before starting the next.
7. Stop and document the blocker if an official migration path conflicts with CRXJS, Tauri, PrimeVue styling, Tailwind/DaisyUI, router behavior, Node 20.19+ compatibility, or Convex/Auth security assumptions.

Packages allowed:

- Existing stack packages and official migration packages documented by the framework maintainers.
- `@primeuix/themes` for PrimeVue 4 styled mode.
- `@tailwindcss/vite` if Tailwind 4 migration proceeds.
- `semver` only if `scripts/vue-tsc-fixed.cjs` remains.
- `cargo-audit` or `cargo-deny` for RustSec scanning.
- Official framework migration packages such as `@tailwindcss/vite` only inside their dedicated migration stage.

Packages avoided:

- New UI frameworks.
- Alternative routers.
- Alternative package managers.
- Forced transitive major overrides for security advisories without official compatibility.
- New auth providers, token-storage libraries, routers, native shell frameworks, or security abstractions outside this dependency chantier.

Fresh-docs stop conditions:

- Stop if current official docs contradict any migration step in this spec.
- Stop if a package's current major requires Node higher than 20.
- Stop if a Vite or ESLint stage cannot prove Node 20.19+ locally and in CI.
- Stop if a migration requires dropping Chrome, Firefox, web, desktop, or Android support.
- Stop if a migration requires disabling tests, audit, lint, frozen lockfile, or lockfile integrity.
- Stop if native packaging cannot be validated after build tooling changes; do not treat `tauri:build` alone as release proof.
- Stop if a Convex/Auth package update changes JWT, refresh-token, anonymous sign-in, password-provider, or authorization semantics beyond this spec's checks.

# Open Questions

None blocking for spec readiness. This spec assumes the project remains private/proprietary, keeps Node 20, and prioritizes staged dependency safety over fastest possible latest-version adoption.

# Skill Run History

| Date UTC | Skill | Model | Action | Result | Next step |
|----------|-------|-------|--------|--------|-----------|
| 2026-04-30 10:25:48 | sf-spec | GPT-5 Codex | Created dependency hygiene and major-line migration spec from sf-deps intake and local investigation | Draft chantier saved with staged tasks, docs freshness evidence, and validation plan | /sf-ready SocialGlowz Dependency Hygiene and Major-Line Migration |
| 2026-04-30 12:44:17 | sf-ready | GPT-5 Codex | Ran strict readiness gate against dependency migration spec, local package/workflow scripts, audit evidence, and current official docs for named migration claims | Not ready: validation commands can miss native Tauri release breakage, release RustSec coverage is ambiguous, fresh-docs coverage is partial for in-scope major stages, and Convex/Auth runtime migration security checks are underspecified | /sf-spec SocialGlowz Dependency Hygiene and Major-Line Migration |
| 2026-04-30 15:49:33 | sf-spec | GPT-5 Codex | Updated the dependency migration spec after readiness findings and current official docs review | Draft updated with native packaging validation, release RustSec gate, full fresh-docs evidence, Node 20.19+ constraint, and Convex/Auth security checks | /sf-ready SocialGlowz Dependency Hygiene and Major-Line Migration |
| 2026-04-30 16:20:48 | sf-ready | GPT-5 Codex | Ran strict readiness gate against the updated dependency migration spec, local package/workflow evidence, and current official docs/package metadata for named migration stages | Ready: structure, metadata, task ordering, fresh-docs gate, adversarial review, and proportional security review pass for staged implementation | /sf-start SocialGlowz Dependency Hygiene and Major-Line Migration |
| 2026-05-30 06:58:27 | sf-migrate | GPT-5 Codex | Reviewed current major upgrade targets for Vite 8, PrimeVue 4, Pinia 3, Vue Router 5, Tailwind 4, ESLint 10, TypeScript 6, and unplugin-vue-router deprecation | Confirmed the work remains a staged migration chantier; produced a migration order and affected-pattern matrix without applying package changes | /sf-start SocialGlowz Dependency Hygiene and Major-Line Migration |
| 2026-05-30 07:12:01 | sf-verify | GPT-5 Codex | Verified the migration-planning run with fresh official docs, local checks, cross-target builds, and GPT 5.3 Codex Spark review | Partial: plan is coherent and current repo checks pass, but no migration implementation has started and native Tauri packaging/device proof is not run | /sf-start SocialGlowz Dependency Hygiene and Major-Line Migration |
| 2026-05-30 07:33:16 | sf-start | GPT-5.3 Codex Spark | Added bounded Node-floor prep: explicit `Node 20.19.0` in `package.json`, `.nvmrc`, and CI Node setup steps; recorded the decision in dependency risk register | partial | /sf-start SocialGlowz Dependency Hygiene and Major-Line Migration |
| 2026-05-30 20:21:18 | sf-verify | GPT-5 Codex | Verified the Node-floor prep from the `sf-start` micro-step with local checks, cross-target frontend builds, and targeted config inspection | partial: Node-floor prep is verified, but ship-readiness remains partial because unrelated dirty billing/site/spec changes and open high bugs are present in the worktree | /sf-start SocialGlowz Dependency Hygiene and Major-Line Migration |
| 2026-05-30 20:31:37 | sf-verify | GPT-5 Codex | Checked verification readiness while the delegated GPT-5.3 Codex Spark `sf-start` run was still active | blocked: no completed implementation result from the active sub-agent is available to verify yet; current worktree remains dirty with unrelated billing/site changes | Wait for active `sf-start` sub-agent result, then rerun `/sf-verify` |
| 2026-05-30 21:38:22 | sf-start | GPT-5 Codex | Completed Stage 11/12 implementation slices: PrimeVue 4 + Tailwind 4 migration changes in `src/ui/setup/pages/SocialGlowz/main.ts`, `package.json`, `pnpm-lock.yaml`, `postcss.config.cjs`, `src/assets/base.scss`, `vite.config.ts`, and `vite.tauri.config.ts` | partial: `typecheck`, `lint:check`, `test:once`, `build:chrome`, `build:firefox`, `tauri:build`, and `lint:manifest` pass; `corepack pnpm build:web` is not defined; existing Sass `@import` deprecation warnings remain from `src/assets/base.scss` | /sf-verify SocialGlowz Dependency Hygiene and Major-Line Migration |
| 2026-05-30 20:44:16 | sf-verify | GPT-5 Codex | Verified the Stage 11/12 PrimeVue 4 + Tailwind 4 migration slice with fresh Tailwind docs, frozen install, typecheck, lint, tests, Chrome/Firefox/Tauri frontend builds, manifest lint, and targeted diff inspection | partial: compile/build proof passes, but global ship-readiness is blocked by open high bugs, missing native/device proof, Sass/Tailwind v4 compatibility debt in `src/assets/base.scss`, `web-ext` warnings, and unrelated dirty billing/site/tracker changes | Fix or explicitly defer the proof gaps, then continue next isolated migration stage |

# Current Chantier Flow

| Step | Status | Notes |
|------|--------|-------|
| sf-spec | done | Draft spec updated after readiness findings in `shipflow_data/workflow/specs/socialglowz-dependency-hygiene-and-major-line-migration.md`. |
| sf-ready | ready | 2026-04-30 readiness gate passed after fresh-docs, native packaging, RustSec, Node floor, and Convex/Auth security updates. |
| sf-start | partial | Stage 11/12 migration work executed in this pass (PrimeVue and Tailwind v4 migration steps, `@tailwindcss/vite`, package/config updates). Remaining migration stages remain blocked until `sf-verify` runs and completes. |
| sf-verify | partial | Stage 11/12 compile/build proof passes, but release readiness remains partial due open high bugs, native/device proof gap, Sass/Tailwind compatibility debt, manifest warnings, and unrelated dirty worktree changes. |
| sf-end | not launched | Use after verified task completion if not shipping immediately. |
| sf-ship | not launched | Use only after checks and release decision. |

Next command: fix or explicitly defer the Stage 11/12 proof gaps, then continue the next isolated migration stage.
