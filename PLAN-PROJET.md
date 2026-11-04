# Plan du projet — Sécurité de la communication entre microservices

Projet DevSecOps (ENSIAS). Application : Spring PetClinic Microservices réduite à **3 microservices métier**
(`customers-service` 8081, `visits-service` 8082, `vets-service` 8083) + **2 composants d'infrastructure**
(`api-gateway` 8080, `discovery-server` 8761).
Rapport final : **30-40 pages avec références**, en LaTeX.

> Légende : ✅ déjà fait et vérifié · ⬜ à faire
> Chaque étape indique **pourquoi**, **quoi modifier**, et **comment prouver que c'est fait** (preuve à mettre dans le rapport).

---

## Phase 0 — Cadrage (0,5 jour)

### Étape 0.1 ✅ Figer le périmètre
- 3 microservices métier ; gateway et Eureka = infrastructure, hors décompte.
- `config-server`, `genai-service`, `admin-server` : sources conservées, **hors build**.
- Preuve : réacteur Maven à 5 modules, `docker compose config` valide.
- Rapport : § « Périmètre et justification des choix ».

### Étape 0.2 ✅ Vérifier la chaîne d'outils
- JDK 21, Maven 3.9 (ou `mvnw`), Docker Desktop, un compte GitHub + un compte SonarCloud (ou SonarQube local).
- Preuve : `mvn -v`, `docker version`, build complet.

### Étape 0.3 ⬜ Créer le dépôt Git et la stratégie de branches
- `git init`, premier commit, dépôt distant, branches `main` et `dev`.
- Protection de `main` : PR obligatoire, checks CI requis.
- Preuve : capture des règles de branche + une PR de démonstration.
- Rapport : § « Démarche DevSecOps — intégration continue et revue ».

---

## Phase A — Application (fait)

### Étape A.1 ✅ Réduction à 3 microservices
`pom.xml`, `docker-compose.yml`, 5 `pom.xml`, 5 `application.yml`, front (`ChatBox` débranché), `README.md`.
Preuve : build 5 modules, 4 services enregistrés dans Eureka, 3 routes métier en 200 via le gateway, `/api/genai/**` en 404.

### Étape A.2 ✅ Configuration embarquée
Config du config-server réinjectée dans chaque `application.yml` ; `spring-cloud-starter-config` retiré.
Preuve : démarrage sans variable de config externe.

### Étape A.3 ✅ Interface modernisée
Rail de navigation, colonnes en cartes, listes à avatars, vert PetClinic conservé.
Preuve : 6 écrans parcourus, styles calculés vérifiés.
Note technique : `css/theme.css` écrit à la main car `libsass-maven-plugin:0.2.29` ne se résout plus ; les fichiers `scss/` restent la source.

---

## Phase B — Qualité et intégration continue (TP03) — 3 à 4 jours

### Étape B.1 ⬜ Écrire les tests manquants des 3 services métier
État actuel : **0 test** dans `customers`, `vets`, `visits` (seuls gateway et discovery en ont).
- `@WebMvcTest` sur chaque contrôleur REST : 200 nominal, 404 inconnu, 400 payload invalide.
- `@DataJpaTest` sur les repositories (HSQLDB en mémoire, scripts `db/hsqldb`).
- Tests de mapping DTO ↔ entité, tests de cache pour `vets` (`@Cacheable`).
- Cible : **≥ 60 % de couverture de lignes** par service.
- Preuve : `mvn test` vert, rapport `target/site/jacoco/index.html`.
- Rapport : § « Tests et couverture » (tableau par module).

### Étape B.2 ⬜ Ajouter JaCoCo au `pom.xml` parent
- `jacoco-maven-plugin` : `prepare-agent` + `report` (phase `verify`) + `check` avec seuils par module.
- Multi-module : un seul déclenchement au parent, rapports par module.
- Preuve : build en échec si un seuil n'est pas atteint (le montrer volontairement une fois).
- Rapport : § « Mesure de la qualité ».

### Étape B.3 ⬜ Ajouter l'analyse Sonar
- `sonar-maven-plugin` au parent.
- Propriétés : `sonar.projectKey`, `sonar.organization`, `sonar.host.url`,
  `sonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml`.
- Preuve : tableau de bord Sonar avec couverture, dette technique, 0 bug bloquant, Quality Gate verte.
- Rapport : § « Analyse statique » + capture du Quality Gate.

### Étape B.4 ⬜ Écrire le workflow GitHub Actions (TP03)
Fichier `.github/workflows/deploy.yml` (remplace/étend `maven-build.yml`) :
- `actions/checkout@v4` avec `fetch-depth: 0` (nécessaire au blame Sonar) ;
- `actions/setup-java@v4` : `distribution: temurin`, matrice `java: ['17', '21']`, `cache: maven` ;
- étape build+tests : `mvn -B clean verify` ;
- étape analyse : `mvn -B sonar:sonar -Dsonar.token=${{ secrets.SONAR_TOKEN }}` ;
- publication des rapports JaCoCo/Surefire en artefacts ;
- déclencheurs : `push` sur `dev`/`main`, `pull_request` vers `main`.
- Preuve : pipeline vert sur une PR `feature/*` → `dev`, puis `dev` → `main`.
- Rapport : § « Pipeline CI/CD » + capture des runs.

