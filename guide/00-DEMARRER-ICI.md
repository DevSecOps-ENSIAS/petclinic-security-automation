# Guide complet A→Z — PetClinic microservices sécurisé (DevSecOps)

Ce guide suit **les TP dans l'ordre** (TP1 OpenSSL → TP2 pipeline → TP3 HTTPS + déploiement Docker/AWS → TP4 RDS → TP5 Sonar) et les applique au projet **PetClinic** (customers 8081, visits 8082, vets 8083, gateway 8080, Eureka 8761).

> Chaque tâche indique **qui** la fait (🅰️ Membre A ou 🅱️ Membre B), la **commande**, le **résultat attendu** ✅ et la **preuve** 📸 à garder pour le rapport.

---

## 0. Répartition des rôles

| | 🅰️ Membre A — « Pipeline & Cloud » | 🅱️ Membre B — « Sécurité & Données » |
|---|---|---|
| TP | TP2, TP3 suite (Docker/AWS), TP5 | TP1 (OpenSSL), TP3 (HTTPS), TP4 (RDS) |
| Responsable de | GitHub, Actions, JaCoCo, Sonar, Trivy, ECR, EC2, Docker | CA/certificats, TLS, JWT/Keycloak, RDS, profil `prod`, durcissement, ZAP |
| Dépôt | Propriétaire du dépôt, protège `main` | Collaborateur (invité), travaille sur ses branches |
| Rapport | Chapitres CI/CD, qualité, déploiement | Chapitres crypto, TLS, authentification, base de données, menaces |

**Chapitres communs** : modèle de menace (§ 3), tableau avant/après, observabilité, relecture croisée, soutenance.

### Règles de collaboration
1. Branches : `main` (protégée) ← `dev` ← `feature/<prenom>-<sujet>`. **Jamais de push direct sur `main`.**
2. Chaque PR est **relue par l'autre membre** avant merge (preuve de revue).
3. Commits : `feat:`, `fix:`, `ci:`, `sec:`, `docs:`.
4. **Aucun secret dans le dépôt** (mots de passe, tokens, clés privées `.pem`/`.p12`). Tout va dans GitHub → Settings → Secrets.
5. Toujours `./mvnw`, jamais `mvn` seul. Après chaque modification : `./mvnw -B -DskipTests package` → `BUILD SUCCESS`.
6. Point de synchronisation de 10 min à chaque fin de phase.

---

## Planning

| Phase | Contenu | TP | Qui | Dépend de |
|---|---|---|---|---|
| 1 | Outils + dépôt Git | TP2 | A (B clone) | — |
| 2 | Modèle de menace | — | A + B | 1 |
| 3 | Pipeline CI : Actions + JaCoCo | TP2 | A | 1 |
| 4 | Crypto OpenSSL : CA + certificats | TP1 | B | 1 |
| 5 | Sonar + Trivy + Dependabot | TP5 | A | 3 |
| 6 | HTTPS sur les services | TP3 | B | 4 |
| 7 | Authentification JWT (Keycloak) | — | B | 6 |
| 8 | RDS sécurisé + profil `prod` | TP4 | B (A pour secrets) | 1 |
| 9 | Docker + ECR + EC2 (Deploy Job) | TP3 suite | A | 3, 8 |
| 10 | Durcissement + ZAP | — | B | 6, 7 |
| 11 | Avant/après + rapport + soutenance | — | A + B | tout |

A et B peuvent travailler **en parallèle** : phases 3→5 (A) et 4→6 (B).

---

## PHASE 1 — Mise en route (TP2 étapes 1-3)

### 1.1 Outils — 🅰️ et 🅱️ chacun sur sa machine
```bash
java -version      # 17 ou 21
./mvnw -v
docker version
git --version
```
✅ Les 4 commandes répondent. Sous Windows, utiliser **Git Bash**.

### 1.2 Dépôt local — 🅰️
```bash
cd /c/Users/aitel/Downloads/PetClinic
git init -b main
```
`.gitignore` (à la racine) :
```gitignore
target/
!.mvn/wrapper/maven-wrapper.jar
*.log
.idea/
*.iml
.vscode/
.env
.DS_Store
# secrets et crypto : JAMAIS dans Git
*.pem
*.key
*.p12
*.jks
.keystore
*.srl
secrets/
```
```bash
git add -A
git commit -m "chore: initial PetClinic microservices"
```

