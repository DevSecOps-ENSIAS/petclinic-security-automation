# Guide A→Z — Sécurité de la communication entre microservices (PetClinic)

Guide écrit **pour votre dépôt réel** (`petclinic-security-automation`), pas pour un projet générique. Il suit les TP :
TP1 (OpenSSL) → TP2 (pipeline GitHub Actions) → TP3 (HTTPS keytool, puis Docker/ECR/EC2) → TP4 (RDS) → TP5 (SonarCloud).

---

## 0. Ce que contient réellement votre projet (à connaître avant de commencer)

| Élément | État réel dans le dépôt |
|---|---|
| Modules Maven | 5 : `customers`, `vets`, `visits`, `discovery-server` (Eureka), `api-gateway` |
| Versions | Spring Boot **4.0.1**, Spring Cloud 2025.1.0, `java.version` = **17** (JDK 21 OK) |
| Gateway | **WebFlux** (Spring Cloud Gateway). Sert aussi le front Vue. Routes `/api/customer/**`, `/api/visit/**`, `/api/vet/**` (`StripPrefix=2`, `lb://`). Il appelle aussi les services via `RestTemplate`/`WebClient` `@LoadBalanced` (endpoint `/api/gateway/owners/{id}`) |
| Services métier | **Spring MVC**. Port `0` (aléatoire) en local ; ports fixes 8081/8082/8083 avec le profil `docker` |
| Données | HSQLDB **en mémoire** par défaut. Scripts `db/mysql/*.sql` et `mysql-connector-j` déjà présents, mais **aucun profil MySQL configuré** |
| Sécurité | **Aucune** dépendance Spring Security. Actuator `include: '*'` partout. Eureka ouvert |
| Docker | `docker/Dockerfile` unique, `eclipse-temurin:17`, **root**. Build via `./mvnw -PbuildDocker install`. Compose avec images `springcommunity/*` |
| Tests | Existants : `OwnerResourceTest`, `PetResourceTest`, `VetResourceTest`, `VisitResourceTest`, tests gateway et discovery |
| CI | `maven-build.yml` (Java 17 seul, `mvn`) + `check-pr-template.yml` (**hérité du projet amont**, voir phase 1) |
| Scripts | `scripts/tagImages.sh` et `pushImages.sh` citent encore config-server, admin-server, genai (hors build) |
| Rapport | `rapport/main.tex` **vide** ; `PLAN-PROJET.tex/.md` existent |

Trois pièges propres à ce dépôt, traités dans le guide :
1. Les 3 scripts `db/mysql/schema.sql` font `CREATE DATABASE petclinic; USE petclinic;` → **ils écrasent la base choisie dans l'URL JDBC** (phase 8).
2. Les services s'enregistrent dans Eureka avec `prefer-ip-address: true` → le gateway se connecte à une **IP**, et un certificat TLS sans SAN IP échoue (phase 7).
3. Le service `vets` active le profil `production` (cache) dans son YAML ; définir `SPRING_PROFILES_ACTIVE` **remplace** ce profil (phase 7).

---

## 1. Organisation de l'équipe

### 1.1 Rôles

| | 🅰️ **Membre A — Pipeline, qualité, cloud** | 🅱️ **Membre B — Sécurité, crypto, données** |
|---|---|---|
| Phases principales | 1, 2, 3, 4, 4 bis, 8, 9, 11 | 5, 6, 7, 10 |
| Fichiers dont il est propriétaire | `pom.xml` racine, `.github/**`, `docker/**`, `docker-compose*.yml`, `scripts/*.sh` (hors `pki`) | `application*.yml` des 5 services, classes `*Security*`, `scripts/pki/**`, `docs/**` sécurité |
| Comptes à créer | GitHub (propriétaire), SonarCloud, AWS (ECR/EC2/IAM) | AWS (RDS), Keycloak local |
| Chapitres du rapport | CI/CD, qualité, supply chain, données, déploiement, supervision | Cryptographie, TLS/mTLS, JWT, durcissement |
| En commun | Phase 12 (rapport), phase 13 (soutenance) |

**Pourquoi cette séparation de fichiers :** elle évite 90 % des conflits Git. Si l'un doit modifier un fichier de l'autre, il ouvre une PR et l'autre la relit.

### 1.2 Règles de travail

1. Branches : `main` (protégée) ← `dev` ← `feature/<phase>-<sujet>` (ex. `feature/p3-ci`).
2. **Jamais de push direct sur `main` ni `dev`.** Toute PR est relue et approuvée par **l'autre** membre.
3. Messages de commit : `ci:`, `sec:`, `feat:`, `fix:`, `docs:`, `chore:`.
4. **Aucun secret dans Git** (mot de passe, token, `.pem`, `.p12`, `.env`). Vérification finale : `git log -p | grep -iE "password|token|PRIVATE KEY"`.
5. Toujours `./mvnw` (pas `mvn`). Après chaque changement de `pom.xml` ou Java : `./mvnw -B -DskipTests package` → `BUILD SUCCESS`.
6. Après chaque changement de `application.yml` : redémarrer le service concerné.
7. Synchronisation de 10 minutes à la fin de chaque phase (qui a fait quoi, qu'est-ce qui bloque).
8. Chaque phase se termine par des **preuves** (captures, sorties de commandes) rangées dans `docs/preuves/pXX-…/` (images committées, jamais de secret visible).

### 1.3 Planning (≈ 15 jours-personne chacun, adaptable)

| Phase | Contenu | TP | Resp. | Durée | Dépend de |
|---|---|---|---|---|---|
| 0 | Préparation des outils et comptes | — | A + B | 0,5 j | — |
| 1 | Dépôt, branches, nettoyage | TP2 | A | 0,5 j | 0 |
| 2 | Modèle de menace | — | A seul | 0,5 j | 1 |
| 3 | CI + JaCoCo | TP2 | A | 1,5 j | 1 |
| 4 | Sonar + SCA + secrets | TP5 | A | 1 j | 3 |
| 4 bis | Sécurité de la chaîne (SBOM, SAST, blocage, SHA) | — | A | 1 j | 4 |
| 5 | PKI avec OpenSSL | TP1 | B | 1 j | 1 |
| 6 | Authentification JWT | — | B | 2 j | 1 |
| 7 | TLS puis mTLS | TP3 | B | 3 j | 5, 6 |
| 8 | RDS MySQL + profil prod | TP4 | A | 2 j | 1 |
| 9 | Docker, ECR, EC2 | TP3 suite | A | 3 j | 3, 8 |
| 10 | Durcissement | — | B (+A) | 2 j | 6, 7, 9 |
| 11 | Observabilité et alertes | — | A | 1,5 j | 7, 10 |
| 12 | Rapport LaTeX | — | A + B | 4 j | tout |
| 13 | Soutenance | — | A + B | 1 j | 12 |

**En parallèle :** pendant que A fait 3 → 4, B fait 5 → 6. Ensuite A enchaîne **8** (RDS) puis **9** (Docker/ECR/EC2) pendant que B fait **7** (TLS/mTLS) puis **10** (durcissement applicatif). Les phases AWS (8 et 9) sont assurées par A.

---

## PHASE 0 — Préparation (A + B, chacun sur sa machine)

### 0.1 Outils
Ouvrir **Git Bash** (pas cmd.exe) à la racine du projet (le dossier qui contient `pom.xml`).
```bash
java -version      # 17 ou 21
./mvnw -v          # Maven 3.9.x
docker version     # client ET serveur (Docker Desktop lancé)
git --version
openssl version    # fourni par Git for Windows
keytool -help | head -3
```
Si `java` est introuvable :
```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
```
Installer en plus (B) : **MySQL Workbench** (TP4) et un client REST (Postman) ; (A) AWS CLI (`aws --version`).

### 0.2 Comptes
| Compte | Qui | Remarque |
|---|---|---|
| GitHub | A (propriétaire), B (collaborateur) | Activer l'authentification à deux facteurs |
| SonarCloud | A | Connexion avec GitHub |
| AWS | A et B (ou un compte partagé) | Activer une alerte de budget (AWS Budgets) dès le début |
| Docker Hub | optionnel | Non nécessaire : on utilise ECR |

### 0.3 Vérifier que le projet compile et que les tests passent
```bash
./mvnw -B clean verify
```
**✅** `BUILD SUCCESS` pour les 5 modules, tests verts.
**⛔** Si un test gateway échoue, noter lequel et lire `spring-petclinic-api-gateway/target/surefire-reports/` avant de continuer ; ne pas avancer sur un build rouge.

---

## PHASE 1 — Dépôt, branches, nettoyage (🅰️, TP2 étapes 2-3)

### 1.1 Initialiser Git
Le zip ne contient pas de dossier `.git`. Vérifier : `ls -a | grep "^\.git$"`.
```bash
git init -b main
git config --local user.name  "Prénom A"
git config --local user.email "a@exemple.com"
```

### 1.2 Compléter `.gitignore` (obligatoire avant toute génération de clé)
Ajouter à la fin de `.gitignore` :
```gitignore
# Secrets et cryptographie : JAMAIS dans Git
*.pem
*.key
*.p12
*.jks
*.csr
*.srl
.keystore
.env
.env.*
secrets/
pki/
docs/preuves/**/*.har
```

### 1.3 Supprimer les fichiers hérités du projet amont
```bash
git rm -f .github/workflows/check-pr-template.yml .github/PULL_REQUEST_TEMPLATE.md
```
**Pourquoi :** `check-pr-template.yml` a été écrit pour le dépôt officiel Spring PetClinic. Il se déclenche sur `pull_request_target` avec des droits d'écriture et contient une logique qui **ferme automatiquement** des PR jugées « devoirs d'étudiants ». Dans votre dépôt, il risquerait de fermer ou d'étiqueter vos propres PR, et `pull_request_target` est un déclencheur sensible. (Je n'ai pas analysé en détail toute sa logique de détection ; le supprimer est la solution la plus sûre.)
Supprimer aussi l'ancien workflow qui sera remplacé en phase 3 :
```bash
git rm -f .github/workflows/maven-build.yml
```
(A le remplace par `deploy.yml` en phase 3. Faites ce `git rm` dans la même PR que le nouveau workflow pour ne pas rester sans CI.)

