# Guide v2 — Authentification et autorisation interservices (PetClinic, 3 microservices)

**Ce guide remplace `00-DEMARRER-ICI.md`.** Il est aligné à 100 % sur l'énoncé officiel transmis par l'encadrant
(*Projet Ingénierie des Systèmes Sécurisés — Authentification et autorisation interservices*, A.U. 2026/2027)
et indique, phase par phase, **ce qui est déjà fait** dans l'ancien guide.

Aujourd'hui : **lundi 9 novembre 2026**. Dépôt Moodle : **mardi 24 novembre 2026 avant minuit, aucune exception**. Il reste 15 jours.

> Légende : ✅ fait · 🟨 partiel · ⬜ à faire · ♻️ conservé mais adapté · 🚫 hors périmètre officiel (à ne pas faire, ou en bonus seulement si tout le reste est fini)

---

## 1. Ce que l'énoncé officiel exige (et rien d'autre)

| # | Exigence extraite de l'énoncé | Où c'est traité dans ce guide |
|---|---|---|
| E1 | Implémenter une solution d'authentification/autorisation reposant sur **une des solutions citées** (mTLS, JWT, OAuth 2.0/OIDC ; RBAC, ABAC, PBAC, ReBAC), **au choix**. L'énoncé précise que « plusieurs mécanismes peuvent être combinés » | §3 (choix), phases P3 à P6 |
| E2 | Principe **Zero Trust** : chaque microservice prouve son **identité** et ses **droits** avant de communiquer avec un autre | P5 (JWT vérifié dans *chaque* service), P6 (mTLS + autorisation par identité de service) |
| E3 | Test de validité via une application faisant collaborer **3 microservices** pour répondre à la requête d'un client | P7 (scénario « dossier propriétaire » : customers + visits + vets) |
| E4 | Pour **chaque mécanisme/modèle choisi** : avantages et inconvénients **par rapport aux existants**, comme **critères de décision pour les architectes** | §8.3 (tableaux pré-remplis) + chapitre 3 du rapport |
| E5 | **Livrable 1 — Rapport** : solutions existantes, architecture du module, fonctionnement des composants, avantages, inconvénients, etc. + **mise en place** (installation et configuration) | §8 (plan du rapport) |
| E6 | **Livrable 2 — Code Java**, archive **importable dans IntelliJ** | §9 |
| E7 | Dépôt Moodle avant le 24/11/2026 minuit ; **présentation** ultérieure ; travail en **binôme** | §5 (planning), §10 (soutenance) |

**Ce que l'énoncé ne demande pas** : AWS (RDS, ECR, EC2), SonarCloud, JaCoCo, Dependabot/Trivy/gitleaks, Docker de production, Prometheus/Grafana, LaTeX, ni un nombre de pages. Ces éléments venaient de l'ancien guide (inspiré des TP). Ils sortent du périmètre pour que les 15 jours servent le cœur du sujet.

> ⚠️ **À confirmer avec l'encadrant** : le « 30-40 pages » et le « LaTeX » de l'ancien plan ne figurent pas dans l'énoncé. Demandez-lui si une taille ou un format est attendu. Sans réponse : visez un rapport complet et dense (≈ 25-35 pages), au format de votre choix (LaTeX conservé si vous êtes à l'aise).

---

## 2. Bilan de l'ancien guide : ce qui est fait, ce qui change

**Base de la vérification** : l'archive `petclinic-security-automation-main.zip` (fichiers datés du 06/11/2026), le `PLAN-PROJET.md` (statuts ✅/⬜ que vous y avez notés) et l'ancien guide. Je n'ai accès ni à votre dépôt Git ni à vos machines : **si vous avez avancé depuis le 06/11, cochez simplement la colonne « Votre confirmation »**.

### 2.1 Ce qui est réellement fait (preuves dans l'archive)

| Élément | Preuve | Réutilisé dans |
|---|---|---|
| Application réduite à **3 microservices métier** (`customers`, `visits`, `vets`) + `api-gateway` + `discovery-server` | `pom.xml` à 5 modules ; PLAN A.1 ✅ | E3 : c'est l'application de test exigée |
| Configuration embarquée (config-server retiré du build) | PLAN A.2 ✅ ; `application.yml` autonomes | P0 |
| Interface web modernisée | PLAN A.3 ✅ | démonstration (lecture) |
| Chaîne d'outils validée (JDK 21, Maven 3.9, Docker Desktop) | PLAN 0.2 ✅ | P0 |
| Tests unitaires des services : `OwnerResourceTest`, `PetResourceTest`, `VetResourceTest`, `VisitResourceTest` (+ tests gateway et discovery) | présents dans `src/test` | filet de non-régression pendant P5 |
| Analyse du dépôt (pièges Eureka/IP, profil `production` de `vets`, etc.) | ancien guide §0 | repris dans P6 |
| Plans et guides rédigés | `PLAN-PROJET.md/.tex`, `guide/*.md` | — |

### 2.2 Statut des 13 phases de l'ancien guide

| Ancienne phase | État constaté | Verdict vs énoncé officiel | Nouvelle phase | Votre confirmation |
|---|---|---|---|---|
| 0 Outils et comptes | 🟨 outils ✅ ; comptes GitHub à confirmer ; SonarCloud/AWS inutiles | ♻️ réduit (plus de compte cloud) | **P0** | ☐ |
| 1 Dépôt, branches, nettoyage | ⬜ pas de `.git` dans l'archive ; `.gitignore` sans règles secrets ; `check-pr-template.yml` encore présent | ♻️ conservé, allégé | **P1** | ☐ |
| 2 Modèle de menace | ⬜ `SECURITE-modele-de-menace.md` absent de l'archive | ♻️ raccourci : il justifie le Zero Trust dans le rapport | **P2** | ☐ |
| 3 CI + JaCoCo | ⬜ `maven-build.yml` d'origine | 🚫 hors périmètre. Gardez `maven-build.yml` tel quel (CI gratuite) | — | ☐ |
| 4 SonarCloud, SCA, secrets | ⬜ | 🚫 bonus uniquement | — | ☐ |
| 5 PKI avec OpenSSL | ⬜ pas de `scripts/pki/` | ✅ **requis** (mTLS) → conservé | **P3** | ☐ |
| 6 Authentification JWT | ⬜ aucune dépendance de sécurité dans les poms | ♻️ conservé et **durci** (voir 2.3) | **P4, P5** | ☐ |
| 7 TLS puis mTLS | ⬜ | ♻️ conservé, simplifié, + autorisation par identité de service | **P6** | ☐ |
| 8 RDS MySQL | ⬜ | 🚫 hors périmètre (HSQLDB mémoire suffit) | — | ☐ |
| 9 Docker, ECR, EC2 | ⬜ | 🚫 remplacé par exécution locale + IntelliJ ; Docker réservé à Keycloak | — | ☐ |
| 10 Durcissement | ⬜ | ♻️ réduit : Eureka, Actuator, Retry (Grafana, ZAP, Trivy sortis) | **P8** | ☐ |
| 11 Observabilité et alertes | ⬜ | 🚫 bonus | — | ☐ |
| 12 Rapport LaTeX | ⬜ `rapport/main.tex` vide | ♻️ **plan réaligné sur les livrables de l'énoncé** | **P10** | ☐ |
| 13 Soutenance | ⬜ | ♻️ conservé (présentation annoncée par l'encadrant) | **P12** | ☐ |

### 2.3 Quatre écarts de l'ancien guide avec l'énoncé, corrigés ici

1. **Le JWT n'était vérifié qu'au gateway** (la vérification dans les services était « optionnelle », ancien §6.9). L'énoncé demande que *chaque microservice* prouve identité et droits : la vérification dans chaque service devient **obligatoire** (P5).
2. **Les requêtes GET étaient publiques** (pour garder le front fonctionnel). En Zero Trust, « toujours vérifier » : toute requête `/api/**` exige désormais un jeton. Conséquence assumée : le front ne peut plus lire les données sans connexion ; la démonstration passe par `curl`/Postman (le front reste une perspective, §8.3).
3. **L'identité du service appelant n'était qu'authentifiée** (mTLS) mais jamais *autorisée*. Ajout d'une liste blanche d'appelants fondée sur le CN du certificat client (P6) : c'est la partie « droits du service » du Zero Trust.
4. **Les 3 microservices ne collaboraient pas tous** sur une requête (l'agrégation existante n'appelait que `customers` et `visits`). P7 ajoute un scénario qui appelle les trois (E3).

---

## 3. Décision de conception (à figer aujourd'hui, à justifier dans le rapport)

| Besoin | Choix retenu | Alternatives étudiées (pour le rapport) |
|---|---|---|
| Identité de l'**utilisateur**, délégation, émission des jetons | **OAuth 2.0 / OIDC** avec **Keycloak** | Spring Authorization Server, Auth0/Okta, Amazon Cognito |
| Format du jeton porté entre services | **JWT** signé (RS256), validé localement par chaque service | jeton opaque + introspection, API keys, Basic |
| Identité du **service** appelant | **mTLS** (PKI OpenSSL, CA de projet) | SPIFFE/SPIRE, service mesh (Istio/Linkerd) |
| Modèle d'autorisation | **RBAC** (rôles `viewer`, `staff`) + une règle **ABAC** (attribut = CN du certificat de l'appelant) | ABAC pur, PBAC (OPA, Cedar), ReBAC (OpenFGA, SpiceDB) |

