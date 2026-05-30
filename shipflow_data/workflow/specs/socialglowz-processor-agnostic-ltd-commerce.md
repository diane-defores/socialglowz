---
artifact: spec
metadata_schema_version: "1.0"
artifact_version: "1.0.0"
project: "socialglowz"
created: "2026-05-30"
created_at: "2026-05-30 17:54:09 UTC"
updated: "2026-05-30"
updated_at: "2026-05-30 21:01:19 UTC"
status: ready
source_skill: sf-spec
source_model: "GPT-5 Codex"
scope: "commerce / payment-provider-agnostic-ltd"
owner: "Diane"
user_story: "En tant que creatrice de SocialGlowz, je veux vendre des Lifetime Deals directs via une architecture commerce agnostique des processeurs de paiement, afin de commencer a encaisser avec Lemon Squeezy sans verrouiller les entitlements, sans supprimer Polar, et sans envoyer les acheteurs directs vers une marketplace a commission."
confidence: high
risk_level: high
security_impact: "yes"
docs_impact: "yes"
linked_systems:
  - "SocialGlowz public pricing site"
  - "SocialGlowz app billing/access UI"
  - "WinFlowz suite commerce/API layer"
  - "WinFlowz suite identity and productEntitlements ledger"
  - "Lemon Squeezy checkout and webhooks"
  - "Polar checkout and webhooks"
  - "Lifetime Deal activation codes"
  - "direct / partner / marketplace internal sources"
depends_on:
  - artifact: "/home/claude/shipflow/skills/references/product-entitlements-playbook.md"
    artifact_version: "1.0.1"
    required_status: "active"
  - artifact: "shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md"
    artifact_version: "1.0.0"
    required_status: "ready"
  - artifact: "shipflow_data/workflow/specs/socialglowz-redemption-ui.md"
    artifact_version: "0.1.0"
    required_status: "shipped"
  - artifact: "/home/claude/winflowz/shipflow_data/workflow/specs/unified-suite-authentication.md"
    artifact_version: "1.0.25"
    required_status: "active"
supersedes:
  - "Provider-specific checkout surfaces as the only commerce architecture for new Flowz products"
evidence:
  - "/home/claude/socialglowz/site/src/pages/pricing.astro:192 exposes a Lifetime Deal section but the CTA still goes to appUrl() instead of a checkout."
  - "/home/claude/socialglowz/shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md:153 explicitly scoped out provider checkout integrations."
  - "/home/claude/winflowz/winflowz_site/src/pages/api/polar/checkout.ts:37 creates a Polar-specific checkout with POLAR_* env vars and formation-specific product metadata."
  - "/home/claude/winflowz/winflowz_site/convex/http.ts:34 handles Polar webhooks directly instead of a provider-normalized commerce event router."
  - "/home/claude/winflowz/winflowz_site/convex/schema.ts:27 defines the canonical productEntitlements ledger used by SocialGlowz through the suite adapter."
  - "Official Lemon Squeezy docs checked 2026-05-30: checkouts are created via POST /v1/checkouts, use product_options.redirect_url for the success redirect, accept checkout_data.custom, and return a hosted checkout URL."
  - "Official Lemon Squeezy docs checked 2026-05-30: webhooks use X-Event-Name and X-Signature headers, HMAC SHA-256 signatures, meta.custom_data, and order_created/order_refunded events for single payments."
next_step: "/sf-verify socialglowz-processor-agnostic-ltd-commerce after Lemon Squeezy test-mode and hosted Convex refund/replay smoke"
---

# Title

SocialGlowz Processor-Agnostic LTD Commerce

# Status

Ready for `/sf-start`. The product decision is fixed in this spec: direct SocialGlowz sales start from the public site, Lemon Squeezy is the first checkout provider, Polar is preserved as an adapter, and product access remains owned by the suite entitlement ledger.

# User Story

En tant que creatrice de SocialGlowz, je veux vendre des Lifetime Deals directs via une architecture commerce agnostique des processeurs de paiement, afin de commencer a encaisser avec Lemon Squeezy sans verrouiller les entitlements, sans supprimer Polar, et sans envoyer les acheteurs directs vers une marketplace a commission.

Acteurs:

- Diane, operatrice de SocialGlowz et de la suite Flowz.
- Acheteur direct d'un Lifetime Deal ou early-bird.
- Utilisateur SocialGlowz connecte qui active ou verifie son acces.
- Backend suite WinFlowz, proprietaire du ledger d'entitlements.
- Provider de paiement courant ou futur: Lemon Squeezy d'abord, Polar conserve, Stripe/Paddle/etc. possibles plus tard.
- Source marketplace/partenaire interne, sans exposition publique de marque marketplace sur le parcours direct.

Declencheur principal: un visiteur clique "Get Lifetime Access" sur le site SocialGlowz.

