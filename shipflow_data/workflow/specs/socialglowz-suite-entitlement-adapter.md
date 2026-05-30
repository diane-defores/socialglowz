---
artifact: spec
metadata_schema_version: "1.0"
artifact_version: "1.0.0"
project: "socialglowz"
created: "2026-05-30"
created_at: "2026-05-30 07:45:11 UTC"
updated: "2026-05-30"
updated_at: "2026-05-30 17:17:01 UTC"
status: ready
source_skill: sf-spec
source_model: "GPT-5 Codex"
scope: "architecture-migration / entitlement-ledger"
owner: "Diane"
user_story: "En tant qu'operatrice de la suite Flowz, je veux que SocialGlowz consomme le ledger canonique d'entitlements de la suite au lieu de posseder une source de verite locale, afin d'eviter les doublons d'infrastructure, les divergences d'acces et les migrations couteuses."
confidence: high
risk_level: high
security_impact: "yes"
docs_impact: "yes"
linked_systems:
  - "SocialGlowz Convex Auth"
  - "SocialGlowz billing UI"
  - "WinFlowz suite identity"
  - "WinFlowz suite productEntitlements"
  - "WinFlowz suite productAccessEvents"
  - "Lifetime Deal activation codes"
  - "direct / partner / marketplace sources"
depends_on:
  - artifact: "/home/claude/shipflow/skills/references/product-entitlements-playbook.md"
    artifact_version: "1.0.1"
    required_status: "active"
  - artifact: "/home/claude/winflowz/shipflow_data/workflow/specs/unified-suite-authentication.md"
    artifact_version: "1.0.25"
    required_status: "active"
  - artifact: "/home/claude/winflowz/winflowz_app/docs/technical/suite-authentication.md"
    artifact_version: "1.0.9"
    required_status: "reviewed"
  - artifact: "shipflow_data/workflow/specs/socialglowz-billing-entitlements-foundation.md"
    artifact_version: "0.1.0"
    required_status: "shipped"
  - artifact: "shipflow_data/workflow/specs/socialglowz-redemption-ui.md"
    artifact_version: "0.1.0"
    required_status: "shipped"
supersedes:
  - "SocialGlowz local entitlement ledger as durable source of truth"
evidence:
  - "skills/references/product-entitlements-playbook.md:55 now defines suite-owned entitlement ledger as the default for suite products."
  - "skills/references/product-entitlements-playbook.md:72 defines Canonical Ledger Preflight before adding target-project entitlement tables."
  - "/home/claude/winflowz/winflowz_site/convex/schema.ts defines globalUsers, identityAccounts, productEntitlements and productAccessEvents."
  - "/home/claude/winflowz/winflowz_site/convex/bridge.ts exposes suite identity and entitlement snapshot patterns."
  - "/home/claude/socialglowz/convex/schema.ts currently defines local entitlements, redemptionCodes and billingEvents, creating a second ledger."
  - "/home/claude/socialglowz/convex/billing.ts currently grants SocialGlowz access from local Convex tables."
next_step: "/sf-start socialglowz-suite-entitlement-adapter"
---

# Title

SocialGlowz Suite Entitlement Adapter

# Status

Ready for `/sf-start`. This chantier repairs the architecture before more billing or Lifetime Deal work is built on top of the local SocialGlowz ledger. Do not add more product-local entitlement writes outside this migration path.

# User Story

En tant qu'operatrice de la suite Flowz, je veux que SocialGlowz consomme le ledger canonique d'entitlements de la suite au lieu de posseder une source de verite locale, afin d'eviter les doublons d'infrastructure, les divergences d'acces et les migrations couteuses.

Acteur principal: Diane, operatrice de la suite et de SocialGlowz.

Acteurs secondaires:

- utilisateur SocialGlowz avec compte Convex Auth;
- utilisateur qui active un code Lifetime Deal, early-bird, partner ou manuel;
- operateur support qui doit verifier un acces sans exposer de secrets;
- futur provider de paiement ou marketplace;
- backend WinFlowz suite qui possede l'identite globale et les entitlements.

Declencheurs:

- ajout ou activation d'un code Lifetime Deal dans SocialGlowz;
- lecture de l'etat d'acces produit dans les settings SocialGlowz;
- futur remboursement, revoke, migration provider ou support grant;
- decouverte d'une infrastructure canonique existante apres implementation locale partielle.