Pourquoi cette combinaison : Keycloak et JWT répondent à « qui est l'utilisateur et quels rôles a-t-il », mTLS répond à « quel service m'appelle », RBAC suffit pour une matrice de droits à deux rôles, et la liste blanche de CN montre un contrôle fondé sur un attribut sans introduire un moteur de politiques. Les modèles non retenus (PBAC, ReBAC) sont **comparés dans le rapport** (E4), pas implémentés.

### Matrice RBAC cible (identique dans le gateway et dans chaque service)

| Requête | Sans jeton | `carol` (aucun rôle) | `bob` (`viewer`) | `alice` (`staff`) |
|---|---|---|---|---|
| `GET /api/**` | 401 | 403 | 200 | 200 |
| `POST`, `PUT` sur `/api/**` | 401 | 403 | 403 | 201 / 204 |
| Appel direct à un service sans certificat client | échec TLS | échec TLS | échec TLS | échec TLS |
| Appel direct avec certificat d'un service **non autorisé** | 403 | 403 | 403 | 403 |

### Architecture visée

```
Client (curl/Postman)
   │  1. jeton JWT (Keycloak, OIDC)
   ▼
api-gateway :8080  ──(valide JWT, RBAC)──►  Keycloak :8180 (clés publiques JWKS)
   │  2. mTLS (certificat « api-gateway ») + jeton relayé
   ├──► customers-service :8081 ─┐
   ├──► visits-service    :8082 ─┼─ chacun : mTLS exigé + CN autorisé + JWT validé + RBAC
   └──► vets-service      :8083 ─┘
Eureka :8761 (authentification basique) — annuaire
```

---

## 4. Organisation du binôme

