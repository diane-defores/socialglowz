---
artifact: spec
metadata_schema_version: "1.0"
artifact_version: "0.1.0"
project: socialglowz
created: "2026-05-29"
updated: "2026-05-29"
status: ready
source_skill: sf-build
scope: billing-entitlements
owner: "Diane"
confidence: medium
risk_level: medium
security_impact: yes
docs_impact: yes
user_story: "En tant que fondatrice, je veux vendre SocialGlowz via AppSumo ou un processeur futur sans enfermer l'accès produit dans un provider de paiement."
linked_systems:
  - convex/schema.ts
  - convex/billing.ts
  - convex/test.setup.ts
depends_on: []
supersedes: []
evidence:
  - "SocialGlowz has no AI/provider usage cost in its current documented product model."
  - "Existing Convex schema has a simple subscriptions table but no processor-agnostic entitlement ledger."
next_step: "/sf-start socialglowz-billing-entitlements-foundation"
---

# SocialGlowz Billing Entitlements Foundation

## Intent

Create a processor-agnostic access layer before adding any checkout integration.

Payment providers, AppSumo, and manual grants may create access, but SocialGlowz must read access from its own Convex entitlement state.

## Scope

- Add internal entitlement records for SocialGlowz product access.
- Add redemption codes for AppSumo/manual LTD codes.
- Add an append-only billing event ledger for traceability.
- Add a user mutation to redeem a code.
- Add a user query to read current product access.
- Keep existing `subscriptions` table compatible for now.

## Out of Scope

- Lemon Squeezy, Polar, Paddle, Stripe, or AppSumo API webhooks.
- Public checkout pages.
- Feature gating UI changes.
- Tax, invoices, refunds, dunning, or accounting automation.

## Product Defaults

- Product id: `socialglowz`
- First paid plan: `founder_ltd`
- Sources: `appsumo`, `manual`, future `lemon_squeezy`, `polar`, `stripe`, `paddle`
- LTD entitlements should have no `expiresAt` unless explicitly revoked/refunded.

## Acceptance Criteria

- An authenticated user can redeem one valid available code and receive active `socialglowz/founder_ltd` access.
- A redeemed code cannot be reused by another user.
- Re-redeeming the same code by the same user is idempotent.
- Invalid, disabled, or already-used codes return clear errors without creating access.
- `getProductAccess` returns active access from entitlements and falls back to legacy subscriptions if needed.
- Every redemption writes a billing event.
- Admin code creation requires a server-side secret.

## Current Chantier Flow

sf-spec ✅ -> sf-ready ✅ -> sf-start ✅ -> sf-verify ✅ -> sf-end ✅ -> sf-ship ✅🎯

## Skill Run History

| Date UTC | Skill | Model | Action | Result | Next step |
|----------|-------|-------|--------|--------|-----------|
| 2026-05-29 | sf-build | GPT-5 Codex | Created ready spec for SocialGlowz processor-agnostic entitlement and redemption foundation. | implemented | `/sf-start socialglowz-billing-entitlements-foundation` |
| 2026-05-29 | sf-build | GPT-5 Codex | Implemented Convex schema, redemption mutations, access query, and targeted tests. | partial | `/sf-verify socialglowz-billing-entitlements-foundation` |
| 2026-05-29 | sf-build | GPT-5 Codex | Verified targeted Convex tests and core typecheck; updated technical docs and task tracker. | implemented | `Add redemption UI and AppSumo import runbook` |
| 2026-05-29 22:26:56 UTC | sf-ship | GPT-5 Codex | Quick shipped SocialGlowz processor-agnostic billing entitlement foundation with targeted checks. | shipped | `Add redemption UI and AppSumo import runbook` |