Resultat observable attendu: SocialGlowz garde son UX d'activation et d'etat d'acces, mais la reponse durable "ce compte a-t-il acces a socialglowz/lifetime_deal ?" vient du ledger suite. Les tables locales deja creees ne sont plus etendues comme source de verite et sont documentees comme transitionnelles jusqu'a migration/suppression controlee.

# Minimal Behavior Contract

SocialGlowz accepte un utilisateur authentifie via son auth actuelle, puis demande au backend suite de verifier, creer ou mettre a jour l'identite globale associee et l'entitlement `product_id=socialglowz`. L'activation d'un code Lifetime Deal, early-bird, partner ou manuel est traitee dans le ledger suite avec idempotence, audit et statut canonique. SocialGlowz affiche l'etat retourne par la suite et refuse de considerer ses tables locales comme autorite durable. Si le bridge suite est indisponible, mal configure, ou retourne un payload invalide, l'UI affiche un etat recuperable "acces non verifiable" sans accorder de capacite premium. L'edge case facile a rater est de remplacer un doublon par un autre: l'adaptateur ne doit pas fusionner silencieusement un compte SocialGlowz Convex Auth avec un compte Clerk existant sur la meme adresse email.

# Success Behavior

- Given le ledger suite existe, when SocialGlowz ajoute `socialglowz` comme produit, then le produit est allowliste dans la suite sans creer un second ledger durable.
- Given un utilisateur SocialGlowz est connecte, when l'UI lit son acces, then SocialGlowz appelle un bridge serveur et affiche l'etat retourne par le ledger suite.
- Given un utilisateur saisit un code Lifetime Deal valide, when SocialGlowz le soumet, then le code est valide cote suite, marque comme utilise, et cree ou met a jour un `productEntitlements` suite pour `socialglowz/lifetime_deal`.
- Given le meme utilisateur re-soumet le meme code, when l'entitlement existe deja, then l'operation est idempotente et ne cree pas de doublon.
- Given un second utilisateur tente d'utiliser un code deja utilise, when la suite verifie le code, then l'acces est refuse sans exposer le compte qui l'a utilise.
- Given un provider futur envoie un refund/revoke, when l'evenement est traite par la suite, then SocialGlowz voit l'acces inactif au prochain refresh ou via un cache/mirror invalide.
- Given les anciennes tables SocialGlowz existent, when un nouveau developpement touche billing/entitlements, then ces tables sont traitees comme migration/compat uniquement, pas comme source de verite.
- Given le bridge suite est indisponible, when l'utilisateur ouvre les settings, then l'UI affiche un etat indisponible et n'accorde aucun nouvel acces.

# Error Behavior

- Secret de bridge absent cote SocialGlowz: l'action serveur retourne une erreur redigee; l'UI affiche que l'acces ne peut pas etre verifie.
- Secret de bridge invalide cote suite: la suite refuse la requete avec un statut non ambigu et aucun entitlement n'est ecrit.
- Utilisateur SocialGlowz sans email verifie ou sans identifiant stable: l'activation est bloquee ou placee en `pending_review`, selon le contrat final, sans grant implicite.
- Compte Clerk suite existant avec la meme adresse email: aucun merge silencieux; le bridge cree ou utilise une identite liee au provider SocialGlowz, ou demande un flow de linking explicite dans une future spec.
- Code invalide, desactive ou deja utilise par autrui: erreur UX sure, pas de details sensibles.
- Produit, plan ou source non allowlistes: evenement `pending_review` ou rejet, jamais `active`.
- Tables locales SocialGlowz contenant deja des rows: ne pas les supprimer automatiquement; les migrer ou les laisser en lecture de compat selon un plan explicite.
- Retour suite malformed: SocialGlowz considere l'acces non verifiable et loggue seulement un diagnostic redige.

# Problem

SocialGlowz a recu une fondation locale d'entitlements (`entitlements`, `redemptionCodes`, `billingEvents`) avant que le preflight du ledger suite existant soit applique. Or WinFlowz possede deja le socle transverse: `globalUsers`, `identityAccounts`, `productEntitlements`, `productAccessEvents`, bridge suite et doctrine `Clerk/Firebase/provider ids are references; entitlements are server-owned truth`.