| | 🅰️ Membre A — Identité utilisateur (OIDC, JWT, RBAC) | 🅱️ Membre B — Identité des services (PKI, TLS, mTLS) |
|---|---|---|
| Phases | P1, P2, P4, P5, P7 (gateway) | P3, P6, P8, P7 (services) |
| Fichiers propres | `spring-petclinic-security/**` (sauf `ServiceIdentityFilter`), `SecurityConfig` du gateway, `keycloak/**`, `docker-compose.security.yml` | `scripts/pki/**`, blocs `tls` des `application.yml`, `ServiceIdentityFilter`, `EurekaSecurityConfig` |
| Chapitres du rapport | OAuth2/OIDC, JWT, RBAC, scénario | PKI, TLS/mTLS, Eureka, durcissement |
| En commun | P9 (tests), P10 (rapport : introduction, état de l'art, comparaisons, conclusion), P11, P12 |

**Règles** : branches `main` ← `dev` ← `feature/<phase>` ; jamais de push direct sur `main`/`dev` ; relecture croisée de chaque PR ; **aucun secret dans Git** (`*.p12`, `*.pem`, `*.key`, mots de passe) ; toujours `./mvnw` ; après chaque modification de `pom.xml` ou Java : `./mvnw -B -DskipTests package` → `BUILD SUCCESS` ; chaque phase se termine par des **preuves** rangées dans `docs/preuves/pXX/` (captures et sorties de commandes sans secret). Ces preuves nourrissent directement le rapport.

---

## 5. Planning jusqu'au 24/11 (16 jours calendaires)

| Jour | A | B | Sortie attendue |
|---|---|---|---|
| Lun 9 | P0, P1, décision §3 | P0, P3 (ateliers OpenSSL) | dépôt prêt, choix figé |
| Mar 10 | P4 Keycloak + jeton | P3 `generate.sh` (CA + certificats) | jeton `eyJ…` ; 5 certificats vérifiés |
| Mer 11 | P5 module sécurité + services | P6 palier 1 (HTTPS sur customers) | 401/403/200 sur un service ; HTTPS ok |
| Jeu 12 | P5 gateway | P6 palier 1 sur 3 services + palier 2 | tout en HTTPS via le gateway |
| Ven 13 | P7 endpoint « dossier » | P6 palier 3 mTLS + liste blanche | scénario complet en mTLS |
| Sam 14 – Dim 15 | tampon : corrections, P2 (menace) | tampon : corrections, P8 | **gel du code le dimanche soir** |
| Lun 16 | P9 tests automatisés | P9 script de contrôle | tableau 401/403/200 vert |
| Mar 17 | rapport ch. 1-3 | rapport ch. 4-5 | état de l'art et comparaisons |
| Mer 18 – Sam 21 | rapport | rapport | version complète du rapport |
| Dim 22 | P11 archive IntelliJ testée à froid | relecture croisée | archive + rapport finalisés |
| Lun 23 | préparation de la présentation, **dépôt Moodle dans la soirée** | idem | livrables déposés |
| Mar 24 | marge de sécurité uniquement | | limite absolue avant minuit |

> Déposez le **23 au soir** : aucun livrable n'est accepté après le 24 minuit, et une plateforme saturée le dernier jour est un risque réel.

---

## P0 — Préparation (A + B, 0,5 j)

Ouvrir **Git Bash** à la racine du projet (dossier contenant `pom.xml`).
```bash
java -version      # 17 ou 21
./mvnw -v          # Maven 3.9.x
docker version     # client ET serveur (Docker Desktop lancé) — nécessaire pour Keycloak
git --version
openssl version    # fourni par Git for Windows
keytool -help | head -3
./mvnw -B clean verify   # point de départ : BUILD SUCCESS, tests verts
```
Si `java` est introuvable : `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"; export PATH="$JAVA_HOME/bin:$PATH"`.
Installer en plus : IntelliJ IDEA (Community suffit), Postman (facultatif).
Comptes nécessaires : **GitHub uniquement** (plus d'AWS ni de SonarCloud).

**✅** `BUILD SUCCESS` sur 5 modules. **⛔** Ne pas avancer sur un build rouge.

---

## P1 — Dépôt Git et hygiène des secrets (A, 0,5 j) — ⬜

```bash
git init -b main
git config --local user.name "Prénom A" && git config --local user.email "a@exemple.com"
```
Ajouter à la fin de `.gitignore` **avant** toute génération de clé :
```gitignore
# Secrets et cryptographie : JAMAIS dans Git
*.pem
*.key
*.p12
*.jks
*.csr
*.srl
*.ext
.env
.env.*
secrets/
logs/
```
Retirer le workflow hérité du projet amont (`pull_request_target`, ferme des PR automatiquement) :
```bash
git rm -f .github/workflows/check-pr-template.yml .github/PULL_REQUEST_TEMPLATE.md
```
Exclure de l'archive de rendu les modules hors build, avec un fichier `.gitattributes` (ajouter ces lignes) :
```
spring-petclinic-config-server export-ignore
spring-petclinic-genai-service export-ignore
spring-petclinic-admin-server  export-ignore
```
```bash
git add -A && git status        # aucun target/, .p12 ou .pem ne doit apparaître
git commit -m "chore: initial import"
git remote add origin https://github.com/COMPTE_A/petclinic-security-automation.git
git push -u origin main
git checkout -b dev && git push -u origin dev
```
Inviter B (Settings → Collaborators), qui clone et configure son identité. Le workflow `maven-build.yml` d'origine peut rester : il fournit un build automatique sans effort supplémentaire.

**✅** `git ls-files | grep -E "\.(pem|p12|key)$"` ne renvoie rien.

---

## P2 — Modèle de menace court (A, 2 h) — ⬜ (à faire pendant le tampon du week-end)

Objectif : justifier **pourquoi** le Zero Trust, dans le chapitre 1 du rapport. Dans un tableau STRIDE réduit aux flux utiles (client→gateway, gateway→service, service→Eureka), consignez le **constat « avant »** de l'état actuel, avec preuve :

| Constat avant | Commande de preuve |
|---|---|
| Aucune authentification | `curl -i http://localhost:8080/api/customer/owners` → 200 sans jeton |
| Contournement du gateway | `curl -i http://localhost:8081/owners` (port fixe via `SERVER_PORT=8081`) → 200 |
| Trafic interne en clair | capture réseau ou simple constat `http://` dans Eureka |
| Annuaire Eureka ouvert | `curl -i http://localhost:8761/eureka/apps` → 200 |
| Actuator exposé (`*`) | `curl http://localhost:8080/actuator/env` → JSON complet |

Faites ces captures **avant** de modifier le code (ou sur la branche `main` d'origine). Elles constituent l'« avant » du tableau avant/après du rapport.

---

## P3 — PKI avec OpenSSL (B, 1 j) — ⬜ — requis pour mTLS

Tout se fait dans `secrets/` (ignoré par Git). Seul `scripts/pki/generate.sh` est committé.

### 3.1 Ateliers de base (illustrent le chapitre « Cryptographie »)
```bash
mkdir -p secrets/tp1 && cd secrets/tp1
openssl genrsa -out laClePv.pem 2048
openssl rsa -in laClePv.pem -pubout -out laClePu.pem
echo "message secret" > data.txt
openssl pkeyutl -encrypt -pubin -inkey laClePu.pem -in data.txt -out dataChiffre.bin
openssl pkeyutl -decrypt -inkey laClePv.pem -in dataChiffre.bin -out dataClair.txt && cat dataClair.txt
openssl dgst -sha256 -sign laClePv.pem -out signature.sig data.txt
openssl dgst -sha256 -verify laClePu.pem -signature signature.sig data.txt     # Verified OK
cp data.txt data2.txt && echo "x" >> data2.txt
openssl dgst -sha256 -verify laClePu.pem -signature signature.sig data2.txt    # Verification Failure
cd ../..
```
📸 Les deux résultats (OK puis Failure) illustrent intégrité et authenticité.

### 3.2 CA du projet et certificats des services
Créer `scripts/pki/generate.sh` :
```bash
#!/usr/bin/env bash
# Génère une CA, un certificat serveur par service, un certificat client pour le gateway
# et un certificat client « intrus » (CA valide mais CN non autorisé, pour la démonstration).
# Usage : PKI_PASS='mot-de-passe-fort' ./scripts/pki/generate.sh
set -euo pipefail
export MSYS_NO_PATHCONV=1          # Git Bash : empêche la déformation de -subj "/C=..."
: "${PKI_PASS:?définir PKI_PASS}"
OUT="${1:-secrets}"
mkdir -p "$OUT" && cd "$OUT"

openssl genrsa -out ca.key 2048
openssl req -x509 -new -key ca.key -days 365 -sha256 -out ca.pem \
  -subj "/C=MA/ST=Maroc/L=Rabat/O=ensias/OU=info/CN=PetClinic-CA"

issue() {  # issue <nom> <serverAuth|clientAuth> <SAN>
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
issue api-gateway  clientAuth "DNS:localhost,DNS:api-gateway"
issue rogue-client clientAuth "DNS:localhost"

keytool -importcert -noprompt -alias petclinic-ca -file ca.pem \
  -keystore truststore.p12 -storetype PKCS12 -storepass "$PKI_PASS"

for f in customers vets visits api-gateway rogue-client; do openssl verify -CAfile ca.pem "$f.crt"; done
```
```bash
chmod +x scripts/pki/generate.sh
export PKI_PASS='un-mot-de-passe-local'     # ne jamais le committer
./scripts/pki/generate.sh
```
**✅** Cinq lignes `…crt: OK` ; fichiers `ca.pem`, `*.p12`, `truststore.p12` dans `secrets/`.

À expliquer dans le rapport : le **SAN** (Java et `curl` vérifient le nom contre le SAN, pas le CN) ; l'**EKU** (`serverAuth` pour les services, `clientAuth` pour les clients : requis par mTLS) ; le format **PKCS12** (lu directement par Spring Boot). Les fichiers de `secrets/` se transmettent entre A et B **hors Git** ; **une seule CA** doit servir pour la démonstration finale.
📸 `openssl x509 -in secrets/ca.pem -noout -text | head -20`, sortie de `openssl verify`.

---

## P4 — Keycloak : émission de jetons OIDC (A, 0,5 j) — ⬜

Pour que le correcteur reproduise l'environnement sans clic, on **importe le realm automatiquement**.

`docker-compose.security.yml` (à la racine) :
```yaml
name: petclinic-security
services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    command: start-dev --import-realm
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: ${KC_ADMIN_PASS:-admin-dev-only}
    ports:
      - "8180:8080"
    volumes:
      - ./keycloak/petclinic-realm.json:/opt/keycloak/data/import/petclinic-realm.json:ro
```
`keycloak/petclinic-realm.json` (**mots de passe de démonstration uniquement**, mode développement) :
```json
{
  "realm": "petclinic",
  "enabled": true,
  "accessTokenLifespan": 300,
  "roles": { "realm": [ { "name": "staff" }, { "name": "viewer" } ] },
  "clients": [
    {
      "clientId": "petclinic-cli",
      "enabled": true,
      "publicClient": true,
      "directAccessGrantsEnabled": true,
      "standardFlowEnabled": false
    }
  ],
  "users": [
    { "username": "alice", "enabled": true, "emailVerified": true, "firstName": "Alice", "lastName": "Staff",
      "email": "alice@petclinic.test", "credentials": [ { "type": "password", "value": "alice-pass", "temporary": false } ],
      "realmRoles": [ "staff" ] },
    { "username": "bob", "enabled": true, "emailVerified": true, "firstName": "Bob", "lastName": "Viewer",
      "email": "bob@petclinic.test", "credentials": [ { "type": "password", "value": "bob-pass", "temporary": false } ],
      "realmRoles": [ "viewer" ] },
    { "username": "carol", "enabled": true, "emailVerified": true, "firstName": "Carol", "lastName": "Norole",
      "email": "carol@petclinic.test", "credentials": [ { "type": "password", "value": "carol-pass", "temporary": false } ] }
  ]
}
```
```bash
docker compose -f docker-compose.security.yml up -d
TOKEN=$(curl -s -X POST http://localhost:8180/realms/petclinic/protocol/openid-connect/token \
  -d grant_type=password -d client_id=petclinic-cli -d username=alice -d password=alice-pass \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
echo "${TOKEN:0:30}..."
```
**✅** Une chaîne commençant par `eyJ`. Décoder le contenu sur https://jwt.io avec un jeton **de test** : les rôles sont dans `realm_access.roles`.
**⛔** `Account is not fully set up` : l'utilisateur n'a pas de prénom, nom ou e-mail (obligatoires dans Keycloak 24+) ; les trois sont présents dans le JSON ci-dessus. Si vous créez des utilisateurs à la main, remplissez-les.
📸 Console http://localhost:8180 (realm, rôles, utilisateurs), jeton décodé (jeton de test).

---

## P5 — Module de sécurité partagé : JWT + RBAC dans chaque service (A, 1,5 j) — ⬜

C'est le « **module d'authentification/autorisation** » de l'énoncé : un module Maven `spring-petclinic-security`, réutilisé par les 3 services et le gateway.

### 5.1 Créer le module
Ajouter dans `pom.xml` racine, dans `<modules>`, **en premier** : `<module>spring-petclinic-security</module>`.

`spring-petclinic-security/pom.xml` :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.samples</groupId>
        <artifactId>spring-petclinic-microservices</artifactId>
        <version>4.0.1</version>
    </parent>
    <groupId>org.springframework.samples.petclinic.client</groupId>
    <artifactId>spring-petclinic-security</artifactId>
    <description>Module d'authentification/autorisation interservices (JWT, RBAC, identité de service)</description>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```
> Spring Boot 4 a renommé ses starters : `spring-boot-starter-security-oauth2-resource-server` est le nom actuel (l'ancien `spring-boot-starter-oauth2-resource-server` existe encore mais est déprécié). Si Maven ne résout pas `jakarta.servlet-api` (version non gérée), remplacez cette dépendance par `spring-boot-starter-webmvc` en scope `provided` : les services l'apportent déjà à l'exécution, et le gateway (WebFlux) n'utilise que `KeycloakRoles`.

### 5.2 Classes du module (package `org.springframework.samples.petclinic.security`)

`KeycloakRoles.java` (partagée par le gateway réactif et les services MVC) :
```java
package org.springframework.samples.petclinic.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Traduit le claim Keycloak realm_access.roles en autorités Spring ROLE_xxx. */
public final class KeycloakRoles {

    private KeycloakRoles() { }

    public static Collection<GrantedAuthority> authorities(Map<String, Object> realmAccess) {
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            return roles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        }
        return List.of();
    }
}
```

`ServletSecurityConfiguration.java` (services MVC) :
```java
package org.springframework.samples.petclinic.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ServletSecurityConfiguration {

    @Bean
    SecurityFilterChain petclinicSecurityFilterChain(
            HttpSecurity http,
            @Value("${petclinic.security.allowed-callers:api-gateway}") List<String> allowedCallers,
            @Value("${petclinic.security.mtls-required:false}") boolean mtlsRequired) throws Exception {

        http
            .csrf(csrf -> csrf.disable())                                   // API sans session : jeton Bearer
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 1) qui est l'appelant (service) ?  2) qui est l'utilisateur (JWT) ?  3) a-t-il le droit (RBAC) ?
            .addFilterBefore(new ServiceIdentityFilter(Set.copyOf(allowedCallers), mtlsRequired),
                BearerTokenAuthenticationFilter.class)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/**").hasAnyRole("viewer", "staff")
                .requestMatchers(HttpMethod.POST, "/**").hasRole("staff")
                .requestMatchers(HttpMethod.PUT, "/**").hasRole("staff")
                .requestMatchers(HttpMethod.PATCH, "/**").hasRole("staff")
                .requestMatchers(HttpMethod.DELETE, "/**").hasRole("staff")
                .anyRequest().denyAll())                                    // refus par défaut
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter())));
        return http.build();
    }

    private static JwtAuthenticationConverter converter() {
        JwtAuthenticationConverter c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(jwt -> KeycloakRoles.authorities(jwt.getClaimAsMap("realm_access")));
        return c;
    }
}
```
`ServiceIdentityFilter.java` (propriété de **B**, utilisé en P6 ; la classe doit exister dès maintenant pour compiler) :
```java
package org.springframework.samples.petclinic.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autorisation par identité de service : après le handshake mTLS, lit le CN du certificat client
 * et ne laisse passer que les appelants de la liste blanche (petclinic.security.allowed-callers).
 */
public class ServiceIdentityFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceIdentityFilter.class);
    private static final Pattern CN = Pattern.compile("CN=([^,]+)");

    private final Set<String> allowedCallers;
    private final boolean mtlsRequired;

    public ServiceIdentityFilter(Set<String> allowedCallers, boolean mtlsRequired) {
        this.allowedCallers = allowedCallers;
        this.mtlsRequired = mtlsRequired;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        X509Certificate[] certs = (X509Certificate[]) req.getAttribute("jakarta.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            if (mtlsRequired) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);   // certificat client requis
                return;
            }
            chain.doFilter(req, res);
            return;
        }
        Matcher m = CN.matcher(certs[0].getSubjectX500Principal().getName());
        String caller = m.find() ? m.group(1) : "";
        if (!allowedCallers.contains(caller)) {
            LOG.warn("Appelant refuse : CN={}", caller);
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);          // identité valide mais non autorisée
            return;
        }
        LOG.debug("Appelant autorise : CN={}", caller);
        chain.doFilter(req, res);
    }
}
```

### 5.3 Brancher les 3 services
Dans chacun des `pom.xml` de `customers`, `visits` et `vets`, ajouter :
```xml
        <dependency>
            <groupId>org.springframework.samples.petclinic.client</groupId>
            <artifactId>spring-petclinic-security</artifactId>
            <version>${project.version}</version>
        </dependency>
```
Dans chacune des 3 classes `*ServiceApplication`, ajouter l'activation explicite :
```java
@Import(org.springframework.samples.petclinic.security.ServletSecurityConfiguration.class)
```
Dans le `application.yml` de chacun (obligatoire, sinon le service ne démarre pas faute de `JwtDecoder`), sous `spring:` :
```yaml
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8180/realms/petclinic}
```
```bash
./mvnw -B -DskipTests package
```
**⛔** Des tests `@WebMvcTest` existants échouent en 401/403 : ils testent la logique métier, pas la sécurité. Ajouter `@AutoConfigureMockMvc(addFilters = false)` sur ces classes (l'import a changé en Spring Boot 4 : laisser l'IDE le proposer).

### 5.4 Brancher le gateway (WebFlux)
`spring-petclinic-api-gateway/pom.xml` : ajouter le module de sécurité (comme ci-dessus, pour `KeycloakRoles`) **et** :
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
        </dependency>
```
`application.yml` du gateway, sous `spring:` (à côté de `cloud:`) : le même bloc `security.oauth2.resourceserver.jwt.issuer-uri` qu'au §5.3.

Nouvelle classe `…/petclinic/api/config/SecurityConfig.java` (le gateway est WebFlux : `ServerHttpSecurity`, pas `HttpSecurity`) :
```java
package org.springframework.samples.petclinic.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.samples.petclinic.security.KeycloakRoles;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(a -> a
                .pathMatchers("/actuator/health/**", "/fallback").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/**").hasAnyRole("viewer", "staff")
                .pathMatchers("/api/**").hasRole("staff")                   // POST, PUT, DELETE…
                .pathMatchers(HttpMethod.GET, "/**").permitAll()            // fichiers statiques du front
                .anyExchange().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter())))
            .build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> converter() {
        return new ReactiveJwtAuthenticationConverterAdapter(jwt ->
            new JwtAuthenticationToken(jwt, KeycloakRoles.authorities(jwt.getClaimAsMap("realm_access"))));
    }
}
```
Le gateway transmet l'en-tête `Authorization` aux services par défaut : chaque service **revalide** donc le jeton (défense en profondeur).

### 5.5 Test de la phase (démarrage sans TLS pour l'instant)
Démarrer dans l'ordre : Keycloak (P4), `discovery-server`, les 3 services, le gateway (`scripts/run_all.sh` ou `java -jar`). Puis, avec `TOKEN` (alice), `TOKEN_BOB`, `TOKEN_CAROL` obtenus comme en P4 :
```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/vet/vets                                    # 401
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN_CAROL" http://localhost:8080/api/vet/vets   # 403
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN_BOB"   http://localhost:8080/api/vet/vets   # 200
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/customer/owners \
  -H "Authorization: Bearer $TOKEN_BOB" -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","address":"1 rue X","city":"Rabat","telephone":"0612345678"}'          # 403
# même POST avec $TOKEN (alice)                                                                                  # 201
```
**✅** La matrice RBAC du §3 est respectée. **⛔** `401` malgré un jeton valide : l'`issuer` du jeton diffère de `issuer-uri` (utiliser le même nom d'hôte, `localhost`, pour obtenir le jeton et configurer l'application). `ConnectException` : Keycloak non lancé (l'émetteur est contacté au premier appel).
📸 Les cinq codes, extrait de `ServletSecurityConfiguration` et de `SecurityConfig`.

### 5.6 Correctif de résilience (constat de l'ancien guide)
Dans le `application.yml` du gateway, filtre `Retry` : remplacer `methods: POST` par `methods: GET` (rejouer un POST peut créer deux fois la ressource).

---

## P6 — TLS puis mTLS et autorisation par identité de service (B, 3 j) — ⬜

Un seul profil `tls` par service ; la rigueur se règle par deux variables (`CLIENT_AUTH`, `MTLS_REQUIRED`), ce qui évite de réécrire le YAML entre les paliers.

### 6.0 Pièges propres à ce dépôt (analysés dans l'ancien guide)
- Les services écoutent sur un port aléatoire (`port: 0`) : fixer 8081/8082/8083 via `SERVER_PORT`.
- Eureka enregistre par **IP** (`prefer-ip-address: true`) : un certificat valable pour des noms échouerait. On enregistre par **nom d'hôte** et on déclare le port **sécurisé**.
- Pour que `lb://` produise du `https://`, l'instance s'enregistre avec `secure-port-enabled: true` et `non-secure-port-enabled: false`.
- `vets` active le profil `production` (cache) dans son YAML : `SPRING_PROFILES_ACTIVE` le **remplace**. Lancer vets avec `production,tls`.
- `PKI_DIR` doit être un **chemin absolu avec `/`** (ex. `C:/Users/nom/projet/secrets`) : le répertoire de travail d'IntelliJ n'est pas toujours la racine.

### 6.1 Bloc de profil `tls` des services
À la fin de `application.yml` de `customers` (8081, alias `customers`), puis `visits` (8082, `visits`) et `vets` (8083, `vets`), en adaptant port et alias :
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
    client-auth: ${CLIENT_AUTH:need}                 # none | want | need
    trust-store: file:${PKI_DIR:secrets}/truststore.p12
    trust-store-type: PKCS12
    trust-store-password: ${PKI_PASS}
eureka:
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:localhost}
    prefer-ip-address: false
    secure-port-enabled: true
    non-secure-port-enabled: false
    secure-port: ${server.port}
petclinic:
  security:
    mtls-required: ${MTLS_REQUIRED:true}
    allowed-callers: api-gateway                     # liste blanche des CN (séparés par des virgules)
```

### 6.2 Palier 1 — HTTPS côté serveur (un service, puis les trois)
```bash
export PKI_PASS='…' PKI_DIR="$(pwd)/secrets"
./mvnw -B -DskipTests package
CLIENT_AUTH=none MTLS_REQUIRED=false SPRING_PROFILES_ACTIVE=tls \
  java -jar spring-petclinic-customers-service/target/spring-petclinic-customers-service-*.jar
curl --cacert secrets/ca.pem -i https://localhost:8081/owners     # 401 : TLS ok, jeton manquant (JWT, P5)
curl -i https://localhost:8081/owners                              # échec : certificat non reconnu
curl -i http://localhost:8081/owners                               # échec : port TLS
```
**✅** Réponse HTTP (401 attendu faute de jeton) avec `--cacert`, échec TLS sans. Avec `-H "Authorization: Bearer $TOKEN"` → 200. 📸 Navigateur sur `https://localhost:8081` (avertissement), puis après import de `ca.pem` : cadenas valide.

### 6.3 Palier 2 — Le gateway fait confiance à la CA
Ajouter au `application.yml` du gateway (bloc de profil), et la **même configuration** pour le client de l'endpoint d'agrégation (§7) :
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
                - ${PKI_DIR:secrets}/ca.pem
              key-store: ${PKI_DIR:secrets}/api-gateway.p12
              key-store-type: PKCS12
              key-store-password: ${PKI_PASS}
              key-password: ${PKI_PASS}
petclinic:
  mtls:
    key-store: ${PKI_DIR:secrets}/api-gateway.p12
    trust-store: ${PKI_DIR:secrets}/truststore.p12
    password: ${PKI_PASS}
```
Lancer discovery, les 3 services puis le gateway avec `CLIENT_AUTH=none MTLS_REQUIRED=false` et `SPRING_PROFILES_ACTIVE=tls` (`production,tls` pour vets). Dans http://localhost:8761, les instances doivent apparaître en `https` avec un nom d'hôte.
```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/customer/owners     # 200, canal interne chiffré
```
**⛔** `PKIX path building failed` : la CA n'est pas approuvée par le client qui échoue. `No instances available` ou `http` au lieu de `https` : l'instance s'enregistre sans port sécurisé (vérifier dans Eureka). Si une clé `httpclient.ssl.*` n'est pas reconnue, consulter la documentation Spring Cloud Gateway 2025.1 pour le nom exact.

### 6.4 Palier 3 — mTLS strict + liste blanche
Relancer les services **sans** `CLIENT_AUTH` ni `MTLS_REQUIRED` (valeurs par défaut `need` / `true`). Tests :
```bash
# 1. Sans certificat client : handshake refusé
curl --cacert secrets/ca.pem -i https://localhost:8081/owners
# 2. Certificat du gateway, sans jeton : TLS accepté, JWT absent → 401
curl --cacert secrets/ca.pem --cert secrets/api-gateway.crt --key secrets/api-gateway.key -i https://localhost:8081/owners
# 3. Certificat du gateway + jeton alice → 200
curl --cacert secrets/ca.pem --cert secrets/api-gateway.crt --key secrets/api-gateway.key \
  -H "Authorization: Bearer $TOKEN" -i https://localhost:8081/owners
# 4. Certificat « intrus » (CA valide, CN non autorisé) + jeton alice → 403
curl --cacert secrets/ca.pem --cert secrets/rogue-client.crt --key secrets/rogue-client.key \
  -H "Authorization: Bearer $TOKEN" -i https://localhost:8081/owners
# 5. Via le gateway → 200
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/customer/owners
```
**✅** (1) échec `certificate required`/`handshake failure` ; (2) 401 ; (3) 200 ; (4) 403 ; (5) 200. Les cas 3 et 4 montrent la différence entre **authentifier** un service (mTLS) et **l'autoriser** (liste blanche de CN).
📸 Les cinq commandes, extrait des blocs `tls`, liste Eureka en `https`.

### 6.5 Limite à documenter si non résolue
Le certificat client du gateway doit aussi être présenté par le `WebClient` de l'endpoint d'agrégation : c'est l'objet de la classe `MtlsHttpClientFactory` du §7. Si vous ne la réalisez pas, documentez honnêtement que l'agrégation reste en TLS simple (les 3 routes proxy sont en mTLS). Si le palier 3 bloque plus d'une journée, documentez-le comme limite plutôt que d'y passer le week-end.

---

## P7 — Scénario à 3 microservices (A pour le gateway, B pour mTLS du client) (1 j) — ⬜

**Requête cliente** : `GET /api/gateway/owners/{id}/dossier` → le gateway interroge `customers` (propriétaire et animaux), `visits` (visites des animaux) **et** `vets` (vétérinaires disponibles), puis assemble la réponse. Chaque appel porte le certificat du gateway (identité du service) et le jeton de l'utilisateur (identité et rôles).

### 7.1 Client mTLS (B) — `…/api/config/MtlsHttpClientFactory.java`
```java
package org.springframework.samples.petclinic.api.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

/** Construit un client HTTP présentant le certificat du gateway (mTLS). Sans configuration : client standard. */
public final class MtlsHttpClientFactory {

    private MtlsHttpClientFactory() { }

    public static HttpClient create(String keyStore, String trustStore, String password) {
        if (keyStore == null || keyStore.isBlank()) {
            return HttpClient.create();
        }
        try {
            char[] pw = password.toCharArray();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(load(keyStore, pw), pw);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(load(trustStore, pw));
            SslContext ssl = SslContextBuilder.forClient().keyManager(kmf).trustManager(tmf).build();
            return HttpClient.create().secure(spec -> spec.sslContext(ssl));
        } catch (Exception e) {
            throw new IllegalStateException("Configuration mTLS du gateway impossible", e);
        }
    }

    private static KeyStore load(String path, char[] pw) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(Path.of(path))) {
            store.load(in, pw);
        }
        return store;
    }
}
```
Dans `ApiGatewayApplication`, remplacer le bean `loadBalancedWebClientBuilder` (le bean `RestTemplate` n'est utilisé par aucun client : le laisser ou le supprimer) :
```java
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder(
            @Value("${petclinic.mtls.key-store:}") String keyStore,
            @Value("${petclinic.mtls.trust-store:}") String trustStore,
            @Value("${petclinic.mtls.password:}") String password) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                MtlsHttpClientFactory.create(keyStore, trustStore, password)))
            .filter(new ServerBearerExchangeFilterFunction());   // relaie le jeton de l'utilisateur
    }
