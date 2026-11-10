# Journal de preuves — rapport 30-40 pages

> **Règle** : 5 minutes à la fin de **chaque** phase, remplir le bloc correspondant.
> Ne jamais attendre la phase 12 : les commandes, les chiffres et les noms de captures s'oublient.
> Ce fichier n'est pas le rapport ; c'est la matière première du rapport.

## Budget de pages (guide, phase 12)

| # | Chapitre | Pages | Rédige | Relit |
|---|---|---|---|---|
| 1 | Introduction, contexte, problématique | 2-3 | A | B |
| 2 | État de l'art : microservices, DevSecOps, OWASP, JWT, mTLS | 4-5 | A + B | croisé |
| 3 | Architecture du projet et périmètre | 3-4 | A | B |
| 4 | Démarche : SSDLC, STRIDE, DREAD, lien avec les TP | 3 | B | A |
| 5 | CI/CD et qualité (TP2, TP5) : pipeline, JaCoCo, Sonar, Trivy | 5 | A | B |
| 6 | Cryptographie et PKI (TP1) | 3 | B | A |
| 7 | Sécurisation des communications : JWT, TLS, mTLS (TP3) | 6-7 | B | A |
| 8 | Données et infrastructure (TP4) : RDS, avant/après | 3-4 | B | A |
| 9 | Déploiement Docker, ECR, EC2 (TP3 suite) | 3 | A | B |
| 10 | Observabilité et réponse à incident | 2-3 | A | B |
| 11 | Résultats, limites, discussion | 3 | A + B | croisé |
| 12 | Conclusion et perspectives | 1-2 | A + B | croisé |
| | Références | 2 | A + B | |

**Cible** : 34-38 pages hors annexes, pour rester dans la marge 30-40 **références incluses**.
Contrôle final : `pdfinfo rapport/main.pdf | grep Pages`.

## Où sont les preuves

