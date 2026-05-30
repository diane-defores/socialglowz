---
artifact: technical_module_context
metadata_schema_version: "1.0"
artifact_version: "0.1.0"
project: socialglowz
created: "2026-05-30"
updated: "2026-05-30"
status: draft
source_skill: sf-docs
scope: platform-usage-lemonsqueezy
owner: Diane
confidence: high
risk_level: medium
security_impact: yes
docs_impact: yes
linked_systems:
  - shipflow_data/technical/code-docs-map.md
  - /home/claude/shipflow/shipflow_data/technical/external-platforms/lemonsqueezy.md
  - /home/claude/winflowz/shipflow_data/technical/platforms/lemonsqueezy.md
  - site/src/config/site.ts
  - site/src/pages/pricing.astro
  - site/src/pages/purchase/success.astro
  - site/src/pages/purchase/cancel.astro
depends_on:
  - artifact: "/home/claude/shipflow/shipflow_data/technical/external-platforms/lemonsqueezy.md"
    artifact_version: "0.1.0"
    required_status: "draft"
  - artifact: "/home/claude/winflowz/shipflow_data/technical/platforms/lemonsqueezy.md"
    artifact_version: "0.1.0"
    required_status: "draft"
supersedes: []
evidence:
  - "SocialGlowz public site delegates direct Lifetime Deal checkout to the WinFlowz suite commerce route."
  - "Fresh Lemon Squeezy docs checked on 2026-05-30; no official CLI or MCP was identified."
next_review: "2026-06-30"
next_step: "/sf-verify socialglowz-processor-agnostic-ltd-commerce after Lemon Squeezy test-mode and hosted Convex refund/replay smoke"
---

# Lemon Squeezy Usage

## Purpose

Document SocialGlowz's direct dependency on Lemon Squeezy through the WinFlowz suite commerce layer.

SocialGlowz does not own Lemon Squeezy API keys, webhooks, refunds, or durable payment truth. The public site starts the buyer journey; WinFlowz owns checkout creation, webhook verification, provider normalization, and suite entitlement fulfillment.

Use these provider notes:

- Global source note: `/home/claude/shipflow/shipflow_data/technical/external-platforms/lemonsqueezy.md`
- Suite usage note: `/home/claude/winflowz/shipflow_data/technical/platforms/lemonsqueezy.md`

## Usage Summary

- Provider role: checkout provider behind the direct SocialGlowz Lifetime Deal path.
- SocialGlowz role: public CTA, success/cancel pages, app activation/status flow.
- Applies to paths:
  - `site/src/config/site.ts`
  - `site/src/pages/pricing.astro`
  - `site/src/pages/purchase/success.astro`
  - `site/src/pages/purchase/cancel.astro`
  - `site/.env.example`
  - `site/README.md`
- Environments used: local/static site build, future hosted site, future WinFlowz suite checkout environment.
- Validation surface: static site build, CTA URL inspection, WinFlowz suite checkout/provider tests, Lemon Squeezy test-mode smoke.
- Owner: Diane.
- Last verified: 2026-05-30 by local build and official docs review; provider smoke not yet executed.

## Local Configuration

| Item | Value or rule | Secret? | Notes |
| --- | --- | --- | --- |
| Suite checkout base URL | `PUBLIC_WINFLOWZ_CHECKOUT_URL` | no | Public site points to WinFlowz suite checkout route. |
| Public site URL | `PUBLIC_SITE_URL` | no | Used for success/cancel callback URLs. |
| App URL | `PUBLIC_APP_URL` | no | Used by result pages for activation/status guidance. |
| Lemon Squeezy API key | not stored in SocialGlowz site | yes | Must remain in WinFlowz server environment only. |
| Lemon Squeezy webhook secret | not stored in SocialGlowz site | yes | Must remain in WinFlowz server environment only. |

## Runtime And Integration Notes

- Pricing CTA should link to the WinFlowz suite checkout route for `offerId=socialglowz/lifetime_deal`.
- Public pages should say "Lifetime Deal", "early-bird", or "activation code"; do not use "founder" wording.
- Public pages must not route direct buyers to AppSumo or another commission marketplace as the default fallback.
- The success page is not payment proof. It should guide the buyer to app activation/status while the suite waits for a signed webhook.
- SocialGlowz app access must come from the suite entitlement adapter, not Lemon Squeezy payloads.

## MCP / CLI Policy

Current status:

- Official Lemon Squeezy CLI: not identified.
- Official Lemon Squeezy MCP: not identified.
- SocialGlowz-adopted automation layer: none.

SocialGlowz should not connect a third-party Lemon Squeezy MCP directly to public site or app code. Any future MCP/CLI automation belongs in the WinFlowz suite/operator layer after review.

## Invariants

- SocialGlowz site remains acquisition/checkout-start only.
- WinFlowz suite remains the payment and entitlement fulfillment owner.
- No Lemon Squeezy secret appears in SocialGlowz client/site env.
- Direct buyers stay on the direct Lifetime Deal path.
- SocialGlowz app never requires users to understand Lemon Squeezy.

## Failure Modes

- `PUBLIC_WINFLOWZ_CHECKOUT_URL` missing or wrong -> CTA cannot start checkout; fix site env and rebuild.
- WinFlowz checkout provider missing env -> buyer receives unavailable checkout response; no AppSumo fallback.
- Lemon Squeezy webhook delayed -> success page still only gives activation/status guidance; no access grant from redirect alone.
- Third-party MCP suggested for SocialGlowz -> route to WinFlowz/provider usage note; do not add it to SocialGlowz app/site.

## Security Notes

- Do not add Lemon Squeezy API keys, webhook secrets, store ids, variant ids, raw order payloads, customer emails, or provider logs to SocialGlowz public docs or code.
- Treat checkout URLs and activation codes as potentially sensitive support artifacts when tied to a real buyer.
- SocialGlowz public copy must avoid exposing marketplace fallback channels that route users away from direct checkout.

## Validation

Local checks:

```bash
npm -C /home/claude/socialglowz/site run build
python3 /home/claude/shipflow/tools/shipflow_metadata_lint.py /home/claude/socialglowz/shipflow_data/technical/platforms/lemonsqueezy.md
```

Cross-project provider smoke is owned by WinFlowz:

```text
Create checkout from SocialGlowz pricing -> complete Lemon Squeezy test order -> receive WinFlowz signed webhook -> verify suite entitlement/code path -> refund -> verify non-granting SocialGlowz access.
```

## Reader Checklist

- `site/src/config/site.ts` or `pricing.astro` changed -> verify checkout URL, success/cancel URLs, and no public marketplace fallback.
- Purchase result pages changed -> verify they do not claim payment/access before webhook fulfillment.
- SocialGlowz app billing/access changed -> verify suite entitlement adapter remains the source of truth.
- Lemon Squeezy CLI/MCP question appears -> use global and WinFlowz notes; do not introduce a SocialGlowz-local provider automation layer.

## Maintenance Rule

Update this note when the SocialGlowz checkout CTA, public checkout copy, result pages, app activation path, suite checkout base URL, or Lemon Squeezy tool/automation policy changes.
