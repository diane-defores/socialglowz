---
artifact: exploration_report
metadata_schema_version: "1.0"
artifact_version: "1.0.0"
project: "socialglowz"
created: "2026-05-23"
updated: "2026-05-23"
status: draft
source_skill: sf-explore
scope: "socialglowz product improvement ideas"
owner: "Diane"
confidence: medium
risk_level: medium
security_impact: yes
docs_impact: yes
linked_systems:
  - "shipflow_data/business/product.md"
  - "shipflow_data/workflow/TASKS.md"
  - "src/ui/setup/pages/SocialGlowz"
  - "src/config/socialNetworks.ts"
  - "src-tauri/plugins/android-webview"
evidence:
  - "shipflow_data/business/product.md defines target users, problems, desired outcomes, and risks."
  - "shipflow_data/workflow/TASKS.md records Android WebView isolation as completed and OAuth positive callback testing as deferred."
  - "sf-explore/SKILL.md requires durable exploration reports for substantial multi-option exploration."
depends_on:
  - artifact: "shipflow_data/business/product.md"
    artifact_version: "1.0.1"
    required_status: reviewed
  - artifact: "shipflow_data/workflow/TASKS.md"
    artifact_version: unknown
    required_status: unknown
supersedes: []
next_step: "/sf-spec SocialGlowz profile session health and network readiness improvements"
---

# Exploration Report: SocialGlowz Product Improvement Ideas

## Starting Question

The user asked what other ideas could improve SocialGlowz after the Android WebView storage isolation work.

## Context Read

- `shipflow_data/business/product.md` - anchors ideas in target users: creators, operators, marketers, and small teams managing multiple social accounts.
- `shipflow_data/workflow/TASKS.md` - shows Android WebView storage isolation is complete and Android OAuth positive callback testing remains deferred.
- `/home/claude/shipflow/skills/sf-explore/SKILL.md` - defines this as exploration only, not implementation.

## Internet Research

None. This exploration is based on local project truth and prior shipped session-isolation work.

## Problem Framing

SocialGlowz's strongest product direction is not "more networks at any cost"; it is predictable multi-profile social operations. Improvements should reduce session confusion, make network readiness visible, and speed up everyday switching without expanding into full campaign analytics or ad management.

## Option Space

### Option A: Session Health Center

- Summary: expose a per-profile, per-network health view that shows whether cookies, `localStorage`, backup, and degraded WebView modes are OK.
- Pros: directly reduces support/debug time; builds user trust around profile boundaries.
- Cons: risks exposing sensitive concepts if copy is too technical; needs careful redaction and no secret values.

### Option B: Network Readiness Matrix

- Summary: show network capabilities and limits in product/admin UI: main origin, extra origins, auth storage covered, not-covered storage, Android degraded state.
- Pros: turns the current technical matrix into an operator/contributor feature; helps safely add new networks.
- Cons: can become noisy if shown to normal users; likely needs an advanced view.

### Option C: A/B/A Session Test Wizard

- Summary: guided manual test flow for a network: Profile A login, Profile B isolation check, Profile A restore check.
- Pros: matches the real validation mode for Android; excellent for new networks and QA handoff.
- Cons: still manual unless device automation is added; must avoid storing account identifiers.

### Option D: Profile Switch Guardrails

- Summary: before opening a sensitive network in a different profile, show lightweight context cues: active profile, network, session age, and last verified state.
- Pros: prevents accidental work in the wrong profile; useful beyond Android.
- Cons: too many warnings can slow operators down; needs a quiet UI.

### Option E: Smart Network Onboarding Checklist

- Summary: when adding a network, guide the contributor/operator through URL, storage origins, auth behavior, mobile QA, and docs checklist.
- Pros: turns implicit architecture into a repeatable workflow; reduces drift in `src/config/socialNetworks.ts`.
- Cons: more valuable for contributors than end users unless paired with custom network setup.

### Option F: Session Backup Inspector

- Summary: show what categories a backup contains per profile/network: cookies, `localStorage` origins, settings, and explicit non-covered areas.
- Pros: improves trust before restore/migration; supports power users.
- Cons: sensitive surface; must never show raw cookie or `localStorage` values.

## Comparison

Highest immediate leverage:

1. Session Health Center - strongest user trust and support value.
2. A/B/A Session Test Wizard - strongest QA value, especially Android.
3. Network Readiness Matrix - strongest contributor/platform scalability value.

Most sensitive:

- Session Backup Inspector, because it touches backup and auth-adjacent data.
- Session Health Center, if it accidentally reveals too much detail.

Best product fit:

- Session Health Center plus A/B/A Test Wizard as one "Session Safety" area.

## Emerging Recommendation

Start with a small "Session Safety" feature:

- per active profile/network state summary,
- visible degraded Android WebView warnings,
- A/B/A checklist mode for manual validation,
- no raw storage values,
- links to the technical isolation explainer for power users.

This builds directly on the shipped Android isolation work and stays aligned with the product promise: sessions stay where they should.

## Non-Decisions

- No decision to add automated Android device testing.
- No decision to expose raw storage, cookies, or localStorage values.
- No decision to build a full network capability dashboard for every user.

## Rejected Paths

- Full analytics suite - outside product non-goals.
- Large public capability matrix for all users - likely too noisy before an advanced/settings surface exists.
- Per-network custom logic everywhere - conflicts with the shared isolation model; prefer declarative metadata.

## Risks And Unknowns

- UI placement: settings, network detail panel, profile dashboard, or QA/admin view.
- Data sensitivity: summaries must not leak cookies, tokens, localStorage values, account IDs, or customer data.
- Android validation: real proof still depends on GitHub Actions / Blacksmith APK install for Android behavior.
- Cross-platform promise: desktop, extension, and web may not support the same signals as Android.

## Redaction Review

- Reviewed: yes
- Sensitive inputs seen: none
- Redactions applied: none
- Notes: The report avoids raw storage values and account identifiers.

## Decision Inputs For Spec

- User story seed: As a SocialGlowz operator using multiple profiles, I want to see whether each profile/network session is healthy and isolated, so I can trust switches before login-sensitive work.
- Scope in seed: session safety status, degraded warnings, A/B/A checklist, no raw secrets, Android validation copy.
- Scope out seed: automated device farm, full analytics, raw cookie/localStorage inspector.
- Invariants/constraints seed: no secret values; preserve `${profileId}-${networkId}` as session boundary; Android proof remains APK artifact install.
- Validation seed: unit/type checks, UI smoke if applicable, site/docs checks if docs updated, manual Android APK A/B/A test for behavior changes.

## Handoff

- Recommended next command: `/sf-spec SocialGlowz profile session health and network readiness improvements`
- Why this next step: the best idea affects product UI, session/auth-adjacent behavior, Android validation, and docs, so it deserves a spec before implementation.

## Exploration Run History

| Date UTC | Prompt/Focus | Action | Result | Next step |
|----------|--------------|--------|--------|-----------|
| 2026-05-23 19:24:47 UTC | Other app improvement ideas | Compared product ideas after Android WebView isolation work. | Recommended a small Session Safety feature as the strongest next direction. | `/sf-spec SocialGlowz profile session health and network readiness improvements` |
