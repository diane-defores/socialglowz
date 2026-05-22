---
name: socialglowz-add-network
description: Add or audit a built-in SocialGlowz webview network using the project-local network catalog, with checks for UI, native Android follow-ups, sessions, cookies, and validation.
argument-hint: <network name, url, optional notes>
---

# SocialGlowz Add Network

Use this project-local skill when adding, reviewing, or planning a built-in webview network in this repository.

This skill is intentionally local to SocialGlowz. Do not use it for generic social-network research or for unrelated projects.

## Core Rule

Start from the frontend catalog:

- `src/config/socialNetworks.ts`

Do not reintroduce duplicated built-in network metadata in sidebar, mobile layout, onboarding, profile sheets, or `WEBVIEW_URLS`.

## Scope Gate

Classify the request before editing:

- **Frontend-only network**: add catalog metadata and validate generated UI surfaces.
- **Native Android network**: add catalog metadata, then inspect and update Android native metadata when needed.
- **Research/planning only**: do not edit code; produce the checklist and missing decisions.

Ask one concise question before editing if any of these are unclear:

- canonical URL
- stable network ID
- whether it should appear in onboarding
- whether it needs Android bottom-bar/session/cookie support now
- whether login requires a special URL, desktop UA, or cookie-domain handling

## Frontend Steps

1. Edit `src/config/socialNetworks.ts`.
2. Add one `BuiltInSocialNetwork` entry with:
   - `id`
   - `label`
   - `route`
   - `url`
   - `icon`
   - `color`
   - optional `tileColor`
   - optional `customIcon` only when a matching Vue icon component exists and consumers support it
   - `onboarding`
   - `defaultSelected`
3. Preserve the desired display order in `builtInSocialNetworks`.
4. Confirm these surfaces still derive from the catalog:
   - `src/stores/webviewState.ts`
   - `src/ui/setup/pages/SocialGlowz/components/AppSidebar.vue`
   - `src/ui/setup/pages/SocialGlowz/components/MobileLayout.vue`
   - `src/ui/setup/pages/SocialGlowz/components/MobileProfileSheet.vue`
   - `src/ui/setup/pages/SocialGlowz/components/OnboardingFlow.vue`

Keep Kanban and custom links outside the built-in catalog.

## Native Android Checklist

For every new built-in webview network, inspect:

- `src-tauri/plugins/android-webview/android/src/main/java/com/socialglowz/webview/NativeWebViewPlugin.kt`

Decide whether to update:

- native `NETWORKS` list for bottom bar icon/color/url
- auth cookie names
- cookie URL/domain list
- login URL override
- desktop UA set
- intent/app-banner blocking or cookie-consent handling

If Android support is intentionally deferred, record that as a residual risk in the final report.

## DNS And Docs

Consider whether to update:

- `src/ui/setup/pages/SocialGlowz/index.html` DNS prefetch entries
- `AGENT.md` only if the workflow or source of truth changes
- `TASKS.md` only if the user asks to track the work

## Validation

Run, at minimum:

```bash
pnpm typecheck
pnpm typecheck:full
pnpm test:once
```

Run `pnpm build:web` when the change touches app entrypoints, Vite-facing code, or visible network UI.

Manual sanity checklist:

- Network appears in desktop sidebar in the intended order.
- Network appears in mobile dashboard in the intended order.
- Network appears or stays hidden in onboarding according to `onboarding`.
- Default onboarding selection matches `defaultSelected`.
- Profile visibility toggles still hide/show the network.
- Clear-cookie profile sheet shows the expected label/icon.
- Opening the network selects the expected URL.
- Custom links still work.
- Android bottom bar behavior is validated or explicitly deferred.

## Stop Conditions

Stop and report blocked when:

- the network URL or ID is ambiguous
- adding the network requires native Android behavior but the desired session/cookie policy is unknown
- validation fails on a change introduced by this work
- the request would mix network addition with unrelated refactors

## Final Report

Report:

- networks added or audited
- files changed
- Android/native decision
- validation results
- manual checks still needed
- residual risks