Resultat observable attendu: le visiteur est redirige vers un checkout Lemon Squeezy configure pour l'offre `socialglowz/lifetime_deal`; le webhook paye normalise l'evenement en entree commerce agnostique; le ledger suite cree l'acces ou un code d'activation traçable; l'app SocialGlowz affiche ou permet d'activer l'acces sans lire de payload provider direct.

# Minimal Behavior Contract

Le site public SocialGlowz accepte une intention d'achat Lifetime Deal et demande a une API serveur commerce de creer un checkout pour l'offre `socialglowz/lifetime_deal`. L'API choisit le provider configure, Lemon Squeezy en premier, puis retourne ou redirige vers une URL de checkout sans exposer de secrets. Quand le provider signale une commande payee ou remboursee par webhook signe, la suite transforme l'evenement en contrat commerce normalise, l'ecrit de facon idempotente, puis accorde ou retire l'acces dans le ledger canonique. Si le provider, la signature, le produit, le plan, l'environnement ou l'identite ne sont pas verifiables, aucun acces n'est accorde et l'evenement passe en erreur recuperable ou `pending_review`. L'edge case facile a rater est de croire que l'email de paiement suffit: il ne doit pas fusionner silencieusement des comptes ni devenir la preuve durable d'autorisation.

# Success Behavior

- Given le site SocialGlowz affiche l'offre Lifetime Deal, when un visiteur clique le CTA, then le site appelle une route checkout serveur et redirige vers une URL Lemon Squeezy ou retourne une erreur claire si le provider n'est pas configure.
- Given un checkout Lemon Squeezy est cree, when la requete API inclut store, variant, redirect URL et custom data, then la reponse contient une URL de checkout hebergee et aucune cle API n'est exposee au navigateur.
- Given Lemon Squeezy envoie `order_created` pour l'offre SocialGlowz, when la signature HMAC est valide et l'environnement correspond, then la suite ecrit un evenement commerce idempotent et cree soit un entitlement lie a une identite verifiee, soit un code d'activation/direct grant a activer par l'utilisateur.
- Given Lemon Squeezy envoie `order_refunded`, when l'ordre mappe un acces SocialGlowz actif, then la suite marque l'acces `refunded` ou equivalent non-granting et SocialGlowz ne considere plus l'utilisateur premium.
- Given le meme webhook est rejoue, when l'idempotency key provider existe deja, then aucun doublon d'entitlement, de code ou de revoke n'est cree.
- Given Polar existe deja pour WinFlowz formation, when l'architecture commerce est introduite, then le flux Polar actuel continue a fonctionner ou passe derriere un adaptateur avec regression tests, sans suppression de code fonctionnel.
- Given un canal marketplace ou partenaire genere des codes, when l'operateur importe ces codes, then ils utilisent le meme ledger suite et les sources restent internes, sans rediriger les acheteurs directs vers une marketplace.

# Error Behavior

- Provider non configure: route checkout renvoie 503 ou une page d'indisponibilite, aucun fallback vers AppSumo/marketplace.
- Offre inconnue, product id ou plan id non allowliste: rejet ou `pending_review`, jamais `active`.
- Signature webhook absente ou invalide: 401/403, aucun write entitlement, log redige.
- Payload webhook valide mais incomplet: evenement stocke comme `pending_review` si utile pour support, aucun access grant.
- Identite utilisateur absente ou seulement inferable par email: creer un code d'activation ou une entree reviewable, pas de merge silencieux.
- Environnement test/live incoherent: rejet ou pending review; ne jamais accorder un entitlement production depuis un evenement test.
- Provider timeout ou API rate limit pendant checkout: message recuperable, pas de side effect partiel.
- Refund, chargeback ou revoke apres activation: l'etat non-granting gagne sur tout cache local SocialGlowz.

# Problem

Le chantier precedent a corrige la source de verite des entitlements: SocialGlowz consomme maintenant le ledger suite. Mais le paiement lui-meme reste incomplet. Le site SocialGlowz montre une offre Lifetime Deal et envoie encore le CTA vers l'app, tandis que WinFlowz possede un flux Polar codé autour d'un cas formation. Ce n'est pas suffisant pour vendre des LTD directs, changer de provider plus tard, ni garder le cout administratif bas.

Le risque architectural est de refaire au niveau SocialGlowz un deuxieme systeme de commerce ou de brancher Lemon Squeezy directement dans l'app sans contrat transversal. Cela reproduirait le meme probleme que l'entitlement local: un provider devient la source de verite implicite, et chaque futur provider cree une nouvelle logique de grants/refunds/support.

# Solution

Construire une couche suite commerce agnostique:

