---
artifact: editorial_blog_article_policy
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
  - "site/src/content/blog/"
  - "site/src/pages/blog/"
  - "shipflow_data/editorial/content-map.md"
depends_on:
  - artifact: "shipflow_data/editorial/content-map.md"
    artifact_version: "1.1.0"
    required_status: reviewed
supersedes: []
evidence:
  - "site content and routing files"
next_step: "/sf-docs editorial"
next_review: "2026-09-11"
---

# Blog and Article Surface Policy

## Scope

Controls governance for `site/src/pages/blog/` and `site/src/content/blog/` outputs.

## Rules

- Keep public claims in these surfaces aligned with the latest context artifacts.
- Use Astro schema-compatible frontmatter only.
- Tag legal/commercial claims with a link to the owning context artifact.
- Cross-reference implementation truth when claims involve security, pricing, availability, or platform limits.

## Surface guardrails

- If a claim changes materially, update:
  - `shipflow_data/business/*` (product/branding/GTM)
  - relevant technical artifact if behavior changed
  - `Editorial Update Plan`/`Claim Impact Plan` in governance output

## Review cadence

- Recheck blog and article claims after release notes or pricing/product updates.