### 1.4 Premier commit et dépôt distant
```bash
git add -A
git status               # vérifier qu'aucun fichier target/ ni .p12 n'apparaît
git commit -m "chore: initial import of PetClinic security project"
```
Sur https://github.com/new : nom `petclinic-security-automation`, **Private**, **aucune** case cochée (ni README, ni .gitignore, ni licence).
```bash
git remote add origin https://github.com/COMPTE_A/petclinic-security-automation.git
git push -u origin main
```
**⛔** Authentification refusée : créer un *Personal Access Token (classic)* avec la portée `repo` (GitHub → Settings → Developer settings) et l'utiliser comme mot de passe.

### 1.5 Branches (TP2 étape 3 : `feature/dev`)
```bash
git checkout -b dev && git push -u origin dev
git checkout -b feature/p1-cleanup && git push -u origin feature/p1-cleanup
```
Les suppressions de 1.3 sont committées sur `feature/p1-cleanup`, puis PR vers `dev` (voir phase 3.6 pour la procédure PR).

### 1.6 Inviter B
GitHub → Settings → Collaborators → Add people. B accepte, clone, configure son identité :
```bash
git clone https://github.com/COMPTE_A/petclinic-security-automation.git
cd petclinic-security-automation
git config --local user.name "Prénom B"; git config --local user.email "b@exemple.com"
git checkout dev
```

### 1.7 Protection de `main` (après que le premier pipeline a tourné, voir phase 3.7)

**✅ Fin de phase :** les deux membres ont le dépôt, `main` et `dev` existent sur GitHub, aucun fichier secret suivi (`git ls-files | grep -E "\.(pem|p12|key)$"` ne renvoie rien).
**📸 Preuves :** page GitHub du dépôt avec les branches.

---

## PHASE 2 — Modèle de menace (🅰️ seul, 1 h)

> **Exception à la répartition A/B** : cette phase est réalisée **entièrement par A**. Les tâches
> initialement attribuées à B (captures, relecture) sont reprises par A. **Une seule personne suffit** :
> les constats sont des commandes, les captures des écrans, et le tableau est déjà rédigé. Seule la règle
> GitHub « PR approuvée » impose un second compte pour le clic *Approve*.

Le fichier `SECURITE-modele-de-menace.md` est fourni dans `guide/` (constats C1 à C14, tableau STRIDE par flux F1 à F9).

1. **A** le copie à la racine du dépôt (branche `feature/p2-threat-model`) :
   ```bash
   git checkout dev && git pull
   git checkout -b feature/p2-threat-model
   cp guide/SECURITE-modele-de-menace.md .
   ```
2. **A** relit les sections 3 et 4 et exécute les commandes de la colonne « Où le vérifier ».
3. **A** produit les captures « avant » :
   - `grep -l security spring-petclinic-*/pom.xml pom.xml` (aucun résultat) ;
   - `curl http://localhost:8080/actuator/env` (réponse JSON complète) ;
   - Grafana ouvert sur http://localhost:3030 **sans mot de passe** ;
   - `curl http://localhost:8081/owners` en contournant le gateway (si compose lancé).
4. **A** ajoute au moins 2 menaces propres (F10, F11) et confirme gravités et priorités.
5. PR vers `dev`. **Approbation** : le binôme clique *Approve* (aucune production de sa part), ou un second
   compte ; à défaut, fusionner et documenter la revue comme simulée.

**✅ Fin de phase :** fichier mergé dans `dev`, ≥ 3 captures « avant ».
Cette phase alimente le chapitre « Analyse de risque » du rapport.

---

## PHASE 3 — Intégration continue et couverture (🅰️, TP2 étapes 4-9)

### 3.1 Mesurer la couverture en local (profil `quality`)
Dans le `pom.xml` racine, **juste après** la ligne `<profiles>`, coller :
```xml
        <profile>
            <id>quality</id>
            <properties>
                <jacoco.version>0.8.12</jacoco.version>
                <jacoco.minimum>0.40</jacoco.minimum>
                <jacoco.check.skip>false</jacoco.check.skip>
                <sonar.projectKey>REMPLACER_PROJECT_KEY</sonar.projectKey>
                <sonar.organization>REMPLACER_ORGANISATION</sonar.organization>
                <sonar.host.url>https://sonarcloud.io</sonar.host.url>
                <sonar.coverage.jacoco.xmlReportPaths>**/target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
                <sonar.exclusions>**/static/**,**/*.scss,**/webjars/**</sonar.exclusions>
            </properties>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.jacoco</groupId>
                        <artifactId>jacoco-maven-plugin</artifactId>
                        <version>${jacoco.version}</version>
                        <executions>
                            <execution>
                                <id>prepare-agent</id>
                                <goals><goal>prepare-agent</goal></goals>
                            </execution>
                            <execution>
                                <id>report</id>
                                <phase>verify</phase>
                                <goals><goal>report</goal></goals>
                            </execution>
                            <execution>
                                <id>check</id>
                                <phase>verify</phase>
                                <goals><goal>check</goal></goals>
                                <configuration>
                                    <skip>${jacoco.check.skip}</skip>
                                    <rules>
                                        <rule>
                                            <element>BUNDLE</element>
                                            <limits>
                                                <limit>
                                                    <counter>LINE</counter>
                                                    <value>COVEREDRATIO</value>
                                                    <minimum>${jacoco.minimum}</minimum>
                                                </limit>
                                            </limits>
                                        </rule>
                                    </rules>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
```
Le TP utilise JaCoCo 0.8.15 (pour Java 25) ; avec JDK 17/21, 0.8.12 convient. Si votre encadrant exige la version du TP, changer `jacoco.version`.