Le risque n'est pas specifique a SocialGlowz. C'est l'anti-pattern general suivant: recreer partiellement dans un produit cible une infrastructure canonique qui existe deja au niveau suite. Cela produit deux sources de verite, des divergences support, des migrations inutiles, et un risque de grant/revoke incoherent.

# Solution

Converger vers une architecture "suite ledger first":

- WinFlowz suite reste proprietaire de l'identite globale, des entitlements, des events et des codes canoniques.
- SocialGlowz garde l'UX, les textes produit, les gates et l'appel serveur.
- Le code local SocialGlowz existant est neutralise comme autorite durable, puis remplace par un adaptateur/bridge.
- La suppression des tables locales attend une migration ou la preuve qu'aucune donnee utile n'existe en production.

La premiere implementation doit privilegier un bridge SocialGlowz Convex Auth -> suite plutot qu'une migration immediate de SocialGlowz vers Clerk. Cette route preserve le produit actuel, reduit le risque de casser l'onboarding, et respecte la regle "pas de merge silencieux par email". Une migration Clerk complete peut rester une future spec.

# Scope In

- Ajouter `socialglowz` et `lifetime_deal` aux allowlists suite necessaires.
- Generaliser ou ajouter un endpoint/bridge suite pour verifier l'entitlement d'un produit allowliste, pas seulement ReplayGlowz ou WinFlowz App.
- Ajouter dans la suite le stockage canonique des codes d'activation si absent, ou un equivalent documente attache a `productEntitlements` / `productAccessEvents`.
- Ajouter un chemin serveur SocialGlowz qui appelle le bridge suite pour:
  - lire l'acces produit;
  - activer un code;
  - retourner un snapshot d'acces redige a l'UI.
- Garder les signatures UX SocialGlowz proches de l'existant: Lifetime Deal, code d'activation, pas de marque AppSumo visible par defaut.
- Documenter les tables locales SocialGlowz comme transitionnelles, cache, migration adapter ou deprecated.
- Ajouter tests unitaires ou integration legere pour:
  - idempotence code;
  - refus second utilisateur;
  - fail-closed bridge;
  - non-merge silencieux email.
- Mettre a jour docs SocialGlowz et suite pour indiquer la source de verite.

# Scope Out

- Pas de `git revert` brutal des commits deja pousses.
- Pas de suppression immediate des tables locales SocialGlowz sans audit de donnees.
- Pas de migration complete de SocialGlowz vers Clerk dans cette tranche.
- Pas de provider de paiement complet Stripe/Paddle/Lemon Squeezy/Polar pour SocialGlowz.
- Pas de mise en avant publique d'AppSumo dans l'UI SocialGlowz.
- Pas de pricing public, facturation, taxes, invoices ou comptabilite.
- Pas de feature gating premium complet tant que les fonctionnalites payantes SocialGlowz ne sont pas nommees.
- Pas de fusion automatique entre identites SocialGlowz Convex Auth et Clerk sur email seul.

# Constraints

- Le ledger canonique est suite-owned par defaut selon `product-entitlements-playbook.md`.
- Les activation codes sont des bearer credentials: ne pas logger les codes bruts, ne pas les persister cote client, ne pas exposer la table codes aux clients.
- Les provider ids, emails, Clerk ids, Firebase ids ou Convex Auth ids sont des references d'identite, pas des preuves d'autorisation produit.
- Les environnements local, preview/staging et production ne doivent pas melanger les entitlements.
- Tout bridge serveur doit verifier un secret ou une signature serveur-serveur.
- Les erreurs UI doivent rester redigees et actionnables.
- Les anciennes specs SocialGlowz restent des traces historiques; elles ne doivent plus guider de nouveaux writes locaux d'entitlements sans cette spec.
- Les fichiers sales preexistants dans les repos SocialGlowz, WinFlowz et ShipFlow ne doivent pas etre embarques dans ce chantier sauf s'ils sont explicitement lies a cette migration.

# Test Contract

surface: Convex suite ledger, Astro/API bridge suite, SocialGlowz Convex actions/composables, settings UI.

proof_profile: regression-first and scenario-first. This is security/billing-adjacent work, so local unit checks are necessary but insufficient without lifecycle smoke proof.

