# Partie 1 — Cadrage et pipeline (TP03)

> À faire dans l'ordre, de l'étape 1 à l'étape 14. Ouvre Git Bash et lance d'abord :
> `cd /c/Users/aitel/Downloads/PetClinic`

---

## ÉTAPE 1 — Vérifier les outils

**Fais :** lance ces 4 commandes.

```bash
java -version
./mvnw -v
docker version
git --version
```

**✅ Tu dois voir :** une version Java 17 ou plus (21 chez toi), une version Maven 3.9, un client et un serveur Docker, une version Git.

**⛔ Si ça échoue :**
- `java -version` inconnu → le JDK n'est pas dans le PATH : dans Git Bash, lance
  `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"` puis `export PATH="$JAVA_HOME/bin:$PATH"`.
- `docker version` affiche une erreur de démon → ouvre Docker Desktop et attends « Engine running ».

---

## ÉTAPE 2 — Créer le dépôt Git local

**Fais :** dans Git Bash, une commande à la fois.

```bash
cd /c/Users/aitel/Downloads/PetClinic
git init -b main
```

Crée le fichier `.gitignore` à la racine (colle exactement ceci) :

```gitignore
target/
!.mvn/wrapper/maven-wrapper.jar
*.log
.idea/
*.iml
.vscode/
.env
.DS_Store
```

Puis :

```bash
git add -A
git commit -m "chore: reduce PetClinic to three business microservices"
```

**✅ Tu dois voir :** un message `files changed` et un `git status` qui affiche `nothing to commit, working tree clean`.

**⛔ Si `git commit` demande ton identité :**

```bash
git config --local user.name "Ton Nom"
git config --local user.email "ton.email@exemple.com"
```
Puis relance `git commit -m "..."`.

---

## ÉTAPE 3 — Créer le dépôt sur GitHub et pousser

**Où :** navigateur → `https://github.com/new`

**Fais :**
1. Repository name : `petclinic-microservices-security`
2. Visibilité : **Private** (ou Public, mais jamais de secret dedans)
3. **Ne coche rien** (pas de README, pas de .gitignore, pas de licence)
4. Clique **Create repository**

Puis dans Git Bash (remplace `TON_COMPTE` par ton identifiant GitHub) :

```bash
git remote add origin https://github.com/TON_COMPTE/petclinic-microservices-security.git
git push -u origin main
```

**✅ Tu dois voir :** `branch 'main' set up to track 'origin/main'` et les fichiers visibles sur la page GitHub.

**⛔ Si l'authentification est refusée :** GitHub n'accepte plus le mot de passe. Crée un token :
GitHub → ta photo → Settings → Developer settings → Personal access tokens → Tokens (classic) →
Generate new token → coche `repo` → copie le token et utilise-le **comme mot de passe** au moment du `git push`.

---

## ÉTAPE 4 — Créer la branche `dev`

**Fais :**

```bash
git checkout -b dev
git push -u origin dev
git checkout main
```

**✅ Tu dois voir :** deux branches sur GitHub (`main` et `dev`).

---

## ÉTAPE 5 — Protéger `main`

**Où :** GitHub → ton dépôt → **Settings** → **Branches** → **Add branch protection rule**

