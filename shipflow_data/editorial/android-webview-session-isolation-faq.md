---
artifact: editorial_article_source
metadata_schema_version: "1.0"
artifact_version: "1.0.0"
project: "socialglowz"
created: "2026-05-23"
updated: "2026-05-23"
status: reviewed
source_skill: sf-redact
scope: android-webview-session-isolation-faq
owner: "Diane"
confidence: medium
risk_level: medium
security_impact: yes
docs_impact: yes
business_intent: informational
content_status: promoted_to_public_article
target_audience: "utilisateurs technophiles et contributeurs SocialGlowz"
primary_keyword: "isolation sessions Android WebView SocialGlowz"
depends_on:
  - artifact: "README.md"
    artifact_version: unknown
    required_status: unknown
  - artifact: "shipflow_data/technical/context.md"
    artifact_version: "1.0.0"
    required_status: reviewed
  - artifact: "shipflow_data/editorial/content-map.md"
    artifact_version: "1.0.1"
    required_status: reviewed
  - artifact: "shipflow_data/workflow/specs/android-webview-storage-isolation.md"
    artifact_version: "0.3.3"
    required_status: ready
supersedes: []
evidence:
  - "README.md"
  - "shipflow_data/technical/context.md"
  - "shipflow_data/editorial/content-map.md"
  - "shipflow_data/workflow/specs/android-webview-storage-isolation.md"
  - "site/src/content/blog/android-webview-session-isolation.md"
next_review: "2026-06-23"
next_step: "Auditer l'article public avant ship si la surface blog est incluse dans le prochain release scope."
---

# Comment SocialGlowz isole les sessions Android WebView par profil

Ce brouillon explique le modèle d'isolation Android WebView de SocialGlowz pour les utilisateurs technophiles et les contributeurs. Il décrit le comportement attendu sans élargir la promesse au-delà des mécanismes documentés dans le contexte technique et la spec Android WebView storage isolation.

## Résumé court

Sur Android, SocialGlowz isole les sessions WebView par profil et par réseau avec une clé de session de la forme `${profileId}-${networkId}`. Cette clé sert à séparer les cookies et les snapshots `localStorage` associés aux origines web chargées dans les WebViews natives.

Le cas CinderReels a déclenché ce durcissement parce que son authentification repose sur `localStorage`, pas uniquement sur les cookies. Les autres réseaux suivent le même principe global d'isolation, en utilisant leur URL principale comme origine de référence, avec la possibilité de déclarer des origines additionnelles quand un réseau utilise plusieurs domaines.

## Qu'est-ce qui est isolé ?

SocialGlowz vise deux types de stockage Android WebView pour chaque session `${profileId}-${networkId}` :

- les cookies, restaurés pour la session active avant la navigation ;
- les valeurs `localStorage`, persistées et restaurées par origine.

La séparation se fait donc sur deux axes :

- le profil SocialGlowz sélectionné ;
- le réseau ouvert dans la WebView Android.

En pratique, une session CinderReels du Profil A et une session CinderReels du Profil B ne doivent pas partager le même état de connexion via les cookies ou `localStorage`, même si elles chargent le même site.

## Pourquoi l'origine web compte

Les WebViews Android exposent du stockage web par origine. Une origine correspond au triplet schéma, hôte et port, par exemple `https://example.com`.

SocialGlowz ne traite pas `localStorage` comme un bloc global du réseau. Les snapshots sont liés à la session `${profileId}-${networkId}` et à l'origine exacte. Cela évite qu'une valeur capturée sur une origine soit restaurée sur une autre origine qui ne devrait pas la recevoir.

## Pourquoi CinderReels a une origine explicite

CinderReels a une règle plus explicite parce que son authentification utilise `localStorage`. Si SocialGlowz isolait seulement les cookies, deux profils pourraient encore se retrouver avec un état d'authentification partagé par le stockage web global de la WebView.

L'origine CinderReels est donc déclarée explicitement afin que l'isolation scriptée s'applique au bon domaine dès la navigation Android WebView concernée.

## Comment les autres réseaux sont couverts

Pour les autres réseaux Android WebView, SocialGlowz applique l'isolation globale par défaut avec la clé `${profileId}-${networkId}`. Quand aucun besoin multi-domaine particulier n'est documenté, l'origine de référence vient de l'URL principale du réseau.

Ce comportement couvre le cas courant : un réseau chargé depuis son domaine principal, avec cookies et `localStorage` associés à cette origine.

## Quand faut-il ajouter une matrice d'origines ?

Certains réseaux peuvent répartir l'authentification, les redirections ou les vues embarquées sur plusieurs domaines. Dans ce cas, une seule URL principale peut être insuffisante pour capturer tous les endroits où le réseau lit ou écrit son `localStorage`.

Une matrice d'origines additionnelles devient utile quand un réseau :

- redirige l'utilisateur vers un domaine d'authentification séparé ;
- charge une application web sur un sous-domaine différent de l'URL principale ;
- revient ensuite vers le domaine principal avec une session déjà initialisée ;
- stocke des valeurs d'authentification ou de préférence dans plusieurs origines HTTPS.

La règle de contribution est simple : ajouter uniquement les origines observées et nécessaires, puis valider qu'elles appartiennent au flux réel du réseau. Une origine additionnelle ne doit pas être ajoutée par supposition.

## Ce que cette isolation ne couvre pas

L'isolation Android WebView décrite ici ne promet pas une séparation complète de tous les stockages possibles du navigateur embarqué.

Ne sont pas couverts par cette garantie :

- `IndexedDB` ;
- `CacheStorage` ;
- les service workers ;
- le cache HTTP global de la WebView ;
- les credential stores système ;
- les autres mécanismes de stockage non capturés par cookies ou snapshots `localStorage`.

Ces limites sont importantes pour le support et les tests. Si un réseau déplace son authentification vers `IndexedDB` ou vers un service worker, l'isolation décrite ici ne suffit pas à elle seule pour garantir une séparation complète entre profils.

## Comment tester le comportement attendu

Le test utilisateur le plus lisible reste un scénario A/B/A :

1. ouvrir un réseau Android WebView avec le Profil A ;
2. se connecter au compte A ;
3. passer au Profil B pour le même réseau ;
4. vérifier que le compte A n'est pas visible ;
5. se connecter au compte B ;
6. revenir au Profil A ;
7. vérifier que le compte A revient sans afficher le compte B.

Pour CinderReels, ce test doit vérifier à la fois le comportement cookie et le comportement `localStorage`, puisque l'authentification connue du réseau dépend de `localStorage`.

## Note pour contributeurs

Les changements liés à cette isolation doivent rester alignés sur les invariants suivants :

- conserver la clé canonique `${profileId}-${networkId}` pour séparer les sessions ;
- restaurer cookies et `localStorage` avant que la page ne puisse utiliser l'état d'un autre profil ;
- valider les origines avant de persister ou restaurer des données ;
- ne pas utiliser un nettoyage global WebView comme mécanisme normal de changement de profil ;
- documenter explicitement tout mode dégradé quand une fonctionnalité Android WebView nécessaire n'est pas disponible.

Ce modèle protège le cas documenté des cookies et de `localStorage` par origine. Il ne doit pas être présenté comme une isolation totale de tout l'état navigateur Android.