proof_order:

1. Suite unit tests for product allowlist, code redemption, idempotency, used-code refusal, unknown product/plan handling.
2. SocialGlowz composable tests for safe error mapping and bridge unavailable state.
3. SocialGlowz Convex/backend tests for server action fail-closed behavior where feasible.
4. Typecheck suite for changed Convex/API code in WinFlowz.
5. Typecheck/lint targeted SocialGlowz surfaces.
6. Manual or scripted smoke with realistic test account:
   - create/import code in suite;
   - activate from SocialGlowz;
   - verify active access through SocialGlowz UI path;
   - attempt reuse by another account;
   - revoke/refund/disable in suite;
   - verify SocialGlowz no longer grants access.

checklist_path: `shipflow_data/workflow/test-checklists/socialglowz-suite-entitlement-adapter.md`

required_scenario_ids:

- `suite-product-allowlist-socialglowz`
- `suite-code-import-lifetime-deal`
- `socialglowz-redeem-code-active`
- `socialglowz-redeem-code-same-user-idempotent`
- `socialglowz-redeem-code-second-user-denied`
- `socialglowz-bridge-unavailable-denied`
- `socialglowz-revoked-access-denied`
- `socialglowz-no-silent-email-merge`

required_results:

- Suite code lifecycle tests show create/import, redeem, same-user idempotency, second-user denial and revoke/refund/disable access removal.
- SocialGlowz UI-facing access state reads from the suite adapter, not from local entitlement truth.
- Bridge unavailable or malformed suite response returns a safe inactive/unavailable state.
- Documentation names the suite ledger as canonical and marks SocialGlowz local tables as transition/migration only.

exception_with_proof:

- If deployed bridge smoke cannot run because env secrets are absent, the implementation may stop before ship only with a redacted env-gap note, passing local tests, and no production-ready claim.
- If local SocialGlowz entitlement rows exist and cannot be migrated immediately, they may remain as compatibility data only after a documented audit/runbook proves they do not override suite revokes.

exception_without_proof:

- None. Access grants, code redemption, and revoke/refund behavior require proof before this chantier can ship.

# Dependencies

- `/home/claude/shipflow/skills/references/product-entitlements-playbook.md`: canonical doctrine for suite-owned ledgers and duplicate-ledger stop condition.
- `/home/claude/winflowz/shipflow_data/workflow/specs/unified-suite-authentication.md`: suite identity and entitlement architecture.
- `/home/claude/winflowz/winflowz_site/convex/schema.ts`: current suite ledger schema.
- `/home/claude/winflowz/winflowz_site/convex/bridge.ts`: current bridge patterns and product allowlist.
- `/home/claude/winflowz/winflowz_site/convex/polar.ts`: current idempotent provider event pattern.
- `/home/claude/socialglowz/convex/billing.ts`: local implementation to replace or turn into adapter.
- `/home/claude/socialglowz/src/composables/useBillingAccess.ts`: UI-facing access composable.
- `/home/claude/socialglowz/src/ui/setup/pages/SocialGlowz/components/BillingAccessPanel.vue`: current activation UI.

Fresh-docs verdict: fresh-docs not needed for spec creation because the decision is governed by local suite architecture and the existing ShipFlow product entitlement playbook. Implementation should check current Convex docs only if action/auth/HTTP behavior is unclear while coding.

# Invariants

- Account existence never grants product access.
- SocialGlowz product access is `product_id=socialglowz`.
- `lifetime_deal` is the canonical plan id for LTD/early-bird/direct permanent access unless a later pricing spec changes it.
- Public UI says "Lifetime Deal" / "code d'activation"; marketplace source names remain internal by default.
- Suite ledger writes are idempotent.
- Revoke/refund/disable removes access without deleting identity.
- Same email across providers is not enough for automatic account linking.
- Local SocialGlowz entitlement rows cannot override a revoked or missing suite entitlement after migration.

# Links & Consequences

