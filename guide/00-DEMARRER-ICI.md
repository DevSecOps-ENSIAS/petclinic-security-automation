# Guide A→Z — Sécurité de la communication entre microservices (PetClinic)

Guide écrit **pour votre dépôt réel** (`petclinic-security-automation`). Il garde la trace des phases **0 à 7**,
déjà réalisées avec leurs preuves, puis renvoie à **`guide/GuideV2!!!.md`** pour toute la suite.

> ## ⚠️ La suite du projet est régie par `guide/GuideV2!!!.md`
>
> Depuis le **9 novembre 2026**, `GuideV2!!!.md` remplace ce guide pour l'exécution : il suit à 100 % l'énoncé officiel
> (*Authentification et autorisation interservices*). L'énoncé ne demande **ni AWS (RDS, ECR, EC2), ni SonarCloud,
> ni JaCoCo, ni observabilité, ni Docker de production** : les anciennes phases 8, 9 et 11 sont donc supprimées,
> et les phases 10 à 13 sont remplacées par les phases v2 (P8, P10, P11, P12).
>
> **Dépôt Moodle : mardi 24 novembre 2026 avant minuit — viser le 23 au soir.**

### État des phases de ce guide

| Phase | Contenu | État | Suite |
|---|---|---|---|
| 0 | Préparation (outils) | ✅ terminée | — |
| 1 | Dépôt, branches, hygiène des secrets | ✅ terminée | — |
| 2 | Modèle de menace | ✅ terminée | à raccourcir dans le rapport (**P2**) |
| 3 | CI + JaCoCo | ✅ terminée | conservée telle quelle (bonus) |
| 4 | SonarCloud, SCA, secrets | ✅ terminée | conservée telle quelle (bonus) |
| 4 bis | Sécurité de la chaîne (SBOM, SAST, SHA) | ✅ terminée (B, PR #44) | conservée telle quelle |
| 5 | PKI avec OpenSSL | ✅ terminée | à compléter par `rogue-client` (**P3**) |
| 6 | JWT en bordure | ✅ terminée | **à durcir** : vérification dans chaque service (**P5**) |
| 7 | TLS puis mTLS | ✅ terminée | à compléter : liste blanche de CN (**P6**) |
| 8 | RDS MySQL + profil `prod` | ❌ **supprimée** | hors périmètre v2 (HSQLDB en mémoire suffit) |
| 9 | Docker, ECR, EC2 | ❌ **supprimée** | remplacée par l'exécution locale + IntelliJ |
| 10 | Durcissement | ✅ **faite** le 10/11/2026 | devenue **P8** (Actuator + Eureka) |
| 11 | Observabilité et alertes | ❌ **supprimée** | bonus éventuel |
| 12 | Rapport | ♻️ remplacée | **P10** (plan en 9 chapitres de l'énoncé) |
| 13 | Soutenance | ♻️ remplacée | **P12** (présentation) |

➡️ **Les phases restantes (P2, P4, P5, P6, P7, P8, P9, P10, P11, P12) sont en PARTIE 2 de ce fichier**,
avec l'ordre, les livrables, les critères de sortie et les pièges. Le détail d'implémentation (code, YAML, commandes)
est dans `GuideV2!!!.md` : c'est **la** référence à suivre.

---

## 0. Ce que contient réellement votre projet (état d'origine, avant toute modification)

| Élément | État réel dans le dépôt |
|---|---|
| Modules Maven | 5 : `customers`, `vets`, `visits`, `discovery-server` (Eureka), `api-gateway` |
| Versions | Spring Boot **4.0.1**, Spring Cloud 2025.1.0, `java.version` = **17** (JDK 21 OK) |
| Gateway | **WebFlux** (Spring Cloud Gateway). Sert aussi le front Vue. Routes `/api/customer/**`, `/api/visit/**`, `/api/vet/**` (`StripPrefix=2`, `lb://`). Il appelle aussi les services via `RestTemplate`/`WebClient` `@LoadBalanced` (endpoint `/api/gateway/owners/{id}`) |
| Services métier | **Spring MVC**. Port `0` (aléatoire) en local ; ports fixes 8081/8082/8083 avec le profil `docker` |
| Données | HSQLDB **en mémoire** — c'est la cible retenue par la v2 (les scripts `db/mysql/*.sql` et le profil `prod` relèvent de l'ancienne phase 8, supprimée) |
| Sécurité (origine) | **Aucune** dépendance Spring Security. Actuator `include: '*'` partout. Eureka ouvert |
| Docker | `docker/Dockerfile` unique, `eclipse-temurin:17`, **root**. Docker ne sert plus qu'à **Keycloak** (v2) |
| Tests | Existants : `OwnerResourceTest`, `PetResourceTest`, `VetResourceTest`, `VisitResourceTest`, tests gateway et discovery |
| CI | `maven-build.yml` (Java 17 seul, `mvn`) + `check-pr-template.yml` (**hérité du projet amont**, voir phase 1) |
| Rapport | `rapport/main.tex` **vide** ; `PLAN-PROJET.tex/.md` existent |

Trois pièges propres à ce dépôt, tous repris dans la v2 (§6.0) :

1. Les services s'enregistrent dans Eureka avec `prefer-ip-address: true` → le gateway se connecte à une **IP**,
   et un certificat TLS sans SAN IP échoue : il faut enregistrer par **nom d'hôte** avec port sécurisé (**P6**).
2. Le service `vets` active le profil `production` (cache) dans son YAML ; définir `SPRING_PROFILES_ACTIVE`
   **remplace** ce profil → lancer `vets` avec `production,tls` (**P6**).
3. Les services écoutent sur un port aléatoire (`port: 0`) → fixer 8081/8082/8083 via `SERVER_PORT` (**P6**).

---

## 1. Organisation de l'équipe

### 1.1 Rôles (répartition v2)

| | 🅰️ **Membre A — Identité de l'utilisateur** | 🅱️ **Membre B — Identité des services** |
|---|---|---|
| Phases restantes | **P2**, **P4** (OIDC, jetons), **P5** (JWT + RBAC), **P7** (endpoint `dossier`) | **P3** (PKI), **P6** (TLS/mTLS + liste blanche), **P8** (durcissement), **P7** (client mTLS) |
| Fichiers propres | `spring-petclinic-security/**` (sauf `ServiceIdentityFilter`), `SecurityConfig` du gateway, `keycloak/**`, `docker-compose.security.yml` | `scripts/pki/**`, blocs `tls` des `application.yml`, `ServiceIdentityFilter`, `EurekaSecurityConfig` |
| Comptes à créer | **GitHub uniquement** (plus d'AWS ni de SonarCloud) | — (Keycloak tourne en local, dans Docker) |
| Chapitres du rapport | OAuth2/OIDC, JWT, RBAC, scénario `dossier` | PKI, TLS/mTLS, Eureka, durcissement |
| En commun | **P9** (tests), **P10** (rapport), **P11** (archive), **P12** (présentation) | idem |

**Pourquoi cette séparation de fichiers :** elle évite 90 % des conflits Git. Si l'un doit modifier un fichier de l'autre, il ouvre une PR et l'autre la relit.

### 1.2 Règles de travail

1. Branches : `main` (protégée) ← `dev` ← `feature/<phase>-<sujet>` (ex. `feature/p4-keycloak`).
2. **Jamais de push direct sur `main` ni `dev`.** Toute PR est relue et approuvée par **l'autre** membre.
3. Messages de commit : `ci:`, `sec:`, `feat:`, `fix:`, `docs:`, `chore:`.
4. **Aucun secret dans Git** (mot de passe, token, `.pem`, `.p12`, `.key`, `.env`, `secrets/`). Vérification finale : `git log -p | grep -iE "password|PRIVATE KEY"`.
5. Toujours `./mvnw` (pas `mvn`). Après chaque changement de `pom.xml` ou Java : `./mvnw -B -DskipTests package` → `BUILD SUCCESS`.
6. Après chaque changement de `application.yml` : redémarrer le service concerné.
7. Synchronisation de 10 minutes à la fin de chaque phase (qui a fait quoi, qu'est-ce qui bloque).
8. Chaque phase se termine par des **preuves** (captures, sorties de commandes) rangées dans `docs/preuves/pXX-…/` (images committées, jamais de secret visible). Ces preuves nourrissent directement le rapport.

### 1.3 Planning

Le planning à jour (du mardi 10 novembre au dépôt du 23, avec la répartition A / B) est en **PARTIE 2, section B.3** :
c'est la seule version à utiliser. Les anciennes phases 8 à 13 ne sont plus planifiées.

---
## PHASE 0 — Préparation (A + B, chacun sur sa machine) — ✅ TERMINÉE

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
| Docker Hub | optionnel | Non nécessaire : Docker ne sert plus qu'à Keycloak (P4) |

### 0.3 Vérifier que le projet compile et que les tests passent
```bash
./mvnw -B clean verify
```
**✅** `BUILD SUCCESS` pour les 5 modules, tests verts.
**⛔** Si un test gateway échoue, noter lequel et lire `spring-petclinic-api-gateway/target/surefire-reports/` avant de continuer ; ne pas avancer sur un build rouge.

---

## PHASE 1 — Dépôt, branches, nettoyage (🅰️, TP2 étapes 2-3) — ✅ TERMINÉE

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

## PHASE 2 — Modèle de menace (🅰️ seul, 1 h) — ✅ TERMINÉE (à raccourcir en P2)

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

## PHASE 3 — Intégration continue et couverture (🅰️, TP2 étapes 4-9) — ✅ TERMINÉE

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
Rien à faire ici : le build Docker n'est plus au programme (seul Keycloak tourne en conteneur — voir **P4**).

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

## PHASE 4 — SonarCloud, SCA et détection de secrets (🅰️, TP5) — ✅ TERMINÉE

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
Épingler une version précise (vérifier la dernière sur la page de l'action dans le Marketplace GitHub). `exit-code: '0'` informe sans bloquer ; vous passerez à `'1'` une fois les CVE critiques traitées — c'est ce que fait la phase 4 bis, désormais livrée.

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


## PHASE 4 bis — Sécurité de la chaîne logicielle (🅱️, PR #44) — ✅ TERMINÉE

> **But** : passer d'un pipeline « qui mesure » à un pipeline « qui bloque ».
> **Dépend de** : phase 4 (Sonar, Trivy, Dependabot).
> **Durée** : ~1 jour. **Reportée en bonus** : sans incidence sur les phases v2 (P4 à P12).
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

## PHASE 5 — PKI avec OpenSSL (🅱️, TP1) — ✅ TERMINÉE (compléter par `rogue-client` en P3)

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

## PHASE 6 — Authentification JWT en bordure (🅱️) — ✅ TERMINÉE (à durcir en P5 : vérification dans chaque service)

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
`anyExchange().denyAll()` : tout ce qui n'est pas explicitement autorisé est refusé (principe du refus par défaut). Les actuator autres que `health/info/prometheus` seront bloqués ici ET retirés de l'exposition en **P8**.

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
⚠️ **Le guide v2 va plus loin** : *toutes* les requêtes `/api/**` exigent un jeton (Zero Trust, « toujours vérifier »). Le front Vue n'a pas d'écran de connexion : il ne peut donc **ni lire ni écrire** les données, et la démonstration passe entièrement par `curl`/Postman. Le front reste une **perspective** du rapport (gateway en client OAuth2 : `oauth2Login` + `TokenRelay`).

### 6.9 Défense en profondeur — ⚠️ devenu **obligatoire** en P5
Les services valident aussi le JWT (dépendances `spring-boot-starter-oauth2-resource-server`, `SecurityFilterChain` MVC, `@PreAuthorize`). Cela couvre le cas où l'on contourne le gateway (menace F6), en complément de mTLS. En v2, ce n'est plus une option : c'est le cœur de **P5** (module `spring-petclinic-security`), parce que l'énoncé exige que *chaque* microservice prouve identité et droits.

**📸 Preuves :** les 4 réponses curl (401/403/201/200) ; capture Keycloak (realm, rôle, utilisateur) ; extrait de `SecurityConfig`.

---

## PHASE 7 — TLS puis mTLS (🅱️, TP3) — ✅ TERMINÉE (compléter par la liste blanche de CN en P6)

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
Eureka reste en HTTP dans cette phase (annuaire interne) ; sa protection (authentification basique) est en **P8**.

**📸 Preuves :** les `curl` des trois paliers (succès et échecs), liste Eureka avec instances `https`, extrait des YAML (sans mot de passe), capture du navigateur avec/sans la CA.

---

# PARTIE 2 — Travaux restants, alignés sur le guide v2

> **À partir d'ici, `guide/GuideV2!!!.md` fait foi.** Il suit à 100 % l'énoncé officiel
> (*Projet Ingénierie des Systèmes Sécurisés — Authentification et autorisation interservices*, A.U. 2026/2027).
> La partie 1 de ce guide garde la trace des phases 0 à 7, déjà faites, avec leurs preuves.
> Cette partie 2 remplace les anciennes phases 8 à 13 par la numérotation v2.
>
> **Répartition du travail** : ce fichier donne l'ordre, les livrables, les critères de sortie et les pièges.
> Le code, les YAML et les commandes exactes sont dans `GuideV2!!!.md` (sections P2 à P12) — ne pas les réécrire ici.

## B.1 Ce qui change par rapport aux phases 8 à 13 de ce guide

L'énoncé officiel ne demande **ni AWS, ni SonarCloud, ni observabilité**. Les phases qui les traitaient sont donc supprimées :

| Ancienne phase | Ce qu'elle faisait | Décision v2 | Raison |
|---|---|---|---|
| 4 bis — Sécurité de la chaîne | SBOM, Semgrep, Trivy bloquant, épinglage SHA | ✅ **faite (B, PR #44)** | livrée : sans coût de maintenance, elle reste en place |
| 8 — RDS MySQL + profil `prod` | RDS, schémas, secrets GitHub | ❌ **supprimée** | « HSQLDB en mémoire suffit » (v2 §2.2) |
| 9 — Docker, ECR, EC2 | images, registry, déploiement | ❌ **supprimée** | « remplacé par exécution locale + IntelliJ ; Docker réservé à Keycloak » (v2 §2.2) |
| 10 — Durcissement | Actuator, Eureka, en-têtes, Grafana, ZAP | ♻️ **réduite** → **P8** | seuls Actuator et Eureka renforcent l'authentification interservices |
| 11 — Observabilité | Prometheus, Grafana, alertes | ❌ **supprimée** | hors périmètre (bonus éventuel) |
| 12 — Rapport LaTeX 30-40 p. | rapport | ♻️ **P10** | plan réaligné sur les 9 chapitres exigés par l'énoncé |
| 13 — Soutenance | démo | ♻️ **P12** | conservée |

**Ménage effectué le 10/11/2026** : les artefacts de la phase 8 ont été purgés du dépôt — `application-prod.yml` (×3),
`docs/preuves/p08-rds/` et le profil `prod`. Les dépendances MySQL et `db/mysql/*.sql` viennent du premier commit (amont)
et restent en place, non activées. Restent hors périmètre, à ne **pas** merger :
`scripts/tagImages.sh`, `scripts/pushImages.sh`, `docker-compose.prod.yml` et `docs/preuves/p09-deploy/`.

**Ce qui est récupérable** (déjà dans le dépôt, à adapter et non à refaire) :

| Existant | Réutilisé en | Adaptation nécessaire |
|---|---|---|
| `scripts/pki/generate.sh` (CA + 3 certificats serveur + `api-gateway`) | P3 / P6 | ajouter le certificat **`rogue-client`** (CN non autorisé) : sans lui, pas de preuve 403 |
| `spring-petclinic-api-gateway/.../config/SecurityConfig.java` | P5.4 | réécrire selon le modèle v2 (WebFlux + `KeycloakRoles` + refus par défaut) |
| `docker/keycloak/realm-petclinic.json` | P4 | remplacer l'utilisateur `demo` et le client `petclinic-ui` par `alice`/`bob`/`carol`, rôles `staff`/`viewer`, client `petclinic-cli` |
| Profils `tls` des `application.yml` | P6 | compléter par `mtls-required`, `allowed-callers`, `secure-port-enabled` |

## B.2 Correspondance ancienne numérotation → v2

| Ancienne | Nouvelle | État | Responsable |
|---|---|---|---|
| 0 Préparation | **P0** | ✅ fait | A + B |
| 1 Dépôt, branches, secrets | **P1** | ✅ fait | A |
| 2 Modèle de menace | **P2** | ✅ fait (à raccourcir dans le rapport) | A |
| 3 CI + JaCoCo | — | ✅ fait, conservé tel quel | A |
| 4 Sonar + SCA + secrets | — | ✅ fait, hors périmètre mais sans coût | A |
| 5 PKI OpenSSL | **P3** | ✅ fait (manque `rogue-client`) | B |
| 6 JWT en bordure | **P5** | 🟨 partiel : le JWT n'est vérifié **qu'au gateway** | A |
| 7 TLS puis mTLS | **P6** | 🟨 partiel : HTTPS et mTLS en place, **liste blanche de CN absente** | B |
| — | **P4** Keycloak (realm v2 + jetons) | ⬜ à faire | A |
| — | **P5** module `spring-petclinic-security` | ⬜ à faire (le cœur du livrable) | A |
| — | **P7** scénario `dossier` à 3 services | ⬜ à faire | A (gateway) + B (client mTLS) |
| 10 (réduite) | **P8** Actuator + Eureka | ⬜ à faire | B |
| — | **P9** `security-smoke.sh` + tests JUnit | ⬜ à faire | A + B |
| 12 | **P10** Rapport | ⬜ à faire | A + B |
| — | **P11** Archive importable IntelliJ | ⬜ à faire | A + B |
| 13 | **P12** Présentation | ⬜ à faire | A + B |

## B.3 Planning restant

Dépôt Moodle : **mardi 24 novembre 2026 avant minuit** — viser le **23 au soir**.
Date de rédaction de cette partie : **mardi 10 novembre 2026**, soit 14 jours.

**État au 10/11/2026 (soir)** : 4 bis ✅ (B, PR #44) · **P8 ✅** (A, durcissement livré et vérifié) · la phase 8 (RDS) a été purgée du dépôt.
Il reste donc **P3** (complément `rogue-client`), **P4**, **P5**, **P6**, **P7**, **P9**, **P10**, **P11**, **P12**.

| Jour | A (identité utilisateur) | B (identité des services) |
|---|---|---|
| Mar 10 | **P4** Keycloak + jeton `eyJ…` | **P3** : ajouter `rogue-client` à `generate.sh`, vérifier les 5 certificats |
| Mer 11 | **P5** module + branchement des 3 services | **P6** palier 1 (HTTPS sur customers) |
| Jeu 12 | **P5** gateway | **P6** palier 1 sur les 3 services + palier 2 |
| Ven 13 | **P7** endpoint `dossier` | **P6** palier 3 (mTLS strict + liste blanche) |
| Sam 14 – Dim 15 | tampon, corrections, P2 dans le rapport | tampon, corrections, **P8** |
| Lun 16 | **P9** tests automatisés | **P9** `security-smoke.sh` |
| Mar 17 → Sam 21 | **P10** rapport (ch. 1-5 puis relecture) | **P10** rapport (ch. 6-9) |
| Dim 22 | **P11** archive testée à froid | relecture croisée |
| Lun 23 | présentation + **dépôt Moodle** | idem |

---

## P2 — Modèle de menace court (🅰️) — ✅ fait, à réaligner

Le travail de la phase 2 est acquis (`SECURITE-modele-de-menace.md`, preuves `docs/preuves/p02-menace/`).
La v2 n'en attend qu'une chose : **le tableau « avant » du rapport** (chapitre 1 et 7), c'est-à-dire les constats
mesurés sur le code d'origine, avec leur commande de preuve.

| Constat avant | Commande |
|---|---|
| Aucune authentification | `curl -i http://localhost:8080/api/customer/owners` → 200 sans jeton |
| Contournement du gateway | `curl -i http://localhost:8081/owners` (`SERVER_PORT=8081`) → 200 |
| Trafic interne en clair | `http://` visible dans Eureka |
| Annuaire Eureka ouvert | `curl -i http://localhost:8761/eureka/apps` → 200 |
| Actuator exposé | `curl http://localhost:8080/actuator/env` → JSON complet |

**✅** Les cinq constats sont capturés (idéalement sur `main` d'origine, avant les modifications de P4-P8).
**⛔** Ne pas refaire la phase 2 : réutiliser les captures existantes, seul le classement STRIDE est à raccourcir.
📸 `docs/preuves/p02-menace/`.

---

## P4 — Keycloak : émission de jetons OIDC (🅰️, 0,5 j) — ⬜

**But** : disposer d'un émetteur de jetons reproductible, importé automatiquement (aucun clic dans la console).

À produire :
- `docker-compose.security.yml` à la racine : image `quay.io/keycloak/keycloak:26.0`, `start-dev --import-realm`, port **8180**, volume du realm en lecture seule ;
- `keycloak/petclinic-realm.json` : realm `petclinic`, `accessTokenLifespan: 300`, rôles `staff` et `viewer`, client public `petclinic-cli` avec `directAccessGrantsEnabled: true`, et **trois** utilisateurs — `alice` (staff), `bob` (viewer), `carol` (aucun rôle) — chacun avec prénom, nom et e-mail.

```bash
docker compose -f docker-compose.security.yml up -d
TOKEN=$(curl -s -X POST http://localhost:8180/realms/petclinic/protocol/openid-connect/token \
  -d grant_type=password -d client_id=petclinic-cli -d username=alice -d password=alice-pass \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
echo "${TOKEN:0:30}..."   # eyJ...
```

**✅** Une chaîne commençant par `eyJ` ; les rôles apparaissent dans `realm_access.roles` du jeton décodé.
**⛔** `Account is not fully set up` : prénom/nom/e-mail manquants (obligatoires depuis Keycloak 24).
**⛔** Un seul compte ne suffit pas : sans `bob` (viewer) et `carol` (sans rôle), la matrice RBAC de P5 n'est pas démontrable.
📸 `docs/preuves/p04-keycloak/` : console (realm, rôles, utilisateurs), jeton **de test** décodé.

---

## P5 — Module `spring-petclinic-security` : JWT + RBAC dans chaque service (🅰️, 1,5 j) — ⬜

**C'est le module d'authentification/autorisation exigé par l'énoncé** (E1, E2). Tout le détail est en v2 §5.1 à §5.6.

Livrables :
1. module Maven `spring-petclinic-security`, déclaré **en premier** dans `<modules>` du `pom.xml` racine ;
2. `KeycloakRoles` (traduction `realm_access.roles` → `ROLE_xxx`, partagée par le gateway réactif et les services MVC) ;
3. `ServletSecurityConfiguration` : `csrf` désactivé, `STATELESS`, `denyAll()` par défaut, GET → `viewer` **ou** `staff`, écritures → `staff` ;
4. `ServiceIdentityFilter` (propriété de B, mais nécessaire pour compiler) : lit le CN du certificat client, refuse 401 sans certificat si `mtls-required`, 403 si CN hors liste blanche ;
5. branchement des **3 services** : dépendance + `@Import(ServletSecurityConfiguration.class)` + bloc `spring.security.oauth2.resourceserver.jwt.issuer-uri` (obligatoire, sinon le service ne démarre pas) ;
6. gateway : `SecurityConfig` WebFlux réécrit sur le modèle v2 (le gateway transmet `Authorization`, chaque service revalide).

```bash
./mvnw -B -DskipTests package
# matrice attendue (TOKEN = alice, TOKEN_BOB, TOKEN_CAROL)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/vet/vets                              # 401
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN_CAROL" http://localhost:8080/api/vet/vets  # 403
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN_BOB"   http://localhost:8080/api/vet/vets  # 200
```

**✅** La matrice RBAC du §3 de la v2 est respectée (401 / 403 / 200 / 403 / 201) et `BUILD SUCCESS`.
**⛔** Les GET ne sont plus publics : c'est voulu (v2 §2.3, écart 2), le front ne peut plus lire les données — la démo passe par `curl`.
**⛔** `401` avec un jeton valide = `issuer` différent : utiliser **le même nom d'hôte** (`localhost`) pour obtenir le jeton et configurer `issuer-uri`.
**⛔** Tests `@WebMvcTest` existants en 401/403 : ajouter `@AutoConfigureMockMvc(addFilters = false)` sur les tests **métier**.
📸 `docs/preuves/p05-jwt-rbac/` : les cinq codes, extrait de `ServletSecurityConfiguration` et de `SecurityConfig`.

---

## P6 — TLS, mTLS et autorisation par identité de service (🅱️, 3 j) — 🟨 partiel

Acquis de la phase 7 : profil `tls`, HTTPS, gateway faisant confiance à la CA, mTLS `client-auth: need`.
Reste, en v2 §6.2 à §6.5 :
1. un certificat **`rogue-client`** dans `scripts/pki/generate.sh` (CA valide, CN non autorisé) ;
2. l'enregistrement Eureka par **nom d'hôte** avec port sécurisé (`prefer-ip-address: false`, `secure-port-enabled: true`, `non-secure-port-enabled: false`) ;
3. `petclinic.security.mtls-required` et `allowed-callers: api-gateway` dans les blocs `tls` ;
4. les **cinq** tests du palier 3 :

```bash
curl --cacert secrets/ca.pem -i https://localhost:8081/owners                                  # (1) handshake refusé
curl --cacert secrets/ca.pem --cert secrets/api-gateway.crt --key secrets/api-gateway.key -i https://localhost:8081/owners          # (2) 401
curl ... --cert secrets/api-gateway.crt --key secrets/api-gateway.key -H "Authorization: Bearer $TOKEN" -i https://localhost:8081/owners  # (3) 200
curl ... --cert secrets/rogue-client.crt  --key secrets/rogue-client.key  -H "Authorization: Bearer $TOKEN" -i https://localhost:8081/owners  # (4) 403
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/customer/owners            # (5) 200
```

**✅** Les cas (3) et (4) montrent la différence entre **authentifier** un service (mTLS) et **l'autoriser** (liste blanche de CN).
**⛔** `lb://` appelle `http://` : l'instance s'est enregistrée sans port sécurisé.
**⛔** `PKIX path building failed` : la CA n'est pas dans le truststore du client qui échoue.
📸 `docs/preuves/p06-mtls/` : les cinq commandes, liste Eureka en `https`.

---

## P7 — Scénario `dossier` à 3 microservices (🅰️ gateway + 🅱️ client mTLS, 1 j) — ⬜

**Exigence E3 de l'énoncé** : une requête cliente fait collaborer **les trois** services.

`GET /api/gateway/owners/{id}/dossier` → le gateway interroge `customers` (propriétaire + animaux), `visits` (visites) **et** `vets`, puis assemble la réponse. Chaque appel porte le certificat du gateway (identité de service) **et** le jeton de l'utilisateur (identité + rôles).

À produire (v2 §7.1 et §7.2) : `VetsServiceClient`, `OwnerDossier`, `DossierController` (classe distincte pour ne pas casser `ApiGatewayControllerTest`), `MtlsHttpClientFactory` + bean `loadBalancedWebClientBuilder` avec `ServerBearerExchangeFilterFunction`.

**✅** `200` avec les données des trois services ; `401` sans jeton ; `403` avec `carol` ; les journaux des services montrent l'appel en HTTPS avec CN `api-gateway`.
**⛔** `vets` vide : la route `vets` n'est pas passée en `https` (voir P6).
📸 `docs/preuves/p07-dossier/` : réponse JSON, journaux des trois services, diagramme de séquence (schéma central du rapport).

---

## P8 — Durcissement minimal (🅰️, 30 min) — ✅ FAIT le 10/11/2026

Seul ce qui renforce l'authentification interservices a été fait (v2 §P8) :

1. **Actuator** : `management.endpoints.web.exposure.include: health,info` + `management.endpoint.health.show-details: never`
   sur les **cinq** applications (gateway, customers, vets, visits, discovery) ; `tracing`/`zipkin` conservé.
2. **Annuaire protégé** : dépendance `spring-boot-starter-security` et classe `EurekaSecurityConfig` dans `discovery-server`
   (CSRF ignoré sur `/eureka/**`, `httpBasic`, `/actuator/health` et `/actuator/info` publics), identifiants
   `EUREKA_USER` / `EUREKA_PASSWORD` (défaut `eureka` / `eureka-pass`) intégrés au `defaultZone` des quatre applications
   et de leurs blocs `docker`.
3. `scripts/dev-start.sh` et `scripts/run_all.sh` exportent ces identifiants : **tous** les processus en héritent.

**Mesures du 10/11/2026** (discovery sur 8761, customers sur 8081) :

| Test | Résultat attendu | Obtenu |
|---|---|---|
| `curl -i http://localhost:8761/eureka/apps` | accès refusé | **401** |
| `curl -u eureka:eureka-pass …/eureka/apps` | lecture autorisée | **200** |
| `curl -u eureka:faux …/eureka/apps` | mot de passe refusé | **401** |
| `curl -i http://localhost:8761/` (console Eureka) | accès refusé | **401** |
| `curl …/actuator/health` | public (scripts) | **200** |
| `curl …/actuator/env` et `/actuator/prometheus` | masqué | **404** |
| Enregistrement de `customers` | toujours enregistré | `registration status: 204`, instance `UP` |
| `./mvnw -pl spring-petclinic-discovery-server test` | vert | 1 test, **0 échec** |

**⛔** Services disparus d'Eureka : `EUREKA_PASSWORD` manquant sur un seul processus, ou CSRF non exclu de `/eureka/**`.
**⛔** Conséquence assumée : Prometheus/Grafana (hors périmètre v2) ne peuvent plus scraper `/actuator/prometheus`.
À écrire dans le rapport : l'authentification basique de l'annuaire reste une protection **faible** (identifiants statiques, sans rotation, sans TLS en local).
📸 `docs/preuves/p08-durcissement/` : les lignes du tableau ci-dessus (à capturer avant la démonstration).

---

## P9 — Tests automatisés et script de contrôle (🅰️ + 🅱️, 1 j) — ⬜

1. `scripts/security-smoke.sh` (v2 §9.1) : vérifie toute la matrice — 401/403/200/403/201 sur le gateway, `dossier` 200/401, `actuator/env` masqué, et les quatre cas mTLS (`000`, 401, 200, 403). Code de sortie non nul si une ligne échoue.
2. Tests JUnit de sécurité, **au moins un par service** (v2 §9.2) : `@SpringBootTest` + `@AutoConfigureMockMvc` (filtres **actifs**), `jwt()` de `spring-security-test`.

**✅** `./mvnw -B test` vert **et** `security-smoke.sh` entièrement `OK`.
**⛔** Ne pas réutiliser les tests métier (`addFilters = false`) pour prouver la sécurité : ils ne testent rien des filtres.
📸 `docs/preuves/p09-tests/` : sortie du script, sortie Maven.

---

## P10 — Rapport (🅰️ + 🅱️, 17 → 22/11) — ⬜

Plan en **9 chapitres**, exactement ceux de l'énoncé (v2 §8.1) : introduction et problématique (Zero Trust, constats P2) ; solutions existantes ; critères de décision et comparaison ; architecture du module ; fonctionnement des composants ; **installation et configuration** ; validation ; avantages et inconvénients ; conclusion.

Les tableaux comparatifs (mTLS, JWT, OAuth2/OIDC, API key, service mesh / RBAC, ABAC, PBAC, ReBAC) et la grille de critères pour l'architecte sont **pré-remplis** en v2 §8.3 : il faut les vérifier, les compléter et les sourcer — pas les recopier tels quels.

**✅** Chaque affirmation de sécurité renvoie à une figure ou une sortie de commande ; figures numérotées et citées ; **aucune capture ne montre un mot de passe, un jeton réel ou une clé**.
**⛔** Le « 30-40 pages » et le « LaTeX » ne figurent pas dans l'énoncé : viser 25-35 pages denses, format libre (LaTeX possible).
Les limites de la v2 §8.4 sont à assumer telles quelles (front sans écran de connexion, Keycloak en mode dev, CA sans révocation, jetons relayés sans contrôle d'audience, Eureka en basic).

---

## P11 — Archive importable dans IntelliJ (🅰️ + 🅱️, 22/11) — ⬜

`git archive --format=zip --prefix=petclinic-secure/ -o petclinic-secure-NOM1-NOM2.zip HEAD` : n'exporte que les fichiers suivis — ni `target/`, ni `secrets/`, ni `.idea/`. Ajouter `LISEZMOI-RENDU.md` et `scripts/run-secure.sh` (v2 §9.2 à §9.4).

**✅ Test à froid obligatoire (≈ 1 h)** : décompresser dans un dossier vide et dérouler le `LISEZMOI-RENDU.md` tel quel.
**⛔** Toute correction faite pendant le test à froid se fait **dans le dépôt**, jamais dans l'archive.
**⛔** `PKI_DIR` doit être un chemin **absolu avec des `/`** sous IntelliJ.

---

## P12 — Présentation (🅰️ + 🅱️, date communiquée par l'encadrant) — ⬜

Démonstration de 10 minutes (v2 §10) : (1) `security-smoke.sh` vert ; (2) `dossier` avec `alice` → 200 ; (3) sans jeton → 401, `carol` → 403, POST de `bob` → 403 ; (4) appel direct à `customers` sans certificat (échec), avec celui du gateway (200), avec `rogue-client` (403) ; (5) architecture et tableau des compromis. Prévoir **captures et vidéo de secours**.

Les questions probables et les réponses attendues sont en v2 §10 — chacun doit savoir répondre sur la partie de l'autre (notamment : pourquoi mTLS **et** JWT, et pourquoi valider le JWT dans chaque service).

---

## Check-list finale (avant dépôt Moodle)

**Exigences officielles**
- [ ] E1 : OIDC/JWT + mTLS + RBAC implémentés **et justifiés** (P4, P5, P6)
- [ ] E2 : chaque service valide le JWT **et** exige un certificat client autorisé (P5, P6)
- [ ] E3 : scénario `dossier` fonctionnel avec les 3 microservices (P7)
- [ ] E4 : avantages/inconvénients de chaque mécanisme **comparés aux existants**, avec critères de décision (P10)
- [ ] E5 : rapport avec solutions existantes, architecture, composants, avantages/inconvénients **et installation/configuration**
- [ ] E6 : archive Java **ouverte et testée à froid** dans IntelliJ (P11)
- [ ] E7 : dépôt Moodle **avant le 24/11 minuit** (visé : le 23 au soir)

**Qualité**
- [ ] `./mvnw -B clean verify` → `BUILD SUCCESS`
- [ ] `security-smoke.sh` → toutes les lignes `OK`
- [ ] `git log -p | grep -iE "password|PRIVATE KEY"` ne renvoie que des mots de passe de démonstration documentés
- [ ] Captures sans secret, preuves rangées dans `docs/preuves/pXX-…/`
- [ ] Keycloak arrêté après la démonstration (`docker compose -f docker-compose.security.yml down`)

---

## Annexe — Dépannage

Le tableau de dépannage complet (24 symptômes, de `JwtDecoder` introuvable à `No subject alternative names matching`) est dans **`GuideV2!!!.md`, section « Annexe — Dépannage rapide »**. S'y référer plutôt qu'à l'ancienne annexe de ce guide, qui mentionnait RDS, ECR et EC2 — désormais hors périmètre.
