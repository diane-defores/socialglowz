---
artifact: manual_test_checklist
metadata_schema_version: "1.0"
artifact_version: "0.1.0"
project: "socialglowz"
created: "2026-05-30"
created_at: "2026-05-30 16:55:38 UTC"
updated: "2026-05-30"
updated_at: "2026-05-30 17:17:01 UTC"
status: draft
source_skill: "sf-start"
scope: "socialglowz-suite-entitlement-adapter"
owner: "Diane"
target_scope: "shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md"
stack_profile: "mixed"
proof_profile: "automated -> contract -> hosted bridge smoke"
confidence: medium
risk_level: high
security_impact: yes
docs_impact: yes
evidence:
  - "shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md"
depends_on:
  - artifact: "shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md"
    artifact_version: "1.0.0"
    required_status: "ready"
supersedes: []
next_step: "/sf-test socialglowz-suite-entitlement-adapter --preview"
---

# Manual Test Checklist: socialglowz-suite-entitlement-adapter

## Contract

- Target scope: `shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md`
- Stack profile: `mixed`
- Proof profile: `automated -> contract -> hosted bridge smoke`
- Required proof rows: `PASS`/`FAIL`/`BLOCKED`/`N/A`/`NOT_RUN` are machine-read.

## Status Vocabulary

- `NOT_RUN`: not executed yet
- `PASS`: required checks and result observed
- `FAIL`: failure reproduced with a concrete observation
- `BLOCKED`: could not execute due to environment/accessibility/dependency blockers
- `N/A`: not applicable with an explicit reason in Notes

## Scenarios

| Scenario ID | Surface | Scenario | Required | Expected | Status | Observed | Evidence pointer | Notes | Bug Link |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| suite-product-allowlist-socialglowz | suite-allowlist | SocialGlowz product, plan, and source values are allowlisted in the suite layer. | yes | `socialglowz` and `lifetime_deal` are accepted without changing existing suite products. | PASS | Helper tests cover allowlist behavior; code review confirms suite product constants. | winflowz_site/tests/bridge/suiteBridge.test.ts | Cross-repo evidence lives in `/home/claude/winflowz`. | |
| suite-code-import-lifetime-deal | suite-code-lifecycle | Import or upsert a real Lifetime Deal activation code in suite-owned storage. | yes | Suite stores the code canonically and rejects invalid product/plan/source values. | BLOCKED | No deployed bridge or staging fixture was available in this run for a real import smoke; real marketplace/AppSumo activation also waits for operator application/approval. | shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md | Local adapter tests mock the bridge. Technical smoke can use a `manual`/`direct` test code; marketplace-source proof remains a follow-up. | |
| socialglowz-redeem-code-active | socialglowz-adapter | Authenticated SocialGlowz user redeems a valid suite-owned code. | yes | SocialGlowz returns active `socialglowz/lifetime_deal` access from the suite adapter. | PASS | Unit test covers suite-backed redemption through `billing.redeemCode`. | convex/billing.test.ts | Bridge response is mocked locally. | |
| socialglowz-redeem-code-same-user-idempotent | socialglowz-adapter | Same SocialGlowz user submits the same code again. | yes | Response remains active and marks the redemption as idempotent. | PASS | Unit test covers `alreadyRedeemed` response mapping. | convex/billing.test.ts | Bridge response is mocked locally. | |
| socialglowz-redeem-code-second-user-denied | socialglowz-adapter | Second SocialGlowz user submits a code already used by another account. | yes | Access is denied without exposing the first account. | PASS | Unit test maps suite `code_already_used` failure to safe denial. | convex/billing.test.ts | Bridge response is mocked locally. | |
| socialglowz-bridge-unavailable-denied | socialglowz-adapter | Suite bridge URL or secret is missing/unreachable. | yes | SocialGlowz fails closed and UI maps the state to bridge unavailable. | PASS | Unit tests cover missing bridge config and safe UI error mapping. | convex/billing.test.ts | Also covered by `src/composables/useBillingAccess.test.ts`. | |
| socialglowz-revoked-access-denied | suite-revoke-lifecycle | Suite revoke/refund/disable state is reflected as inactive in SocialGlowz. | yes | SocialGlowz no longer grants access after suite revoke/refund/disable. | BLOCKED | Revoke/refund operations are covered locally, but no end-to-end bridge smoke verified a post-revoke SocialGlowz access refresh. | shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md | Requires configured non-production suite bridge and two test accounts. | |
| socialglowz-no-silent-email-merge | suite-identity | SocialGlowz Convex Auth identity is not silently merged with Clerk by email only. | yes | Identity lookup uses SocialGlowz provider account id; email is reference data only. | PASS | Code review confirms lookup through `provider=socialglowz_convex` and `providerAccountId`. | winflowz_site/convex/bridge.ts | Cross-repo evidence lives in `/home/claude/winflowz`. | |

## Blockers

- Hosted smoke proof for the full lifecycle remains blocked by the absence of a configured non-production SocialGlowz suite bridge environment in this run.
- Real marketplace/AppSumo activation proof remains blocked until the operator has applied for and obtained marketplace access; keep the brand as an internal source and avoid public UX redirection toward it.
- Required blocked rows must be cleared before claiming production-ready entitlement behavior.
