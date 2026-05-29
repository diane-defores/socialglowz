---
artifact: spec
metadata_schema_version: "1.0"
artifact_version: "0.1.0"
project: socialglowz
created: "2026-05-29"
updated: "2026-05-29"
status: ready
source_skill: sf-build
scope: billing-redemption-ui
owner: "Diane"
confidence: medium
risk_level: medium
security_impact: yes
docs_impact: yes
user_story: "En tant qu'utilisatrice SocialGlowz qui achète un Lifetime Deal ou une offre early bird, je veux saisir mon code dans l'app et voir immédiatement que mon accès à vie est actif."
linked_systems:
  - src/ui/setup/pages/SocialGlowz/components/MobileSettingsSheet.vue
  - src/ui/setup/pages/SocialGlowz/components/AppSettings.vue
  - src/ui/setup/pages/SocialGlowz/components/BillingAccessPanel.vue
  - src/composables/useBillingAccess.ts
  - src/locales/en.json
  - src/locales/fr.json
depends_on:
  - shipflow_data/workflow/specs/socialglowz-billing-entitlements-foundation.md
supersedes: []
evidence:
  - "Backend entitlement layer is shipped in commit 46d6341."
  - "TASKS.md lists sg-billing-redemption-ui as the next billing step."
next_step: "/sf-start socialglowz-redemption-ui"
---

# SocialGlowz Redemption UI

## Intent

Expose the processor-agnostic billing access layer inside SocialGlowz settings so Lifetime Deal, early-bird, partner, or manual customers can redeem a code without needing Lemon Squeezy, Polar, Stripe, Paddle, AppSumo, or another checkout provider in the app UI.

## Scope

- Add a settings panel that reads `billing.getProductAccess`.
- Let authenticated users redeem Lifetime Deal, early-bird, partner, or manual codes through `billing.redeemCode`.
- Show clear states for inactive/free access, active lifetime access, loading, success, and recoverable errors.
- Reuse the same panel in desktop and mobile settings.
- Keep raw redemption codes in component memory only; do not persist or log them.
- Update internal technical docs and task tracker.

## Out of Scope

- Marketplace/direct batch import tooling.
- Provider checkout integrations.
- Feature gating or paywall enforcement across the app.
- Public landing/pricing copy.

## Acceptance Criteria

- Authenticated users can enter a valid code from settings and receive active `lifetime_deal` access.
- Already-active users see their access status without needing to redeem again.
- Unauthenticated or unconfigured Convex states do not attempt redemption and explain that sign-in is required.
- Redeem submit is disabled for empty codes, unauthenticated users, or in-flight requests.
- Errors from invalid/used/disabled codes are shown without exposing sensitive backend detail or logging raw codes.
- Desktop and mobile settings use the same product access component.
- Typecheck, targeted tests, and lint pass.

## Current Chantier Flow

sf-spec ✅ -> sf-ready ✅ -> sf-start ✅ -> sf-verify ✅ -> sf-end ✅ -> sf-ship ✅🎯

## Skill Run History

| Date UTC | Skill | Model | Action | Result | Next step |
|----------|-------|-------|--------|--------|-----------|
| 2026-05-29 22:28:52 UTC | sf-build | GPT-5 Codex | Created ready spec for SocialGlowz redemption UI after backend entitlement ship. | implemented | `/sf-start socialglowz-redemption-ui` |
| 2026-05-29 22:28:52 UTC | sf-build | GPT-5 Codex | Started bounded implementation for shared desktop/mobile redemption UI and product access read state. | partial | `/sf-verify socialglowz-redemption-ui` |
| 2026-05-29 22:32:45 UTC | sf-build | GPT-5 Codex | Implemented shared redemption panel in desktop/mobile settings and ran requested validation. | implemented | `/sf-end socialglowz-redemption-ui` |
| 2026-05-29 22:35:50 UTC | sf-build | GPT-5 Codex | Integrated worker changes, added targeted billing error test, updated docs/tracker/changelog, and verified targeted checks. | implemented | `/sf-ship socialglowz-redemption-ui` |
| 2026-05-29 22:35:50 UTC | sf-end | GPT-5 Codex | Closed SocialGlowz redemption UI bookkeeping with docs and changelog aligned. | closed | `/sf-ship socialglowz-redemption-ui` |
| 2026-05-29 22:36:44 UTC | sf-ship | GPT-5 Codex | Prepared bounded ship for SocialGlowz redemption UI with explicit staging and targeted checks. | shipped | `Add Lifetime Deal code import runbook` |
| 2026-05-29 22:53:52 UTC | sf-build | GPT-5 Codex | Renamed public billing offer from founder/AppSumo wording to Lifetime Deal/early-bird activation and made direct/manual redemption the default source. | implemented | `Add Lifetime Deal code import runbook` |