- Security: reduces duplicate authorization state and enforces fail-closed bridge behavior.
- Support: one canonical place to inspect grants, revokes, refunds and code usage.
- Product: preserves the beginner-friendly SocialGlowz activation UX while removing architecture drift.
- Data migration: local SocialGlowz tables may require a one-time audit/import if any codes or entitlements exist outside dev.
- Deployment: requires secrets on both sides, likely `SOCIALGLOWZ_SUITE_BRIDGE_SECRET` and a suite endpoint URL.
- Documentation: SocialGlowz technical docs must stop calling local `convex/billing.ts` the durable source of truth.
- Future providers: Stripe/Paddle/Lemon Squeezy/AppSumo/direct import should write to suite events, not SocialGlowz local tables.

# Documentation Coherence

Update or verify:

- `/home/claude/socialglowz/shipflow_data/technical/context.md`
- `/home/claude/socialglowz/shipflow_data/technical/code-docs-map.md`
- `/home/claude/socialglowz/README.md`
- `/home/claude/socialglowz/CHANGELOG.md`
- `/home/claude/socialglowz/shipflow_data/workflow/TASKS.md`
- `/home/claude/winflowz/shipflow_data/workflow/specs/unified-suite-authentication.md` or a suite-auth follow-up note if product allowlist/bridge behavior changes
- `/home/claude/winflowz/winflowz_site/README.md` if operator bridge setup changes

Docs must explicitly say that the local SocialGlowz billing tables are not the long-term source of truth after this migration starts.

# Edge Cases

- Local dev has no suite bridge secret configured.
- User has anonymous or incomplete SocialGlowz auth state.
- User has same email in Clerk suite and SocialGlowz Convex Auth.
- Activation code was imported locally before this migration.
- Activation code exists in both local and suite stores.
- Suite returns active, then later revoke/refund arrives.
- App is offline or Convex Auth session is stale.
- Browser/mobile UI opens settings while bridge request is in flight.
- Operator imports a partner batch with duplicate codes.
- Old local `founder_ltd` plan id appears in data or tests.

# Implementation Tasks

- [x] Task 1: Freeze local authority in docs and code comments.
  - Files: `convex/billing.ts`, `shipflow_data/technical/context.md`, `shipflow_data/technical/code-docs-map.md`.
  - Action: Mark current local entitlement path as transitional/deprecated authority before new writes are extended.
  - Validation: grep docs/code for "source of truth" and ensure suite ledger is named canonical.

- [x] Task 2: Add SocialGlowz to suite product/plan/source allowlists.
  - Files: `/home/claude/winflowz/winflowz_site/src/lib/suiteBridge.ts`, `/home/claude/winflowz/winflowz_site/convex/bridge.ts`, related tests.
  - Action: Add `socialglowz`, `lifetime_deal`, and required sources without changing existing WinFlowz/ReplayGlowz behavior.
  - Validation: suite tests prove existing products still resolve.

- [x] Task 3: Add suite-owned activation-code storage or equivalent.
  - Files: `/home/claude/winflowz/winflowz_site/convex/schema.ts`, new or existing suite Convex module.
  - Action: Store activation codes in the suite layer with status, source, productId, plan, redeemedBy, redeemedAt, idempotency/audit references.
  - Validation: same-user idempotent redemption and second-user denial.

- [x] Task 4: Add generic suite entitlement operations.
  - Files: `/home/claude/winflowz/winflowz_site/convex/bridge.ts` or a new `convex/entitlements.ts`.
  - Action: Provide protected operations for product snapshot, code redemption, manual grant/revoke, and audit event writes for allowlisted products.
  - Validation: unknown product/plan/source rejected or pending_review; revoke removes access.

- [x] Task 5: Add SocialGlowz server-to-server bridge endpoint.
  - Files: `/home/claude/winflowz/winflowz_site/src/pages/api/bridge/*`, suite server env docs.
  - Action: Accept only secret-protected SocialGlowz backend calls, normalize identity payload, and call suite Convex operations.
  - Validation: missing/invalid secret denies; malformed payload denies; no raw code logging.

- [x] Task 6: Replace SocialGlowz local billing calls with suite adapter.
  - Files: `convex/billing.ts` or new `convex/suiteBilling.ts`, `src/composables/useBillingAccess.ts`, `src/ui/setup/pages/SocialGlowz/components/BillingAccessPanel.vue`.
  - Action: Keep UI behavior but route read/redeem through server-side suite bridge; local tables become compatibility only.
  - Validation: UI shows active, free/no access, bridge unavailable, invalid code and already-used states.