Dans `spring-petclinic-discovery-server/pom.xml`, ajouter dans `<properties>` (le module n'a presque aucun code à couvrir) :
```xml
<jacoco.check.skip>true</jacoco.check.skip>
```
```bash
./mvnw -B -P quality clean verify
```
**✅** `BUILD SUCCESS` et `spring-petclinic-<module>/target/site/jacoco/index.html` pour chaque module.
Ouvrir chaque `index.html`, noter le pourcentage de lignes : c'est la **mesure de départ**.

**⛔** `Rule violated for bundle … lines covered ratio is 0.xx, but expected minimum is 0.40` : un module est sous le seuil. Solutions, dans l'ordre : (1) **B ajoute des tests** (modèle : `VetResourceTest.java`) ; (2) à défaut, baisser temporairement `jacoco.minimum` pour ce module (dans son `pom.xml`) et le remonter plus tard. Ne jamais laisser `0.00` dans le rendu final. Objectif du plan : 60 %.
**📸** Capture de chaque rapport JaCoCo.

### 3.2 Workflow `.github/workflows/deploy.yml` (nom du TP)
```yaml
name: CI

on:
  push:
    branches: [ main, dev ]
  pull_request:
    branches: [ main, dev ]

permissions:
  contents: read

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        java: [ '17', '21' ]
    steps:
      - name: Vérification du code source
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: temurin
          cache: maven

      - name: Wrapper exécutable
        run: chmod +x mvnw

      - name: Build, tests et couverture
        run: ./mvnw -B -P quality clean verify

      - name: Publier les rapports JaCoCo
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-java-${{ matrix.java }}
          path: '**/target/site/jacoco/'
```
`permissions: contents: read` applique le moindre privilège (la CI par défaut a plus de droits que nécessaire).

### 3.3 Corriger les scripts et le build Docker plus tard
Rien à faire ici ; voir phase 9.

### 3.4 Branche, PR, revue (TP2 étape 8-9)
```bash
git checkout dev && git pull
git checkout -b feature/p3-ci
git add -A
git commit -m "ci: add quality profile (JaCoCo) and GitHub Actions workflow"
git push -u origin feature/p3-ci
```
GitHub → **Compare & pull request** : base `dev` ← compare `feature/p3-ci`. Renseigner une description. **B relit** (onglet *Files changed*, commentaire ou « Approve »), puis **Merge**.
Ensuite PR `dev` → `main`, même procédure.

### 3.5 Lire le résultat
Onglet **Actions** → run `CI` → job `build` (Java 17 et Java 21) → étapes.
**✅** Deux jobs verts ; artefacts `jacoco-java-17` et `jacoco-java-21` téléchargeables.
**⛔** `./mvnw: Permission denied` → l'étape `chmod +x mvnw` doit précéder le build. Échec de téléchargement Maven → relancer le job (*Re-run jobs*).

### 3.6 Protéger `main` (TP2 étape 8, une fois que le check `build` existe)
GitHub → Settings → Branches → **Add branch protection rule** : pattern `main` ; cocher *Require a pull request before merging* (1 approbation), *Require status checks to pass* (chercher `build`), *Do not allow bypassing*.
**✅** Un `git push origin main` direct est refusé.

**📸 Preuves :** PR avec checks verts + approbation de l'autre membre ; règle de protection ; onglet Actions.

---

## PHASE 4 — SonarCloud, SCA et détection de secrets (🅰️, TP5)

### 4.1 Créer le projet SonarCloud (TP5 étapes 1-3)
1. https://sonarcloud.io/login → **With GitHub**.
2. **+** (en haut à droite) → **Analyze new project** → sélectionner `petclinic-security-automation` → **Set Up**.
3. Menu gauche **Project Information** : noter **Project Key** et **Organization Key**.
4. *Administration → Analysis Method* : **désactiver « Automatic Analysis »** (sinon conflit avec l'analyse faite par le workflow).

### 4.2 Renseigner le `pom.xml` (TP5 étapes 5-6)
Dans le profil `quality`, remplacer `REMPLACER_PROJECT_KEY` et `REMPLACER_ORGANISATION` par les valeurs notées. Ajouter dans `<plugins>` du profil, après le plugin JaCoCo :
```xml
                    <plugin>
                        <groupId>org.sonarsource.scanner.maven</groupId>
                        <artifactId>sonar-maven-plugin</artifactId>
                        <version>5.0.0.4389</version>
                    </plugin>
```
(Le TP indique `3.10.0.2594`. Si la version choisie n'est pas résolue, utiliser celle du TP.)
`sonar.exclusions` (déjà dans le profil) évite d'analyser le front (JS, SCSS, webjars) du gateway, qui fausserait la duplication et la couverture.

### 4.3 Token et secret (TP5 étapes 8-10)
1. SonarCloud → avatar → **My Account** → **Security** (ou *Access Tokens*) → nom `petclinic` → durée → **Generate Token** → **copier tout de suite**.
2. GitHub → Settings → **Secrets and variables → Actions → New repository secret** : nom `SONAR_TOKEN`.
3. ⚠️ Le token ne va dans **aucun** fichier, capture ou message. Les tokens ont une date d'expiration (30 jours par défaut) : noter la date pour le renouveler avant la soutenance. Le token qui apparaît en clair dans l'énoncé du TP5 est à considérer comme compromis.

### 4.4 Étape d'analyse dans le workflow (TP5 étape 7)
Ajouter dans le job `build`, après « Build, tests et couverture » :
```yaml
      - name: Analyse SonarCloud
        if: matrix.java == '21'
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: ./mvnw -B -P quality sonar:sonar
```
Le scanner lit `SONAR_TOKEN` dans l'environnement : pas besoin de `-Dsonar.token=…` (le secret n'apparaît pas dans la ligne de commande).

### 4.5 SCA : Dependabot et Trivy
`.github/dependabot.yml` :
```yaml
version: 2
updates:
  - package-ecosystem: maven
    directory: "/"
    schedule:
      interval: weekly
  - package-ecosystem: github-actions
    directory: "/"
    schedule:
      interval: weekly
  - package-ecosystem: docker
    directory: "/docker"
    schedule:
      interval: weekly
```
Dans `deploy.yml`, à la fin du job `build` :
```yaml
      - name: Scan Trivy (dépendances et configuration)
        if: matrix.java == '21'
        uses: aquasecurity/trivy-action@0.28.0
        with:
          scan-type: fs
          scan-ref: .
          scanners: vuln,secret,misconfig
          severity: CRITICAL,HIGH
          exit-code: '0'
          format: table
```
Épingler une version précise (vérifier la dernière sur la page de l'action dans le Marketplace GitHub). `exit-code: '0'` informe sans bloquer ; vous passerez à `'1'` quand les CVE critiques seront traitées (phase 10).

### 4.6 Détection de secrets
```yaml
      - name: Gitleaks
        if: matrix.java == '21'
        uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```
(Gratuit pour un compte personnel ; une organisation GitHub demande une licence : dans ce cas remplacer par l'étape Trivy `secret` déjà activée ci-dessus.)

### 4.7 Lancer et interpréter (TP5 étapes 11-12)
```bash
git checkout dev && git pull && git checkout -b feature/p4-sonar
git add -A && git commit -m "ci: add SonarCloud, Dependabot, Trivy and gitleaks"
git push -u origin feature/p4-sonar
```
PR → `dev` (B relit) → merge → PR `dev` → `main`.

**✅** Run vert ; tableau de bord SonarCloud avec *Quality Gate*, couverture, duplications, hotspots ; onglet *Security → Dependabot alerts* ; tableau Trivy dans les logs.

### 4.8 Rédiger l'interprétation (exigée par le TP : « Interprétez le rapport »)
Pour chaque indicateur, noter valeur, note (A à E) et décision :

| Indicateur Sonar | Valeur obtenue | Note | Action décidée |
|---|---|---|---|
| Security / Reliability / Maintainability | | | corriger / accepter + justification |
| Coverage | | | tests ajoutés par B |
| Duplications | | | |
| Security Hotspots | | | |
| Dependency risks | | | |
| Quality Gate | Passed / Failed | | |

**📸 Preuves :** dashboard Sonar, tableau Trivy, onglet Dependabot, tableau ci-dessus.

---


## PHASE 4 bis — Sécurité de la chaîne logicielle (🅰️)

> **But** : passer d'un pipeline « qui mesure » à un pipeline « qui bloque ».
> **Dépend de** : phase 4 (Sonar, Trivy, Dependabot).
> **Durée** : ~1 jour. **Peut être faite après la phase 9** sans bloquer B.
> **Chapitre du rapport** : 5 (CI/CD, qualité, supply chain).

Les TP s'arrêtent à JaCoCo + Sonar + RDS. Un projet DevSecOps ajoute quatre contrôles :
SBOM, SAST dédié, politique de blocage, épinglage des actions. Cette phase les met en place.

---

### 4bis.0 — Réduire d'abord la surface de dépendances

Avant de rendre Trivy bloquant, supprimer les dépendances inutiles. Depuis le retrait d'`admin-server`,
`jolokia-core` (pont JMX) et `chaos-monkey` ne servent plus à rien et sont une surface d'attaque.

Dans les 4 `pom.xml` (`customers`, `vets`, `visits`, `api-gateway`), supprimer :

```xml
<dependency>
    <groupId>org.jolokia</groupId>
    <artifactId>jolokia-core</artifactId>
</dependency>
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>chaos-monkey-spring-boot</artifactId>
</dependency>
```

Puis :

```bash
./mvnw -B -P quality clean verify
```

**✅** Build vert. Si le profil `chaos-monkey` de `run_all.sh` n'est plus utilisé, le retirer aussi.

**Preuve** : tableau Trivy avant/après (nombre de CVE).

> Si un besoin de démonstration Chaos Monkey subsiste, le conserver uniquement en profil Maven `chaos`
> et le documenter. Par défaut, il ne doit pas être dans le jar de production.

---

### 4bis.1 — SBOM CycloneDX

### Plugin

Dans le `pom.xml` racine, profil `quality`, ajouter dans `<plugins>` :

```xml
<plugin>
    <groupId>org.cyclonedx</groupId>
    <artifactId>cyclonedx-maven-plugin</artifactId>
    <version>2.9.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>makeAggregateBom</goal></goals>
        </execution>
    </executions>
</plugin>
```

```bash
./mvnw -B -P quality -DskipTests package
ls target/bom.json
```

**✅** `target/bom.json` contient les composants (nom, version, licence, hash).

### Publication en CI

Dans `deploy.yml`, après l'upload JaCoCo :

```yaml
      - name: Publier le SBOM
        if: matrix.java == '21'
        uses: actions/upload-artifact@<SHA>
        with:
          name: sbom-cyclonedx
          path: target/bom.json
```

**Preuve** : artefact `sbom-cyclonedx` téléchargeable ; capture de `bom.json` (premières lignes).

---

### 4bis.2 — SAST dédié : Semgrep

Sonar est un SAST « généraliste » ; Semgrep apporte des règles Spring/Java ciblées.

### Local

```bash
docker run --rm -v "$(pwd):/src" semgrep/semgrep \
  semgrep --config=p/java --config=p/spring --severity=ERROR /src
```

### CI

```yaml
      - name: Semgrep (SAST)
        if: matrix.java == '21'
        run: |
          docker run --rm -v "$PWD:/src" semgrep/semgrep \
            semgrep --config=p/java --config=p/spring --severity=ERROR --error /src
```

`--error` fait échouer l'étape dès qu'une règle de sévérité ERROR est trouvée.

**Démarche** :
1. premier run : noter les findings ;
2. corriger les vrais positifs ;
3. ajouter `.semgrepignore` pour les faux positifs **avec un commentaire** ;
4. conserver l'étape bloquante.

**Preuve** : sortie Semgrep avant/après, capture des règles déclenchées.

---

### 4bis.3 — Trivy bloquant (politique)

Aujourd'hui `exit-code: '0'` : Trivy informe mais ne bloque pas. On passe à une politique explicite.

#### 4bis.3.1 — Ignorer les CVE sans correctif

```yaml
      - name: Scan Trivy (dépendances, secrets, configuration)
        if: matrix.java == '21'
        uses: aquasecurity/trivy-action@<SHA>   # v0.36.0
        with:
          scan-type: fs
          scan-ref: .
          scanners: vuln,secret,misconfig
          severity: CRITICAL
          ignore-unfixed: true
          exit-code: '1'
          format: table
```

#### 4bis.3.2 — Exceptions justifiées

Créer `.trivyignore` à la racine :

```text
# CVE-2024-XXXXX — dépendance transitive de <paquet>, pas de correctif amont,
# non exploitable dans notre contexte (pas d'exposition réseau). Revue le 2026-11-09.
CVE-2024-XXXXX
```

Chaque ligne doit avoir une justification et une date de revue.

#### 4bis.3.3 — Périmètre

Ajouter `skip-dirs` pour les modules hors build (sinon le scan compte 3 fois) :

```yaml
          skip-dirs: '.git,**/target,spring-petclinic-config-server,spring-petclinic-genai-service,spring-petclinic-admin-server'
```

**Preuve** : un run rouge provoqué volontairement par une CVE critique (ou un secret factice), puis vert après correction/exception.

> Sans cette étape, le rapport ne peut pas affirmer que « le pipeline bloque » : il ne fait qu'afficher.

---

### 4bis.4 — Épingler toutes les actions par SHA

Règle Sonar `S7637`. Les tags (`@v4`) sont mobiles ; un SHA est immuable.

### Récupérer les SHA

```bash
for repo in actions/checkout actions/setup-java actions/upload-artifact; do
  echo -n "$repo: "
  curl -s "https://api.github.com/repos/$repo/git/ref/tags/v4" | grep -m1 '"sha"' | cut -d'"' -f4
done
```

(Adapter `v4` à la version majeure réellement utilisée.)

### Remplacer dans `deploy.yml`

```yaml
- uses: actions/checkout@<sha>          # v4.x
- uses: actions/setup-java@<sha>        # v4.x
- uses: actions/upload-artifact@<sha>   # v4.x
```

Conserver le commentaire de version : Dependabot sait mettre à jour un pin SHA commenté.

**Preuve** : `grep -n "uses:" deploy.yml` ne montre plus aucun tag.

---

### 4bis.5 — Détection de secrets sur tout l'historique

Trivy `secret` scanne les fichiers actuels ; il faut aussi l'historique Git.

```bash
docker run --rm -v "$(pwd):/p" trufflesecurity/trufflehog:latest \
  git file:///p --only-verified
```

En CI (facultatif, l'action nécessite parfois une licence selon l'organisation) :

```yaml
      - name: TruffleHog
        if: matrix.java == '21'
        uses: trufflesecurity/trufflehog@<SHA>
        with:
          path: ./
          extra_args: --only-verified
```

**Attendu** : `0 verified secrets`. Sinon : révoquer la clé, puis nettoyer l'historique (`git filter-repo`).

**Preuve** : sortie « 0 verified secrets » + `git log -p | grep -iE "ghp_|sqp_|AKIA|PRIVATE KEY"` propre.

---

### 4bis.6 — Récapitulatif de la politique

À écrire dans le rapport (une page) :

| Contrôle | Outil | Seuil | Comportement |
|---|---|---|---|
| Couverture | JaCoCo | < 60 % | **bloquant** |
| Qualité/sécurité | Sonar Quality Gate | gate projet | **bloquant** |
| SAST | Semgrep | règle ERROR | **bloquant** |
| SCA | Trivy | CRITICAL corrigeable | **bloquant** |
| Secrets | Trivy + TruffleHog | secret vérifié | **bloquant** |
| IaC | Trivy config | erreur | avertissement puis bloquant |
| Mises à jour | Dependabot | hebdomadaire | PR, revue humaine |

C'est cette table qui transforme un « pipeline de TP » en **politique DevSecOps**.

---

## Checkpoint phase 4 bis

- [ ] `jolokia-core` et `chaos-monkey` retirés des 4 poms
- [ ] `target/bom.json` généré et publié en artefact
- [ ] Semgrep exécuté, findings traités, étape bloquante
- [ ] Trivy en `exit-code: 1`, `ignore-unfixed`, `.trivyignore` justifié
- [ ] Toutes les actions épinglées par SHA
- [ ] TruffleHog sur l'historique : 0 secret
- [ ] Tableau de politique ajouté au rapport
- [ ] PR fusionnée dans `dev`

## Effort

| Tâche | Durée |
|---|---|
| 4bis.0 suppression dépendances | 1 h |
| 4bis.1 SBOM | 1 h |
| 4bis.2 Semgrep | 2 h |
| 4bis.3 Trivy bloquant | 1 h |
| 4bis.4 SHA pins | 30 min |
| 4bis.5 TruffleHog | 30 min |
| 4bis.6 rédaction | 30 min |
| **Total** | **~1 jour** |

## PHASE 5 — PKI avec OpenSSL (🅱️, TP1)

Tout se fait dans `secrets/` à la racine (**ignoré par Git**) pour ne jamais committer une clé. Seul le script `scripts/pki/generate.sh` est committé.

### 5.1 Atelier 1 — Clés RSA, chiffrement (TP1)
```bash
mkdir -p secrets/tp1 && cd secrets/tp1
openssl genrsa -out laClePv.pem 2048                       # TP : 1024 bits, insuffisant aujourd'hui
openssl genrsa -aes256 -out laClePvCrypte.pem 2048         # TP : -des3, protégée par passphrase
openssl rsa -in laClePv.pem -pubout -out laClePu.pem       # clé publique
echo "message secret" > data.txt
openssl pkeyutl -encrypt -pubin -inkey laClePu.pem -in data.txt -out dataChiffre.bin
openssl pkeyutl -decrypt -inkey laClePv.pem -in dataChiffre.bin -out dataClair.txt
cat dataClair.txt
```
**✅** `message secret`. **📝** Rapport : pourquoi 1024 bits est faible et 2048 recommandé (NIST SP 800-57).

### 5.2 Atelier 2 — Empreinte et signature
```bash
openssl dgst -sha256 -out dataEmpreinte.txt data.txt
openssl dgst -sha256 -sign laClePv.pem -out signature.sig data.txt
openssl dgst -sha256 -verify laClePu.pem -signature signature.sig data.txt     # Verified OK
cp data.txt data2.txt && echo "x" >> data2.txt
openssl dgst -sha256 -verify laClePu.pem -signature signature.sig data2.txt    # Verification Failure
cd ../..
```
**📸** Les deux résultats (OK puis Failure) illustrent intégrité et authenticité.

### 5.3 Atelier 3 — Autorité de certification et certificats (script)
Créer `scripts/pki/generate.sh` :
```bash
#!/usr/bin/env bash
# Génère une CA, un certificat serveur par service et un certificat client pour le gateway.
# Usage : PKI_PASS='mot-de-passe-fort' ./scripts/pki/generate.sh
set -euo pipefail
: "${PKI_PASS:?définir PKI_PASS}"
OUT="${1:-secrets}"
mkdir -p "$OUT" && cd "$OUT"

# 1. CA (TP1 : req -x509 -days 365 -new -key ...)
openssl genrsa -out ca.key 2048
openssl req -x509 -new -key ca.key -days 365 -sha256 -out ca.pem \
  -subj "/C=MA/ST=Maroc/L=Rabat/O=ensias/OU=info/CN=PetClinic-CA"

issue() {  # issue <nom> <usage: serverAuth|clientAuth> <SAN>
  local name="$1" eku="$2" san="$3"
  openssl genrsa -out "$name.key" 2048
  openssl req -new -key "$name.key" -out "$name.csr" \
    -subj "/C=MA/ST=Maroc/L=Casa/O=ensias/OU=petclinic/CN=$name"
  printf "subjectAltName=%s\nextendedKeyUsage=%s\nbasicConstraints=CA:FALSE\nkeyUsage=digitalSignature,keyEncipherment\n" \
    "$san" "$eku" > "$name.ext"
  openssl x509 -req -in "$name.csr" -CA ca.pem -CAkey ca.key -CAcreateserial \
    -days 365 -sha256 -extfile "$name.ext" -out "$name.crt"
  openssl pkcs12 -export -in "$name.crt" -inkey "$name.key" -certfile ca.pem \
    -name "$name" -out "$name.p12" -passout "pass:$PKI_PASS"
}

for svc in customers vets visits; do
  issue "$svc" serverAuth "DNS:localhost,DNS:$svc-service,IP:127.0.0.1"
done
issue api-gateway clientAuth "DNS:localhost,DNS:api-gateway"

# 2. Truststore Java contenant la CA
keytool -importcert -noprompt -alias petclinic-ca -file ca.pem \
  -keystore truststore.p12 -storetype PKCS12 -storepass "$PKI_PASS"

# 3. Contrôles
for f in customers vets visits api-gateway; do openssl verify -CAfile ca.pem "$f.crt"; done
```
```bash
chmod +x scripts/pki/generate.sh
export PKI_PASS='un-mot-de-passe-fort-local'     # ne pas le committer ni l'écrire dans un fichier suivi
./scripts/pki/generate.sh
```
**✅** Quatre lignes `…crt: OK`. Fichiers créés dans `secrets/` (`ca.pem`, `*.p12`, `truststore.p12`).
Points clés expliqués (à reprendre dans le rapport) :
- **SAN** (`subjectAltName`) : Java et `curl` vérifient le nom contre le SAN, pas contre le CN.
- **EKU** : `serverAuth` pour les services, `clientAuth` pour le gateway (requis par mTLS).
- **PKCS12** : format directement lu par Spring Boot (le TP utilise `keytool -genkey … -keystore .keystore`).

### 5.4 Démonstration du TP3 à la lettre (pour la capture)
```bash
keytool -genkey -alias tomcat -keyalg RSA -keystore secrets/.keystore -keysize 2048
```
Montrer en capture l'avertissement du navigateur (certificat auto-signé, pas de CA reconnue) et expliquer la différence avec une CA que **vous** faites approuver au client (truststore).

### 5.5 Partage des fichiers entre A et B
Les fichiers de `secrets/` se transmettent **hors Git** (clé USB, ou service de partage chiffré). Alternative sans partage : A exécute lui aussi `generate.sh` avec **sa propre** CA pour ses tests locaux, mais une seule CA doit servir pour la démo finale.

**📸 Preuves :** captures atelier 1, atelier 2, `openssl x509 -in secrets/ca.pem -noout -text | head -20`, `openssl verify`.

---

## PHASE 6 — Authentification JWT en bordure (🅱️)

Objectif : sans jeton → 401 ; jeton valide sans le bon rôle → 403 ; jeton valide avec rôle → succès, **sur les écritures** (POST, PUT, DELETE). Les lectures (GET) restent publiques pour que le front fonctionne en consultation.

### 6.1 Lancer Keycloak (développement)
```bash
docker run -d --name keycloak -p 8180:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD="$KC_ADMIN_PASS" \
  quay.io/keycloak/keycloak:26.0 start-dev
```
(Définir `KC_ADMIN_PASS` dans votre shell, sans l'écrire dans un fichier suivi.) Console : http://localhost:8180.

### 6.2 Configurer Keycloak (interface)
1. Menu realm → **Create realm** : `petclinic`.
2. **Clients → Create client** : ID `petclinic-cli`, *Client authentication* **Off** (client public), cocher **Direct access grants**. Enregistrer.
3. **Realm roles → Create role** : `staff`.
4. **Users → Create user** : `alice` (+ mot de passe dans l'onglet *Credentials*, *Temporary* Off) → onglet *Role mapping* → **Assign role** `staff`.
5. Créer un second utilisateur `bob` **sans** rôle (pour tester le 403).
6. Les rôles apparaissent dans le jeton sous `realm_access.roles`.

### 6.3 Obtenir un jeton
```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/petclinic/protocol/openid-connect/token \
  -d grant_type=password -d client_id=petclinic-cli -d username=alice -d password='LE_MDP' \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
echo "${TOKEN:0:30}..."
```
**✅** Une chaîne qui commence par `eyJ`. Décoder le contenu sur https://jwt.io avec un jeton **de test** uniquement.

### 6.4 Dépendances du gateway
Dans `spring-petclinic-api-gateway/pom.xml` :
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
```
**⛔** Spring Boot 4 a réorganisé ses starters. Si Maven ne résout pas ce nom, essayer `spring-boot-starter-security-oauth2-resource-server` (rechercher sur Maven Central « spring-boot-starter » + « resource-server » pour la version 4.0.1). Le gateway étant **WebFlux**, on utilise `ServerHttpSecurity`, pas `HttpSecurity`.

### 6.5 Configuration de sécurité du gateway
`spring-petclinic-api-gateway/src/main/resources/application.yml`, ajouter sous `spring:` (à côté de `cloud:`) :
```yaml
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8180/realms/petclinic}
```
Nouvelle classe `…/petclinic/api/config/SecurityConfig.java` :
```java
package org.springframework.samples.petclinic.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)   // API sans session : jeton Bearer
            .authorizeExchange(auth -> auth
                .pathMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                .pathMatchers(HttpMethod.GET, "/**").permitAll()           // front + lectures
                .pathMatchers(HttpMethod.POST, "/api/**").hasRole("staff")
                .pathMatchers(HttpMethod.PUT, "/api/**").hasRole("staff")
                .pathMatchers(HttpMethod.DELETE, "/api/**").hasRole("staff")
                .anyExchange().denyAll())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
                jwt.jwtAuthenticationConverter(keycloakRoleConverter())))
            .build();
    }

    /** Traduit realm_access.roles (Keycloak) en autorités ROLE_xxx. */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> keycloakRoleConverter() {
        return new ReactiveJwtAuthenticationConverterAdapter(jwt -> {
            Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
            Collection<GrantedAuthority> authorities = List.of();
            if (realm != null && realm.get("roles") instanceof Collection<?> roles) {
                authorities = roles.stream()
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            }
            return new JwtAuthenticationToken(jwt, authorities);
        });
    }
}
```
`anyExchange().denyAll()` : tout ce qui n'est pas explicitement autorisé est refusé (principe du refus par défaut). Les actuator autres que `health/info/prometheus` seront bloqués ici ET retirés de l'exposition en phase 10.

### 6.6 Corriger le retry sur POST (constat C9)
Dans `api-gateway/application.yml`, filtre `Retry` : remplacer `methods: POST` par `methods: GET`.
Raison : rejouer un POST peut créer deux fois la même ressource ; seuls les GET (idempotents) se rejouent.

### 6.7 Tests
```bash
./mvnw -B -DskipTests package
# Démarrer discovery, les 3 services, puis le gateway (voir scripts/run_all.sh ou README)
curl -i -X POST http://localhost:8080/api/customer/owners \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","address":"1 rue X","city":"Rabat","telephone":"0612345678"}'
# 401
TOKEN_BOB=...   # jeton de bob (sans rôle)  -> même requête avec -H "Authorization: Bearer $TOKEN_BOB" : 403
curl -i -X POST http://localhost:8080/api/customer/owners \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","address":"1 rue X","city":"Rabat","telephone":"0612345678"}'
# 201
curl -i http://localhost:8080/api/customer/owners        # 200 sans jeton (GET public)
```
**✅** 401 sans jeton ; 403 pour `bob` ; 201 pour `alice` ; 200 en GET.
**⛔** `401` malgré un jeton valide : l'`issuer` du jeton ne correspond pas à `issuer-uri` (le nom d'hôte utilisé pour obtenir le jeton doit être identique). `ConnectException` au démarrage : Keycloak non lancé (le gateway contacte l'issuer au premier appel).

### 6.8 Limite à assumer dans le rapport
Le front Vue n'a pas d'écran de connexion : ses boutons d'écriture recevront 401. Deux options : (1) **retenue ici** — démonstration des écritures via `curl`/Postman, le front reste en lecture ; (2) extension : gateway en client OAuth2 (`oauth2Login` + `TokenRelay`) pour une vraie connexion navigateur. À mentionner comme perspective si non réalisée.

### 6.9 Défense en profondeur (optionnel, si le temps le permet)
Les services valident aussi le JWT (dépendances `spring-boot-starter-oauth2-resource-server`, `SecurityFilterChain` MVC, `@PreAuthorize`). Cela couvre le cas où l'on contourne le gateway (menace F6), en complément de mTLS.

**📸 Preuves :** les 4 réponses curl (401/403/201/200) ; capture Keycloak (realm, rôle, utilisateur) ; extrait de `SecurityConfig`.

---

## PHASE 7 — TLS puis mTLS (🅱️, TP3)

On avance en **trois paliers**. Valider chaque palier avant le suivant ; si le palier 3 bloque plus d'une journée, le documenter comme limite.

### 7.0 Principe et pièges propres à votre projet
- Les services écoutent sur **un port aléatoire** (`port: 0`) hors profil `docker`. Pour le TLS, fixer les ports : 8081/8082/8083 via `SERVER_PORT`.
- Eureka enregistre par **IP** (`prefer-ip-address: true`). Le gateway tenterait `https://172.x.x.x:8081` ; votre certificat est valable pour des **noms**. On enregistre donc par **nom d'hôte** (`prefer-ip-address: false`, `hostname` = nom du conteneur ou `localhost`) et on déclare le port **sécurisé** à Eureka.
- Pour que `lb://` produise du `https://`, l'instance doit s'enregistrer avec `secure-port-enabled: true` et `non-secure-port-enabled: false`.
- Séparer le port **management** (Actuator) du port applicatif : le port applicatif exige un certificat client, pas Prometheus.