- Captures : `docs/preuves/pXX-<sujet>/` (commitées, jamais de secret visible).
- Commandes : recopier la commande **et** sa sortie utile (pas d'écran entier illisible).
- Chaque affirmation du rapport doit renvoyer à une figure, un tableau ou une sortie de commande.

## Plan de captures — quand capturer

> Une capture se prend **au moment où la preuve existe** : la PR avant de la fusionner, le 401 au moment du test,
> le rapport Sonar avant de corriger. Après, il est souvent trop tard (branche supprimée, seuil modifié, etc.).
> Outil : `Win + Maj + S`, enregistrer en PNG dans le dossier indiqué, puis `git add`.

| Phase | Moment précis | À capturer | Fichier | Chapitre |
|---|---|---|---|---|
| 1 | PR #1 encore visible (onglet *Pull requests*, filtre *Closed*) | page avec `Merged` en violet | `p01-depot/01-pr-mergee.png` | 5 |
| 1 | page du dépôt | sélecteur de branches `main` + `dev` | `p01-depot/02-branches.png` | 3 |
| 2 | avant de démarrer les services | fenêtre Git Bash avec les 6 commandes + sorties | `p02-menace/01-constats-code.png` | 5 |
| 2 | gateway + services démarrés | `localhost:8080/actuator/env` (JSON, barre d'URL visible) | `p02-menace/02-actuator-env.png` | 5 |
| 2 | idem | `localhost:8081/owners` **sans passer par le gateway** | `p02-menace/03-contournement-gateway.png` | 5 |
| 2 | idem | `localhost:8761` (Eureka, aucune connexion) | `p02-menace/04-eureka-ouvert.png` | 5 |
| 2 | Grafana démarré | `localhost:3030` sans authentification | `p02-menace/05-grafana-anonyme.png` | 5 |
| 3 | PR ouverte, checks en cours/terminés | checks verts (JDK 17 + 21) | `p03-ci/01-pr-checks.png` | 5 |
| 3 | onglet Actions, run terminé | détail du run + matrice | `p03-ci/02-run-actions.png` | 5 |
| 3 | après `clean verify` | `target/site/jacoco/index.html` (par module) | `p03-ci/03-jacoco.png` | 5 |
| 3 | Settings → Branches | règle de protection de `main` | `p03-ci/04-protection-main.png` | 5 |
| 4 | SonarCloud, analyse terminée | tableau de bord + Quality Gate | `p04-qualite/01-sonar-dashboard.png` | 5 |
| 4 | Sonar, onglet Issues | problèmes fiabilité/sécurité + couverture | `p04-qualite/02-sonar-issues.png` | 5 |
| 4 | logs du run | tableau Trivy (CVE) | `p04-qualite/03-trivy.png` | 5 |
| 4 | GitHub → Security | alertes Dependabot | `p04-qualite/04-dependabot.png` | 5 |
| 4 | terminal / CI | sortie TruffleHog ou gitleaks | `p04-qualite/05-secrets.png` | 5 |
| 5 (B) | après génération PKI | chaîne CA → serveur (`openssl verify`, `keytool -list`) | `p05-pki/01-chaine.png` | 6 |
| 6 (B) | Keycloak configuré | realm + client + utilisateur | `p06-jwt/01-keycloak.png` | 7 |
| 6 (B) | test écriture | **401 sans jeton** | `p06-jwt/02-401.png` | 7 |
| 6 (B) | test écriture | **201 avec jeton** | `p06-jwt/03-201.png` | 7 |
| 6 (B) | jwt.io (sans signature) | `sub`, `aud`, `scope` visibles | `p06-jwt/04-jwt-decode.png` | 7 |
| 7 (B) | TLS activé | 200 avec `--cacert`, échec sans | `p07-tls/01-tls.png` | 7 |
| 7 (B) | mTLS strict | handshake refusé sans certificat client, accepté avec | `p07-tls/02-mtls.png` | 7 |
| 7 (B) | Eureka | instances en `https` | `p07-tls/03-eureka-https.png` | 7 |
| 8 (B) | création RDS (réglages du TP4) | Public access Yes, chiffrement/backups décochés | `p08-rds/01-avant.png` | 8 |
| 8 (B) | après durcissement | Private, chiffrement, backups, SG restreint | `p08-rds/02-apres.png` | 8 |
| 8 (B) | Workbench | 3 schémas + tables | `p08-rds/03-schemas.png` | 8 |
| 8 (B) | redémarrage | données toujours présentes | `p08-rds/04-persistance.png` | 8 |
| 8 (B) | GitHub Secrets | **noms seulement**, valeurs masquées | `p08-rds/05-secrets.png` | 8 |
| 9 | build + push | images construites / poussées | `p09-deploy/01-images.png` | 9 |
| 9 | ECR | 5 dépôts + *Scan on push* | `p09-deploy/02-ecr.png` | 9 |
| 9 | EC2 | `docker compose ps` sur l'instance | `p09-deploy/03-ec2.png` | 9 |
| 9 | navigateur | application via l'URL EC2 | `p09-deploy/04-url.png` | 9 |
| 9 | terminal | `whoami` dans le conteneur = `spring` | `p09-deploy/05-nonroot.png` | 9 |
| 10 | après réduction Actuator | `env` = 404, `health` = 200 | `p10-durcissement/01-actuator.png` | 10 |
| 10 | Eureka protégé | 401 sans identifiants | `p10-durcissement/02-eureka-auth.png` | 10 |
| 10 | Grafana | écran de connexion obligatoire | `p10-durcissement/03-grafana-login.png` | 10 |
| 10 | `docker compose config` | ports internes non publiés | `p10-durcissement/04-ports.png` | 10 |
| 10 | ZAP terminé | résumé FAIL/WARN | `p10-durcissement/05-zap.png` | 10 |
| 11 | Grafana | dashboard latence + 401/403 | `p11-observabilite/01-dashboard.png` | 10 |
| 11 | alerte déclenchée | notification / seuil dépassé | `p11-observabilite/02-alerte.png` | 10 |
| 11 | Zipkin | trace gateway → service | `p11-observabilite/03-trace.png` | 10 |

**Règles** : barre d'URL visible quand c'est une page web ; flouter tout mot de passe/token/clé ; nommer `NN-sujet.png` ; committer les captures dans la même PR que la phase.

---

## Phase 0 — Préparation

- **Fait** : outils vérifiés ; build de référence vert.
- **Chiffres** : Maven 3.9.8 ; Java 21.0.11 pour `./mvnw` (JDK 25 dans le PATH) ; Git 2.54 ; 5 modules ; **25 tests** ; `BUILD SUCCESS`.
- **Correctif** : 4 fichiers `mockito-extensions/org.mockito.plugins.MockMaker` (mode subclass, plus de self-attach JVM).
- **Commande** : `./mvnw -B clean verify`.
- **Figure** : aucune (éventuellement sortie build en annexe).
- **Chapitre** : 5 (outillage, reproductibilité) ; annexe A.
- **À retenir pour le rapport** : le projet se compile et se teste **avant** toute modification de sécurité → base saine.

## Phase 1 — Dépôt, branches, nettoyage

- **Fait** : `.gitignore` secrets/certificats ; suppression du bot upstream et du template de PR ; scripts alignés sur 5 modules ; `PLAN-PROJET` corrigé (14 tests métier) ; README genai hors périmètre ; phase 2 affectée à A.
- **Preuve Git** : PR **#1** fusionnée, commit `ec1b8ad`.
- **Capture à faire (si absente)** : page de la PR #1 avec `Merged` → `docs/preuves/p01-depot/01-pr-mergee.png`.
- **Figure** : capture PR (gouvernance).
- **Chapitre** : 5 (revue de code, traçabilité) ; 3 (périmètre).
- **À retenir** : la règle « jamais de push direct sur main/dev » est **matérialisée** par une PR fusionnée.

## Phase 2 — Modèle de menace

- **Fait** : `SECURITE-modele-de-menace.md` à la racine ; 14 constats C1-C14 ; 15 menaces STRIDE F1-F9 (+ F10, F11 ajoutées) ; cotation DREAD ; 5 captures « avant ».
- **Commandes** : `grep -l security ...`, `grep -n "include" ...`, `grep -n -A1 "ports:" docker-compose.yml`, `grep -n -A8 "auth.anonymous" docker/grafana/grafana.ini`, `grep -n "USER" docker/Dockerfile`.
- **Captures** : `01-constats-code`, `02-actuator-env`, `03-contournement-gateway`, `04-eureka-ouvert`, `05-grafana-anonyme`.
- **Figures** : schéma des flux F1-F9 ; tableau STRIDE ; tableau DREAD ; tableau avant/après (à compléter au fil des phases).
- **Chapitres** : 4 (démarche STRIDE/DREAD) et 5 (analyse de risque).
- **À retenir** : les 3 priorités (F4-a Actuator, F1-a authentification, F7 Grafana) justifient l'ordre des phases 6, 7, 10.

## Phase 3 — CI + JaCoCo

- **Fait** : profil `quality` (JaCoCo 0.8.12, `prepare-agent` + `report` + `check`) ; seuil **60 %** de lignes ; `deploy.yml` (matrice JDK 17/21, `fetch-depth: 0`, cache Maven, artefacts) ; ancien `maven-build.yml` supprimé ; **2 tests ajoutés** à `visits` (builder + création).
- **Couverture mesurée** (lignes) : `customers` **67,9 %** · `vets` **64,7 %** · `visits` **84,8 %** · `gateway` **73,9 %** · `discovery` exclu (peu de code).
- **Tests** : 27 au total (customers 10, vets 2, visits 4, discovery 1, gateway 10).
- **Commande locale** : `./mvnw -B -P quality clean verify` → `BUILD SUCCESS`.
- **Captures** : `p03-ci/01-pr-checks` (2 checks verts), `02-run-actions`, `03-jacoco`, `04-protection-main`.
- **Figures** : schéma du pipeline (jobs → steps), PR verte, rapport JaCoCo.
- **Chapitre** : 5.
- **À retenir** : la couverture est une **mesure**, pas une opinion ; elle est reproductible par `-P quality` et bloquante en CI.

## Phase 4 — Sonar + SCA + secrets

- **Fait** : projet SonarCloud `DevSecOps-ENSIAS_petclinic-security-automation` (org `devsecops-ensias`) ; analyse Maven avec import du rapport JaCoCo ; Dependabot (Maven, GitHub Actions, Docker) ; Trivy (`vuln,secret,misconfig`) ; Gitleaks retiré (licence organisation) → les secrets sont couverts par Trivy.
- **Résultats (analyse du 08/11/2026, branche `main`)** :
  - Quality Gate : **Passed**
  - Sécurité : **D** (1 issue High, `java:S4684`) → **A** après correction
  - Fiabilité : **A**
  - Maintenabilité : **A**
  - Couverture : **68,3 %**
  - Duplications : **0,0 %**
  - Security Hotspots : **0**
  - Issues : **1** (S4684) → **0** après correction
  - Lignes de code analysées : **1,9 k**
- **SCA** :
  - Trivy : **270 occurrences** (comptage **par cible**, modules hors build inclus), dont **55 critiques** sur le pom racine ; **0 secret** détecté ; **3 misconfigurations** sur `docker/Dockerfile` (root, HEALTHCHECK, image).
  - Dependabot : **0 alerte ouverte**, **784 dépendances** analysées (graphe de dépendances actif).
  - Sonar : **258 risques de dépendances** signalés dans la décoration de PR.
- **Correctif** : DTO `VisitRequest` (mass assignment S4684), commit `9293e8b`, test d'abus prouvant que `id`/`petId` envoyés par le client sont ignorés.
- **Captures** : `01-pr-checks`, `02-sonar-dashboard`, `03-sonar-issues`, `04-trivy`, `05-dependabot`.
- **Décisions** : corriger S4684 (fait) ; accepter les duplications (0 %) ; planifier la remédiation des CVE en phase 10 (suppression de `jolokia-core` et `chaos-monkey`, mises à jour Dependabot) ; garder Trivy non bloquant (`exit-code: 0`) tant que la remédiation n'est pas terminée.
- **Risque résiduel** : CVE transitives non corrigées, SCA non bloquant, couverture < 100 %, `discovery-server` non mesuré.
- **Phrase clé** : deux outils SCA donnent des chiffres très différents (270 vs 0) car leurs bases (NVD vs GHSA) et leurs périmètres (fichiers vs graphe résolu) diffèrent — écart expliqué dans le rapport.

### Texte prêt à coller — chapitre 5 (phase 4)

> L'analyse SonarCloud du projet (5 modules, 28 tests, 1,9 k lignes) attribue une note de fiabilité **A**, une note de maintenabilité **A** et une couverture de lignes de **68,3 %** (customers 67,9 %, vets 64,7 %, visits 84,8 %, gateway 73,9 %), au-dessus du seuil de 60 % imposé par le Quality Gate. La note de sécurité initiale était **D** en raison d'une vulnérabilité de *mass assignment* (`S4684`) sur `VisitResource` : l'entité JPA était liée directement au corps de la requête, permettant de fixer l'identifiant et le propriétaire de la visite. La remédiation a consisté à introduire un DTO `VisitRequest` restreint aux champs métier et à ajouter un test d'abus ; après réanalyse, l'issue est fermée et la note passe à **A**. Le Quality Gate est **Passed**, sans duplication ni hotspot résiduel.
>
> L'analyse de composition logicielle a été conduite avec deux outils complémentaires. Trivy (base NVD) relève 270 occurrences de vulnérabilités réparties par cible, dont 55 critiques sur le pom racine, ainsi que trois mauvaises configurations du `Dockerfile` (utilisateur root, absence de HEALTHCHECK, image non épinglée). Dependabot, fondé sur le graphe de dépendances résolu (784 paquets) et la base GHSA, ne signale aucune alerte ouverte. Cet écart, loin d'être une contradiction, illustre les limites du SCA : les bases de vulnérabilités et les périmètres d'analyse diffèrent, et un comptage par fichier surestime nécessairement les occurrences. La remédiation est planifiée : suppression des dépendances `jolokia-core` et `chaos-monkey` devenues inutiles, application des mises à jour proposées par Dependabot, puis mesure avant/après.

### Pourquoi la capture 02 doit être refaite

La capture `02-sonar-dashboard.png` prise le 08/11 montre **Security D** et **1 issue** : c'est l'état *avant* correction `S4684`.
Le texte du rapport affirme **Security A** et **0 issue** : la figure doit correspondre à l'état final, sinon le jury voit une contradiction.
Conserver l'ancienne sous `02b-sonar-avant.png` (preuve du « avant » dans le tableau avant/après), et refaire `02-sonar-dashboard.png` après la réanalyse de `main`.

## Phase 4 bis — Sécurité de la chaîne (A)

- **À faire** : SBOM CycloneDX, Semgrep (SAST), Trivy bloquant, épinglage de toutes les actions en SHA, TruffleHog.
- **Chapitre** : 5.

## Phases 5-7 — binôme (B)

- **Fait** : PKI (PR #29), JWT (PR #30), TLS puis mTLS (PR #38).
- **Captures** : `docs/preuves/p05-pki/`, `p06-jwt/`, `p07-tls/`.
- **Chapitres** : 6, 7.
- **À compléter par B** : chiffres exacts (handshake, 401/201), figures.

## Phase 8 — RDS MySQL (A)

- **Fait** : instance RDS MySQL 8, `db.t3.micro`, région `us-east-1`, endpoint `petclinic-db.chciaky805n5.us-east-1.rds.amazonaws.com`.
- **Durcissement** : chiffrement activé, backups 7 jours, SG restreint, accès public désactivé.
- **Schémas** : `petclinic_customers`, `petclinic_vets`, `petclinic_visits` ; utilisateur `petclinic_app` (droits limités + `REFERENCES`).
- **Profil** : `application-prod.yml` ×3, URL par `${SPRING_DATASOURCE_URL}`.
- **Persistance** : après redémarrage, l'owner id 11 « Test Persistance » est toujours présent ; **11** owners (10 + 1), sans doublon (`INSERT IGNORE`).
- **Avant/après** : public + non chiffré + sans backup (TP4) → privé + chiffré + backups.
- **Captures** : `01a-avant-configuration`, `01b-avant-reseau`, `02-apres`, `02b-apres-prive`, `03-schemas`, `04-persistance`, `05-secrets`.
- **Chapitre** : 8.
- **Incident rencontré** : `Access denied` puis `REFERENCES command denied` — corrigés par réinitialisation du mot de passe maître et ajout du privilège `REFERENCES` à `petclinic_app`.

## Phase 9 — Docker, ECR, EC2

- **À remplir** : Dockerfile non-root, images, scan ECR, déploiement, URL de démonstration.
- **Chapitre** : 9.

## Phase 10 — Durcissement

- **À remplir** : Actuator (env 404 / health 200), Eureka 401, Grafana non anonyme, ports non publiés, ZAP.
- **Chapitre** : 10 (et tableau avant/après).

## Phase 11 — Observabilité

- **À remplir** : dashboards, alertes, trace gateway → service, runbook.
- **Chapitre** : 10.

## Phases 12-13 — Rapport et soutenance

- **À remplir** : nombre de pages final, liste des figures, bibliographie, plan de démo.

---

## Tableau « avant / après » (à compléter au fil des phases)

| Domaine | Avant (constat) | Après | Preuve |
|---|---|---|---|
| Authentification | aucune (C1) | JWT Keycloak | phase 6 |
| Transport | HTTP clair (C4) | TLS, mTLS | phase 7 |
| Contournement gateway | ports 8081-8083 publiés (C3) | non publiés | phases 9-10 |
| Actuator | `*` (C2) | `health,info,prometheus` | phase 10 |
| Eureka | ouvert (C5) | basic auth | phase 10 |
| Grafana | anonyme Admin (C6) | connexion obligatoire | phase 10 |
| Conteneurs | root (C7) | non-root | phase 9 |
| Retry | POST rejoué (C9) | GET seulement | phase 6 |
| Base | HSQLDB mémoire | RDS chiffrée, 3 schémas, 11 owners persistés | phase 8 |
| Qualité | build seul | JaCoCo, Sonar, Trivy, Dependabot | phases 3-4 |
| Chaîne logicielle | scans non bloquants | SBOM, Semgrep, Trivy bloquant, SHA pins | phase 4 bis |
| Gouvernance | main ouverte | PR + checks | phase 3 |

## Références à intégrer (liste courante)

- OWASP Top 10 (2021) ; OWASP API Security Top 10 (2023).
- NIST SP 800-204 (sécurité des microservices) ; NIST SP 800-57 (clés).
- RFC 8446 (TLS 1.3) ; RFC 7519 (JWT) ; RFC 5280 (X.509).
- Documentation Spring Security, Spring Cloud Gateway, Spring Boot Actuator.
- Documentation Keycloak ; AWS RDS ; AWS ECR ; SonarQube Cloud ; JaCoCo.
- Microsoft STRIDE (threat modeling).