- `CommerceOffer`: identifiant interne stable (`socialglowz/lifetime_deal`), mapping provider, prix public/config, environnement, URLs.
- `CommerceProviderAdapter`: interface checkout + webhook verification + normalisation d'evenements.
- `NormalizedCommerceEvent`: contrat commun pour `paid`, `refunded`, `cancelled/revoked`, `pending_review`, avec idempotency key, provider refs et payload redige.
- `CommerceFulfillment`: applique l'evenement normalise au ledger suite existant (`productEntitlements`, `productActivationCodes`, `productAccessEvents`) sans ecrire de durable truth dans SocialGlowz.

Lemon Squeezy est le premier adaptateur parce qu'il correspond au besoin operateur: checkout heberge, webhooks, single payments, et Merchant of Record pour reduire la charge TVA/sales tax sur les ventes via sa plateforme. Polar n'est pas supprime; son flux actuel est enveloppe progressivement dans le meme contrat ou conserve derriere un adaptateur `polar` compatible.

# Scope In

- Ajouter une architecture commerce agnostique dans le repo suite WinFlowz, proche de `winflowz_site`, parce que le ledger canonical y vit deja.
- Creer une route serveur checkout generique pour offres Flowz, avec une premiere offre `socialglowz/lifetime_deal`.
- Brancher le CTA Lifetime Deal du site SocialGlowz vers cette route checkout, pas vers l'app.
- Ajouter un adaptateur Lemon Squeezy pour creer un checkout heberge via API et verifier les webhooks signes.
- Normaliser les events Lemon Squeezy `order_created` et `order_refunded` pour le cas single-payment LTD.
- Refactorer ou envelopper le flux Polar existant comme provider `polar` sans supprimer le code actuel.
- Fulfillment suite: grant direct quand une identite verifiee existe; sinon generer/importer un code d'activation ou une entree `pending_review` avec support path clair.
- Garder AppSumo/marketplace/partner comme sources internes; aucune redirection publique vers AppSumo depuis le parcours direct.
- Ajouter une page success/cancel ou un retour checkout qui explique l'etape suivante: ouvrir l'app, se connecter, verifier/activer l'acces.
- Ajouter tests et checklist pour checkout, webhook signature, idempotence, refund, provider fallback et regression Polar.
- Mettre a jour README/docs/env examples pour les variables Lemon Squeezy, route commerce, sources internes et limites de preuve.

# Scope Out

- Pas de suppression du code Polar existant.
- Pas de migration complete de toute la facturation WinFlowz vers Lemon Squeezy.
- Pas d'abonnement Pro/Team SocialGlowz dans cette tranche; le focus est le single-payment Lifetime Deal.
- Pas d'App Store / Play Store in-app purchases.
- Pas de checkout embarque dans l'app mobile/desktop pour la premiere tranche.
- Pas de comptabilite complete, reporting fiscal interne, ou CRM.
- Pas de publicite AppSumo dans le parcours direct.
- Pas de fusion d'identites par email seul.
- Pas de changement de provider d'auth SocialGlowz.

# Constraints

- Source de verite d'acces: suite ledger seulement.
- Site = acquisition/checkout; app = statut, activation, usage produit.
- Lemon Squeezy et Polar sont des event sources, pas des authorization stores.
- Les secrets provider restent cote serveur uniquement.
- Webhooks verifies avec signature officielle et comparaison timing-safe.
- Idempotency obligatoire sur event id provider, fallback webhook id + order id quand necessaire.
- Environnements test/live/prod strictement separes.
- Activation codes traites comme bearer credentials: pas de log brut, pas de stockage client durable.
- Aucun libelle public "fondateur"; utiliser "Lifetime Deal", "early-bird", "code d'activation".
- Les montants, variant ids, product ids provider et campagne sont config, pas hardcodes dans le domaine commerce.

# Test Contract

surface: Astro site checkout route, WinFlowz suite API/Convex commerce layer, Lemon Squeezy webhook route, Polar regression path, SocialGlowz pricing CTA and app access state.

proof_profile: money/security integration. Automated tests are required before any manual/provider smoke claim, and provider smoke must run in test mode before production use.

proof_order:

1. Unit tests for offer config, provider selection, product/plan/source allowlists and redacted payload mapping.
2. Unit tests for Lemon Squeezy signature verification using raw body and `X-Signature`.
3. Route tests for checkout creation: missing env, invalid offer, provider API error, success redirect/URL.
4. Convex/suite tests for normalized event idempotence, direct grant, code creation/pending review, refund/revoke.
5. Polar regression tests proving existing WinFlowz formation checkout/webhook behavior is unchanged or intentionally adapted.
6. SocialGlowz site build/typecheck and link smoke for the Lifetime Deal CTA.
7. Lemon Squeezy test-mode smoke: create checkout, complete test order, receive `order_created`, verify SocialGlowz access path, simulate/perform refund, verify access removed.
8. Manual checklist for buyer journey and operator support recovery.