### 7.1 Palier 1 — HTTPS côté serveur sur un service (customers)
Dans `customers-service/src/main/resources/application.yml`, ajouter un document de profil à la fin :
```yaml
---
spring:
  config:
    activate:
      on-profile: tls
server:
  port: ${SERVER_PORT:8081}
  ssl:
    enabled: true
    key-store: file:${PKI_DIR:secrets}/customers.p12
    key-store-type: PKCS12
    key-store-password: ${PKI_PASS}
    key-alias: customers
management:
  server:
    port: 8091
eureka:
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:localhost}
    prefer-ip-address: false
    secure-port-enabled: true
    non-secure-port-enabled: false
    secure-port: ${server.port}
```
```bash
export PKI_PASS='…'
./mvnw -B -DskipTests package
SPRING_PROFILES_ACTIVE=tls java -jar spring-petclinic-customers-service/target/*.jar
```
Test (dans un autre terminal) :
```bash
curl --cacert secrets/ca.pem https://localhost:8081/owners      # 200
curl https://localhost:8081/owners                              # échec : certificat non reconnu
curl http://localhost:8081/owners                               # échec : port TLS
```
**✅** Succès avec `--cacert`, échec sans. **📸** Navigateur sur `https://localhost:8081` (avertissement) puis après import de `ca.pem` dans le magasin de confiance : cadenas valide.
Reproduire pour `vets` (8083, alias `vets`, management 8093) et `visits` (8082, alias `visits`, management 8092).
**⛔ `vets` :** son YAML active le profil `production` (cache). Définir `SPRING_PROFILES_ACTIVE` le **remplace**. Lancer vets avec `SPRING_PROFILES_ACTIVE=production,tls`. (Dans Docker, `ENV SPRING_PROFILES_ACTIVE=docker` écrase déjà ce profil : le cache n'y est donc pas actif ; à noter dans le rapport.)

### 7.2 Palier 2 — Le gateway fait confiance à la CA
Dans `api-gateway/application.yml`, document de profil :
```yaml
---
spring:
  config:
    activate:
      on-profile: tls
  cloud:
    gateway:
      server:
        webflux:
          httpclient:
            ssl:
              trustedX509Certificates:
                - file:${PKI_DIR:secrets}/ca.pem
```
Lancer discovery, les 3 services (`tls`) puis le gateway (`tls`). Les routes `lb://` doivent résoudre en `https://`.
```bash
curl -i http://localhost:8080/api/customer/owners      # 200 via gateway, canal interne chiffré
```
**⛔** `PKIX path building failed` / `unable to find valid certification path` : la CA n'est pas approuvée par le client qui échoue. **Remarque :** le préfixe des propriétés Gateway dans votre version est `spring.cloud.gateway.server.webflux.*` (vu dans votre YAML) ; si la clé `httpclient.ssl.*` n'est pas reconnue, consulter la documentation Spring Cloud Gateway 2025.1 pour le nom exact.
**⛔** `No instances available` ou http au lieu de https : les services ne s'enregistrent pas avec le port sécurisé ; vérifier dans http://localhost:8761 les URL des instances (elles doivent être en `https`, avec un nom d'hôte).

