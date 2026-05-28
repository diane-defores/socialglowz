---
artifact: claude_instructions
metadata_schema_version: "1.0"
artifact_version: "1.0.1"
project: socialglowz
created: "2026-04-26"
status: active
source_skill: sf-docs
scope: technical-guidance
owner: "Diane"
updated: "2026-05-25"
confidence: high
risk_level: low
security_impact: medium
docs_impact: high
depends_on: []
evidence:
  - "README.md"
  - "package.json"
  - "src/lib/convex.ts"
  - "src/lib/convexAuth.ts"
supersedes: []
next_step: "/sf-docs audit CLAUDE.md"
---

# CLAUDE.md

## Purpose

This repository is **SocialGlowz**, a multi-platform social networking dashboard built with **Vue 3 + Vite** and extended through **Tauri 2**.

Primary goals for any agent:

- Keep changes scoped and low-risk.
- Preserve backward compatibility across Chrome, Firefox, desktop, and mobile/Tauri targets.
- Respect existing architecture: shared Vue source with platform-specific Vite configs.

## Operating constraints

- Use `pnpm` for dependency and script execution.
- Prefer incremental edits; avoid broad refactors.
- Keep environment-dependent behavior guarded (offline modes and optional integrations must not crash the app).
- Never change runtime behavior of navigation or webview persistence without explicit intent.

## Runtime structure

- `src/ui/setup/pages/SocialGlowz/` contains the main app.
- Platform variants are controlled by:
  - `vite.chrome.config.ts`
  - `vite.firefox.config.ts`
  - `vite.tauri.config.ts`
- Backend stack uses Convex + Convex Auth:
  - Frontend client entry: `src/lib/convex.ts`
  - Auth wiring: `src/lib/convexAuth.ts`

## Extension parity guardrails

- Extension launcher logic must stay in `src/platform/` and must not import Tauri APIs directly.
- Custom links in extension mode must be normalized `https://` URLs only.
- Reject dangerous schemes (`javascript:`, `data:`, `file:`, `chrome:`, `moz-extension:`) and embedded credentials.
- Do not log tokens, cookies, backup payloads, or sensitive full URLs in extension paths.
- Chrome side panel is Chrome-only; Firefox must not receive broken side panel promises.
- Extension mode must state native limitations honestly (no Tauri WebView isolation/haptics/native backup).

## Common commands

- `pnpm dev` — run Chrome + Firefox extension dev flows.
- `pnpm build:chrome` / `pnpm build:firefox` — extension builds.
- `pnpm tauri:dev` / `pnpm tauri:bundle` — Tauri desktop flows.
- `pnpm test:once` / `pnpm test`.
- `pnpm typecheck` / `pnpm exec tsc -p convex/tsconfig.json --noEmit`.
- `pnpm lint` / `pnpm format`.

## Safety checks before release

1. Validate local auth startup (`VITE_CONVEX_URL`) remains optional for graceful offline mode.
2. Ensure webview-heavy screens still work when cookies/sync state is unavailable.
3. Confirm no hard dependencies on optional credentials (`VITE_GMAIL_*`) are introduced.
4. Run `pnpm test:once`, `pnpm typecheck`, `pnpm exec tsc -p convex/tsconfig.json --noEmit`, and `pnpm lint` before release.