checklist_path: `shipflow_data/workflow/test-checklists/socialglowz-processor-agnostic-ltd-commerce.md`

required_scenario_ids:

- `socialglowz-ltd-site-cta-checkout`
- `commerce-offer-config-socialglowz-ltd`
- `lemonsqueezy-create-checkout-url`
- `lemonsqueezy-webhook-signature-invalid-denied`
- `lemonsqueezy-order-created-grants-or-code`
- `lemonsqueezy-order-refunded-revokes`
- `commerce-webhook-idempotent-replay`
- `commerce-unknown-offer-pending-review`
- `polar-formation-regression`
- `appsumo-marketplace-not-public-fallback`
- `socialglowz-app-access-after-direct-purchase`

required_results:

- Checkout creation returns a Lemon Squeezy hosted checkout URL only from server-side provider credentials.
- Invalid, missing, malformed or wrong-environment webhook data never grants access.
- A valid paid event produces exactly one normalized commerce event and one suite-ledger fulfillment result.
- A valid refund/revoke event makes SocialGlowz access non-granting even if a local/app cache previously showed active access.
- Fulfillment without a verified suite identity creates a safe activation/support path and never merges accounts by email alone.
- Polar's existing WinFlowz formation checkout/webhook path remains covered by regression proof or an explicitly documented compatibility adapter.
- Public SocialGlowz copy keeps direct buyers on the direct Lifetime Deal path and does not expose AppSumo as a public fallback.
- Documentation and env examples allow a fresh agent/operator to configure Lemon Squeezy test mode, webhook secret, offer mapping and smoke proof without conversation history.

exception_with_proof:

- If Lemon Squeezy account/store activation is not ready, implementation can complete local architecture and mocked adapter tests, but production-ready claim remains blocked.
- If no real buyer identity is present at checkout, fulfillment may create a one-time activation code or `pending_review` record; it must not grant access by email alone.

exception_without_proof:

- None for production access grants, webhook signature verification, or refund/revoke behavior.

# Dependencies

- `/home/claude/shipflow/skills/references/product-entitlements-playbook.md`: source of truth for provider events vs product entitlements.
- `/home/claude/socialglowz/shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md`: current suite adapter and bridge contract.
- `/home/claude/socialglowz/site/src/pages/pricing.astro`: current Lifetime Deal CTA surface.
- `/home/claude/winflowz/winflowz_site/convex/schema.ts`: canonical `productEntitlements`, `productActivationCodes`, `productAccessEvents`.
- `/home/claude/winflowz/winflowz_site/convex/bridge.ts`: current SocialGlowz suite bridge and activation-code lifecycle.
- `/home/claude/winflowz/winflowz_site/src/pages/api/polar/checkout.ts`: existing Polar checkout to preserve/refactor.
- `/home/claude/winflowz/winflowz_site/convex/http.ts`: existing Polar webhook processing to preserve/refactor.
- Lemon Squeezy official docs:
  - `https://docs.lemonsqueezy.com/api/checkouts/create-checkout`
  - `https://docs.lemonsqueezy.com/help/webhooks/webhook-requests`
  - `https://docs.lemonsqueezy.com/help/webhooks/signing-requests`
  - `https://docs.lemonsqueezy.com/help/webhooks/event-types`
  - `https://docs.lemonsqueezy.com/help/payments/sales-tax-vat`
- Local provider packages: no Lemon Squeezy SDK is currently installed in `winflowz_site` or SocialGlowz, so the first implementation should use the official REST API directly unless `/sf-start` deliberately adds a maintained SDK after fresh docs review. Polar is currently installed as `@polar-sh/sdk@0.43.1` in `winflowz_site`.

Fresh-docs verdict: fresh-docs checked. Official Lemon Squeezy docs confirm checkout creation by API, hosted checkout URL response, `product_options.redirect_url`, `checkout_data.custom`, webhook headers `X-Event-Name` and `X-Signature`, `meta.custom_data`, HMAC SHA-256 verification on raw body, `order_created` / `order_refunded` for single payments, and Merchant of Record sales tax/VAT handling for sales through Lemon Squeezy.

# Invariants

- `productId=socialglowz` and `plan=lifetime_deal` remain canonical for LTD access.
- Provider product/variant ids never replace internal product ids.
- Provider customer/order/subscription/license ids are references only.
- Refund/revoke status always wins over cached active access.
- Duplicate provider events are safe.
- Checkout success page is not proof of payment; webhook is the grant trigger.
- Direct checkout users are not sent to AppSumo.
- SocialGlowz app never requires the user to understand the provider.
- Public offer copy uses "Lifetime Deal" or "early-bird"; legacy internal aliases may exist only for migration compatibility.

# Links & Consequences