### 1.3 Dépôt GitHub — 🅰️
1. https://github.com/new → `petclinic-microservices-security`, **sans** README/.gitignore/licence.
2. ```bash
   git remote add origin https://github.com/COMPTE_A/petclinic-microservices-security.git
   git push -u origin main
   ```
3. GitHub → Settings → **Collaborators** → inviter 🅱️ (qui accepte l'invitation).

### 1.4 Branches (comme le TP2 avec `feature/dev`) — 🅰️
```bash
git checkout -b dev && git push -u origin dev
git checkout -b feature/ci && git push -u origin feature/ci
```
✅ Les branches `main`, `dev`, `feature/ci` sont visibles sur GitHub.

### 1.5 🅱️ clone et configure son identité
```bash
git clone https://github.com/COMPTE_A/petclinic-microservices-security.git
cd petclinic-microservices-security
git config --local user.name "Prénom B"
git config --local user.email "b@exemple.com"
git checkout dev
```

---

## PHASE 2 — Modèle de menace — 🅰️ + 🅱️ (1 h, ensemble)

Tableau à remplir dans `docs/modele-de-menace.md` (méthode STRIDE simplifiée) :

| Actif | Menace (STRIDE) | Exemple concret | Mesure | Phase |
|---|---|---|---|---|
| Gateway → services | Interception (I) | Trafic en HTTP clair | TLS | 6 |
| API d'écriture | Usurpation (S) | POST sans identité | JWT | 7 |
| Base RDS | Divulgation (I) | Port 3306 ouvert à tous, pas de chiffrement | SG restreint + chiffrement | 8 |
| Secrets | Divulgation (I) | Token Sonar collé dans un énoncé/dépôt | GitHub Secrets, révocation | 3 |
| Dépendances | Élévation (E) | CVE dans une librairie | Trivy, Dependabot | 5 |
| Images Docker | Élévation (E) | Conteneur root | utilisateur non-root | 9-10 |
| Eureka/Actuator | Divulgation (I) | Endpoints exposés | auth + limitation | 10 |

📸 Preuve : le fichier `docs/modele-de-menace.md` committé.

---

## PHASE 3 — Pipeline CI : GitHub Actions + JaCoCo (TP2 étapes 3-4) — 🅰️

### 3.1 Secret Sonar — voir Phase 5. Pour l'instant, workflow sans Sonar.

### 3.2 Profil Maven `quality` (JaCoCo) — 🅰️
Dans `pom.xml` racine, juste après `<profiles>` :
```xml
<profile>
  <id>quality</id>
  <properties>
    <sonar.projectKey>REMPLACER_PROJECT_KEY</sonar.projectKey>
    <sonar.organization>REMPLACER_ORGANISATION</sonar.organization>
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
          <execution><id>prepare-agent</id><goals><goal>prepare-agent</goal></goals></execution>
          <execution><id>report</id><phase>verify</phase><goals><goal>report</goal></goals></execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</profile>
```
> Le TP utilise JaCoCo 0.8.15 pour supporter Java 25. Avec JDK 17/21, 0.8.12 convient ; si ton prof veut la version du TP, mets `0.8.15`.

```bash
./mvnw -B -P quality clean verify
```
✅ `BUILD SUCCESS` et `spring-petclinic-*/target/site/jacoco/index.html`.
📸 Capture du rapport JaCoCo (pourcentage de couverture = mesure de départ).

### 3.3 Workflow `.github/workflows/deploy.yml` — 🅰️ (version CI, le Deploy Job viendra en Phase 9)
```yaml
name: CI

on:
  push:
    branches: [ main, dev ]
  pull_request:
    branches: [ main ]

permissions:
  contents: read

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

      - name: Build et tests
        run: ./mvnw -B -P quality clean verify

      - name: Publier les rapports JaCoCo
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-java-${{ matrix.java }}
          path: '**/target/site/jacoco/'
```

### 3.4 Branche → PR → merge (TP2 étape 8-9) — 🅰️ puis 🅱️ relit
```bash
git checkout feature/ci
git add -A
git commit -m "ci: add quality profile and GitHub Actions workflow"
git push
```
GitHub → **Compare & pull request** (`feature/ci` → `dev`), 🅱️ approuve, merge. Puis PR `dev` → `main`.
✅ Onglet **Actions** : run `CI` vert (2 jobs : Java 17 et 21).
📸 Capture de la PR avec checks verts + du run Actions.

### 3.5 Protéger `main` — 🅰️
Settings → Branches → Add rule : `main` ; coche *Require a pull request* (1 approbation) et *Require status checks* (`build`).

---

## PHASE 4 — Cryptographie avec OpenSSL (TP1) — 🅱️

Travailler dans un dossier **hors dépôt** (ex. `~/pki-petclinic`), car il contient des clés privées.
```bash
mkdir -p ~/pki-petclinic/baseCle && cd ~/pki-petclinic
```

### Atelier 1 — Clés RSA, chiffrement
```bash
# Clé privée (2048 bits ; le TP utilise 1024, trop faible aujourd'hui, à justifier dans le rapport)
openssl genrsa -out ./baseCle/laClePv.pem 2048
# Clé privée protégée par passphrase (TP : -des3)
openssl genrsa -aes256 -out ./baseCle/laClePvCrypte.pem 2048
# Clé publique
openssl rsa -in ./baseCle/laClePv.pem -pubout -out ./baseCle/laClePu.pem
# Chiffrer / déchiffrer un petit fichier
echo "message secret" > data.txt
openssl pkeyutl -encrypt -in data.txt -inkey ./baseCle/laClePu.pem -pubin -out dataChiffre.txt
openssl pkeyutl -decrypt -in dataChiffre.txt -inkey ./baseCle/laClePv.pem -out dataClair.txt
cat dataClair.txt
```
✅ `dataClair.txt` = `message secret`.

### Atelier 2 — Empreinte et signature
```bash
openssl dgst -sha256 -out dataEmpreinte.txt data.txt
openssl dgst -sha256 -sign ./baseCle/laClePv.pem -out signature.sig data.txt
openssl dgst -sha256 -verify ./baseCle/laClePu.pem -signature signature.sig data.txt   # Verified OK
cp data.txt data2.txt && echo "x" >> data2.txt
openssl dgst -sha256 -verify ./baseCle/laClePu.pem -signature signature.sig data2.txt  # Verification Failure
```
✅ `Verified OK` puis `Verification Failure` sur le fichier modifié.

### Atelier 3 — Autorité de certification (CA) et certificat serveur
```bash
# 1. CA auto-signée, 1 an (TP : req -x509 -days 365 -new -key ... -out Cert_Autorite.pem)
openssl genrsa -out maCle.pem 2048
openssl req -x509 -days 365 -new -key maCle.pem -out Cert_Autorite.pem \
  -subj "/C=MA/ST=Maroc/L=Rabat/O=ensias/OU=info/CN=adminENSIASRabat/emailAddress=adminRabat@ensias.ma"
openssl x509 -in Cert_Autorite.pem -noout -text | head -20
```
Pour chaque service (`customers`, `vets`, `visits`) on génère une clé, une CSR et on la signe. Le **SAN** est obligatoire pour que Java/curl acceptent le certificat :

```bash
for svc in customers vets visits; do
  openssl genrsa -out $svc.key 2048
  openssl req -new -key $svc.key -out $svc.csr \
    -subj "/C=MA/ST=Maroc/L=Casa/O=ensias/OU=Compta/CN=$svc-service"
  printf "subjectAltName=DNS:localhost,DNS:$svc-service,IP:127.0.0.1\n" > $svc.ext
  openssl x509 -req -in $svc.csr -CA Cert_Autorite.pem -CAkey maCle.pem \
    -CAcreateserial -CAserial ca.srl -days 365 -sha256 -extfile $svc.ext -out $svc.crt
  # Conversion en PKCS12 pour Spring Boot
  openssl pkcs12 -export -in $svc.crt -inkey $svc.key -certfile Cert_Autorite.pem \
    -name $svc -out $svc.p12 -passout pass:CHANGE_MOI
done
# Truststore contenant la CA (pour le gateway)
keytool -importcert -alias petclinic-ca -file Cert_Autorite.pem \
  -keystore truststore.p12 -storetype PKCS12 -storepass CHANGE_MOI -noprompt
```
Remplace `CHANGE_MOI` par un vrai mot de passe **non commité**.

✅ Vérification :
```bash
openssl verify -CAfile Cert_Autorite.pem customers.crt   # customers.crt: OK
```
📸 Captures : contenu du certificat de la CA, `Verified OK`/`Failure`, `customers.crt: OK`.
📝 Rapport : expliquer pourquoi 1024 bits est insuffisant et pourquoi on utilise 2048.

### Distribution des fichiers (par canal sûr, pas par Git)
Copier `*.p12` et `truststore.p12` dans `secrets/` (ignoré par Git) sur la machine de 🅰️ si il doit lancer les services, **par messagerie chiffrée ou clé USB**, jamais par le dépôt.

---

## PHASE 5 — Qualité et sécurité du code : Sonar, Trivy, Dependabot (TP5) — 🅰️

### 5.1 SonarCloud (TP5 étapes 1-3)
1. https://sonarcloud.io/login → **With GitHub**.
2. `+` → **Analyze new project** → sélectionner `petclinic-microservices-security` → **Set Up**.
3. Project Information (menu gauche) : noter **Project Key** et **Organization Key**.
4. Administration → Analysis Method : **désactiver « Automatic Analysis »** (sinon conflit avec l'analyse CI).

### 5.2 Renseigner le pom (TP5 étapes 5-6)
Remplacer dans le profil `quality` :
```xml
<sonar.projectKey>VOTRE_PROJECT_KEY</sonar.projectKey>
<sonar.organization>VOTRE_ORG</sonar.organization>
```
Ajouter dans `<plugins>` du profil `quality` :
```xml
<plugin>
  <groupId>org.sonarsource.scanner.maven</groupId>
  <artifactId>sonar-maven-plugin</artifactId>
  <version>5.0.0.4389</version>
</plugin>
```
(le TP indique `3.10.0.2594` ; si la version choisie pose problème, utiliser celle du TP.)

### 5.3 Token et secret (TP5 étapes 8-10)
1. SonarCloud → avatar → **My Account** → **Security / Access Tokens** → nom `petclinic` → **Generate Token**. **Copier immédiatement.**
2. GitHub → dépôt → Settings → Secrets and variables → **Actions** → **New repository secret** : `SONAR_TOKEN`.
3. ⚠️ Le token ne va **jamais** dans un fichier, une capture publique ou un énoncé. Si un token a été exposé (comme dans l'énoncé du TP), le **révoquer** et en créer un nouveau.

### 5.4 Ajouter l'analyse au workflow (TP5 étape 7)
Dans le job `build`, après « Build et tests » :
```yaml
      - name: Analyse SonarCloud
        if: matrix.java == '21'
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: ./mvnw -B -P quality sonar:sonar -Dsonar.token=$SONAR_TOKEN
```
> Le token est passé par variable d'environnement (pas interpolé dans la commande), ce qui évite sa fuite dans les logs. Sonar récent demande Java 17+ ; on l'exécute sur la ligne 21 de la matrice.

### 5.5 Trivy + Dependabot — SCA
`.github/dependabot.yml` :
```yaml
version: 2
updates:
  - package-ecosystem: maven
    directory: "/"
    schedule: { interval: weekly }
  - package-ecosystem: github-actions
    directory: "/"
    schedule: { interval: weekly }
```
Étape Trivy à la fin du job `build` (version **épinglée**, pas `@master`) :
```yaml
      - name: Scan Trivy (dépendances)
        if: matrix.java == '21'
        uses: aquasecurity/trivy-action@0.28.0
        with:
          scan-type: fs
          scan-ref: .
          severity: CRITICAL,HIGH
          exit-code: '0'
          format: table
```
Vérifier sur GitHub le tag exact le plus récent de `trivy-action` avant d'utiliser.

### 5.6 Lancer et interpréter (TP5 étape 11-12)
```bash
git checkout -b feature/sonar
git add -A && git commit -m "ci: add Sonar, Trivy and Dependabot"
git push -u origin feature/sonar
```
PR → `dev` (🅱️ relit) → merge → PR `dev` → `main`.

✅ Run vert ; SonarCloud affiche Quality Gate et Coverage.
📝 **Interprétation à rédiger** (comme le TP) : pour Security / Reliability / Maintainability / Coverage / Duplications / Hotspots / Dependency risks, noter la valeur, la note (A-E), et l'action décidée.
📸 Capture du dashboard Sonar, du tableau Trivy, de l'onglet Security → Dependabot.

---

## PHASE 6 — HTTPS (TP3 : keytool/Tomcat → Spring Boot) — 🅱️

Le TP3 configure un connecteur SSL 8443 sur Tomcat avec un keystore. Ici, Spring Boot embarque Tomcat : on obtient **le même résultat** via `application.yml`. 🅱️ peut en plus faire d'abord la **démonstration du TP à la lettre** (keystore auto-signé) pour la capture :
```bash
keytool -genkey -alias tomcat -keyalg RSA -keystore .keystore -keysize 2048
```
(puis le connecteur 8443 comme dans le TP, à des fins de capture « site non fiable » et d'explication de l'avertissement navigateur : certificat auto-signé = pas de CA reconnue.)

### 6.1 Stocker les keystores — 🅱️
Copier `customers.p12` etc. dans `spring-petclinic-<svc>-service/` **localement** (ignoré par Git via `*.p12`), ou dans `secrets/`.

### 6.2 `application.yml` de chaque service — 🅱️
Exemple `customers` (8443 ; vets 8444 ; visits 8445) :
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: file:${KEYSTORE_PATH:secrets/customers.p12}
    key-store-type: PKCS12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-alias: customers
```
Le mot de passe vient d'une **variable d'environnement** :
```bash
export KEYSTORE_PASSWORD='...'
```

### 6.3 Le gateway fait confiance à la CA — 🅱️
Dans le gateway :
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        ssl:
          trustedX509Certificates: [ "file:secrets/Cert_Autorite.pem" ]
```
et les routes passent en `https://customers-service:8443` (ou `lb://` avec Eureka configuré en HTTPS, selon l'architecture). Ajouter dans Eureka le métadata `securePort` si besoin.
> Si cette configuration résiste, la solution de repli : `-Djavax.net.ssl.trustStore=secrets/truststore.p12 -Djavax.net.ssl.trustStorePassword=...` au lancement du gateway.

### 6.4 Tests — 🅱️
```bash
./mvnw -B -DskipTests package
KEYSTORE_PASSWORD=... java -jar spring-petclinic-customers-service/target/*.jar &
curl --cacert secrets/Cert_Autorite.pem https://localhost:8443/actuator/health   # OK
curl http://localhost:8443/                                                        # échec attendu
curl -k https://localhost:8443/...                                                 # -k = à éviter, uniquement démonstration
```
✅ `--cacert` fonctionne ; sans CA, `curl` signale un certificat non reconnu.
📸 Capture du navigateur sur HTTPS (avertissement avant/après import de la CA), sortie `curl`.

### 6.5 (Option avancée) mTLS
`server.ssl.client-auth: need` + `trust-store` : seuls les clients (gateway) avec un certificat signé par la CA sont acceptés. À faire seulement si le temps le permet.

---

## PHASE 7 — Authentification JWT (Keycloak) — 🅱️

1. Lancer Keycloak (port 8180) :
   ```bash
   docker run -d --name keycloak -p 8180:8080 \
     -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=<mdp-local> \
     quay.io/keycloak/keycloak:26.0 start-dev
   ```
2. Console http://localhost:8180 : créer le realm `petclinic`, un client `gateway`, un rôle `staff`, un utilisateur de test.
3. Obtenir un jeton :
   ```bash
   curl -s -X POST http://localhost:8180/realms/petclinic/protocol/openid-connect/token \
     -d grant_type=password -d client_id=gateway -d username=test -d password=<mdp>
   ```
4. Gateway : dépendance `spring-boot-starter-oauth2-resource-server`, `issuer-uri: http://localhost:8180/realms/petclinic`, et une `SecurityWebFilterChain` qui exige un JWT valide sur `POST/PUT/DELETE` et laisse les `GET` publics.
5. Tests :
   ```bash
   curl -i -X POST http://localhost:8080/api/customer/owners ...            # 401
   curl -i -X POST -H "Authorization: Bearer $TOKEN" ...                    # 201
   ```
✅ 401 sans jeton, 201 avec jeton.
📸 Les deux réponses.

---

## PHASE 8 — Base de données RDS (TP4 + TP5 étape 6) — 🅱️ (🅰️ pour les secrets GitHub)

### 8.1 Création (TP4 étapes 1-6) — 🅱️
AWS → **RDS** → Databases → **Create database** → *Full configuration* :
- Engine **MySQL**, Template **Free tier**
- Identifier `petclinic-db`, master user `admin`, mot de passe fort
- VPC security group : **create new** `sg_petclinicdb`
- Initial database name : `petclinic` (le TP créait `employedb`)
- Public access : **Yes** *uniquement pour la phase de test du TP* (voir 8.2)

### 8.2 Version « TP » puis version « durcie »
| Réglage | Version TP | Version durcie (à retenir pour le rapport) |
|---|---|---|
| Inbound 3306 | `0.0.0.0/0` (AnyWhere-IPv4) | Votre IP uniquement (`/32`), puis uniquement le SG de l'EC2 |
| Chiffrement | décoché | **activé** |
| Sauvegardes | décochées | activées (rétention 1 à 7 jours) |
| Public access | Yes | No une fois le déploiement sur EC2 en place |
| Compte | `admin` | un utilisateur applicatif à droits limités |

Éditer la règle : RDS → Security → Security group → **Edit inbound rules** → Type MySQL/Aurora, Source **My IP**.
> ⚠️ Les runners GitHub ont des IP variables : **ne pas** ouvrir RDS au monde pour les tests CI. Les tests CI utilisent une base H2/Testcontainers, pas RDS (voir 8.5).

### 8.3 Trois schémas — 🅱️
Se connecter (MySQL Workbench ou `mysql -h <endpoint> -u admin -p`) :
```sql
CREATE DATABASE petclinic_customers;
CREATE DATABASE petclinic_vets;
CREATE DATABASE petclinic_visits;
CREATE USER 'petclinic_app'@'%' IDENTIFIED BY '<mdp-fort>';
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX ON petclinic_customers.* TO 'petclinic_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX ON petclinic_vets.* TO 'petclinic_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX ON petclinic_visits.* TO 'petclinic_app'@'%';
```
📸 Capture de la connexion Workbench réussie.

### 8.4 Profil `prod` (TP5 étape 6 : `application-prod.properties`) — 🅱️
Dans **chaque** service, `src/main/resources/application-prod.yml` :
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate      # le TP met "update" ; "validate" est plus sûr en prod
    show-sql: false
```
Et dans `application.yml` **ne pas** forcer `spring.profiles.active=prod` (le TP le fait, mais cela casse les tests/dev). On l'active à l'exécution : `SPRING_PROFILES_ACTIVE=prod`.
Dépendance driver MySQL (`mysql-connector-j`) dans chaque service.

### 8.5 Secrets GitHub (TP5 étape 6) — 🅰️
Settings → Secrets → Actions, créer :

| Nom | Valeur |
|---|---|
| `DB_URL_CUSTOMERS` | `jdbc:mysql://<endpoint>:3306/petclinic_customers` |
| `DB_URL_VETS` | `jdbc:mysql://<endpoint>:3306/petclinic_vets` |
| `DB_URL_VISITS` | `jdbc:mysql://<endpoint>:3306/petclinic_visits` |
| `DB_USERNAME` | `petclinic_app` |
| `DB_PASSWORD` | mot de passe fort |

🅱️ transmet les valeurs à 🅰️ par canal sûr, **jamais** dans le dépôt ni dans une issue.

### 8.6 Test de persistance — 🅱️
```bash
SPRING_PROFILES_ACTIVE=prod SPRING_DATASOURCE_URL=... SPRING_DATASOURCE_USERNAME=... SPRING_DATASOURCE_PASSWORD=... \
  java -jar spring-petclinic-customers-service/target/*.jar
```
Créer un propriétaire, **redémarrer** le service, vérifier qu'il est toujours là.
✅ Données persistantes.
📸 Tables visibles dans Workbench.

---

## PHASE 9 — Docker, ECR et EC2 : le Deploy Job (TP3 suite) — 🅰️

Le schéma du TP : *Configure AWS credentials → Login to Amazon ECR → Build, Tag & Push Docker image → Deploy to EC2.*

### 9.1 Dockerfile durci (un par service) — 🅰️
`spring-petclinic-customers-service/Dockerfile` :
```dockerfile
FROM eclipse-temurin:21-jre
RUN addgroup --system app && adduser --system --ingroup app app
WORKDIR /app
COPY target/*.jar app.jar
USER app
EXPOSE 8443
ENTRYPOINT ["java","-jar","/app/app.jar"]
```
Test local :
```bash
./mvnw -B -DskipTests package
docker build -t customers:test spring-petclinic-customers-service
```
✅ Image construite ; `docker run --rm customers:test id` indique un utilisateur non-root.

### 9.2 AWS : ECR, IAM, EC2 — 🅰️
1. **ECR** → Create repository : `petclinic-customers`, `petclinic-vets`, `petclinic-visits`, `petclinic-gateway`, `petclinic-discovery` (privés, *scan on push* activé).
2. **IAM** → utilisateur `github-deployer` avec une politique **minimale** : droits ECR (push/pull sur ces 5 repos). Créer une access key.
3. **EC2** : Ubuntu `t2.micro`/`t3.micro`, installer Docker (`sudo apt install docker.io docker-compose-v2`), Security Group : SSH (22) **depuis votre IP seulement**, 8080 selon besoin.
4. Paire de clés SSH : garder le `.pem` hors dépôt.

### 9.3 Secrets GitHub pour le déploiement — 🅰️
`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` (contenu du `.pem`).
> Alternative plus sûre à citer dans le rapport : OIDC GitHub→AWS (aucune clé longue durée). Le TP utilise des clés ; on fait comme le TP, puis on mentionne l'amélioration.

### 9.4 Deploy Job — 🅰️
À ajouter dans `.github/workflows/deploy.yml`, **après** le job `build` :
```yaml
  deploy:
    needs: build
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service:
          - spring-petclinic-customers-service
          - spring-petclinic-vets-service
          - spring-petclinic-visits-service
          - spring-petclinic-api-gateway
          - spring-petclinic-discovery-server
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: temurin
          cache: maven

      - name: Build du jar
        run: |
          chmod +x mvnw
          ./mvnw -B -DskipTests -pl ${{ matrix.service }} -am package

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ secrets.AWS_REGION }}

      - name: Login to Amazon ECR
        id: ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build, tag & push image
        env:
          REGISTRY: ${{ steps.ecr.outputs.registry }}
          REPO: ${{ matrix.service }}
        run: |
          IMAGE=$REGISTRY/$REPO
          docker build -t $IMAGE:${{ github.sha }} -t $IMAGE:latest $REPO
          docker push $IMAGE:${{ github.sha }}
          docker push $IMAGE:latest

  deploy-ec2:
    needs: deploy
    runs-on: ubuntu-latest
    steps:
      - name: Déploiement sur EC2
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd ~/petclinic
            aws ecr get-login-password --region ${{ secrets.AWS_REGION }} | docker login --username AWS --password-stdin ${{ secrets.ECR_REGISTRY }}
            docker compose pull
            docker compose up -d
```
Notes :
- Les noms de dossiers de modules (`spring-petclinic-*`) doivent correspondre exactement à ceux du dépôt ; les repos ECR doivent porter le même nom, ou ajuster la variable `REPO`.
- Ajouter le secret `ECR_REGISTRY` (`<compte>.dkr.ecr.<région>.amazonaws.com`) et installer AWS CLI sur l'EC2 avec un rôle d'instance ayant `ecr:Get*`.
- Un `docker-compose.yml` doit exister sur l'EC2 dans `~/petclinic`, avec les images ECR, les variables d'environnement (`SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_*`, `KEYSTORE_PASSWORD`) lues depuis un fichier `.env` **créé à la main sur l'EC2**, pas dans Git.
- Ce job est le plus sensible aux détails de l'environnement ; prévoir plusieurs essais et lire les logs de l'onglet Actions.

✅ Le run `deploy` est vert, les 5 images sont dans ECR, `curl http://<EC2_HOST>:8080/` répond.
📸 Capture du workflow avec les deux jobs (Build Job / Deploy Job), de la liste des images ECR, de l'application sur l'IP EC2.

---

## PHASE 10 — Durcissement et scan dynamique — 🅱️ (🅰️ pour Docker)

| Action | Comment | Preuve |
|---|---|---|
| Protéger Eureka | Spring Security basic auth, identifiants en variables d'environnement | `curl` sans identifiants → 401 |
| Réduire Actuator | `management.endpoints.web.exposure.include=health,info` ; `management.endpoint.health.show-details=never` | `/actuator/env` → 404 |
| Headers de sécurité | HSTS, X-Content-Type-Options, X-Frame-Options sur le gateway | `curl -I` |
| Conteneurs non-root | fait en 9.1 | `docker run ... id` |
| Scan image | Trivy `image` sur les images construites | tableau Trivy |
| **OWASP ZAP** | `docker run -t ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t http://host.docker.internal:8080` | rapport HTML |

📝 Rapport : pour chaque alerte ZAP, noter *corrigée* ou *acceptée (justification)*.

---

## PHASE 11 — Bilan, rapport, soutenance — 🅰️ + 🅱️

### 11.1 Tableau avant / après (à mettre dans le rapport)

| Domaine | État du TP | État du projet | Preuve |
|---|---|---|---|
| Transport | HTTP clair (TP2) | HTTPS + CA interne | Phase 6 |
| Authentification | aucune | JWT Keycloak | Phase 7 |
| Base de données | RDS ouverte à `0.0.0.0/0`, non chiffrée, sans sauvegarde | SG restreint, chiffrée, sauvegardes, utilisateur dédié | Phase 8 |
| Secrets | token dans un énoncé / mots de passe en clair | GitHub Secrets, variables d'environnement | Phases 5, 8, 9 |
| Qualité | build seul | JaCoCo + Sonar + Quality Gate | Phases 3, 5 |
| Dépendances | non suivies | Trivy + Dependabot | Phase 5 |
| Conteneurs | non fournis | images non-root, scan ECR | Phases 9, 10 |
| Branches | `main` ouverte | `main` protégée, PR relues | Phase 3 |

### 11.2 Plan du rapport (partage des chapitres)

| Chapitre | Auteur | Relecteur |
|---|---|---|
| 1. Contexte, architecture, modèle de menace | A + B | A + B |
| 2. Pipeline CI/CD (Actions, JaCoCo, branches, PR) | A | B |
| 3. Qualité : Sonar (interprétation), Trivy, Dependabot | A | B |
| 4. Cryptographie : OpenSSL, signature, CA | B | A |
| 5. HTTPS et mTLS | B | A |
| 6. Authentification JWT | B | A |
| 7. RDS et profil `prod` | B | A |
| 8. Docker, ECR, EC2 (Deploy Job) | A | B |
| 9. Durcissement, ZAP | B | A |
| 10. Bilan avant/après, limites, perspectives | A + B | A + B |

### 11.3 Soutenance
- Chacun présente **ses** phases et peut expliquer celles de l'autre (relecture croisée).
- Démo en direct conseillée : (1) push → pipeline vert, (2) `curl` 401 puis 201, (3) navigateur HTTPS avec la CA, (4) données RDS persistantes.
- Prévoir captures de secours si le réseau ou AWS tombe.

### 11.4 Check-list finale

- [ ] `main` protégée, PR relues par l'autre membre
- [ ] Run CI vert (Java 17 et 21), rapports JaCoCo en artefacts
- [ ] Dashboard Sonar + interprétation rédigée
- [ ] Tableau Trivy, onglet Dependabot
- [ ] Commandes OpenSSL du TP1 avec captures
- [ ] HTTPS fonctionnel, 401/201 prouvés
- [ ] RDS durcie, trois schémas, persistance vérifiée
- [ ] Images dans ECR, application sur EC2
- [ ] Rapport ZAP traité
- [ ] **Aucun secret** dans `git log -p` (vérifier : `git log -p | grep -iE "password|token|BEGIN .*PRIVATE"`)
- [ ] Ressources AWS arrêtées après la soutenance (EC2, RDS) pour éviter des frais

---

## Dépannage rapide

| Symptôme | Cause probable | Action |
|---|---|---|
| `./mvnw: Permission denied` en CI | bit exécutable | étape `chmod +x mvnw` avant le build |
| Sonar 401 | token faux/expiré (30 jours par défaut) | régénérer, mettre à jour `SONAR_TOKEN` |
| Sonar « Automatic Analysis conflict » | analyse auto activée | la désactiver dans SonarCloud |
| `PKIX path building failed` | CA non reconnue par Java | ajouter la CA au truststore du client |
| Certificat refusé par `curl` | pas de SAN | régénérer avec `subjectAltName` |
| Connexion RDS expire | SG ne contient pas votre IP | corriger la règle inbound |
| Deploy job : `denied` sur ECR | droits IAM insuffisants | ajouter les actions ECR au `github-deployer` |
| `Rule violated ... covered ratio` | couverture < seuil | ajouter des tests ou ajuster le seuil temporairement |