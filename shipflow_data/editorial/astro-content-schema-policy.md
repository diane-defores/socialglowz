---
artifact: editorial_astro_schema_policy
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
risk_level: medium
security_impact: medium
docs_impact: high
linked_systems:
  - "site/src/content.config.ts"
  - "site/src/content/blog/"
  - "site/src/pages/blog/"
depends_on:
  - artifact: "shipflow_data/editorial/content-map.md"
    artifact_version: "1.1.0"
    required_status: reviewed
supersedes: []
evidence:
  - "site/src/content.config.ts"
next_step: "/sf-docs editorial"
next_review: "2026-09-11"
---

# Astro Content Schema Policy

## Source of truth

`site/src/content.config.ts` defines the runtime schema for site content. It must remain authoritative.

## Editing rule

- Do not add ShipFlow metadata frontmatter to runtime `site/src/content/**` entries.
- Preserve existing frontmatter keys and required schema fields.
- If governance metadata is required, store it in `shipflow_data/editorial/*.md`, not in runtime content files.
- Any schema mismatch must be surfaced as a compatibility note before merge.

## Validation

- Keep lint/build checks aligned with Astro/collection schema.
- For content-only edits, prefer validating through existing site build path.