- Product: SocialGlowz can start selling without waiting for AppSumo approval.
- Architecture: commerce joins entitlements at the suite layer, preserving provider replaceability.
- Security: signed webhooks and idempotency become required gates before access writes.
- Admin: Lemon Squeezy reduces sales tax/VAT handling on sales through its MoR model, but operator still needs account/store/product activation and payout setup.
- Support: pending purchases must be inspectable by provider order id, email, internal offer id and activation-code/source ref, without exposing secrets.
- SEO/conversion: site pricing CTA becomes a real purchase path; app remains focused on activation/status.
- Deploy: requires env vars in the suite site and SocialGlowz site deploy contexts.
- Existing blockers: the suite entitlement adapter still needs hosted bridge smoke; this commerce chantier should not claim full production readiness until that lifecycle proof is cleared or incorporated.

# Documentation Coherence

Update or verify:

- `/home/claude/socialglowz/README.md`
- `/home/claude/socialglowz/shipflow_data/technical/context.md`
- `/home/claude/socialglowz/shipflow_data/technical/code-docs-map.md`
- `/home/claude/socialglowz/site/src/pages/pricing.astro`
- `/home/claude/winflowz/winflowz_site/README.md`
- `/home/claude/winflowz/winflowz_site/.env.example`
- `/home/claude/winflowz/shipflow_data/technical/context.md`
- `/home/claude/winflowz/shipflow_data/technical/code-docs-map.md`

Docs must say explicitly:

- Checkout is on the public site first.
- Entitlements are suite-owned.
- Lemon Squeezy is the first provider adapter, not the domain model.
- Polar is preserved as a provider adapter.
- AppSumo/marketplace sources are internal and not a public fallback for direct buyers.

# Edge Cases

- Buyer pays with one email but signs into SocialGlowz with another: do not auto-merge; provide code/support flow.
- Lemon Squeezy sends test-mode webhook to production URL or production webhook to preview URL.
- Checkout returns success but webhook is delayed.
- Webhook arrives before the user opens the app.
- Partial refund for a one-time LTD: define as non-granting unless explicit policy says otherwise.
- Duplicate refund after duplicate paid event.
- Provider changes event payload shape; parser must reject unknown critical fields instead of guessing.
- Polar and Lemon Squeezy both reference the same user/order-like source ref.
- Operator changes price or variant id during campaign; existing checkouts should remain traceable.
- User attempts to reuse a direct activation code generated from a paid order.

# Implementation Tasks

- [ ] Task 1: Define suite commerce domain types and offer config.
  - Files: `/home/claude/winflowz/winflowz_site/src/lib/commerce/types.ts`, `/home/claude/winflowz/winflowz_site/src/lib/commerce/offers.ts`.
  - Action: Add `CommerceOffer`, `CheckoutRequest`, `ProviderCheckoutResult`, `NormalizedCommerceEvent`, `CommerceFulfillmentResult`, provider ids and allowlisted `socialglowz/lifetime_deal`.
  - User story link: provider-agnostic direct LTD sales.
  - Depends on: none.
  - Validate with: unit tests for offer lookup, provider selection and invalid offer rejection.
  - Notes: Store provider product/variant ids in env/config mapping; internal ids stay stable.

- [ ] Task 2: Add commerce event storage/processing in the suite.
  - Files: `/home/claude/winflowz/winflowz_site/convex/schema.ts`, new `/home/claude/winflowz/winflowz_site/convex/commerce.ts` or existing bridge module.
  - Action: Add or reuse an append-only commerce/provider event record with idempotency, provider refs, environment, status, redacted payload summary and fulfillment result.
  - User story link: payment events become audit-safe and provider-neutral.
  - Depends on: Task 1.
  - Validate with: Convex tests for paid/refunded/idempotent/pending_review.
  - Notes: Reuse `productAccessEvents` when it remains sufficient; add `commerceEvents` only if provider order lifecycle needs fields that would overload access events.

- [ ] Task 3: Add Lemon Squeezy provider adapter.
  - Files: `/home/claude/winflowz/winflowz_site/src/lib/commerce/providers/lemonSqueezy.ts`, tests.
  - Action: Implement checkout creation, raw-body signature verification, event parsing and normalization for `order_created` and `order_refunded`.
  - User story link: first provider that lets SocialGlowz start selling.
  - Depends on: Task 1.
  - Validate with: mocked fetch tests, signature fixtures, invalid signature tests, unknown event tests.
  - Notes: Use official docs checked in this spec; compare HMAC with timing-safe equality.

- [ ] Task 4: Add generic checkout route.
  - Files: `/home/claude/winflowz/winflowz_site/src/pages/api/commerce/checkout.ts` or product-scoped equivalent.
  - Action: Accept an offer id such as `socialglowz_lifetime_deal`, validate it, create provider checkout and redirect or return URL.
  - User story link: site CTA can start payment without app complexity.
  - Depends on: Tasks 1 and 3.
  - Validate with: route tests for missing env, invalid offer, provider API failure and success.
  - Notes: Public route should not require existing SocialGlowz auth; identity can be resolved after payment via activation/support flow.

