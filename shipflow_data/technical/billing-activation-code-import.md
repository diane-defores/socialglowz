---
artifact: runbook
metadata_schema_version: "1.0"
artifact_version: "1.0.0"
project: "socialglowz"
created: "2026-05-30"
updated: "2026-05-30"
status: active
source_skill: sf-build
scope: "billing / activation-code-import"
owner: "Diane"
confidence: high
risk_level: high
security_impact: yes
docs_impact: yes
linked_systems:
  - "scripts/importSocialGlowzActivationCodes.ts"
  - "convex/billing.ts"
  - "WinFlowz suite entitlement bridge"
  - "SOCIALGLOWZ_BILLING_ADMIN_SECRET"
depends_on:
  - artifact: "shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md"
    artifact_version: "1.0.0"
    required_status: "ready"
  - artifact: "/home/claude/shipflow/skills/references/product-entitlements-playbook.md"
    artifact_version: "1.0.1"
    required_status: "active"
supersedes: []
evidence:
  - "convex/billing.ts exposes adminUpsertRedemptionCode and routes writes through the suite bridge."
  - "scripts/importSocialGlowzActivationCodes.ts imports batches through the Convex admin action without writing local entitlement truth."
next_step: "Run dry-run import with a real operator batch, then import against the target Convex deployment."
---

# SocialGlowz Activation Code Import

This runbook is for direct Lifetime Deal, early-bird, partner, or manual activation-code batches that do not come from a payment provider webhook.

The import path is intentionally provider-agnostic:

1. Operator prepares a batch file.
2. `scripts/importSocialGlowzActivationCodes.ts` validates and normalizes the batch.
3. The script calls `billing.adminUpsertRedemptionCode`.
4. `convex/billing.ts` sends `operation=upsert_code` to the WinFlowz suite bridge.
5. The suite entitlement ledger remains the durable source of truth.

Do not write directly to SocialGlowz local `redemptionCodes` or `entitlements` tables. Those tables are migration/compat surfaces only.

## Required Environment

Set these only in an operator shell or server secret store:

```bash
VITE_CONVEX_URL="https://<deployment>.convex.cloud"
SOCIALGLOWZ_BILLING_ADMIN_SECRET="<operator-admin-secret>"
SOCIALGLOWZ_SUITE_BRIDGE_URL="https://<suite-site>/api/bridge/socialglowz"
SOCIALGLOWZ_SUITE_BRIDGE_SECRET="<suite-bridge-secret>"
```

`SOCIALGLOWZ_BILLING_ADMIN_SECRET` must not be added to browser, mobile, public site, or client-side build environments.

## Batch Formats

Supported formats: JSON, JSONL, CSV.

Required field:

- `code`

Optional fields:

- `source`: `direct`, `partner`, `manual`, `appsumo`, or `legacy`. Default: `direct`.
- `status`: `available` or `disabled`. Default: `available`.
- `planId`: default `lifetime_deal`.
- `productId`: must be `socialglowz` when present.
- `sourceRef`: internal campaign, partner, order, or batch reference.
- `externalOrderId`: optional provider/order reference when applicable.
- `note`: private operator note. Do not include secrets or raw customer data.

CSV example:

```csv
code,source,status,sourceRef,note
LTD-0001,direct,available,early-bird-2026-05,launch batch
PARTNER-0001,partner,disabled,agency-a,enable after partner confirmation
```

JSON example:

```json
[
  {
    "code": "LTD-0001",
    "source": "direct",
    "sourceRef": "early-bird-2026-05"
  }
]
```

## Dry Run

Always parse first without importing:

```bash
pnpm run billing:import-codes -- --file ./private/socialglowz-codes.csv --dry-run
```

Expected result:

- JSON summary prints row counts.
- Codes are redacted in output.
- No Convex request is made.

## Import

After the dry run passes:

```bash
pnpm run billing:import-codes -- --file ./private/socialglowz-codes.csv
```

Use `--continue-on-error` only when a partial import is acceptable and each failed row can be retried from the output summary.

## Security Rules

- Treat activation codes as bearer credentials.
- Do not commit batch files.
- Do not paste raw codes into public docs, issues, or ShipFlow reports.
- Keep batch files in an ignored/private operator directory.
- Prefer `sourceRef` values that identify a campaign/order without exposing customer data.
- If a batch was imported into the wrong environment, disable the affected codes through the same bridge path instead of editing local tables.

## Validation

Local validation:

```bash
pnpm exec vitest run scripts/importSocialGlowzActivationCodes.test.ts convex/billing.test.ts
```

Operator smoke after import:

1. Pick one test code from the batch.
2. Sign into SocialGlowz with a test account.
3. Redeem the code in the billing/access panel.
4. Verify `billing.getProductAccess` returns active `socialglowz/lifetime_deal`.
5. Retry the same code on the same account and confirm idempotent success.
6. Retry the same code on a second account and confirm reuse is rejected.

## Maintenance

Update this runbook when:

- allowed sources change;
- the suite bridge contract changes;
- SocialGlowz stops using Convex for billing actions;
- the operator import script changes input format, output format, or environment variables.
