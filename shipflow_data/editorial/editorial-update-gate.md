---
artifact: editorial_update_gate
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
  - "shipflow_data/editorial/page-intent-map.md"
  - "shipflow_data/editorial/claim-register.md"
depends_on:
  - artifact: "shipflow_data/editorial/page-intent-map.md"
    artifact_version: "0.1.0"
    required_status: draft
supersedes: []
evidence:
  - "editorial corpus bootstrap"
next_step: "/sf-docs editorial"
next_review: "2026-09-11"
---

# Editorial Update Gate

## Purpose

Define when documentation updates need explicit output artifacts.

## Gate criteria

- Any code-path change touching public-facing behavior: produce `Documentation Update Plan`.
- Any sensitive public claim change: produce `Claim Impact Plan`.
- Any addition/removal of public pages or blog content: include scope + surface in final note.
- Any runtime content schema risk: keep Astro schema intact and document incompatibility.

## Required output shape for `300-sf-docs update`

- `Editorial Update Plan` with:
  - Surface(s)
  - Source artifact(s)
  - Claim sensitivity (`none|low|high`)
  - Required validations (manual + build checks)
- `Claim Impact Plan` when impacted claim class in `claim-register` is touched.

## Non-applicability

No gate action needed for:
- Internal source-only files without public exposure.
- Operational trackers (`TEST_LOG.md`, `BUGS.md`, `shipflow_data/workflow/AUDIT_LOG.md`) unless they include durable public-claim decisions.