- [ ] Task 5: Add Lemon Squeezy webhook route.
  - Files: `/home/claude/winflowz/winflowz_site/src/pages/api/commerce/webhooks/lemon-squeezy.ts` or Convex HTTP equivalent.
  - Action: Read raw body, verify `X-Signature`, parse `X-Event-Name`, normalize event and call suite commerce fulfillment.
  - User story link: payment becomes access/code/revoke in suite ledger.
  - Depends on: Tasks 2 and 3.
  - Validate with: signature failure, malformed payload, duplicate event, paid event, refund event tests.
  - Notes: Return 2xx only when the event is safely captured or intentionally ignored; return non-2xx for retryable processing failure.

- [ ] Task 6: Fulfill SocialGlowz LTD purchases through suite ledger.
  - Files: `/home/claude/winflowz/winflowz_site/convex/commerce.ts`, `/home/claude/winflowz/winflowz_site/convex/bridge.ts`.
  - Action: Convert a normalized paid event into `productEntitlements` when a verified global identity is present, or generate/import a suite activation code / `pending_review` record when identity is not proven.
  - User story link: purchase produces usable SocialGlowz access without provider lock-in.
  - Depends on: Tasks 2 and 5.
  - Validate with: paid no-user creates safe activation path, paid verified-user grants, refund revokes.
  - Notes: No email-only merge.

- [ ] Task 7: Wrap or preserve Polar as a commerce provider adapter.
  - Files: `/home/claude/winflowz/winflowz_site/src/pages/api/polar/checkout.ts`, `/home/claude/winflowz/winflowz_site/convex/http.ts`, new `src/lib/commerce/providers/polar.ts`.
  - Action: Extract provider-specific mapping where safe, or add a compatibility adapter around current Polar behavior with no regression.
  - User story link: keep Polar available while architecture becomes agnostic.
  - Depends on: Tasks 1 and 2.
  - Validate with: existing Polar checkout/webhook tests or newly added regression tests.
  - Notes: Do not delete working Polar code in this chantier.

- [ ] Task 8: Wire SocialGlowz site CTA and checkout result pages.
  - Files: `/home/claude/socialglowz/site/src/pages/pricing.astro`, optional new `/home/claude/socialglowz/site/src/pages/purchase/success.astro`, `/home/claude/socialglowz/site/src/pages/purchase/cancel.astro`.
  - Action: Point "Get Lifetime Access" to the suite checkout route; add success/cancel pages that keep users on the direct path and point to app activation/status.
  - User story link: direct buyers can buy without going through marketplace branding.
  - Depends on: Task 4.
  - Validate with: site build and link smoke.
  - Notes: Public copy must not mention AppSumo as fallback.

- [ ] Task 9: Add operator runbook and env examples.
  - Files: `/home/claude/winflowz/winflowz_site/.env.example`, READMEs/docs listed in Documentation Coherence.
  - Action: Document Lemon Squeezy store id, variant id, API key, webhook secret, test mode, route URLs, checkout smoke steps and refund smoke steps.
  - User story link: Diane can operate sales without rediscovering setup details.
  - Depends on: Tasks 3-8.
  - Validate with: docs grep for no contradictory local-entitlement/provider-source claims.
  - Notes: Mention that account/store activation is an operator prerequisite.

- [ ] Task 10: Create manual checklist and run provider smoke.
  - Files: `shipflow_data/workflow/test-checklists/socialglowz-processor-agnostic-ltd-commerce.md`.
  - Action: Add scenarios from Test Contract and record local/test-mode/prod proof separately.
  - User story link: access can be sold and revoked safely.
  - Depends on: Tasks 1-9.
  - Validate with: checklist rows PASS/BLOCKED with evidence pointers.
  - Notes: Real production payment proof is not required before account setup exists, but production-ready claim is blocked without it.

# Acceptance Criteria