L'endpoint d'agrégation `/api/gateway/owners/{id}` utilise `RestTemplate`/`WebClient` : ils ne lisent pas la configuration ci-dessus. Pour ce palier, lancer le gateway avec la truststore JVM :
```bash
JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=secrets/truststore.p12 -Djavax.net.ssl.trustStoreType=PKCS12 -Djavax.net.ssl.trustStorePassword=$PKI_PASS" \
SPRING_PROFILES_ACTIVE=tls java -jar spring-petclinic-api-gateway/target/*.jar
curl -i http://localhost:8080/api/gateway/owners/1
```

### 7.3 Palier 3 — mTLS (le serveur exige un certificat client)
Services, dans le bloc `server.ssl` du profil `tls` :
```yaml
    client-auth: need
    trust-store: file:${PKI_DIR:secrets}/truststore.p12
    trust-store-type: PKCS12
    trust-store-password: ${PKI_PASS}
```
Gateway, dans `httpclient.ssl` :
```yaml
              key-store: ${PKI_DIR:secrets}/api-gateway.p12
              key-store-type: PKCS12
              key-store-password: ${PKI_PASS}
              key-password: ${PKI_PASS}
```
Tests :
```bash
# Sans certificat client : handshake refusé
curl --cacert secrets/ca.pem https://localhost:8081/owners
# Avec le certificat du gateway : accepté
curl --cacert secrets/ca.pem --cert secrets/api-gateway.crt --key secrets/api-gateway.key https://localhost:8081/owners
# Via le gateway
curl -i http://localhost:8080/api/customer/owners
```
**✅** `curl` sans certificat échoue (`alert certificate required` / `handshake failure`) ; avec certificat → 200 ; via le gateway → 200.
**Point délicat :** les clients `RestTemplate`/`WebClient` de l'endpoint d'agrégation ne présentent **pas** de certificat client par défaut (Netty ne lit pas `javax.net.ssl.keyStore`). Deux solutions : (a) configurer un **SSL bundle** Spring Boot (`spring.ssl.bundle.pem`/`jks`) et construire ces clients avec `WebClientSsl` / `RestTemplateBuilder.sslBundle(...)` ; (b) si le temps manque, **documenter** que l'endpoint d'agrégation reste en TLS simple et que les 3 routes principales sont en mTLS. Cette limite est acceptable et honnête dans le rapport (voir « Risques » du plan).

### 7.4 Eureka
Eureka reste en HTTP dans cette phase (annuaire interne) ; sa protection (authentification) est en phase 10.

**📸 Preuves :** les `curl` des trois paliers (succès et échecs), liste Eureka avec instances `https`, extrait des YAML (sans mot de passe), capture du navigateur avec/sans la CA.

---

## PHASE 8 — Base RDS MySQL et profil `prod` (🅰️, TP4)

> **Répartition** : A réalise les phases 8 (RDS) et 9 (Docker/ECR/EC2), qui sont les seules à dépendre d'AWS. B réalise 5, 6, 7 et 10 (local).

### 8.0 Deux écarts à connaître
1. Dans votre dépôt, les 3 scripts `db/mysql/schema.sql` commencent par `CREATE DATABASE IF NOT EXISTS petclinic; USE petclinic;`. Cela **ignore** la base de l'URL JDBC : les 3 services écriraient dans la même base `petclinic`. Pour avoir 3 schémas séparés (principe de séparation des données), **supprimer ces deux lignes** dans les trois fichiers (les `INSERT IGNORE` et `CREATE TABLE IF NOT EXISTS` sont déjà rejouables sans doublon).
2. En CI, les tests restent sur HSQLDB en mémoire : **ne pas** connecter les tests à RDS (les adresses IP des runners GitHub changent, ouvrir RDS à tous est dangereux).

