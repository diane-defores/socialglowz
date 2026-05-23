---
artifact: content_map
metadata_schema_version: "1.0"
artifact_version: "1.1.0"
project: "socialglowz"
created: "2026-04-26"
updated: "2026-05-23"
status: reviewed
source_skill: manual
scope: content_map
owner: "Diane"
confidence: medium
risk_level: medium
security_impact: low
docs_impact: yes
evidence:
  - "README.md and shipflow_data/workflow/TASKS.md serve as the public and execution surfaces."
  - "en/ and fr/ landing pages expose public product positioning."
  - "site/src/content/blog and site/src/pages/blog expose public article content."
  - "src/ui and src-tauri represent operational and desktop surfaces."
  - "shipflow_data/workflow/research and shipflow_data/workflow/specs contain exploration and implementation context."
linked_artifacts:
  - shipflow_data/business/product.md
  - shipflow_data/business/gtm.md
depends_on:
  - artifact: "shipflow_data/business/product.md"
    artifact_version: "1.0.1"
    required_status: reviewed
  - artifact: "shipflow_data/business/gtm.md"
    artifact_version: "1.0.1"
    required_status: reviewed
supersedes: []
next_review: "2026-05-26"
next_step: "/sf-docs audit shipflow_data/editorial/content-map.md"
content_surfaces:
  - repo_docs
  - landing_pages
  - blog_articles
  - static_pages
  - architecture_docs
  - specs
  - research_notes
  - scripts_and_ops
---

# Content Map

## Purpose

`shipflow_data/editorial/content-map.md` defines where SocialGlowz content and product truth should live so future repurposing does not drift from implementation.

## Content Surfaces

### Repo documentation

- `README.md` — canonical entry for architecture and platform matrix.
- `shipflow_data/workflow/TASKS.md` — roadmap and execution priorities.
- `shipflow_data/workflow/AUDIT_LOG.md` — historical decision and review notes.
- `CHANGELOG.md` — release-level evolution.

### Front-end and product surfaces

- `src/ui/setup/pages/SocialGlowz/` — primary application surface.
- `src-tauri/` — desktop and packaging surface.
- `en/` and `fr/` — public landing/content pages.
- `site/src/content/blog/` with `site/src/pages/blog/` — public blog/article surface using the Astro content schema in `site/src/content.config.ts`.
- `404.html` and extension-specific root index as distribution entry docs.

### Technical and discovery surfaces

- `src/` and `convex/` folders — implementation surfaces for workflows.
- `vite.*.config.ts`, `manifest.*.config.ts`, and `vercel.json` — release/build contracts.
- `scripts/` — operational helper surface.
- `shipflow_data/workflow/specs/` and `shipflow_data/workflow/research/` — planning and deep-dive evidence.

## Content Routing Rules

- Use `shipflow_data/business/product.md` and `shipflow_data/business/gtm.md` when changing positioning, audience, offer, or proof assumptions.
- Use `README.md` for scope, platform coverage, and onboarding truth updates.
- Use `shipflow_data/workflow/TASKS.md` for execution shifts and sequencing changes.
- Use `shipflow_data/technical/architecture.md` and `src/*` references when introducing architecture-level changes.
- Use `CHANGELOG.md` for externally visible milestone summaries.

## Repurposing Outputs

- For public-facing positioning content, target `/en` and `/fr` landing structures first, then keep links back to `README.md`.
- For public blog/article content, target `site/src/content/blog/` and respect the schema in `site/src/content.config.ts`.
- For technical summaries and workflow explanations, target `README.md` and `shipflow_data/workflow/TASKS.md`.
- For product truth updates, update linked artifact files in tandem (`shipflow_data/business/product.md`, `shipflow_data/business/gtm.md`) before publication-facing copy changes.