- [ ] CA 1: Given a visitor is on SocialGlowz pricing, when they click "Get Lifetime Access", then the request starts a server-side checkout for `socialglowz/lifetime_deal` rather than opening the app directly.
- [ ] CA 2: Given Lemon Squeezy is configured, when checkout creation succeeds, then the browser receives a hosted checkout URL and no API secret.
- [ ] CA 3: Given Lemon Squeezy is not configured, when checkout is requested, then the site returns a recoverable unavailable state and does not redirect to AppSumo.
- [ ] CA 4: Given a signed `order_created` webhook for the SocialGlowz offer, when it is processed once, then a normalized commerce event is stored and access/code fulfillment is triggered through the suite ledger.
- [ ] CA 5: Given the same webhook is replayed, when it is processed again, then no duplicate entitlement/code/event side effect occurs.
- [ ] CA 6: Given an invalid Lemon Squeezy webhook signature, when the route receives it, then it rejects the request and writes no entitlement.
- [ ] CA 7: Given a signed `order_refunded` webhook, when the order maps to active SocialGlowz access, then access becomes non-granting.
- [ ] CA 8: Given checkout buyer email differs from SocialGlowz login email, when fulfillment runs, then no automatic account merge happens by email only.
- [ ] CA 9: Given Polar checkout for WinFlowz formation is exercised, when this architecture is added, then the existing Polar path still passes regression checks.
- [ ] CA 10: Given public copy is inspected, when the direct LTD path is visible, then it says Lifetime Deal/early-bird/access code and does not expose AppSumo as a fallback route.
- [ ] CA 11: Given docs/env examples are read by a fresh agent, when implementation starts, then they show how to configure Lemon Squeezy test mode, webhook secret, offer mapping and proof steps.

# Test Strategy

- Run targeted unit tests in `winflowz_site` for commerce provider adapters and existing bridge helpers.
- Run Convex/suite tests for event idempotency and entitlement/code fulfillment.
- Run existing Polar tests plus any new regression tests for formation checkout/webhook mapping.
- Run SocialGlowz site build/check after CTA changes.
- Run SocialGlowz app access tests only if the app-side status/copy changes; otherwise use existing billing adapter tests as dependency proof.
- Perform Lemon Squeezy test-mode smoke after env setup:
  - create checkout from SocialGlowz site;
  - complete test order;
  - verify signed webhook;
  - verify suite entitlement/code result;
  - open SocialGlowz app and verify access/activation state;
  - refund/simulate refund and verify access removal.

# Risks

- High: payment/webhook code can grant unauthorized access if signature, product mapping or idempotency is weak.
- High: email-only matching could merge the wrong buyer/account.
- High: refund/chargeback handling can leave revoked buyers with access.
- Medium: provider abstraction can become too generic; keep it small around checkout, webhook normalization and fulfillment.
- Medium: Lemon Squeezy account/store verification can block real provider proof.
- Medium: Polar regression risk if refactor touches current formation checkout too aggressively.
- Medium: public pricing/campaign copy can accidentally push direct buyers toward a commission marketplace.
- Medium: MoR simplifies sales tax/VAT for Lemon Squeezy sales, but it does not remove all operator/legal obligations.

# Execution Notes

- Read first:
  - `/home/claude/socialglowz/shipflow_data/workflow/specs/socialglowz-suite-entitlement-adapter.md`
  - `/home/claude/winflowz/winflowz_site/convex/schema.ts`
  - `/home/claude/winflowz/winflowz_site/convex/bridge.ts`
  - `/home/claude/winflowz/winflowz_site/src/pages/api/polar/checkout.ts`
  - `/home/claude/socialglowz/site/src/pages/pricing.astro`
- Implementation order: domain types -> suite event/fulfillment -> Lemon adapter -> checkout route -> webhook route -> SocialGlowz CTA -> Polar compatibility -> docs/checklist.
- Prefer a narrow provider interface over a large billing framework.
- Keep provider payloads redacted; store provider ids and selected non-secret metadata only.
- Use raw body for webhook signature verification before JSON parsing.
- Stop and reroute if Lemon Squeezy docs/API contradict the assumptions recorded here, if provider setup requires a public claim not reviewed in copy, or if the implementation would grant access without suite ledger proof.
- Fresh external docs checked 2026-05-30:
  - Lemon Squeezy Create Checkout: `POST /v1/checkouts`, `product_options.redirect_url`, `checkout_data.custom`, returned hosted checkout `url`.
  - Lemon Squeezy Webhook Requests: `X-Event-Name`, `X-Signature`, JSON:API payload, retries on non-2xx.
  - Lemon Squeezy Signing Requests: HMAC SHA-256 over raw body and timing-safe comparison.
  - Lemon Squeezy Event Types: `order_created` and `order_refunded` for single-payment lifecycle; webhook custom data is read from `meta.custom_data`.
  - Lemon Squeezy Sales Tax and VAT: Merchant of Record model for platform sales.

# Open Questions

None.

Operator inputs required before real provider smoke, but not before architecture implementation:

- Lemon Squeezy store activation, API key, webhook secret, product/variant id and test/live mode.
- Final public price/currency/campaign cap; default implementation must read this from offer config/env, not provider code.
- Refund policy copy; default technical behavior is refund => non-granting access.
- Direct paid orders must issue an activation/support path by default when no explicit suite identity is known; auto-grant is allowed only when an explicit suite identity is verified. No email-only auto-grant.

# Skill Run History