### 8.1 Créer l'instance (TP4 étapes 1-6)
AWS → **RDS → Databases → Create database → Full configuration** :

| Option | Valeur TP4 | Valeur retenue ici (justification) |
|---|---|---|
| Engine | MySQL | MySQL 8.x |
| Template | Free tier | Free tier |
| Identifier | `instanceEmployeDB` | `petclinic-db` |
| Master user / password | `admin` / choisi | `admin` / **mot de passe long**, stocké dans un gestionnaire de mots de passe |
| Public access | Yes | **Yes seulement pendant la configuration**, puis No une fois l'EC2 en place |
| Security group | create new `sg_employedb` | `sg_petclinicdb` |
| Initial database name | `employedb` | vide (on crée 3 schémas à la main) |
| Chiffrement | décoché | **activé** |
| Sauvegardes automatiques | décochées | **activées** (rétention 1 à 7 jours) |

### 8.2 Règle réseau (TP4 étape 7)
RDS → instance → *Connectivity & security* → groupe de sécurité → **Edit inbound rules** :
- **Version TP** : MySQL/Aurora, port 3306, source *Anywhere-IPv4* (`0.0.0.0/0`). À reproduire **brièvement pour la capture**, puis corriger.
- **Version retenue** : source **My IP** (`/32`), puis, quand l'EC2 existe (phase 9), source = **groupe de sécurité de l'EC2** et *Public access: No*.

Tester depuis MySQL Workbench (TP4 étape 8) : hôte = Endpoint RDS, port 3306, utilisateur `admin`.

### 8.3 Schémas et utilisateur applicatif (MySQL Workbench)
```sql
CREATE DATABASE petclinic_customers CHARACTER SET utf8mb4;
CREATE DATABASE petclinic_vets      CHARACTER SET utf8mb4;
CREATE DATABASE petclinic_visits    CHARACTER SET utf8mb4;

CREATE USER 'petclinic_app'@'%' IDENTIFIED BY '<mot-de-passe-fort>';
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX ON petclinic_customers.* TO 'petclinic_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX ON petclinic_vets.*      TO 'petclinic_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX ON petclinic_visits.*    TO 'petclinic_app'@'%';
```
Ne jamais utiliser `admin` depuis les applications (moindre privilège).

### 8.4 Profil `prod` dans chaque service
Créer `src/main/resources/application-prod.yml` dans **chacun** des trois services :
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none          # le schéma vient des scripts SQL (le TP met "update", plus risqué)
    show-sql: false
  sql:
    init:
      mode: always
      schema-locations: classpath*:db/mysql/schema.sql
      data-locations: classpath*:db/mysql/data.sql
```
Contrairement au TP5, **ne pas** mettre `spring.profiles.active=prod` en dur dans `application.yml` : le profil s'active à l'exécution (`SPRING_PROFILES_ACTIVE=prod`), sinon développement et tests tomberaient aussi sur MySQL.
URL type (exemple pour customers) :
`jdbc:mysql://<endpoint-rds>:3306/petclinic_customers?sslMode=REQUIRED`

### 8.5 Secrets GitHub (TP5 étape 6) — 🅰️ les crée (aucun partage nécessaire)
GitHub → Settings → Secrets and variables → Actions :

| Secret | Valeur |
|---|---|
| `DB_URL_CUSTOMERS` | `jdbc:mysql://<endpoint>:3306/petclinic_customers?sslMode=REQUIRED` |
| `DB_URL_VETS` | idem `petclinic_vets` |
| `DB_URL_VISITS` | idem `petclinic_visits` |
| `DB_USERNAME` | `petclinic_app` |
| `DB_PASSWORD` | mot de passe fort |

