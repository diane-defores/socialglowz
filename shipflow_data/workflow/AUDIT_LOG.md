# Audit Log — SocialGlowz

> Project-local audit history. Append-only.

🟡 [socialglowz] audit: extension dependencies | date: 2026-05-26 | overall: B | issues: prod audit clean; full audit keeps 1 moderate dev-only uuid via web-ext; vite-plugin-pwa advisory path removed; ws via Convex mitigated with ws@8.20.1 override

| Date | Scope | Code | Design | Copy | SEO | GTM | Translate | Deps | Perf | Overall | Issues |
|------|-------|------|--------|------|-----|-----|-----------|------|------|---------|--------|
| 2026-02-27 | Full project | — | B | — | — | — | — | — | — | B | 2 critical + 4 high + 5 medium fixed — all issues resolved |
| 2026-03-07 | Full project | — | B+ | — | — | — | — | — | — | B+ | 1🔴 8🟠 7🟡 found — 1🔴 8🟠 5🟡 fixed, 2🟡 remaining |
| 2026-04-06 | Full project | C | — | — | — | — | — | — | — | C | 2🔴 5🟠 8🟡 found — 2🔴 3🟠 4🟡 fixed |
| 2026-04-27 | Dependencies | — | — | — | — | — | — | D | — | D | 0 critical, 40 high, 34 moderate, 7 low; runtime `vue-i18n` direct vuln + many build-chain advisories |
| 2026-04-27 | Dependencies fix pass | — | — | — | — | — | — | B- | — | B- | 81 advisories reduced to 8; remaining 2 high + 6 moderate require major/migration decisions |
| 2026-04-28 | Full project | C+ | — | — | — | — | — | — | — | C+ | 0 critical + 2 high + 3 medium found — 1 high + 1 medium fixed |
| 2026-04-30 | Dependencies | — | — | — | — | — | — | B- | — | B- | 0 critical, 0 high, 1 moderate dev/build advisory; 10 patch, 14 minor, 28 major outdated; 3 unknown licenses; 31 overrides undocumented |
| 2026-05-11 | Documentation layout | — | — | — | — | — | — | — | — | Pass | Root ShipFlow docs migrated or removed; workflow specs, bug, research, audit log, and competitor registry moved under `shipflow_data/`; metadata lint passed |