- [x] Task 7: Add migration guard for existing local rows.
  - Files: `convex/billing.ts`, optional script/runbook under `scripts/` or docs.
  - Action: Detect existing local entitlements/codes and provide a safe manual migration/import path to suite before deletion.
  - Validation: no automatic destructive delete; runbook explains how to audit and migrate.

- [x] Task 8: Update tests.
  - Ajouté: `socialglowz/convex/billing.test.ts` couvre redemption idempotent/multi-compte, refus bridge indisponible, and parity admin operations.
  - Files: `/home/claude/winflowz/winflowz_site/**/*test*`, `convex/billing.test.ts`, `src/composables/useBillingAccess.test.ts`.
  - Action: Cover idempotency, second-user denial, fail-closed bridge, old-plan compatibility, and safe UI error mapping.
  - Validation: targeted tests pass in both repos.

- [x] Task 9: Update docs and trackers.
  - Files: docs listed in Documentation Coherence.
  - Action: Align docs with suite-owned canonical ledger and deprecate local source-of-truth wording.
  - Validation: `rg -n "local entitlement|source of truth|product access|billing" shipflow_data README.md` shows no contradictory claims.

- [ ] Task 10: Smoke proof.
  - Files: test checklist path.
  - Action: Execute or document the representative lifecycle: import code, redeem, same-user idempotent, second-user denied, revoke denied.
  - Validation: checklist completed or marked blocked with concrete env/config gap.
  - Note: le smoke technique peut utiliser un code suite `manual`/`direct` en environnement de recette; le test réel d'une source marketplace/AppSumo reste bloqué tant que la candidature marketplace n'est pas faite/validée.

- [ ] Task 11: Marketplace activation readiness.
  - Files: test checklist path, operator runbook if needed.
  - Action: Après candidature/validation AppSumo ou autre marketplace LTD, importer un code réel dans le ledger suite avec une source interne, puis tester une activation de bout en bout sans exposer la marque marketplace dans l'UX publique.
  - Validation: un test réel marketplace/LTD est ajouté au checklist ou à un runbook de recette, avec preuve redigée.

# Acceptance Criteria

- [x] AC 1: SocialGlowz no longer treats local `entitlements` as the durable authority for new product access.
- [x] AC 2: The suite ledger supports `product_id=socialglowz` and `plan=lifetime_deal`.
- [x] AC 3: A SocialGlowz user can activate a suite-owned code from the existing settings UI.
- [x] AC 4: Same-user reactivation is idempotent.
- [x] AC 5: A second user cannot reuse a single-use activation code.
- [x] AC 6: Missing or invalid bridge config fails closed with safe UI state.
- [x] AC 7: No code path silently merges SocialGlowz Convex Auth and Clerk identities by email alone.
- [ ] AC 8: Revoked/refunded/disabled suite access is reflected as inactive in SocialGlowz.
- [x] AC 9: Docs identify the suite ledger as canonical and local SocialGlowz billing tables as transitional/compatibility.
- [x] AC 10: No unrelated dirty files are staged, reverted, or shipped by this chantier.

# Test Strategy

Automated:

- WinFlowz suite Convex tests for entitlement operations and code lifecycle.
- SocialGlowz composable tests for UI state/error mapping.
- SocialGlowz Convex/action tests where the test harness supports the bridge boundary.
- Targeted TypeScript/Convex typechecks in both repos.
- Lint for touched TypeScript/Vue surfaces.

Manual / smoke:

- Use non-production or test env secrets.
- Create a realistic test code in suite.
- If marketplace onboarding is not active yet, use a `manual` or `direct` suite-owned test code for technical smoke and keep the real marketplace/AppSumo activation as a follow-up task.
- Sign into SocialGlowz with a test account.
- Redeem from settings.
- Verify access state in UI.
- Attempt second-account reuse.
- Revoke/disable/refund in suite and verify SocialGlowz denies access.

Exception:

- If full deployed smoke is blocked by missing secrets or environment, implementation can stop at local tests only if docs mark the deployment proof gap and no production claim is made.

# Risks

