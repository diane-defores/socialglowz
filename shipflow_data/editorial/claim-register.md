---
artifact: editorial_claim_register
metadata_schema_version: "1.0"
artifact_version: "0.1.0"
project: socialglowz
created: "2026-06-11"
updated: "2026-06-11"
status: draft
source_skill: 300-sf-docs
scope: editorial
owner: "Diane"
confidence: medium
risk_level: high
security_impact: high
docs_impact: high
linked_systems:
  - "shipflow_data/business/product.md"
  - "shipflow_data/business/branding.md"
  - "shipflow_data/business/gtm.md"
  - "shipflow_data/technical/context.md"
depends_on:
  - artifact: "shipflow_data/business/product.md"
    artifact_version: "1.0.1"
    required_status: reviewed
supersedes: []
evidence:
  - "public-facing security and sync-sensitive surfaces"
next_step: "/sf-docs editorial"
next_review: "2026-09-11"
---

# Claim Register

## Purpose

Track explicit public claims that need proof, freshness checks, or legal/compliance alignment.

## Claim classes requiring impact review

- Security, privacy, encryption, and session protection.
- Billing, pricing, trials, refunds, and lifetime offer statements.
- Performance, uptime, and compatibility promises.
- Compliance, anti-fraud, anti-spam, and policy assertions.
- AI/automation efficacy statements.

## Current register

| Claim area | Current status | Evidence anchor | Freshness rule |
|---|---|---|---|
| Multi-platform availability | implemented | `README.md`, `shipflow_data/technical/context.md` | revalidate after release per platform matrix |
| Android auth/session hardening | implemented | `shipflow_data/technical/context.md`, specs | revalidate on security changes |
| Billing redemption and entitlement access | implemented | `shipflow_data/technical/code-docs-map.md`, `shipflow_data/workflow/specs/*` | revalidate with provider-bridge changes |
| WebView isolation behavior | implemented | `shipflow_data/technical/context.md`, `shipflow_data/technical/android-webview-session-isolation.md` | revalidate on storage/session changes |

## Governance rule

Any update to entries above requires `Claim Impact Plan` in `300-sf-docs` flows when code or claims move materially.