**Fais :**
1. Branch name pattern : `main`
2. Coche **Require a pull request before merging**
3. Coche **Require status checks to pass before merging**
4. Dans la recherche de checks, tape `build` et sélectionne-le (il apparaîtra après la première exécution du pipeline — reviens à cette étape après l'étape 13 si la liste est vide)
5. Clique **Create**

**✅ Tu dois voir :** une règle listée pour `main`.

---

## ÉTAPE 6 — Créer le compte SonarCloud et le projet

**Où :** navigateur → `https://sonarcloud.io`

**Fais :**
1. **Log in** → **With GitHub** → autorise.
2. En haut à droite : **+** → **Analyze new project**.
3. Autorise SonarCloud à voir tes dépôts si demandé, puis sélectionne `petclinic-microservices-security`.
4. Choisis le plan **Free**.
5. SonarCloud affiche les instructions : **note les deux valeurs** :
   - `sonar.projectKey` (ressemble à `TON_COMPTE_petclinic-microservices-security`)
   - `sonar.organization` (ressemble à `ton-compte`)
6. Clique **Set Up** / **Finish** (peu importe l'écran final, on configure Maven à l'étape 11).

**✅ Tu dois voir :** un projet vide sur SonarCloud avec son `projectKey`.

---

## ÉTAPE 7 — Récupérer le `SONAR_TOKEN`

**Où :** SonarCloud → ta photo en haut à droite → **My Account** → onglet **Security**

**Fais :**
1. Dans « Generate Tokens », nom : `github-actions`
2. Type : **User Token** (ou Project Token si proposé)
3. Clique **Generate**
4. **Copie le token tout de suite** (il ne sera plus jamais affiché) et colle-le dans un brouillon local.

**✅ Tu dois voir :** une chaîne de 40 caractères environ, commençant souvent par `sqp_`.

---

## ÉTAPE 8 — Ajouter les secrets GitHub

**Où :** GitHub → ton dépôt → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

**Fais :** crée ce secret maintenant (les autres viendront à l'étape 29) :

| Name | Secret |
|---|---|
| `SONAR_TOKEN` | le token copié à l'étape 7 |

**✅ Tu dois voir :** `SONAR_TOKEN` dans la liste, avec une date de mise à jour.

---

## ÉTAPE 9 — Ajouter JaCoCo au pom parent

**Où :** fichier `pom.xml` à la racine du projet.

**Fais :** ouvre `pom.xml`. Cherche la ligne `<profiles>` (vers le milieu du fichier).
**Juste après** cette ligne, colle le bloc ci-dessous (c'est un nouveau profil `quality`, il ne gêne pas les builds normaux) :

```xml
        <profile>
            <id>quality</id>
            <properties>
                <sonar.projectKey>REMPLACE_PAR_TON_PROJECT_KEY</sonar.projectKey>
                <sonar.organization>REMPLACE_PAR_TON_ORGANIZATION</sonar.organization>
                <sonar.host.url>https://sonarcloud.io</sonar.host.url>
                <sonar.coverage.jacoco.xmlReportPaths>**/target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
            </properties>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.jacoco</groupId>
                        <artifactId>jacoco-maven-plugin</artifactId>
                        <version>0.8.12</version>
                        <executions>
                            <execution>
                                <id>prepare-agent</id>
                                <goals>
                                    <goal>prepare-agent</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>report</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>report</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>check</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>check</goal>
                                </goals>
                                <configuration>
                                    <rules>
                                        <rule>
                                            <element>BUNDLE</element>
                                            <limits>
                                                <limit>
                                                    <counter>LINE</counter>
                                                    <value>COVEREDRATIO</value>
                                                    <minimum>0.40</minimum>
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

**Remplace** `REMPLACE_PAR_TON_PROJECT_KEY` et `REMPLACE_PAR_TON_ORGANIZATION` par les valeurs de l'étape 6.

**Commande de vérification :**

```bash
./mvnw -B -P quality -DskipTests package
```

**✅ Tu dois voir :** `BUILD SUCCESS`.

**⛔ Si Maven affiche `Could not resolve jacoco-maven-plugin:0.8.12` :** ta connexion est lente, relance la commande ; le plugin se télécharge une seule fois.

---

## ÉTAPE 10 — Mesurer la couverture

**Fais :**

```bash
./mvnw -B -P quality verify
```

**✅ Tu dois voir :** `BUILD SUCCESS` et, dans chaque module, un fichier
`spring-petclinic-<module>/target/site/jacoco/index.html`.

Ouvre-le : navigateur → `file:///C:/Users/aitel/Downloads/PetClinic/spring-petclinic-customers-service/target/site/jacoco/index.html`.
Note le pourcentage de lignes couvertes : **c'est ta mesure de départ**, à mettre dans le rapport.

**⛔ Si le build échoue avec `Rule violated for bundle ... lines covered ratio is 0.x, but expected minimum is 0.40` :**
la couverture d'un module est sous 40 %. Deux solutions :
- écrire des tests dans ce module (voir la partie « compléter les tests » plus bas) ;
- ou baisser temporairement le seuil : dans le bloc `check`, remplace `0.40` par `0.00`, et remonte-le dès que les tests sont écrits.
  En aucun cas laisser `0.00` dans le rapport final.

**Compléter les tests (si un module est sous le seuil) :** ajoute un fichier
`src/test/java/.../<Nom>ResourceTest.java` dans le module concerné, en copiant le modèle de
`spring-petclinic-vets-service/src/test/java/org/springframework/samples/petclinic/vets/web/VetResourceTest.java`
(il fonctionne : 2 tests passent). Adapte seulement le nom de la ressource et du repository.

---

## ÉTAPE 11 — Ajouter Sonar au profil `quality`

**Où :** toujours `pom.xml`, dans le profil `quality` que tu viens de créer.

**Fais :** dans le bloc `<plugins>` du profil, **juste après** le `</plugin>` de JaCoCo, colle :

```xml
                    <plugin>
                        <groupId>org.sonarsource.scanner.maven</groupId>
                        <artifactId>sonar-maven-plugin</artifactId>
                        <version>5.0.0.4389</version>
                    </plugin>
```

**Commande (avec ton token, jamais dans le dépôt) :**

```bash
./mvnw -B -P quality verify sonar:sonar -Dsonar.token=COLLE_TON_TOKEN_ICI
```

**✅ Tu dois voir :** `BUILD SUCCESS` puis `ANALYSIS SUCCESSFUL` et une URL
`https://sonarcloud.io/dashboard?id=...`. Ouvre-la : tu dois voir la couverture et le Quality Gate.

**⛔ Si Maven dit que la version du plugin n'existe pas :** supprime la ligne
`<version>5.0.0.4389</version>` et relance.

**⛔ Si Sonar répond 401 :** le token est faux ou expiré → refais l'étape 7.

---

## ÉTAPE 12 — Créer le workflow GitHub Actions

**Où :** crée le fichier `.github/workflows/deploy.yml` (le dossier `.github/workflows` existe déjà).

**Colle exactement ceci :**

```yaml
name: CI

on:
  push:
    branches: [ main, dev ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        java: [ '17', '21' ]

    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: temurin
          cache: maven

      - name: Rendre le wrapper exécutable
        run: chmod +x mvnw

      - name: Build et tests
        run: ./mvnw -B -P quality clean verify

      - name: Analyse SonarCloud
        if: matrix.java == '17'
        run: ./mvnw -B -P quality sonar:sonar -Dsonar.token=${{ secrets.SONAR_TOKEN }}

      - name: Publier les rapports JaCoCo
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-java-${{ matrix.java }}
          path: '**/target/site/jacoco/'
```

**Fais :** commit et push.

```bash
git checkout dev
git add -A
git commit -m "ci: add quality profile, JaCoCo, Sonar and GitHub Actions workflow"
git push -u origin dev
```

**✅ Tu dois voir :** sur GitHub → onglet **Actions**, un run `CI` démarré.

**⛔ S'il échoue sur `./mvnw: Permission denied` :** l'étape `chmod +x mvnw` est déjà dans le YAML,
vérifie qu'elle est bien avant le build.

**⛔ S'il échoue sur Sonar :** le secret `SONAR_TOKEN` est absent ou faux (étape 8).

---

## ÉTAPE 13 — Faire une pull request et voir le pipeline

**Où :** GitHub → onglet **Pull requests** → **New pull request**

**Fais :**
1. base : `main` ← compare : `dev`
2. Titre : `feat: quality pipeline`
3. **Create pull request**
4. Attends la fin des checks (2 jobs : Java 17 et Java 21)
5. **Merge pull request** → **Confirm merge**

Puis, **retourne à l'étape 5** et ajoute le check `build` dans la protection de branche (il est maintenant visible).

**✅ Tu dois voir :** une PR verte puis fusionnée, et un run `CI` vert également sur `main`.

**Preuve à garder pour le rapport :** capture d'écran de la PR avec les checks verts.

---

## ÉTAPE 14 — SCA et scan de vulnérabilités

**Fais :** crée `.github/dependabot.yml` :

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
```

Puis ajoute à la fin du job `build` dans `.github/workflows/deploy.yml` :

```yaml
      - name: Scan des dépendances et de l'image (Trivy)
        if: matrix.java == '17'
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: fs
          scan-ref: .
          severity: CRITICAL,HIGH
          exit-code: '0'
          format: table
```

(`exit-code: '0'` = le pipeline ne casse pas ; le tableau reste lisible dans les logs.)

```bash
git checkout -b feature/sca
git add -A
git commit -m "ci: add Dependabot and Trivy scan"
git push -u origin feature/sca
```

Puis sur GitHub : **Compare & pull request** → base `dev` → merge.

**✅ Tu dois voir :** dans les logs du job, un tableau Trivy avec les CVE détectées (ou « 0 » si tout est à jour).

**Preuve pour le rapport :** capture du tableau Trivy + capture de l'onglet Security → Dependabot.
