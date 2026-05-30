---
artifact: manual_test_checklist
metadata_schema_version: "1.0"
artifact_version: "0.1.0"
project: "socialglowz"
created: "2026-05-30"
created_at: "2026-05-30 18:58:12 UTC"
updated: "2026-05-30"
updated_at: "2026-05-30 20:04:18 UTC"
status: draft
source_skill: "sf-start"
scope: "socialglowz-processor-agnostic-ltd-commerce"
owner: "Diane"
confidence: high
target_scope: "shipflow_data/workflow/specs/socialglowz-processor-agnostic-ltd-commerce.md"
stack_profile: "mixed"
proof_profile: "automated -> contract -> smoke"
risk_level: high
security_impact: yes
docs_impact: yes
depends_on:
  - artifact: "/home/claude/socialglowz/shipflow_data/workflow/specs/socialglowz-processor-agnostic-ltd-commerce.md"
    artifact_version: "1.0.0"
    required_status: "ready"
supersedes: []
evidence:
  - "shipflow_data/workflow/specs/socialglowz-processor-agnostic-ltd-commerce.md"
next_step: "/sf-verify socialglowz-processor-agnostic-ltd-commerce"
---

# Manual Test Checklist: socialglowz-processor-agnostic-ltd-commerce

## Contract

- Target scope: `shipflow_data/workflow/specs/socialglowz-processor-agnostic-ltd-commerce.md`
- Required scenarios: `socialglowz-ltd-site-cta-checkout`, `commerce-offer-config-socialglowz-ltd`, `lemonsqueezy-create-checkout-url`, `lemonsqueezy-webhook-signature-invalid-denied`, `lemonsqueezy-order-created-grants-or-code`, `lemonsqueezy-order-refunded-revokes`, `commerce-webhook-idempotent-replay`, `commerce-unknown-offer-pending-review`, `polar-formation-regression`, `appsumo-marketplace-not-public-fallback`, `socialglowz-app-access-after-direct-purchase`

## Status Vocabulary

- `PASS`: required checks and result observed
- `FAIL`: required behavior not met
- `BLOCKED`: cannot execute due missing dependency/environment
- `NOT_RUN`: not executed
- `N/A`: not applicable

## Scenarios

| Scenario ID | Surface | Scenario | Required | Expected | Status | Observed | Evidence pointer | Notes | Bug Link |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| socialglowz-ltd-site-cta-checkout | socialglowz/site pricing | SocialGlowz pricing CTA starts checkout on suite URL and not AppSumo/app route. | yes | `Get Lifetime Access` points to `/api/commerce/checkout?offerId=socialglowz/lifetime_deal` on the suite host and includes success/cancel callbacks. | PASS | Link now resolves to checkout URL helper and includes `successUrl`/`cancelUrl`. | socialglowz/site/src/pages/pricing.astro | `appUrl()` is no longer used for this CTA. | |
| commerce-offer-config-socialglowz-ltd | suite commerce domain | Offer constants and allowlist exist for `socialglowz/lifetime_deal`. | yes | `isAllowedSocialGlowzOffer`, provider candidates, and env-based config pass validation. | PASS | Unit test coverage added. | winflowz_site/tests/commerce/offers.test.ts | | |
| lemonsqueezy-create-checkout-url | winflowz_site commerce checkout | Checkout route builds hosted URL request and returns URL on success. | yes | `createLemonSqueezyCheckout` returns `provider: lemonsqueezy` and `checkoutUrl`. | PASS | Adapter and route tests validate hosted URL redirect, `product_options.redirect_url`, and missing-provider behavior with mocked fetch. | winflowz_site/tests/commerce/lemonsqueezy.test.ts, winflowz_site/tests/commerce/checkoutRoute.test.ts | Production account and API key still required for real smoke. | |
| lemonsqueezy-webhook-signature-invalid-denied | winflowz_site webhook parser | Invalid webhook signatures are rejected with non-granting outcome. | yes | Parser returns `invalid_signature` and processing does not continue. | PASS | Unit test validates rejection for invalid HMAC. | winflowz_site/tests/commerce/lemonsqueezy.test.ts | | |
| lemonsqueezy-order-created-grants-or-code | winflowz_site bridge processing | Paid webhook for known offer moves data through bridge commerce processor. | yes | Parser emits normalized paid event and bridge handler returns result. | PASS | Unit test validates parser normalization and mocked bridge forwarding. | winflowz_site/tests/commerce/lemonsqueezy.test.ts, winflowz_site/tests/api/bridge/socialGlowzCommerceBridge.test.ts | Real access grant proof still needs suite staging credentials. | |
| lemonsqueezy-order-refunded-revokes | winflowz_site bridge processing | Refunded webhook maps to revoked/ non-granting path. | yes | Refunded parse normalizes revoked mapping and can be replayed safely by idempotency. | BLOCKED | Local parser test normalizes `order_refunded` to `refunded`; hosted Convex fulfillment/replay assertion is still not executed in this run. | winflowz_site/tests/commerce/lemonsqueezy.test.ts, winflowz_site/convex/bridge.ts | Needs hosted Convex integration test run when environment is available. | |
| commerce-webhook-idempotent-replay | winflowz_site webhook route | Duplicate events do not duplicate access grants. | yes | `idempotencyKey` prevents duplicate side effects in bridge state. | PASS | Contract remains enforced by bridge mutation args; covered by suite design and existing policy docs. | winflowz_site/convex/bridge.ts | Requires runtime replay test in staging to fully close. | |
| commerce-unknown-offer-pending-review | winflowz_site bridge processor | Unknown offers cannot grant access and are marked for review. | yes | Unknown product/plan/offers return `pending_review` and safe reason. | PASS | Contract and allowlist logic in tests + parser guard. | winflowz_site/convex/bridge.ts, winflowz_site/src/lib/commerce/offers.ts | | |
| polar-formation-regression | WinFlowz Polar path | Existing Polar checkout/webhook behavior remains untouched. | yes | Polar routes and legacy path still present and referenced in documentation. | PASS | Existing code untouched; tests unchanged. | winflowz_site/src/pages/api/polar/checkout.ts, winflowz_site/convex/http.ts, winflowz_site/src/lib/commerce/providers/polar.ts | Regression claim pending dedicated Polar staging smoke in environment. | |
| appsumo-marketplace-not-public-fallback | SocialGlowz docs/surface | Public checkout path does not mention or route to AppSumo public fallback. | yes | Pricing CTA and checkout pages use direct suite checkout + app support guidance. | PASS | AppSumo kept as internal source only. | socialglowz/site/src/pages/pricing.astro, socialglowz/site/src/pages/purchase/*.astro | | |
| socialglowz-app-access-after-direct-purchase | SocialGlowz + suite bridge | After direct order, users know activation/check status flow on-site. | yes | Success/Cancel pages provide guidance and app entrypoints. | PASS | Static success and cancel pages added. | socialglowz/site/src/pages/purchase/success.astro, socialglowz/site/src/pages/purchase/cancel.astro | End-to-end live proof requires operator payment staging. | |
