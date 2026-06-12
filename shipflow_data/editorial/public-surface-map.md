---
artifact: editorial_public_surface_map
metadata_schema_version: "1.0"
artifact_version: "0.1.0"
project: socialglowz
created: "2026-06-11"
updated: "2026-06-11"
status: draft
source_skill: 300-sf-docs
scope: editorial_surface_governance
owner: "Diane"
confidence: medium
risk_level: medium
security_impact: low
docs_impact: high
linked_systems:
  - "shipflow_data/editorial/README.md"
  - "shipflow_data/editorial/content-map.md"
  - "shipflow_data/business/product.md"
depends_on:
  - artifact: "shipflow_data/editorial/README.md"
    artifact_version: "0.1.0"
    required_status: draft
supersedes: []
evidence:
  - "shipflow_data/editorial/content-map.md"
  - "README.md"
  - "shipflow_data/technical/context.md"
next_step: "/sf-docs editorial"
next_review: "2026-09-11"
---

# Public Surface Map

## Repo surfaces

- `README.md` (French primary): architecture, supported platforms, and product scope.
- `CHANGELOG.md`: external release-level summaries.

## Public web surfaces

- `en/` and `fr/`: landing pages and local public copy.
- `site/src/pages/` and `site/src/components/`: Astro/landing runtime pages and components.
- `site/src/content/blog/` + `site/src/pages/blog/`: blog and article surfaces (schema-controlled).

## Product runtime surfaces with public user impact

- `src/ui/setup/pages/SocialGlowz/` (application UX and in-app messaging).
- `src-tauri/` (desktop package metadata and packaging assets).

## Content governance triggers

When a public-facing behavior changes:
- update implementation-linked contract first (`shipflow_data/technical/context.md`, specs, README when needed).
- apply an `Editorial Update Plan` in doc governance notes.
- review claims for the `Claim Impact Plan` path when touching security, privacy, pricing, performance, data, or compliance assertions.