- High: identity bridge can accidentally merge accounts by email if implemented too aggressively.
- High: duplicate local/suite rows can disagree until migration is complete.
- High: activation codes are bearer credentials and must not leak through logs or client persistence.
- Medium: Convex Auth in SocialGlowz and Clerk in suite create support complexity until explicit account linking exists.
- Medium: suite bridge endpoints require new env vars and deployment coordination.
- Medium: tests may need adjustment because SocialGlowz currently uses Convex Auth while suite uses Clerk/Firebase bridge patterns.

# Execution Notes

- Sequence must be one repo/surface at a time; do not parallelize writes without explicit execution batches.
- Start with suite model/bridge, then SocialGlowz adapter, then docs/smoke.
- Do not delete local tables in the first implementation unless an explicit data audit proves they are empty and unused.
- Keep AppSumo as an internal `source` only; direct Lifetime Deal flow remains first-class.
- Before implementing provider-specific APIs, apply the documentation freshness gate and use official provider docs.

# Open Questions

None blocking for readiness. The default decision is to keep SocialGlowz Convex Auth for now and bridge it to the suite ledger without silent email merge. A future spec can decide whether SocialGlowz should migrate fully to Clerk suite identity.

# Skill Run History

| Date UTC | Skill | Model | Action | Result | Next step |
|----------|-------|-------|--------|--------|-----------|
| 2026-05-30 07:45:11 UTC | sf-spec | GPT-5 Codex | Created repair spec after discovering SocialGlowz local entitlement ledger duplicates existing suite-owned infrastructure. | draft | `/sf-ready socialglowz-suite-entitlement-adapter` |
| 2026-05-30 07:48:07 UTC | sf-ready | GPT-5 Codex | Evaluated user-story fit, duplicate-ledger stop condition, suite bridge scope, security constraints and proof contract. | ready | `/sf-start socialglowz-suite-entitlement-adapter` |
| 2026-05-30 15:31:57 UTC | sf-start | GPT-5.3 Codex | Implemented tranche 1 suite-first: allowlists, suite activation-code schema, bridge Convex operations, SocialGlowz API endpoint and transition docs freeze. | partial | `/sf-start socialglowz-suite-entitlement-adapter (tranche 2: SocialGlowz runtime adapter + smoke)` |
| 2026-05-30 15:38:00 UTC | sf-verify | GPT-5 Codex | Verified tranche 1 with targeted suite/SocialGlowz tests, typechecks, metadata lint and diff hygiene; confirmed runtime adapter, revoke/refund parity, checklist and smoke proof remain missing. | partial | `/sf-start socialglowz-suite-entitlement-adapter (tranche 2: SocialGlowz runtime adapter + smoke)` |
| 2026-05-30 16:54:00 UTC | sf-start | GPT-5.3 Codex | Terminé tranche 2: adaptation SocialGlowz vers bridge suite, parité operations suite, mapping erreurs UI et mise à jour docs. | partial | `/sf-verify socialglowz-suite-entitlement-adapter` |
| 2026-05-30 16:55:38 UTC | sf-start | GPT-5.3 Codex Spark | Finalisation tranche 2: validation ciblée (tests/lint/typecheck/metadata/diff), preuve lifecycle et clôture chantier partielle. | partial | `/sf-verify socialglowz-suite-entitlement-adapter` |
| 2026-05-30 17:14:37 UTC | sf-verify | GPT-5 Codex | Vérifié tranche 2, corrigé les appels bridge admin SocialGlowz en actions Convex, remis le checklist au format machine-readable et rejoué les checks ciblés; smoke bridge hébergé encore absent. | partial | `/sf-test socialglowz-suite-entitlement-adapter --preview` après configuration bridge |

# Current Chantier Flow

sf-spec ✅ -> sf-ready ✅ -> sf-start ⚠️ -> sf-verify ⚠️ -> sf-end ⏳ -> sf-ship ⏳

Reste a faire:

- Exécuter la preuve smoke/preview complète (création/import code, activation réelle, idempotence, refus second compte, revoke/refund/refused access).
- Lever les blocs `BLOCKED` dans `shipflow_data/workflow/test-checklists/socialglowz-suite-entitlement-adapter.md` après configuration d’une base bridge de recette.
- Postuler/obtenir l'accès AppSumo ou marketplace LTD équivalente, puis tester une activation marketplace réelle comme source interne du ledger suite.