```
Imports : `org.springframework.beans.factory.annotation.Value`, `org.springframework.http.client.reactive.ReactorClientHttpConnector`, `org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction`, `org.springframework.samples.petclinic.api.config.MtlsHttpClientFactory`.

### 7.2 Endpoint du scénario (A)
`…/api/application/VetsServiceClient.java` :
```java
package org.springframework.samples.petclinic.api.application;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class VetsServiceClient {

    private final WebClient.Builder webClientBuilder;

    public VetsServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<List<Map<String, Object>>> getVets() {
        return webClientBuilder.build().get()
            .uri("http://vets-service/vets")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() { });
    }
}
```
`…/api/dto/OwnerDossier.java` :
```java
package org.springframework.samples.petclinic.api.dto;

import java.util.List;
import java.util.Map;

public record OwnerDossier(OwnerDetails owner, List<Map<String, Object>> vets) { }
```
`…/api/boundary/web/DossierController.java` (classe distincte pour ne pas casser `ApiGatewayControllerTest`) :
```java
package org.springframework.samples.petclinic.api.boundary.web;

import org.springframework.samples.petclinic.api.application.VetsServiceClient;
import org.springframework.samples.petclinic.api.dto.OwnerDossier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Scénario à 3 microservices : customers + visits (via l'agrégation existante) + vets. */
@RestController
@RequestMapping("/api/gateway")
public class DossierController {

    private final ApiGatewayController owners;
    private final VetsServiceClient vets;

    public DossierController(ApiGatewayController owners, VetsServiceClient vets) {
        this.owners = owners;
        this.vets = vets;
    }

    @GetMapping("owners/{ownerId}/dossier")
    public Mono<OwnerDossier> dossier(@PathVariable int ownerId) {
        return Mono.zip(owners.getOwnerDetails(ownerId), vets.getVets(), OwnerDossier::new);
    }
}
```
```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gateway/owners/1/dossier   # 200 : owner + visites + vets
curl -i http://localhost:8080/api/gateway/owners/1/dossier                                     # 401
curl -i -H "Authorization: Bearer $TOKEN_CAROL" http://localhost:8080/api/gateway/owners/1/dossier   # 403
```
**✅** 200 avec les données des trois services. Vérifier dans les journaux des services qu'ils ont bien reçu l'appel en HTTPS avec CN `api-gateway`. **⛔** 200 mais `vets` vide ou erreur : la route `vets` n'est pas en `https` (voir 6.3). 📸 Réponse JSON, journaux des trois services.

> Pour le rapport, un diagramme de séquence de ce scénario (client → gateway → customers → visits ; gateway → vets, avec JWT et mTLS à chaque saut) est le schéma central.

---

## P8 — Durcissement minimal (B, 1 j) — ⬜

Uniquement ce qui renforce l'authentification interservices.

**Actuator** (gateway, customers, visits, vets) : remplacer le bloc `management.endpoints` par
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```
(conserver les clés `tracing`/`zipkin` existantes). `curl -i http://localhost:8080/actuator/env` → 404 ou 403.

**Eureka protégé** : dans `discovery-server/pom.xml`, ajouter `spring-boot-starter-security`, puis :
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
`discovery-server/application.yml` :
```yaml
spring:
  security:
    user:
      name: ${EUREKA_USER:eureka}
      password: ${EUREKA_PASSWORD}
```
`defaultZone` de chaque client (gateway et 3 services) : `http://${EUREKA_USER:eureka}:${EUREKA_PASSWORD}@localhost:8761/eureka/`.
```bash
curl -i http://localhost:8761/eureka/apps                                    # 401
curl -i -u eureka:"$EUREKA_PASSWORD" http://localhost:8761/eureka/apps       # 200
```
**⛔** Les clients ne s'enregistrent plus : `EUREKA_PASSWORD` non défini pour *tous* les processus, ou CSRF non exclu sur `/eureka/**`. À mentionner dans le rapport : l'authentification basique de l'annuaire est une protection **faible** (identifiants statiques), acceptée ici car l'annuaire reste interne ; l'idéal serait mTLS vers Eureka.

📸 `/actuator/env` avant/après, Eureka 401/200.

---

## P9 — Tests automatisés et script de contrôle (A + B, 1 j) — ⬜

### 9.1 Script de contrôle (démonstration et soutenance) — `scripts/security-smoke.sh`
```bash
#!/usr/bin/env bash
# Vérifie la matrice d'autorisation. Prérequis : Keycloak, discovery, 3 services et gateway lancés (profil tls).
set -u
GW=${GW:-http://localhost:8080}
KC=${KC:-http://localhost:8180}
PKI_DIR=${PKI_DIR:-secrets}
fail=0
check() { if [ "$2" = "$3" ]; then echo "OK   $1"; else echo "FAIL $1 (attendu $2, obtenu $3)"; fail=1; fi; }
code()  { curl -s -o /dev/null -w "%{http_code}" "$@"; }
token() { curl -s -X POST "$KC/realms/petclinic/protocol/openid-connect/token" \
            -d grant_type=password -d client_id=petclinic-cli -d "username=$1" -d "password=$2" \
          | sed -E 's/.*"access_token":"([^"]+)".*/\1/'; }

A=$(token alice alice-pass); B=$(token bob bob-pass); C=$(token carol carol-pass)
H() { echo "Authorization: Bearer $1"; }
OWNER='{"firstName":"Test","lastName":"User","address":"1 rue X","city":"Rabat","telephone":"0612345678"}'

check "GET sans jeton"                    401 "$(code $GW/api/vet/vets)"
check "GET carol (aucun rôle)"            403 "$(code -H "$(H $C)" $GW/api/vet/vets)"
check "GET bob (viewer)"                  200 "$(code -H "$(H $B)" $GW/api/vet/vets)"
check "POST bob (viewer)"                 403 "$(code -X POST -H "$(H $B)" -H 'Content-Type: application/json' -d "$OWNER" $GW/api/customer/owners)"
check "POST alice (staff)"                201 "$(code -X POST -H "$(H $A)" -H 'Content-Type: application/json' -d "$OWNER" $GW/api/customer/owners)"
check "dossier alice (3 services)"        200 "$(code -H "$(H $A)" $GW/api/gateway/owners/1/dossier)"
check "dossier sans jeton"                401 "$(code $GW/api/gateway/owners/1/dossier)"
check "actuator/env masqué"               404 "$(code $GW/actuator/env)"

SVC=https://localhost:8081/owners
check "service direct sans certificat"    000 "$(code --cacert $PKI_DIR/ca.pem $SVC)"
check "certificat gateway sans jeton"     401 "$(code --cacert $PKI_DIR/ca.pem --cert $PKI_DIR/api-gateway.crt --key $PKI_DIR/api-gateway.key $SVC)"
check "certificat gateway + jeton alice"  200 "$(code --cacert $PKI_DIR/ca.pem --cert $PKI_DIR/api-gateway.crt --key $PKI_DIR/api-gateway.key -H "$(H $A)" $SVC)"
check "certificat intrus + jeton alice"   403 "$(code --cacert $PKI_DIR/ca.pem --cert $PKI_DIR/rogue-client.crt --key $PKI_DIR/rogue-client.key -H "$(H $A)" $SVC)"
exit $fail
```
> Le code `000` correspond à un échec de handshake (curl n'obtient aucune réponse HTTP). Selon la version d'OpenSSL/Java, un client sans certificat peut recevoir `000` ou une erreur de connexion : les deux prouvent le refus.

### 9.2 Tests JUnit de sécurité (au moins un par service, pour le rapport)
Principe (avec `spring-security-test`, ajouté en `test` ; en Spring Boot 4 le starter de test sécurité s'appelle `spring-boot-starter-security-test`) :
```java
mockMvc.perform(get("/owners")).andExpect(status().isUnauthorized());
mockMvc.perform(get("/owners").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_viewer"))))
       .andExpect(status().isOk());
mockMvc.perform(post("/owners").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_viewer")))
       .contentType(APPLICATION_JSON).content(json)).andExpect(status().isForbidden());
```
Ces tests utilisent un `@SpringBootTest` + `@AutoConfigureMockMvc` (filtres **actifs**, contrairement aux tests métier du §5.3). 📸 Sortie `./mvnw -B test` verte, sortie de `security-smoke.sh` entièrement `OK`.

---

## 8. P10 — Rapport (A + B, du 17 au 22/11)

### 8.1 Plan aligné sur l'énoncé
| # | Chapitre | Contenu exigé par l'énoncé | Rédige |
|---|---|---|---|
| 1 | Introduction et problématique | authentification vs autorisation, microservices, **Zero Trust**, constats « avant » (P2) | A |
| 2 | **Solutions existantes** | mTLS, JWT, OAuth2/OIDC, API keys, service mesh/SPIFFE ; RBAC, ABAC, PBAC, ReBAC ; produits (Keycloak, Spring Authorization Server, Auth0/Okta, Cognito, Istio/Linkerd, SPIRE, OPA, Cerbos, OpenFGA, SpiceDB, Casbin) | A + B |
| 3 | **Critères de décision et comparaison** | tableaux du §8.3 ; justification du choix du §3 | A + B |
| 4 | **Architecture du module** | schéma du §3, diagramme de séquence du scénario P7, découpage `spring-petclinic-security` | A |
| 5 | **Fonctionnement des composants** | Keycloak (realm, rôles, jetons), validation JWT, filtre d'identité de service, PKI, gateway, Eureka | A (identité) / B (transport) |
| 6 | **Installation et configuration** | pas-à-pas reproductible : prérequis, génération des certificats, lancement de Keycloak, variables d'environnement, ordre de démarrage, tests (reprend P3 à P9) | B |
| 7 | Validation | tableaux 401/403/200, mTLS accepté/refusé, sortie de `security-smoke.sh` | A + B |
| 8 | **Avantages et inconvénients du module** | performance (double validation), complexité opérationnelle, limites (voir §8.4) | A + B |
| 9 | Conclusion et perspectives | | A + B |
| | Références, annexes | | A + B |

### 8.2 Tableau avant / après (cœur du chapitre 7)
| Domaine | Avant (constat P2) | Après | Preuve |
|---|---|---|---|
| Authentification utilisateur | aucune | JWT Keycloak (OIDC), vérifié dans gateway **et** chaque service | P5 |
| Autorisation | aucune | RBAC `viewer`/`staff`, refus par défaut | P5 |
| Identité des services | aucune | mTLS, certificat `api-gateway` exigé | P6 |
| Droits des services | aucun | liste blanche de CN (rejet de `rogue-client`) | P6 |
| Transport interne | HTTP en clair | TLS entre gateway et services | P6 |
| Contournement du gateway | accès direct aux services | refusé sans certificat client | P6 |
| Annuaire Eureka | ouvert | authentification basique | P8 |
| Actuator | `*` | `health,info` | P8 |
| Collaboration à 3 services | non démontrée | scénario `dossier` sécurisé | P7 |

### 8.3 Comparaisons pré-remplies (point de départ de E4 : à vérifier, compléter et sourcer)

**Mécanismes d'authentification**

| Mécanisme | Ce qu'il prouve | Avantages | Inconvénients | À préférer quand |
|---|---|---|---|---|
| **mTLS** | l'identité du *service* (certificat) et chiffre le canal | indépendant de l'application, chiffrement et authentification en un seul mécanisme, résiste au vol de jeton | PKI à exploiter (émission, rotation, révocation), pas d'identité utilisateur, débogage plus difficile | on doit authentifier des **services** et chiffrer l'est-ouest |
| **JWT** | l'identité de l'*utilisateur* et ses attributs (claims), signés | validation locale sans appel à l'IdP, standardisé (RFC 7519), porte les rôles | révocation difficile avant expiration, jeton volé utilisable tel quel (bearer), taille, vérifications `iss`/`aud`/`exp` indispensables | on veut propager une identité à travers plusieurs services |
| **OAuth 2.0 / OIDC** | cadre d'émission et de délégation (code + PKCE, client credentials) | standard, SSO, IdP centralisé, scopes, séparation authentification/application | complexité de mise en place, dépendance à la disponibilité de l'IdP, erreurs de configuration fréquentes | plusieurs applications ou clients partagent une identité |
| **API key / Basic** | un secret partagé statique | très simple | pas d'expiration ni de granularité, secret à distribuer partout | à éviter hors cas internes à faible enjeu (Eureka ici) |
| **Service mesh + SPIFFE/SPIRE** | identité de workload automatique, mTLS transparent | rotation automatique, aucun code applicatif | exige Kubernetes/infrastructure dédiée, courbe d'apprentissage, hors périmètre Java | déploiement à grande échelle sur Kubernetes |

**Modèles d'autorisation**

| Modèle | Principe | Avantages | Inconvénients | À préférer quand |
|---|---|---|---|---|
| **RBAC** | droits attachés à des rôles | simple, lisible, auditable, support natif Spring et Keycloak | explosion du nombre de rôles, non contextuel, granularité grossière | peu de profils bien distincts (notre cas) |
| **ABAC** | décision selon attributs (utilisateur, ressource, contexte) | très fin, contextuel (heure, lieu, propriétaire) | règles complexes à écrire, tester et auditer ; gouvernance des attributs | les droits dépendent du contexte ou de la ressource |
| **PBAC** | politiques externalisées « as code » évaluées par un moteur (OPA/Rego, Cedar, Cerbos) | politique centralisée et versionnée, découplée du code | langage et outillage en plus, moteur = point de latence/défaillance | plusieurs services doivent appliquer **la même politique** |
| **ReBAC** | droits déduits de relations entre entités (modèle Zanzibar : OpenFGA, SpiceDB) | naturel pour le partage et les hiérarchies (« propriétaire de l'animal ») | modèle de relations à maintenir, infrastructure dédiée, cohérence entre services | les droits suivent un graphe d'objets (documents, dossiers, organisations) |

**Grille de critères pour l'architecte** (à remplir avec vos mesures et votre expérience) : granularité des droits ; complexité de mise en œuvre ; coût d'exploitation ; latence ajoutée par requête ; révocation et rotation ; auditabilité ; indépendance vis-à-vis du fournisseur ; adéquation au Zero Trust ; maturité de l'écosystème Spring.

### 8.4 Limites à assumer honnêtement dans le rapport
- Le **front Vue n'a pas d'écran de connexion** : il ne peut plus lire les API ; la démonstration passe par `curl`/Postman. Perspective : gateway en client OAuth2 (`oauth2Login` + `TokenRelay`).
- **Keycloak en mode développement** (`start-dev`, HTTP, base H2) ; en production : HTTPS, base externe, secrets gérés.
- **CA auto-signée de projet**, certificats valables 365 jours, pas de révocation (CRL/OCSP) ni de rotation automatique.
- Les jetons sont **relayés** tels quels : pas de contrôle d'audience (`aud`) dédié par service ; perspective : audience par service ou échange de jeton (RFC 8693) et `client_credentials` pour les appels sans utilisateur.
- Eureka : authentification basique, pas de mTLS.
- Double validation du JWT (gateway + service) : surcoût de latence faible mais réel, à mesurer si vous avez le temps (ex. 100 requêtes avec `curl`).
- Rappel : la valeur du certificat de test et des mots de passe du realm est **uniquement démonstrative**.

### 8.5 Références à intégrer
OWASP Top 10 (2021) et API Security Top 10 (2023) ; NIST SP 800-204 et 800-204A/B (sécurité des microservices) ; NIST SP 800-207 (Zero Trust Architecture) ; NIST SP 800-57 (longueur de clés) ; RFC 6749 (OAuth 2.0), RFC 7519 (JWT), RFC 8446 (TLS 1.3), RFC 5280 (X.509) ; OpenID Connect Core 1.0 ; documentation Spring Security, Spring Cloud Gateway, Keycloak. Citez les versions et les dates de consultation.

### 8.6 Contrôles avant dépôt
Chaque affirmation de sécurité renvoie à une preuve (figure ou sortie de commande) ; figures numérotées, légendées et citées ; **aucune capture ne montre un mot de passe, un jeton réel ou une clé** ; noms des deux membres et du module en page de garde.

---

## 9. P11 — Archive importable dans IntelliJ (A + B, dimanche 22/11)

### 9.1 Produire l'archive
Tout est committé sur `main`, puis depuis la racine :
```bash
git archive --format=zip --prefix=petclinic-secure/ -o petclinic-secure-NOM1-NOM2.zip HEAD
```
`git archive` n'exporte que les fichiers suivis : ni `target/`, ni `secrets/`, ni `.idea/`, et les modules `export-ignore` de P1 sont exclus.

### 9.2 Fichier `LISEZMOI-RENDU.md` à inclure à la racine
Prérequis : JDK 17+, Docker Desktop, Git Bash (fournit `openssl`).
```bash
# 1. Importer dans IntelliJ : File > Open > sélectionner pom.xml > Open as Project, attendre l'import Maven.
# 2. Construire
./mvnw -B -DskipTests package
# 3. Générer les certificats de démonstration (PKI_PASS = mot de passe de votre choix)
export PKI_PASS='demo-local'; ./scripts/pki/generate.sh
# 4. Lancer Keycloak (realm importé automatiquement)
docker compose -f docker-compose.security.yml up -d
# 5. Lancer toute la pile sécurisée, puis les contrôles
export PKI_DIR="$(pwd)/secrets"; export EUREKA_PASSWORD='demo-eureka'
./scripts/run-secure.sh
./scripts/security-smoke.sh
```
Comptes de démonstration : `alice` / `alice-pass` (staff), `bob` / `bob-pass` (viewer), `carol` / `carol-pass` (aucun rôle).

### 9.3 Script de lancement — `scripts/run-secure.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail
: "${PKI_PASS:?définir PKI_PASS}"; : "${EUREKA_PASSWORD:?définir EUREKA_PASSWORD}"
export PKI_DIR="${PKI_DIR:-$(pwd)/secrets}"
mkdir -p logs
jar_of() { ls "spring-petclinic-$1/target/"*.jar | grep -vE "sources|javadoc|original" | head -1; }
start() {  # start <module> <profils> [port]
  SERVER_PORT="${3:-}" SPRING_PROFILES_ACTIVE="$2" nohup java -jar "$(jar_of "$1")" > "logs/$1.log" 2>&1 &
  echo "démarré : $1 ($2)"
}
start discovery-server default; sleep 25
start customers-service tls 8081
start visits-service    tls 8082
start vets-service      production,tls 8083
sleep 30
start api-gateway       tls
echo "Attendre ~40 s puis lancer scripts/security-smoke.sh"
```
(`SERVER_PORT` vide pour discovery et gateway : leurs ports sont fixés dans leur YAML. Si `SPRING_PROFILES_ACTIVE=default` pose problème, retirer cette variable pour ces deux modules.)

### 9.4 Lancement depuis IntelliJ (Community ou Ultimate)
Créer une configuration **Application** par module (classe principale `…ServiceApplication`, ou *Spring Boot* en Ultimate) avec : *Active profiles* ou variable `SPRING_PROFILES_ACTIVE` (`tls`, ou `production,tls` pour vets) ; variables d'environnement `PKI_PASS`, `PKI_DIR` (chemin **absolu** en `/`), `SERVER_PORT` (8081 / 8082 / 8083), `EUREKA_PASSWORD`.

### 9.5 Test à froid (obligatoire, ≈ 1 h)
Décompresser l'archive dans un **dossier vide**, sur une machine ou un compte sans votre cache, puis dérouler le `LISEZMOI-RENDU.md` tel quel. Tout ce qui manque est corrigé **dans le dépôt**, pas dans l'archive.

---

## 10. P12 — Présentation (après le dépôt, date communiquée par l'encadrant)

**Démonstration de 10 minutes**, scénarisée : (1) `security-smoke.sh` entièrement vert ; (2) jeton `alice`, appel `dossier` → 200 ; (3) même appel sans jeton → 401, avec `carol` → 403, `POST` de `bob` → 403 ; (4) appel direct à `customers` sans certificat (échec), avec le certificat du gateway (200), avec `rogue-client` (403) ; (5) schéma d'architecture et tableau des compromis. Prévoir **captures et vidéo de secours**.

**Questions probables** (chacun doit savoir répondre sur la partie de l'autre) :
- Pourquoi mTLS **et** JWT ? (le premier authentifie le **service** appelant, le second l'**utilisateur** et ses rôles ; JWT seul ne protège pas contre l'appel direct d'un service compromis.)
- Pourquoi valider le JWT dans chaque service et pas seulement au gateway ? (Zero Trust, défense en profondeur, contournement du gateway.)
- Pourquoi RBAC et pas ABAC/PBAC/ReBAC ? Que changerait ReBAC ici ? (« propriétaire de l'animal » est une relation.)
- Quelle différence entre authentifier et autoriser un service ? (mTLS vs liste blanche de CN.)
- Pourquoi un SAN ? Que change une CA de projet pour le client ?
- Que se passe-t-il si Keycloak tombe ? (les jetons valides continuent d'être vérifiés localement via les clés en cache, mais plus aucun nouveau jeton n'est émis.)
- Quelles sont vos limites ? (§8.4)

---

## 11. Check-list finale (à cocher avant le dépôt Moodle)

**Exigences officielles**
- [ ] E1 : mécanismes choisis (OIDC/JWT, mTLS, RBAC) implémentés et justifiés
- [ ] E2 : chaque service valide JWT **et** exige un certificat client autorisé
- [ ] E3 : scénario `dossier` fonctionnel avec les 3 microservices
- [ ] E4 : avantages/inconvénients de **chaque** mécanisme/modèle choisi, comparés aux existants, avec critères de décision
- [ ] E5 : rapport avec solutions existantes, architecture, composants, avantages, inconvénients **et** installation/configuration
- [ ] E6 : archive en Java, **ouverte et testée à froid** dans IntelliJ
- [ ] E7 : dépôt Moodle effectué **avant le 24/11 minuit** (visé : le 23 au soir) ; binôme identifié

**Qualité**
- [ ] `./mvnw -B clean verify` : `BUILD SUCCESS`
- [ ] `security-smoke.sh` : toutes les lignes `OK`
- [ ] Aucun secret dans l'archive ni dans l'historique : `git log -p | grep -iE "password|PRIVATE KEY"` ne renvoie que des mots de passe de démonstration documentés
- [ ] Captures sans secret, preuves rangées dans `docs/preuves/`
- [ ] Docker Keycloak arrêté après la démonstration

---

## Annexe — Dépannage rapide

| Symptôme | Cause probable | Action |
|---|---|---|
| Service sans démarrage : `JwtDecoder` introuvable | `issuer-uri` absent du YAML | ajouter le bloc `spring.security.oauth2.resourceserver.jwt` (P5.3) |
| Maven ne résout pas le starter OAuth2 | nom de starter Spring Boot 4 | `spring-boot-starter-security-oauth2-resource-server` |
| `Account is not fully set up` (Keycloak) | prénom/nom/e-mail manquants | les renseigner (le JSON d'import le fait) |
| JWT 401 malgré jeton valide | `issuer` différent | même URL (`localhost`) pour obtenir le jeton et configurer `issuer-uri` |
| `openssl req` : sujet bizarre, chemin Windows | conversion de chemin de Git Bash | `export MSYS_NO_PATHCONV=1` (présent dans `generate.sh`) |
| `PKIX path building failed` | CA absente du truststore du client | `trustedX509Certificates` ou truststore JVM |
| `handshake_failure` / `certificate_unknown` | pas de certificat client ou mauvaise CA | vérifier `key-store` du gateway et `trust-store` des services |
| `No subject alternative names matching` | nom d'hôte absent du SAN | régénérer le certificat avec le bon `DNS:` / `IP:` |
| `lb://` appelle `http://` | instance enregistrée sans port sécurisé | `secure-port-enabled: true`, `non-secure-port-enabled: false` |
| 403 alors que le certificat du gateway est présenté | CN absent de `allowed-callers` | vérifier `petclinic.security.allowed-callers` et le CN du certificat |
| Services Eureka disparus après P8 | identifiants Eureka manquants | définir `EUREKA_PASSWORD` pour tous les processus |
| Tests `@WebMvcTest` en 401/403 | filtres de sécurité actifs | `@AutoConfigureMockMvc(addFilters = false)` pour les tests métier |
| Tests du gateway cassés après ajout de la sécurité | contexte de test sans jeton | ajouter `spring-security-test` et un `mockJwt()` ou exclure la configuration pour ces tests |
| `vets` : profil `production` perdu | `SPRING_PROFILES_ACTIVE` remplace le YAML | lancer avec `production,tls` |
| Chemin `PKI_DIR` introuvable sous IntelliJ | répertoire de travail différent | chemin absolu avec des `/` |