### Étape B.5 ⬜ Secrets et configuration de production
- GitHub Secrets : `SONAR_TOKEN`, puis `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (étape D.2).
- Jamais de secret dans le dépôt : vérifier `.gitignore`.
- Preuve : capture de la liste des secrets (noms seulement).

### Étape B.6 ⬜ SAST / SCA (au-delà du TP, très valorisé)
- SCA : `Dependabot` (Maven + GitHub Actions) ou `dependency-check`.
- SAST : Sonar (déjà) + `Trivy fs` pour les dépendances et l'image.
- Secrets : `gitleaks` en pre-commit et en CI.
- Preuve : un rapport de vulnérabilités avant/après mise à jour des dépendances.
- Rapport : § « Sécurité de la chaîne logicielle (SSDLC) ».

---

## Phase C — Sécurité de la communication (cœur du sujet) — 5 à 6 jours

### Étape C.1 ⬜ Modèle de menace
- Cartographier les flux : navigateur → gateway ; gateway → 3 services (`lb://`) ; services → Eureka ; Actuator/Prometheus.
- Appliquer STRIDE par flux, identifier les actifs (données propriétaires, annuaire, journaux).
- Constat initial à documenter : `/actuator/**` en `include: '*'` sans authentification, HTTP en clair, aucune authentification inter-services, image Docker en `root`.
- Preuve : tableau flux × menace × contre-mesure.
- Rapport : § « Analyse de risque » (2-3 pages, le plus fort du rapport).

### Étape C.2 ⬜ Authentification à la bordure (nord-sud)
- Fournisseur d'identité : Keycloak en conteneur, ou `spring-security-oauth2-authorization-server` embarqué.
- Gateway en **resource server** JWT + filtre `TokenRelay` vers les services.
- CORS restrictif, en-têtes de sécurité, limitation de débit sur les routes sensibles.
- Preuve : sans jeton → 401 ; jeton expiré/invalide → 401 ; jeton valide → 200.
- Rapport : § « Sécurisation de la bordure ».