Le TP5 les nomme `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ; trois URL sont nécessaires ici car il y a trois schémas.

### 8.6 Test de persistance
```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL='jdbc:mysql://<endpoint>:3306/petclinic_customers?sslMode=REQUIRED'
export SPRING_DATASOURCE_USERNAME=petclinic_app
export SPRING_DATASOURCE_PASSWORD='…'
SERVER_PORT=8081 java -jar spring-petclinic-customers-service/target/*.jar
```
Créer un propriétaire (phase 6), **arrêter** le service, le relancer, relire : la donnée existe encore (avec HSQLDB elle disparaîtrait).
**✅** Tables visibles dans Workbench ; donnée persistante.
**⛔** `Communications link failure` : règle de groupe de sécurité ou *Public access* ; `Access denied` : mauvais utilisateur ou GRANT manquant ; `Unknown database` : schéma non créé.

**📸 Preuves :** configuration RDS (avant/après), règle entrante TP vs retenue, Workbench avec 3 schémas, test de persistance. **📝** Tableau comparatif TP4 / retenu (chiffrement, sauvegardes, accès, utilisateur).

---

## PHASE 9 — Docker, ECR, EC2 : le Deploy Job (🅰️, TP3 suite)

> **Répartition** : A réalise la phase 9 (AWS) ; elle dépend de la phase 8 (profil `prod`), également réalisée par A.

Le schéma du TP : *Configure AWS credentials → Login to Amazon ECR → Build, Tag & Push Docker image → Deploy to EC2.* Je n'ai que ce schéma (pas le détail des étapes du PDF) : la section ci-dessous est l'implémentation standard de ce schéma, adaptée à vos 5 modules.

### 9.1 Dockerfile durci (`docker/Dockerfile`)
Remplacer le contenu par :
```dockerfile
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /application
ARG ARTIFACT_NAME
COPY ${ARTIFACT_NAME}.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /application
ARG EXPOSED_PORT
EXPOSE ${EXPOSED_PORT}
ENV SPRING_PROFILES_ACTIVE=docker

COPY --from=builder --chown=app:app /application/dependencies/ ./
RUN true
COPY --from=builder --chown=app:app /application/spring-boot-loader/ ./
RUN true
COPY --from=builder --chown=app:app /application/snapshot-dependencies/ ./
RUN true
COPY --from=builder --chown=app:app /application/application/ ./

USER app
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```
Changements : JRE (surface réduite), utilisateur non-root (constat C7). Les `RUN true` sont conservés du fichier d'origine (contournement btrfs).
**⛔** Si `layertools` est refusé (Spring Boot 4), remplacer l'étape par `java -Djarmode=tools -jar application.jar extract --layers --launcher` et ajuster les `COPY` ; vérifier `docker build` avant de continuer.

### 9.2 Construire les images en local
```bash
./mvnw -B -DskipTests -PbuildDocker -Ddocker.image.prefix=petclinic install
docker images | grep petclinic
docker run --rm --entrypoint id petclinic/spring-petclinic-customers-service    # uid != 0
```
**✅** 5 images ; l'utilisateur n'est pas `root`.

### 9.3 Scripts de tag/push à nettoyer
`scripts/tagImages.sh` et `pushImages.sh` listent config-server, admin-server et genai-service (hors build). Les remplacer par la liste des 5 modules :
```bash
#!/bin/bash
set -euo pipefail
MODULES="discovery-server api-gateway customers-service visits-service vets-service"
for m in $MODULES; do
  docker tag  "${REPOSITORY_PREFIX}/spring-petclinic-$m" "${REPOSITORY_PREFIX}/spring-petclinic-$m:${VERSION}"
  docker push "${REPOSITORY_PREFIX}/spring-petclinic-$m:${VERSION}"
done
```
(Le workflow ci-dessous fait la même chose automatiquement.)

### 9.4 Préparer AWS
1. **ECR** → *Create repository* (privé, **Scan on push** activé) ×5 : `petclinic/spring-petclinic-discovery-server`, `…-api-gateway`, `…-customers-service`, `…-visits-service`, `…-vets-service`.
2. **IAM** → utilisateur `github-deployer`, politique **minimale** : `ecr:GetAuthorizationToken` (ressource `*`) et, sur les 5 dépôts, `ecr:BatchCheckLayerAvailability`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecr:PutImage`, `ecr:BatchGetImage`, `ecr:GetDownloadUrlForLayer`. Créer une *access key* (le TP utilise ce mécanisme).
3. **EC2** : Ubuntu 22.04/24.04. **Attention à la taille :** 5 JVM Spring Boot + Keycloak + Prometheus/Grafana/Zipkin ne tiennent pas dans 1 Go (t2/t3.micro). Prévoir **4 Go** (ex. `t3.medium`, payant) ou réduire la pile (sans Grafana/Zipkin/Keycloak sur l'EC2). Vérifier le coût et **arrêter l'instance** après chaque séance.
4. Groupe de sécurité EC2 : SSH 22 depuis **votre IP seulement** ; port 8080 depuis votre IP (ou 0.0.0.0/0 pour la démo, puis fermer).
5. Sur l'EC2 : `sudo apt-get update && sudo apt-get install -y docker.io docker-compose-v2 awscli && sudo usermod -aG docker ubuntu`, puis créer `~/petclinic/` avec `docker-compose.prod.yml` et un fichier `.env` **écrit à la main sur le serveur** (jamais dans Git).
6. Rôle IAM d'instance avec droits de **lecture** ECR (`ecr:GetAuthorizationToken`, `ecr:BatchGetImage`, `ecr:GetDownloadUrlForLayer`) pour que l'EC2 tire les images sans clé stockée.

### 9.5 Secrets GitHub du déploiement
`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `ECR_REGISTRY` (`<compte>.dkr.ecr.<région>.amazonaws.com`), `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` (contenu complet du `.pem`).
Amélioration à citer dans le rapport : **OIDC GitHub → AWS** (aucune clé longue durée) ; le TP utilise des clés.

### 9.6 Deploy Job (à ajouter dans `deploy.yml`, après `build`)
```yaml
  deploy:
    needs: build
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    permissions:
      contents: read
    strategy:
      fail-fast: false
      matrix:
        module:
          - discovery-server
          - api-gateway
          - customers-service
          - visits-service
          - vets-service
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: temurin
          cache: maven

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ secrets.AWS_REGION }}

      - name: Login to Amazon ECR
        id: ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build du jar et de l'image Docker
        run: |
          chmod +x mvnw
          ./mvnw -B -DskipTests -PbuildDocker -Ddocker.image.prefix=${{ steps.ecr.outputs.registry }}/petclinic \
            -pl spring-petclinic-${{ matrix.module }} -am install

      - name: Scan Trivy de l'image
        uses: aquasecurity/trivy-action@0.28.0
        with:
          image-ref: ${{ steps.ecr.outputs.registry }}/petclinic/spring-petclinic-${{ matrix.module }}:latest
          severity: CRITICAL
          exit-code: '0'

      - name: Tag et push
        run: |
          IMG=${{ steps.ecr.outputs.registry }}/petclinic/spring-petclinic-${{ matrix.module }}
          docker tag $IMG $IMG:${{ github.sha }}
          docker push $IMG:${{ github.sha }}
          docker push $IMG:latest

  deploy-ec2:
    needs: deploy
    runs-on: ubuntu-latest
    steps:
      - name: Déploiement sur EC2
        uses: appleboy/ssh-action@v1.2.0
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd ~/petclinic
            aws ecr get-login-password --region ${{ secrets.AWS_REGION }} \
              | docker login --username AWS --password-stdin ${{ secrets.ECR_REGISTRY }}
            docker compose -f docker-compose.prod.yml pull
            docker compose -f docker-compose.prod.yml up -d
```
Les images portent le nom `<registre>/petclinic/spring-petclinic-<module>` (le nom du module vient de l'`artifactId`, voir `docker build -t ${docker.image.prefix}/${project.artifactId}` dans le `pom.xml`). Les dépôts ECR créés en 9.4 doivent porter **exactement** ces noms. Vérifier la dernière version des actions utilisées (Marketplace) et épingler.
Le job `deploy` ne tourne que sur `push` vers `main`, donc **après** merge d'une PR approuvée.

### 9.7 `docker-compose.prod.yml` (sur l'EC2, versionné sans secrets)
À créer à la racine du dépôt (A), copié sur l'EC2. Principes : images ECR, **aucun port de service publié** (corrige C3), deux réseaux, secrets par variables ou fichiers.
```yaml
name: petclinic

x-java: &java
  restart: unless-stopped
  networks: [internal]
  environment: &env
    SPRING_PROFILES_ACTIVE: docker,tls
    EUREKA_URL: http://discovery-server:8761/eureka/
    PKI_DIR: /run/secrets/pki
    PKI_PASS: ${PKI_PASS}
  volumes:
    - ./secrets:/run/secrets/pki:ro

services:
  discovery-server:
    <<: *java
    image: ${ECR}/petclinic/spring-petclinic-discovery-server:latest
    environment:
      SPRING_PROFILES_ACTIVE: docker
  customers-service:
    <<: *java
    image: ${ECR}/petclinic/spring-petclinic-customers-service:latest
    environment:
      <<: *env
      SPRING_PROFILES_ACTIVE: docker,tls,prod
      EUREKA_INSTANCE_HOSTNAME: customers-service
      SPRING_DATASOURCE_URL: ${DB_URL_CUSTOMERS}
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
  # visits-service et vets-service : même modèle (vets : ajouter "production" aux profils)
  api-gateway:
    <<: *java
    image: ${ECR}/petclinic/spring-petclinic-api-gateway:latest
    networks: [internal, edge]
    environment:
      <<: *env
      SPRING_PROFILES_ACTIVE: docker,tls
      JWT_ISSUER_URI: ${JWT_ISSUER_URI}
    ports:
      - "8080:8080"       # seul port publié côté application

networks:
  internal:
    internal: true        # pas d'accès Internet direct, pas de port publié
  edge: {}
```
Le fichier `.env` du serveur contient `ECR`, `PKI_PASS`, `DB_*`, `JWT_ISSUER_URI`. Les keystores (`secrets/*.p12`, `ca.pem`) sont copiés **une fois** sur l'EC2 via `scp` (jamais via Git).
**⛔** Si `docker compose` n'affiche aucun service en bonne santé, lire `docker compose logs <service>` : la cause la plus fréquente est un mot de passe de keystore ou un nom d'hôte Eureka incohérent (voir phase 7).

### 9.8 Vérifications
**✅** Le run Actions montre `build` puis `deploy` (×5) puis `deploy-ec2` verts ; les 5 images sont dans ECR avec le tag du commit ; `curl http://<EC2_HOST>:8080/api/vet/vets` répond ; `curl http://<EC2_HOST>:8081` **ne répond pas** (port non publié).
**📸 Preuves :** run Actions avec les deux jobs, liste ECR avec *scan results*, page de l'application sur l'IP de l'EC2, capture `docker ps` sur l'EC2 (uniquement 8080 publié).
**⚠️ Après la séance :** arrêter l'EC2 et la base RDS, ou les supprimer, pour éviter des frais.

---

## PHASE 10 — Durcissement (🅱️ applicatif ; 🅰️ conteneurs et Grafana)

### 10.1 Actuator (🅱️) — constat C2
Dans `application.yml` des **4** composants qui exposent `'*'` (gateway, customers, visits, vets), remplacer le bloc `management.endpoints` par :
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: never
  metrics:
    export:
      prometheus:
        enabled: true
```
Conserver les autres clés (`tracing`, `zipkin`) existantes.
```bash
curl -i http://localhost:8080/actuator/env       # 404 ou 403
curl -i http://localhost:8080/actuator/health    # 200 {"status":"UP"}
```
**✅** `env`, `beans`, `heapdump`, `loggers` ne répondent plus.

### 10.2 Eureka protégé (🅱️) — constat C5
`discovery-server/pom.xml` : ajouter `spring-boot-starter-security`. Classe :
```java
@Configuration
public class EurekaSecurityConfig {
    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))   // les clients Eureka n'envoient pas de jeton CSRF
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .build();
    }
}
```
Dans `discovery-server/application.yml` :
```yaml
spring:
  security:
    user:
      name: ${EUREKA_USER:eureka}
      password: ${EUREKA_PASSWORD}
```
Dans le `defaultZone` de **chaque client** (gateway et 3 services) : `http://${EUREKA_USER:eureka}:${EUREKA_PASSWORD}@localhost:8761/eureka/` (et `discovery-server` dans le profil `docker`).
```bash
curl -i http://localhost:8761/eureka/apps                        # 401
curl -i -u eureka:"$EUREKA_PASSWORD" http://localhost:8761/eureka/apps   # 200
```
**⛔** Les clients n'arrivent plus à s'enregistrer : mot de passe non défini pour tous les processus, ou CSRF non exclu sur `/eureka/**`.

### 10.3 En-têtes de sécurité et limitation (🅱️)
Dans le gateway, `default-filters`, ajouter la ligne `- SecureHeaders` (X-Content-Type-Options, X-Frame-Options, etc. ; HSTS s'applique en HTTPS).
```bash
curl -I http://localhost:8080/ | grep -iE "x-frame|x-content|referrer"
```
Limitation de débit : le filtre `RequestRateLimiter` de Spring Cloud Gateway nécessite Redis. Si vous ne l'ajoutez pas, **documentez** le risque de déni de service (menace F1-d) comme limite.

### 10.4 Grafana (🅰️) — constat C6
`docker/grafana/grafana.ini` :
```ini
[auth.anonymous]
enabled = false
```
Dans `docker-compose.yml`, service `grafana-server`, ajouter :
```yaml
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD}
```
(`GRAFANA_ADMIN_PASSWORD` dans un `.env` local ignoré par Git.)
**✅** http://localhost:3030 demande une connexion.

### 10.5 Images et compose (🅰️) — constats C3, C7, C8
- Dockerfile non-root et JRE : fait en 9.1.
- Compose local : retirer les `ports:` de `customers-service`, `visits-service`, `vets-service` ; garder 8080 (gateway) et, pour le développement, 8761 (Eureka) et 3030/9091/9411 si besoin.
- Remplacer `image: springcommunity/…` par vos images construites en phase 9 (`petclinic/spring-petclinic-*`).
- Passer Trivy en blocage sur CRITICAL pour les images : `exit-code: '1'` quand les alertes sont traitées.

### 10.6 Script de contrôles automatisés (🅱️)
`scripts/security-smoke.sh` (committé) :
```bash
#!/usr/bin/env bash
set -u
GW=${GW:-http://localhost:8080}
fail=0
check() { # check "description" code_attendu code_obtenu
  if [ "$2" = "$3" ]; then echo "OK   $1"; else echo "FAIL $1 (attendu $2, obtenu $3)"; fail=1; fi
}
code() { curl -s -o /dev/null -w "%{http_code}" "$@"; }

check "GET public"                 200 "$(code $GW/api/vet/vets)"
check "POST sans jeton"            401 "$(code -X POST $GW/api/customer/owners -H 'Content-Type: application/json' -d '{}')"
check "actuator/env masqué"        404 "$(code $GW/actuator/env)"
check "actuator/health"            200 "$(code $GW/actuator/health)"
exit $fail
```
À lancer avant chaque démonstration ; peut être ajouté plus tard au pipeline contre la pile Compose.

### 10.7 Scan dynamique OWASP ZAP (🅱️)
Application lancée sur http://localhost:8080 :
```bash
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd)/docs/preuves:/zap/wrk:rw" -t ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py -t http://host.docker.internal:8080 -r zap-rapport.html
```
Ouvrir `docs/preuves/zap-rapport.html`. Pour chaque alerte : *corrigée*, *acceptée (justification)* ou *faux positif*.
**⛔** Sous Git Bash, `MSYS_NO_PATHCONV=1` évite la déformation du chemin du volume.

**📸 Preuves :** `/actuator/env` avant/après, `curl` Eureka 401/200, `id` du conteneur, Grafana demandant un mot de passe, sortie de `security-smoke.sh`, rapport ZAP annoté, tableau des CVE Trivy avant/après.

---

## PHASE 11 — Observabilité et alertes (🅰️)

### 11.1 Prometheus
`docker/prometheus/prometheus.yml` scrape aujourd'hui `/actuator/prometheus` sur 8080, 8081, 8082, 8083. Avec la phase 7 (management séparé 8091-8093) et la phase 10 (métriques seulement), adapter les cibles :
```yaml
- job_name: customers-service
  metrics_path: /actuator/prometheus
  static_configs:
    - targets: ['customers-service:8091']
- job_name: visits-service
  metrics_path: /actuator/prometheus
  static_configs:
    - targets: ['visits-service:8092']
- job_name: vets-service
  metrics_path: /actuator/prometheus
  static_configs:
    - targets: ['vets-service:8093']
```
(Le gateway reste sur `api-gateway:8080`.)
**✅** http://localhost:9091/targets : toutes les cibles `UP`.

### 11.2 Alertes (simples, sans Alertmanager)
`docker/prometheus/rules.yml` :
```yaml
groups:
  - name: petclinic
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels: { severity: critical }
        annotations: { summary: "{{ $labels.job }} indisponible" }
      - alert: TauxErreurs5xx
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) > 0.1
        for: 2m
        labels: { severity: warning }
        annotations: { summary: "Taux élevé de réponses 5xx" }
      - alert: JetonsRejetes
        expr: sum(rate(spring_security_http_secured_requests_seconds_count{error="AccessDeniedException"}[5m])) > 0.2
        for: 2m
        labels: { severity: warning }
        annotations: { summary: "Rafale de refus d'accès (401/403)" }
```
Référencer le fichier dans `prometheus.yml` : `rule_files: ["rules.yml"]` et le copier dans l'image (`docker/prometheus/Dockerfile`). Le nom exact des métriques de sécurité peut varier ; vérifier dans http://localhost:9091/graph et ajuster l'expression.
**Déclencher volontairement** `ServiceDown` : arrêter un service, attendre ~1 minute, capture de http://localhost:9091/alerts.

### 11.3 Grafana et Zipkin
Grafana : tableau de bord fourni dans `docker/grafana/dashboards/` ; ajouter un panneau « requêtes 401/403 par minute ». Zipkin (http://localhost:9411) : capture d'une trace `api-gateway → customers-service` après un appel via le gateway.

### 11.4 Runbook (1 page dans le rapport)
Que faire en cas de : fuite d'une clé privée (révoquer la CA, régénérer avec `generate.sh`, redéployer), jeton compromis (révoquer la session Keycloak, réduire la durée de vie), annuaire compromis (changer `EUREKA_PASSWORD`, redémarrer les clients), secret CI exposé (révoquer, rotation, `git filter-repo` si committé).

**📸 Preuves :** page *Targets*, alerte déclenchée, trace Zipkin, dashboard Grafana.

---

## PHASE 12 — Rapport LaTeX, 30-40 pages références incluses (🅰️ + 🅱️)

`rapport/main.tex` est vide ; `PLAN-PROJET.tex` fournit une base.

### 12.1 Mise en place (A)
Arborescence : `rapport/main.tex`, `rapport/sections/01-intro.tex` … `10-conclusion.tex`, `rapport/biblio.bib`, `rapport/figures/`.
Paquets conseillés : `babel` (french), `hyperref`, `graphicx`, `listings`, `booktabs`, `tikz` ou images pour les schémas, `biblatex` ou `natbib`.
Compilation : `pdflatex main && bibtex main && pdflatex main && pdflatex main`.

### 12.2 Répartition des chapitres

| # | Chapitre | Pages | Rédige | Relit |
|---|---|---|---|---|
| 1 | Introduction, contexte, problématique | 2-3 | A | B |
| 2 | État de l'art : microservices, DevSecOps, OWASP, JWT, mTLS | 4-5 | A + B | croisé |
| 3 | Architecture du projet et périmètre | 3-4 | A | B |
| 4 | Démarche : SSDLC, STRIDE, lien avec les TP | 3 | B | A |
| 5 | CI/CD et qualité (TP2, TP5) : pipeline, JaCoCo, Sonar, Trivy | 5 | A | B |
| 6 | Cryptographie et PKI (TP1) | 3 | B | A |
| 7 | Sécurisation des communications : JWT, TLS, mTLS (TP3) | 6-7 | B | A |
| 8 | Données et infrastructure (TP4) : RDS, avant/après | 3-4 | A | B |
| 9 | Déploiement Docker, ECR, EC2 (TP3 suite) | 3 | A | B |
| 10 | Observabilité et réponse à incident | 2-3 | A | B |
| 11 | Résultats, limites, discussion | 3 | A + B | croisé |
| 12 | Conclusion et perspectives | 1-2 | A + B | croisé |
| | Références | 2 | A + B | |

### 12.3 Tableau « avant / après » (cœur du rapport)

| Domaine | Avant (constat) | Après | Preuve |
|---|---|---|---|
| Authentification | aucune (C1) | JWT Keycloak, rôle `staff` | Phase 6 |
| Transport | HTTP clair (C4) | TLS, mTLS gateway → services | Phase 7 |
| Contournement du gateway | ports 8081-8083 publiés (C3) | non publiés, réseau interne | Phases 9-10 |
| Actuator | `*` (C2) | `health,info,prometheus` | Phase 10 |
| Eureka | ouvert (C5) | authentification basique | Phase 10 |
| Grafana | anonyme Admin (C6) | connexion obligatoire | Phase 10 |
| Conteneurs | root, JDK (C7) | non-root, JRE | Phase 9 |
| Retry | POST rejoué (C9) | GET uniquement | Phase 6 |
| Base de données | HSQLDB mémoire ; TP4 : `0.0.0.0/0`, non chiffrée | RDS chiffrée, SG restreint, 3 schémas | Phase 8 |
| Qualité | build seul | JaCoCo, Sonar, Trivy, Dependabot, gitleaks | Phases 3-4 |
| Gouvernance | `main` ouverte | PR + revue croisée + checks obligatoires | Phase 3 |

### 12.4 Références à intégrer
OWASP Top 10 (2021) et API Security Top 10 (2023) ; NIST SP 800-204 (sécurité des microservices) ; NIST SP 800-57 (longueur de clés) ; RFC 8446 (TLS 1.3), RFC 7519 (JWT), RFC 5280 (X.509) ; documentation Spring Security, Spring Cloud Gateway, Spring Boot Actuator ; documentation Keycloak ; documentation AWS RDS et ECR ; documentation SonarCloud et JaCoCo.

### 12.5 Contrôles avant rendu
- 30-40 pages **références incluses** (`pdfinfo rapport/main.pdf | grep Pages`).
- Toutes les figures numérotées, légendées et citées dans le texte.
- Aucune capture ne montre un mot de passe, un token ou une clé.
- Chaque affirmation de sécurité renvoie à une preuve (figure, sortie de commande).

---

## PHASE 13 — Soutenance (🅰️ + 🅱️)

### 13.1 Démonstration (10 min, scénarisée)
1. **A** : push d'une branche → PR → checks verts (Java 17/21, JaCoCo, Sonar, Trivy) → merge → déploiement.
2. **B** : `scripts/security-smoke.sh` ; `curl` sans jeton (401), avec jeton `bob` (403), avec `alice` (201).
3. **B** : `curl` direct sur un service sans certificat (échec), avec certificat (succès).
4. **A** : Grafana et alerte `ServiceDown` déclenchée.
5. **B** : persistance RDS après redémarrage.
Prévoir **captures et vidéo de secours** si le réseau ou AWS tombe.

### 13.2 Questions probables (chacun doit savoir répondre à toutes, y compris sur la partie de l'autre)
- Pourquoi mTLS en plus de JWT ? (authentifier le **service** et l'**utilisateur** ; JWT seul ne protège pas contre un appel direct d'un service compromis.)
- Pourquoi 2048 bits ? Pourquoi un SAN ? Qu'est-ce qu'une CA auto-signée change pour le client ?
- Pourquoi `denyAll` par défaut ? Pourquoi GET public ?
- Que fait Eureka et pourquoi le protéger ?
- Pourquoi ne pas tester contre RDS dans la CI ?
- Quelles sont vos limites ? (front sans connexion, agrégation en TLS simple si non finalisée, certificats auto-signés, Keycloak en mode dev.)

### 13.3 Check-list finale

- [ ] `main` protégée ; chaque PR approuvée par l'autre membre
- [ ] CI verte sur `main` (Java 17 et 21), rapports JaCoCo et Sonar accessibles, Quality Gate documentée
- [ ] Trivy, Dependabot, gitleaks en place ; résultats commentés
- [ ] TP1 : captures des 3 ateliers OpenSSL
- [ ] TP3 : démo keytool + HTTPS opérationnel
- [ ] 401 / 403 / 201 prouvés ; `/actuator/env` inaccessible
- [ ] mTLS : appel sans certificat refusé ; limite sur l'agrégation documentée si besoin
- [ ] TP4 : RDS créée, comparaison TP/retenu, persistance prouvée
- [ ] TP5 : interprétation du rapport Sonar rédigée
- [ ] Images dans ECR ; application sur EC2 ; ports 8081-8083 non publiés
- [ ] Rapport compilé (30-40 p.), références complètes
- [ ] `git log -p | grep -iE "password|token|PRIVATE KEY"` ne renvoie aucun secret réel
- [ ] **EC2 et RDS arrêtés ou supprimés** après la soutenance
- [ ] Tokens (Sonar, GitHub, AWS) révoqués ou renouvelés

---

## Annexe — Dépannage rapide

| Symptôme | Cause probable | Action |
|---|---|---|
| `./mvnw: Permission denied` (CI) | bit exécutable | `chmod +x mvnw` avant le build |
| `Rule violated … covered ratio` | couverture sous le seuil | B ajoute des tests ; ajuster `jacoco.minimum` par module |
| Sonar 401 | token faux ou expiré | régénérer, mettre à jour `SONAR_TOKEN` |
| Sonar : conflit d'analyse | « Automatic Analysis » actif | le désactiver dans SonarCloud |
| `PKIX path building failed` | CA absente du truststore du client | configurer `trustedX509Certificates` ou la truststore JVM |
| `certificate_unknown` / `handshake_failure` | pas de certificat client ou mauvaise CA | vérifier `key-store` du gateway et `trust-store` des services |
| `No subject alternative names matching` | nom d'hôte absent du SAN | régénérer le certificat avec le bon `DNS:` / `IP:` |
| `lb://` appelle `http://` | instance enregistrée en non sécurisé | `secure-port-enabled: true`, `non-secure-port-enabled: false` |
| JWT 401 malgré jeton valide | `issuer` différent | obtenir et valider le jeton avec la même URL d'issuer |
| Services Eureka disparus après la phase 10 | identifiants Eureka manquants | définir `EUREKA_PASSWORD` pour tous les processus |
| `Communications link failure` (MySQL) | groupe de sécurité RDS | ajouter votre IP ou le SG de l'EC2 |
| Trois services écrivent dans la même base | `USE petclinic` dans `schema.sql` | supprimer `CREATE DATABASE`/`USE` (phase 8.0) |
| `AccessDenied` ECR au push | politique IAM incomplète | ajouter les actions de la phase 9.4 |
| JVM tuée sur l'EC2 | mémoire insuffisante | instance 4 Go ou pile réduite |