| Date UTC | Skill | Model | Action | Result | Next step |
|----------|-------|-------|--------|--------|-----------|
| 2026-05-30 17:54:09 UTC | sf-spec | GPT-5 Codex | Created draft spec for processor-agnostic direct LTD commerce with Lemon Squeezy first, Polar preserved, and suite entitlement fulfillment. | draft | `/sf-ready socialglowz-processor-agnostic-ltd-commerce` |
| 2026-05-30 18:38:51 UTC | sf-ready | GPT-5 Codex | Evaluated readiness against user story, payment/webhook security, fresh Lemon Squeezy docs, task order, documentation coherence, language doctrine and test contract; added explicit required results and operator prerequisite wording. | ready | `/sf-start socialglowz-processor-agnostic-ltd-commerce` |
| 2026-05-30 19:45:00 UTC | sf-start | GPT-5 Codex | Implemented processor-agnostic suite commerce surfaces for SocialGlowz LTD, including Lemon Squeezy checkout/webhook, provider abstraction, Polar preservation, SocialGlowz CTA success/cancel pages, bridge payload compatibility fixes, env/checklist/doc-map updates, and targeted tests/build verification. | implemented | `/sf-verify socialglowz-processor-agnostic-ltd-commerce` |
| 2026-05-30 19:52:27 UTC | sf-start | GPT-5 Codex + GPT-5.3 Codex Spark subagent | Supervised subagent delivery, repaired post-subagent TypeScript integration errors, reran WinFlowz build check, targeted commerce/auth tests, SocialGlowz site build, and metadata lint. | implemented | `/sf-verify socialglowz-processor-agnostic-ltd-commerce` |
| 2026-05-30 20:04:18 UTC | sf-verify | GPT-5 Codex | Verified local implementation, corrected Lemon Squeezy official-contract details (`product_options.redirect_url`, `meta.custom_data`, exact raw-body signature), added checkout route and refund parser tests, reran targeted tests/builds/metadata/checklist gates, and identified remaining hosted/provider proof blockers. | partial | `/sf-verify socialglowz-processor-agnostic-ltd-commerce after Lemon Squeezy test-mode and hosted Convex refund/replay smoke` |
| 2026-05-30 20:36:12 UTC | sf-docs | GPT-5 Codex | Created Lemon Squeezy external platform and WinFlowz/SocialGlowz usage notes covering official REST/SDK baseline, no official CLI/MCP found, third-party MCP restrictions, validation routes, and code-doc map links. | documented | `/sf-verify socialglowz-processor-agnostic-ltd-commerce after Lemon Squeezy test-mode and hosted Convex refund/replay smoke` |
| 2026-05-30 20:42:17 UTC | sf-tasks | GPT-5 Codex | Added blocked follow-up tasks to SocialGlowz and WinFlowz local trackers for Lemon Squeezy test-mode buyer smoke and hosted Convex webhook/refund/replay proof. | tracked | `/sf-verify socialglowz-processor-agnostic-ltd-commerce after Lemon Squeezy test-mode and hosted Convex refund/replay smoke` |
| 2026-05-30 20:42:17 UTC | sf-ship | GPT-5 Codex | Evaluated ship request and did not formally ship the chantier because payment/webhook proof remains partial until Lemon Squeezy test-mode and hosted Convex refund/replay validation are available. | not shipped | `/sf-verify socialglowz-processor-agnostic-ltd-commerce after Lemon Squeezy test-mode and hosted Convex refund/replay smoke` |
| 2026-05-30 20:59:02 UTC | sf-build | GPT-5 Codex | Added direct/partner Lifetime Deal activation-code import tooling and runbook through the existing suite-backed adminUpsertRedemptionCode path, with parser/redaction tests, docs, env notes, changelog, and tracker closure. | implemented | `/sf-verify socialglowz-processor-agnostic-ltd-commerce after Lemon Squeezy test-mode and hosted Convex refund/replay smoke` |
| 2026-05-30 21:01:19 UTC | sf-verify | GPT-5 Codex | Verified the activation-code import/runbook slice with parser tests, billing adapter regression tests, dry-run CLI proof, targeted TypeScript, ESLint, metadata lint, diff whitespace check, and checklist review; global commerce proof remains partial due hosted Lemon Squeezy/Convex blockers. | partial | `/sf-verify socialglowz-processor-agnostic-ltd-commerce after Lemon Squeezy test-mode and hosted Convex refund/replay smoke` |

# Current Chantier Flow

sf-spec ✅ -> sf-ready ✅ -> sf-start ✅ -> sf-verify ⚠️ partial (import-runbook verified; hosted provider proof pending) -> sf-end ⏳ -> sf-ship ⚠️ not shipped

Reste a faire:

- Clear or incorporate hosted bridge smoke blockers from `socialglowz-suite-entitlement-adapter` before claiming production readiness.
- Configure Lemon Squeezy test-mode account/store/product/webhook before provider smoke.
- Run hosted Convex refund/replay proof for required checklist scenario `lemonsqueezy-order-refunded-revokes`.