### Étape C.3 ⬜ mTLS entre le gateway et les 3 services (est-ouest)
- Générer une CA locale + un certificat par service (`openssl` ou `keytool`), keystore/truststore.
- Services : `server.ssl.enabled=true`, `server.ssl.client-auth=need`, `trust-store`.
- Gateway : client HTTP personnalisé avec contexte SSL *et* résolution `lb://` (c'est le point délicat : `HttpClient` + `LoadBalancer`).
- Preuve : appel direct sans certificat client → échec de handshake ; avec certificat → 200.
- Rapport : § « Canaux est-ouest : mTLS » + extraits de configuration.

### Étape C.4 ⬜ Propagation d'identité et autorisation
- Les services valident le JWT (audience par service) et appliquent des scopes/rôles (`@PreAuthorize`).
- Le `sub` de l'utilisateur apparaît dans les logs et les traces (imputabilité).
- Preuve : 403 sur une route quand le scope manque ; corrélation `sub` + `traceId`.
- Rapport : § « Autorisation et traçabilité ».

### Étape C.5 ⬜ Durcir Eureka et Actuator
- Eureka : authentification basic + TLS ; clients configurés avec identifiants.
- Actuator : `management.endpoints.web.exposure.include=health,info,prometheus`, `show-details: never`, port de management séparé et protégé.
- Preuve : `/actuator/env` → 401/404 ; Prometheus scrape avec authentification.
- Rapport : § « Réduction de la surface d'attaque ».

### Étape C.6 ⬜ Durcir les images et l'exécution
- `Dockerfile` : utilisateur non-root, image de base minimale, `HEALTHCHECK`, pas de secret dans les couches.
- `docker-compose.yml` : secrets par fichiers, réseaux séparés (bordure / services / données).
- Trivy sur les images : montrer la réduction du nombre de CVE.
- Preuve : `docker compose config`, rapport Trivy avant/après.

### Étape C.7 ⬜ Tests de sécurité automatisés
- Tests d'intégration : refus sans mTLS, refus sans jeton, refus de scope.
- Scan dynamique : OWASP ZAP baseline sur le gateway.
- Preuve : rapport ZAP + tests intégrés dans le pipeline.
- Rapport : § « Validation de la sécurité ».

---

## Phase D — Données et infrastructure (TP04) — 2 jours

### Étape D.1 ⬜ Instance RDS MySQL (reproduction du TP)
- MySQL 8, `db.t3.micro` (free tier), **privée**, Security Group limité au groupe applicatif, chiffrement au repos, sauvegardes automatiques.
- Le TP04 laissait « Public access: Yes », « Anywhere-IPv4 », chiffrement et backups désactivés : reproduire puis **corriger**, en documentant l'écart.
- Preuve : captures de configuration + tableau avant/après.
- Rapport : § « Infrastructure de données » + critique argumentée.

### Étape D.2 ⬜ Brancher les 3 services sur MySQL
- 3 schémas sur l'instance : `petclinic_customers`, `petclinic_visits`, `petclinic_vets`.
- Profil `prod` + `application-prod.properties` par service ; `spring.sql.init` sur les scripts `db/mysql/*.sql`.
- Secrets GitHub → variables d'environnement au déploiement.
- Preuve : démarrage en profil `prod`, données persistées après redémarrage.
- Rapport : § « Persistance et séparation des données ».

### Étape D.3 ⬜ Gestion des secrets
- Rotation documentée, pas de secret en clair, `.env` ignoré.
- Preuve : `git log -p | grep` sans secret.

---

## Phase E — Observabilité et réponse — 2 jours

### Étape E.1 ⬜ Métriques et traces
- Prometheus (scrape authentifié), Grafana (dashboards : latence, taux d'erreur, 401/403), Zipkin (traces inter-services).
- Preuve : dashboard affichant une trace gateway → service.
- Rapport : § « Supervision ».

### Étape E.2 ⬜ Alertes et runbook
- Alertes : taux de 5xx, handshake mTLS échoué, jeton rejeté en masse.
- Runbook : que faire en cas de fuite de certificat, de jeton, ou d'annuaire compromis.
- Preuve : une alerte déclenchée volontairement.
- Rapport : § « Réponse à incident ».

---

## Phase F — Rapport LaTeX (30-40 pages) — 4 à 5 jours

### Étape F.1 ⬜ Squelette et bibliographie
`rapport/main.tex`, `sections/`, `biblio.bib` (BibTeX), classe `report` ou `article`, `hyperref`, `listings`, `graphicx`.

### Étape F.2 ⬜ Plan indicatif (pages cibles)
| # | Chapitre | Pages |
|---|---|---|
| 1 | Introduction, contexte, problématique | 2-3 |
| 2 | État de l'art : microservices, sécurité des communication, DevSecOps, OWASP | 4-5 |
| 3 | Présentation du projet et architecture (3 services, flux, justification du périmètre) | 4 |
| 4 | Démarche : SSDLC, STRIDE, correspondance avec les TPs | 3 |
| 5 | CI/CD et qualité : TP03, JaCoCo, Sonar, résultats chiffrés | 5 |
| 6 | Sécurisation des communications : JWT, mTLS, autorisation, preuves | 6-7 |
| 7 | Infrastructure et données : TP04, RDS, durcissement | 3-4 |
| 8 | Résultats, limites, discussion | 3 |
| 9 | Conclusion et perspectives | 2 |
| 10 | Références | 2 |
| | **Total** | **34-40** |

### Étape F.3 ⬜ Rédaction et figures
- Schémas : architecture, flux de confiance, pipeline, modèle de menace.
- Extraits de configuration commentés, captures Sonar/ZAP/Trivy.
- Tableaux avant/après pour chaque durcissement.

### Étape F.4 ⬜ Relecture et compilation
- `pdflatex` + `bibtex` + `pdflatex` ×2, vérifier la pagination (30-40 p. **références incluses**), orthographe, cohérence des numéros de figures.

---

## Ordre conseillé et effort

| Phase | Contenu | Effort | Dépend de |
|---|---|---|---|
| 0 | Cadrage, Git | 0,5 j | — |
| A | Application | ✅ fait | — |
| B | Tests + pipeline (TP03) | 3-4 j | A |
| C | Sécurité des flux | 5-6 j | B |
| D | RDS MySQL (TP04) | 2 j | B |
| E | Observabilité | 2 j | C |
| F | Rapport | 4-5 j | B, C, D |
| | **Total restant** | **~17-20 j** | |

Si le temps manque : faire **B puis C** (le sujet), et réduire D/E à une section du rapport.

---

## Definition of Done

- [ ] `mvn clean verify` vert sur `main`, Quality Gate Sonar verte
- [ ] Couverture ≥ 60 % par service métier
- [ ] Pipeline GitHub Actions vert sur PR et sur `main`
- [ ] Sans jeton : 401 sur les routes métier ; sans certificat client : handshake refusé
- [ ] `/actuator/env` inaccessible anonymement
- [ ] Aucune image ne tourne en root ; Trivy sans CVE critique
- [ ] RDS privée, chiffrée, sauvegardée ; données persistées après redémarrage
- [ ] Rapport compilé, 30-40 pages, références complètes

---

## Risques identifiés

| Risque | Impact | Parade |
|---|---|---|
| mTLS + `lb://` au gateway : configuration délicate | bloque la phase C | prévoir une journée dédiée, valider d'abord en appel direct puis via Eureka |
| `libsass-maven-plugin` introuvable | le SCSS n'est plus compilé | garder `css/theme.css`, ou migrer le build CSS plus tard (hors chemin critique) |
| TP1/TP2 illisibles (scans sans texte) | étapes éventuellement manquantes | confirmer avec l'encadrant la liste exacte attendue |
| Free tier RDS limité | pas de Multi-AZ | assumer et l'expliquer dans les limites du rapport |
| Jeton Sonar / secrets | échec du pipeline | secrets GitHub, jamais dans le dépôt |
