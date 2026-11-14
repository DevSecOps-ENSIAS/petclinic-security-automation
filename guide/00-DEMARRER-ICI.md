# Guide de démarrage — pas à pas, clic par clic (aligné sur le guide v2)

**Version du 10 novembre 2026.** Ce fichier remplace intégralement la version précédente.
Il suit la numérotation du guide officiel de référence : **`guide/GuideV2!!!.md`**, qui fait foi
(il est lui-même aligné sur l'énoncé de l'encadrant : *Authentification et autorisation interservices*, A.U. 2026/2027).

> **Dépôt Moodle : mardi 24 novembre 2026 avant minuit — déposer le lundi 23 au soir.**
> Aujourd'hui : mardi 10 novembre 2026. Il reste 14 jours.

**Légende**

| Symbole | Sens |
|---|---|
| ✅ **TERMINÉ** | fait, avec preuves ; ne pas refaire |
| 🟨 **PARTIEL** | une partie existe dans le dépôt, il reste des étapes listées |
| ⬜ **À FAIRE** | rien n'existe encore |
| 📸 | capture d'écran à produire (chemin exact donné) |
| ⛔ | piège déjà rencontré sur ce projet |

---

## 0. Ce qui a changé depuis l'ancien guide

Trois phases de l'ancien guide ne sont **plus au programme** (l'énoncé ne les demande pas) et trois autres
ont changé de numéro. Ne confondez plus « ancienne phase 10 » et « P10 » : ce ne sont pas les mêmes travaux.

| Ancien guide | Guide v2 | Décision |
|---|---|---|
| 0 Outils | **P0** | ✅ conservé |
| 1 Dépôt, branches, secrets | **P1** | ✅ conservé |
| 2 Modèle de menace | **P2** | ✅ conservé (sert le chapitre 1 du rapport) |
| 3 CI + JaCoCo | — | conservé tel quel (gratuit, ne bloque rien) |
| 4 SonarCloud, SCA, secrets | — | ✅ fait (bonus, conservé) |
| 4 bis Chaîne logicielle | — | ✅ fait par B (PR #44), conservé |
| 5 PKI OpenSSL | **P3** | ✅ fait, **sauf** le certificat `rogue-client` |
| 6 JWT en bordure | **P5** | 🟨 le gateway valide un jeton, **pas** les services |
| 7 TLS puis mTLS | **P6** | 🟨 HTTPS et mTLS en place, **pas** la liste blanche de CN |
| 8 RDS MySQL | ❌ supprimée | HSQLDB en mémoire suffit ; artefacts retirés du dépôt |
| 9 Docker, ECR, EC2 | ❌ supprimée | exécution locale + IntelliJ ; Docker **uniquement** pour Keycloak |
| 10 Durcissement | **P8** | ✅ fait le 10/11/2026 (Actuator + Eureka seulement) |
| 11 Observabilité | ❌ supprimée | bonus éventuel |
| 12 Rapport | **P10** | ⬜ plan réaligné sur les 9 chapitres de l'énoncé |
| 13 Soutenance | **P12** | ⬜ conservée |

---

## 1. Tableau de bord : où en est le projet

| Phase | Titre | État | Resp. | Preuves |
|---|---|---|---|---|
| **P0** | Outils et build de référence | ✅ TERMINÉ | A + B | — |
| **P1** | Dépôt, branches, hygiène des secrets | ✅ TERMINÉ | A | `docs/preuves/p01-depot/` |
| **P2** | Modèle de menace (constats « avant ») | ✅ TERMINÉ | A | `docs/preuves/p02-menace/` |
| **P3** | PKI OpenSSL (CA + certificats) | 🟨 PARTIEL — reste `rogue-client` | B | `docs/preuves/p03-pki/` |
| **P4** | Keycloak : émission de jetons OIDC | ✅ TERMINÉ le 10/11/2026 (PR #54) | A | `docs/preuves/p04-keycloak/` |
| **P5** | Module `spring-petclinic-security` : JWT + RBAC partout | ✅ TERMINÉ le 11/11/2026 (PR #61 et #63) | A | `docs/preuves/p05-jwt-rbac/` |
| **P6** | mTLS strict + autorisation par identité de service | 🟨 PARTIEL — paliers 1-2 faits | B | `docs/preuves/p06-mtls/` |
| **P7** | Scénario `dossier` à 3 microservices | ⬜ À FAIRE | A (gateway) + B (client mTLS) | `docs/preuves/p07-dossier/` |
| **P8** | Durcissement (Actuator + Eureka) | ✅ TERMINÉ le 10/11/2026 | A | `docs/preuves/p08-durcissement/` |
| **P9** | Tests automatisés et `security-smoke.sh` | ⬜ À FAIRE | A + B | `docs/preuves/p09-tests/` |
| **P10** | Rapport (9 chapitres) | ⬜ À FAIRE | A + B | — |
| **P11** | Archive importable + test à froid IntelliJ | ⬜ À FAIRE | A + B | — |
| **P12** | Présentation (10 min) | ⬜ À FAIRE | A + B | — |

### Où sont les preuves (état au 14/11/2026)

| Dossier | Phase v2 | Contenu attendu | État |
|---|---|---|---|
| `p01-depot/` | P1 | PR fusionnée, branches, protection de `main` | ❌ à créer |
| `p02-menace/` | P2 | les 5 constats « avant » + README | ✅ **présent** |
| `p03-pki/` | P3 | CA, sortie `openssl verify`, keystore | ❌ **absent du dépôt** (captures chez B) |
| `p04-keycloak/` | P4 | realm importé, 3 utilisateurs, rôle d'`alice`, jeton décodé | ✅ **présent** (4 captures) |
| `p05-jwt-rbac/` | P5 | matrice 401/403/200/403/201, les deux `SecurityConfig`, jeton décodé | ✅ **présent** (4 captures) |
| `p06-mtls/` | P6 | les 5 tests du palier 3 + Eureka en `https` | ❌ **absent du dépôt** (paliers 1-2 chez B) |
| `p07-dossier/` | P7 | `dossier` 200/401/403 + diagramme de séquence | ❌ à créer |
| `p08-durcissement/` | P8 | `actuator/env` 404, `eureka/apps` 401, service `UP` | 🟨 dossier créé, **captures à prendre** |
| `p09-tests/` | P9 | sortie du smoke, `mvn test`, JUnit de sécurité | ❌ à créer |
| `bonus-ci/` | ancienne phase 3 | JaCoCo, run Actions, protection de `main` | ✅ présent (hors périmètre v2) |
| `bonus-qualite-sca/` | ancienne phase 4 | Sonar, Trivy, Dependabot | ✅ présent (hors périmètre v2) |

> Les dossiers `p03-pki/` et `p06-mtls/` sont **référencés mais absents** : les captures ont été prises par B
> sans être committées. À récupérer avant P10, sinon le rapport citera des figures inexistantes.

**Chemin critique** : P4 ✅ → P5 ✅ → **P7 → P9 → P10**. P3 et P6 avancent en parallèle côté B, mais **P7 a besoin de P6**
(le gateway doit présenter son certificat pour appeler `vets`).

---

## 2. Conventions à respecter dans toutes les phases

1. **Branches** : `main` (protégée) ← `dev` ← `feature/<phase>-<sujet>`. Jamais de push direct sur `main` ni `dev`.
2. **Chaque PR** est relue et approuvée par l'autre membre.
3. **Messages de commit** : `feat:`, `fix:`, `sec:`, `docs:`, `chore:`, `ci:`.
4. **Aucun secret dans Git** : `*.p12`, `*.pem`, `*.key`, `.env`, `secrets/` (déjà dans `.gitignore`).
   Contrôle final : `git log -p | grep -iE "password|PRIVATE KEY"`.
5. **Toujours `./mvnw`**, jamais `mvn`. Après tout changement de `pom.xml` ou de code Java :
   `./mvnw -B -DskipTests package` doit afficher `BUILD SUCCESS`.
6. **Après un changement de `application.yml`** : redémarrer le service concerné.
7. **Chaque phase finit par des preuves** : captures et sorties de commandes dans `docs/preuves/pXX-…/`,
   committées avec la PR de la phase. **Jamais** de mot de passe, de jeton réel ou de clé sur une capture.
8. **Variables d'environnement** utilisées partout (à exporter dans le terminal avant de démarrer) :

```bash
export PKI_PASS='demo-local'                      # mot de passe des keystores (jamais committé)
export PKI_DIR="$(pwd)/secrets"                   # chemin ABSOLU, avec des / (IntelliJ inclus)
export EUREKA_USER=eureka
export EUREKA_PASSWORD='demo-eureka'              # exigé depuis P8, par TOUS les processus
export JWT_ISSUER_URI='http://localhost:8180/realms/petclinic'
```

**Ordre de démarrage (toujours le même)** : Keycloak → `discovery-server` → les 3 services → le gateway.

---

## P0 — Outils et build de référence · ✅ TERMINÉ

**But** : partir d'une base qui compile et dont les tests passent, pour distinguer plus tard une vraie
régression d'un problème d'environnement.

### Étapes (déjà faites — à refaire seulement sur une nouvelle machine)

1. **Installer Git for Windows** (fournit *Git Bash*, `openssl`, `keytool`) → https://git-scm.com/download/win
2. **Installer un JDK 17 ou 21** (Temurin) → https://adoptium.net
3. **Installer Docker Desktop** et **le lancer** (icône baleine dans la barre des tâches, état « Engine running »).
   Nécessaire uniquement pour Keycloak en P4.
4. **Installer IntelliJ IDEA Community** → https://www.jetbrains.com/idea/download
5. **Ouvrir Git Bash à la racine du projet** (le dossier qui contient `pom.xml`) et lancer les contrôles :

```bash
java -version          # 17 ou 21
./mvnw -v              # Maven 3.9.x
docker version         # client ET serveur répondent (Docker Desktop démarré)
git --version
openssl version
./mvnw -B clean verify # BUILD SUCCESS, tests verts
```

6. Si `java` n'est pas trouvé dans Git Bash :

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
```

7. **Ouvrir le projet dans IntelliJ** : `File → Open…` → sélectionner **`pom.xml`** → *Open as Project* →
   attendre la fin de l'import Maven (barre de progression en bas à droite).
   Dans `Settings → Build, Execution, Deployment → Build Tools → Maven`, vérifier que le JDK du projet
   est celui installé (pas « No SDK »).

### Critère de sortie

**✅** `BUILD SUCCESS` sur 5 modules, `docker version` répond, projet importé sans erreur rouge dans IntelliJ.
**⛔** Ne jamais enchaîner sur P4 avec un build rouge ou un Docker arrêté.

---

## P1 — Dépôt, branches et hygiène des secrets · ✅ TERMINÉ

**But** : garantir qu'aucun secret ne peut entrer dans l'historique, et que `main` n'accepte que des PR relues.

### Étapes (déjà faites)

1. **Dépôt distant** créé : `DevSecOps-ENSIAS/petclinic-security-automation`.
2. **`.gitignore`** enrichi **avant** toute génération de clé :

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

3. **Workflow hérité du projet amont retiré** (il fermait les PR automatiquement) :

```bash
git rm -f .github/workflows/check-pr-template.yml .github/PULL_REQUEST_TEMPLATE.md
```

4. **Branche `dev` créée** et poussée ; **B invité** : `Settings → Collaborators → Add people`.

⚠️ **Reste à faire (2 min)** : la règle `logs/` manque encore dans `.gitignore` et cinq journaux sont suivis
par Git (`logs/*.log`, issus de `dev-start.sh`). Les retirer sans les supprimer du disque :

```bash
git rm --cached logs/*.log
echo "logs/" >> .gitignore
git add .gitignore && git commit -m "chore: ignore les journaux générés"
```
5. **Protection de `main`** : `Settings → Branches → Add branch protection rule` → nom `main` →
   cocher *Require a pull request before merging*, *Require approvals: 1*, *Require status checks to pass*
   (choisir le job `build`), *Do not allow bypassing the above settings* → **Create**.
6. **Première PR** de démonstration fusionnée (prouve que le circuit fonctionne).

### Captures 📸 (`docs/preuves/p01-depot/`)

| Fichier | Quand | Quoi |
|---|---|---|
| `01-pr-mergee.png` | onglet *Pull requests*, filtre *Closed* | la PR avec le badge violet **Merged** |
| `02-branches.png` | page d'accueil du dépôt | sélecteur de branches montrant `main` et `dev` |
| `03-protection-main.png` | `Settings → Branches` | la règle de protection active |

### Critère de sortie

**✅** `git ls-files | grep -E "\.(pem|p12|key)$"` ne renvoie **rien** ; une PR ne peut pas être fusionnée sans revue.

---

## P2 — Modèle de menace et constats « avant » · ✅ TERMINÉ

**But** : produire l'« avant » du tableau avant/après du rapport et **justifier le Zero Trust** (chapitre 1).
Ce n'est pas un document décoratif : chaque ligne devient une exigence de P5/P6/P8.

### Les 5 constats à conserver (avec leur commande)

| Constat | Commande | Résultat « avant » |
|---|---|---|
| Aucune authentification | `curl -i http://localhost:8080/api/customer/owners` | `200` sans jeton |
| Contournement du gateway | `SERVER_PORT=8081 …` puis `curl -i http://localhost:8081/owners` | `200` en direct |
| Trafic interne en clair | `curl -s http://localhost:8761/eureka/apps \| grep -o 'http://[^<]*'` | instances en `http://` |
| Annuaire Eureka ouvert | `curl -i http://localhost:8761/eureka/apps` | `200` sans identifiants |
| Actuator exposé | `curl http://localhost:8080/actuator/env` | JSON complet (variables d'environnement !) |

⚠️ **Ces constats ne sont plus reproductibles sur `HEAD`** : P5, P6 et P8 ont fermé ces portes — c'est le but.
Pour les rejouer (par exemple pour refaire une capture), utiliser l'état d'origine :

```bash
git stash list                                  # sécurité : rien en cours
git worktree add ../petclinic-avant e5e1b8d      # ancien état, dossier séparé
cd ../petclinic-avant && ./mvnw -B -DskipTests package
./scripts/run_all.sh
# … refaire les 5 curl ci-dessus, puis :
cd - && git worktree remove ../petclinic-avant
```

### Étapes (déjà faites)

1. Tableau **STRIDE** réduit aux flux utiles : client → gateway, gateway → services, services → Eureka.
2. Cotation **DREAD** des menaces retenues (F1 à F9), avec la priorité qui en découle.
3. Rédaction de `SECURITE-modele-de-menace.md` (racine du dépôt).
4. **5 captures « avant »** prises au moment où les portes étaient encore ouvertes.

### Captures 📸 (`docs/preuves/p02-menace/`)

`01-constats-code.png` (les commandes et leurs sorties) · `02-actuator-env.png` (JSON, barre d'URL visible) ·
`03-contournement-gateway.png` (accès direct au port 8081) · `04-eureka-ouvert.png` (console sans connexion) ·
`05-grafana-anonyme.png` (Grafana sans authentification, hors périmètre mais constaté).

### Ce qui reste à en faire

Rien à coder. Dans le rapport (P10, chapitre 1), **résumer** : garder les constats et la cotation,
supprimer le détail des menaces non traitées. C'est la seule « remise à niveau » demandée.

### Critère de sortie

**✅** Les 5 constats sont capturés, le tableau STRIDE et la cotation DREAD existent, et chaque constat
est relié à la phase qui le corrige (F4-a → P8, F1-a → P5, F7 → P6, F6 → P5+P6).

---

## P3 — PKI avec OpenSSL (CA + certificats) · 🟨 PARTIEL

**But** : disposer d'une autorité de certification de projet et de certificats pour les 4 applications,
plus un certificat volontairement **non autorisé** (`rogue-client`) qui servira à prouver le refus en P6.

**Ce qui existe déjà** : `scripts/pki/generate.sh` génère la CA (`PetClinic-CA`), les certificats serveur
`customers`, `vets`, `visits`, le certificat client `api-gateway` et le `truststore.p12`.
**Ce qui manque** : le certificat `rogue-client` et sa vérification dans la sortie du script.

### 3.1 Ateliers de cryptographie (à garder pour le rapport, chapitre 5)

```bash
mkdir -p secrets/tp1 && cd secrets/tp1
openssl genrsa -out laClePv.pem 2048
openssl rsa -in laClePv.pem -pubout -out laClePu.pem
echo "message secret" > data.txt
openssl pkeyutl -encrypt -pubin -inkey laClePu.pem -in data.txt -out dataChiffre.bin
openssl pkeyutl -decrypt -inkey laClePv.pem -in dataChiffre.bin -out dataClair.txt && cat dataClair.txt
openssl dgst -sha256 -sign laClePv.pem -out signature.sig data.txt
openssl dgst -sha256 -verify laClePu.pem -signature signature.sig data.txt   # Verified OK
cp data.txt data2.txt && echo "x" >> data2.txt
openssl dgst -sha256 -verify laClePu.pem -signature signature.sig data2.txt  # Verification Failure
cd ../..
```

📸 Les deux sorties (OK puis Failure) : elles prouvent intégrité et authenticité dans le rapport.

### 3.2 Ajouter `rogue-client` (≈ 30 min) — **à faire**

Ouvrir `scripts/pki/generate.sh` et vérifier que la boucle d'émission contient bien les **cinq** lignes :

```bash
for svc in customers vets visits; do
  issue "$svc" serverAuth "DNS:localhost,DNS:$svc-service,IP:127.0.0.1"
done
issue api-gateway  clientAuth "DNS:localhost,DNS:api-gateway"
issue rogue-client clientAuth "DNS:localhost"        # ← LA ligne à ajouter
```

Puis, à la fin du script, la vérification doit couvrir les cinq certificats :

```bash
for f in customers vets visits api-gateway rogue-client; do openssl verify -CAfile ca.pem "$f.crt"; done
```

Exécuter :

```bash
export PKI_PASS='demo-local'
./scripts/pki/generate.sh
```

### Critère de sortie

**✅** Cinq lignes `…crt: OK`, et dans `secrets/` : `ca.pem`, `ca.key`, `customers.p12`, `vets.p12`,
`visits.p12`, `api-gateway.p12`, `rogue-client.p12`, `truststore.p12`.
**⛔** Un sujet `C=/C=MA/...` déformé sous Git Bash → `export MSYS_NO_PATHCONV=1` (déjà dans le script).
**⛔** `secrets/` ne doit **jamais** être committé : les fichiers se transmettent entre A et B hors Git,
et **une seule CA** doit servir pour la démonstration finale.

### Captures 📸 (`docs/preuves/p03-pki/`)

| Fichier | Commande / écran |
|---|---|
| `01-chaine.png` | `openssl x509 -in secrets/ca.pem -noout -text \| head -20` |
| `02-verify.png` | la sortie complète des cinq `openssl verify` |
| `03-keystore.png` | `keytool -list -keystore secrets/truststore.p12 -storepass "$PKI_PASS"` |

---

## P4 — Keycloak : émission de jetons OIDC · ✅ TERMINÉ le 10/11/2026 (0,5 j, 🅰️)

**But** : un émetteur de jetons **reproductible** — le realm est importé au démarrage, aucun clic n'est
nécessaire pour le recréer. C'est la brique qui rend P5 possible.

### Ce qui a été livré (PR #54, fusionnée dans `dev` le 10/11/2026)

| Élément | Fichier | Contenu |
|---|---|---|
| Compose | `docker-compose.security.yml` | Keycloak 26.0, `start-dev --import-realm`, port **8180**, admin par `KC_BOOTSTRAP_ADMIN_*` / `KC_ADMIN_PASS` |
| Realm | `keycloak/petclinic-realm.json` | realm `petclinic`, `accessTokenLifespan: 300`, rôles `staff`/`viewer`, client public `petclinic-cli`, utilisateurs `alice`/`bob`/`carol` |
| Preuves | `docs/preuves/p04-keycloak/` | `01-realm.png`, `02-users.png`, `03-roles-alice.png`, `04-jeton.png` |

Rejeu de la vérification (utile seulement sur une nouvelle machine ou après un `down`) :

```bash
docker compose -f docker-compose.security.yml up -d
docker compose -f docker-compose.security.yml logs -f keycloak   # attendre "started"
```

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/petclinic/protocol/openid-connect/token \
  -d grant_type=password -d client_id=petclinic-cli \
  -d username=alice -d password=alice-pass \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
echo "${TOKEN:0:30}..."      # doit afficher eyJ...
```

> Le realm n'est importé qu'à la **création** du conteneur : après toute modification de
> `keycloak/petclinic-realm.json`, faire `docker compose -f docker-compose.security.yml down`
> puis `up -d`, sinon l'ancien realm reste en base.

### 4.1 Créer le fichier Compose

Créer **`docker-compose.security.yml`** à la racine :

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

### 4.2 Créer le realm

Créer **`keycloak/petclinic-realm.json`** (mots de passe **de démonstration uniquement**) :
realm `petclinic`, `accessTokenLifespan: 300`, rôles `staff` et `viewer`, client public `petclinic-cli`
avec `directAccessGrantsEnabled: true`, et trois utilisateurs :

| Utilisateur | Mot de passe | Rôle | Sert à prouver |
|---|---|---|---|
| `alice` | `alice-pass` | `staff` | GET **et** écritures autorisés (201) |
| `bob` | `bob-pass` | `viewer` | GET 200, mais POST **403** |
| `carol` | `carol-pass` | aucun | **403** partout |

> Chaque utilisateur doit avoir `firstName`, `lastName` et `email` : depuis Keycloak 24, un compte
> incomplet provoque `Account is not fully set up` au moment de demander un jeton.

### 4.3 Démarrer et obtenir un jeton

```bash
docker compose -f docker-compose.security.yml up -d
docker compose -f docker-compose.security.yml logs -f keycloak   # attendre "started"
```

Puis (dans un autre terminal) :

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/petclinic/protocol/openid-connect/token \
  -d grant_type=password -d client_id=petclinic-cli \
  -d username=alice -d password=alice-pass \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
echo "${TOKEN:0:30}..."      # doit afficher eyJ...
```

Répéter pour `bob` et `carol` (`TOKEN_BOB`, `TOKEN_CAROL`) : ils serviront à toute la matrice de P5 et P9.

### 4.4 Vérifier dans la console (clic par clic)

1. Ouvrir **http://localhost:8180** → *Administration Console*.
2. Se connecter avec `admin` / `admin-dev-only` (ou la valeur de `KC_ADMIN_PASS`).
3. En haut à gauche, le sélecteur de realm doit afficher **petclinic** (sinon : *master* → choisir `petclinic`).
4. `Realm roles` (menu de gauche) → vérifier la présence de **staff** et **viewer**.
5. `Users` → `alice` → onglet **Role mapping** → `staff` doit être dans *Assigned roles*.
6. `Clients` → `petclinic-cli` → onglet **Settings** → *Direct access grants* = **On**, *Client authentication* = **Off**.
7. Coller un jeton **de test** sur https://jwt.io : le bloc `realm_access.roles` doit contenir le rôle attendu.

### Captures 📸 (`docs/preuves/p04-keycloak/`)

| Fichier | Écran |
|---|---|
| `01-realm.png` | console, realm `petclinic` sélectionné + liste des rôles |
| `02-users.png` | `Users` avec alice / bob / carol |
| `03-roles-alice.png` | `alice` → *Role mapping* → `staff` assigné |
| `04-jeton.png` | jwt.io, claims `sub`, `iss`, `realm_access.roles` (jeton **de test**) |

### Critère de sortie

**✅** `echo "${TOKEN:0:30}..."` affiche `eyJ…` ; les rôles apparaissent dans `realm_access.roles`.
**⛔** `Account is not fully set up` → prénom/nom/e-mail manquants.
**⛔** `invalid_grant` → mauvais mot de passe **ou** *Direct access grants* désactivé sur le client.
**⛔** Un seul utilisateur ne suffit pas : sans `bob` et `carol`, la matrice RBAC n'est pas démontrable.
**⛔** Toujours utiliser `localhost` (jamais `127.0.0.1`) pour obtenir le jeton **et** dans `issuer-uri`,
sinon P5 renverra 401 avec un jeton pourtant valide.

---

## P5 — Module `spring-petclinic-security` : JWT + RBAC dans **chaque** service · ✅ TERMINÉ le 11/11/2026 (1,5 j, 🅰️)

### Ce qui a été livré (PR #61 et #63, fusionnées dans `dev` le 11/11/2026)

| Élément | Fichier | Contenu |
|---|---|---|
| Module | `spring-petclinic-security/` | `KeycloakRoles`, `ServletSecurityConfiguration`, `ServiceIdentityFilter` ; jar **non repackagé** pour rester utilisable comme dépendance |
| Services | `customers`, `vets`, `visits` | dépendance au module, `@Import(ServletSecurityConfiguration.class)`, `issuer-uri` dans `application.yml` |
| Gateway | `spring-petclinic-api-gateway` | `SecurityConfig` réécrit en `ServerHttpSecurity` : `GET /api/**` → `viewer`/`staff`, reste de `/api/**` → `staff`, `anyExchange().denyAll()` |
| Tests | module + gateway | 16 tests du module (couverture 100 % des lignes), 48 tests au total, `./mvnw -B -P quality verify` vert |
| Analyse | `pom.xml` racine | exemption Sonar `java:S4502` limitée aux deux `SecurityConfig` (API stateless à jeton, cf. commentaire dans le code) |
| Preuves | `docs/preuves/p05-jwt-rbac/` | `01-matrice.png`, `02-servlet-security.png`, `03-gateway-security.png`, `04-jwt-claims.png` |

Rejeu de la vérification (pile démarrée, cf. §5.5) :

```bash
tok() { curl -s -X POST http://localhost:8180/realms/petclinic/protocol/openid-connect/token \
  -d grant_type=password -d client_id=petclinic-cli -d username=$1 -d password=$2 \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/'; }

TOKEN=$(tok alice alice-pass); TOKEN_BOB=$(tok bob bob-pass); TOKEN_CAROL=$(tok carol carol-pass)
echo "alice=${#TOKEN} bob=${#TOKEN_BOB} carol=${#TOKEN_CAROL}"   # ≈ 1085, 1075, 1035
```

> Trois valeurs vides (`0`) expliquent **tous** les `401` d'une matrice : les variables sont propres à
> chaque terminal. Vérifier les longueurs **avant** de conclure à un problème de jeton.

Deux points appris en chemin :

- **Nom du starter** : `spring-boot-starter-oauth2-resource-server` fonctionne en Boot 4.0.9 et c'est
  celui du gateway ; inutile de chercher un nom « security- ».
- **Tests** : les quatre tests métier gardent `@AutoConfigureMockMvc(addFilters = false)`, mais le test
  du controller du gateway s'authentifie avec un vrai jeton (`ReactiveJwtDecoder` simulé) : les règles
  RBAC y sont donc réellement exercées, et `mockJwt()` n'y prend pas effet.

**But** : c'est le « module d'authentification/autorisation » exigé par l'énoncé (E1, E2). Un module Maven
unique, réutilisé par les 3 services **et** le gateway, qui vérifie le JWT et applique le RBAC — localement,
sans appeler Keycloak à chaque requête.

> Ce qui existe déjà : le gateway sait valider un jeton (`SecurityConfig` + `SecurityConfigTest`, phase 6).
> Ce qui change : les **3 services** doivent aussi valider, et les GET ne sont plus publics.

### 5.1 Créer le module (10 min)

1. `pom.xml` **racine** : ajouter le module **en premier** dans `<modules>` :

```xml
<module>spring-petclinic-security</module>
```

2. Créer `spring-petclinic-security/pom.xml` avec le parent `spring-petclinic-microservices` (4.0.1),
   `artifactId` = `spring-petclinic-security`, et deux dépendances :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <scope>provided</scope>
</dependency>
```

> Le nom retenu est `spring-boot-starter-oauth2-resource-server` : il résout en Boot 4.0.9 et c'est
> celui qu'utilise déjà le gateway. Si Maven ne résout pas `jakarta.servlet-api`, le remplacer par
> `spring-boot-starter-webmvc` en scope `provided`.

### 5.2 Trois classes (package `org.springframework.samples.petclinic.security`)

- **`KeycloakRoles`** — traduit le claim `realm_access.roles` en autorités Spring `ROLE_xxx`.
  Utilisée par le gateway **et** les services : c'est ce qui rend la matrice RBAC identique partout.
- **`ServletSecurityConfiguration`** — pour les services MVC :
  `csrf` désactivé, `STATELESS`, `denyAll()` par défaut,
  `GET /**` → `hasAnyRole("viewer","staff")`,
  `POST/PUT/PATCH/DELETE` → `hasRole("staff")`,
  `/actuator/health/**` → `permitAll()`,
  et branchement du filtre d'identité de service **avant** `BearerTokenAuthenticationFilter`.
- **`ServiceIdentityFilter`** — propriété de **B**, mais la classe doit exister pour compiler :
  lit le CN du certificat client, `401` si aucun certificat et `mtls-required=true`,
  `403` si le CN n'est pas dans `allowed-callers`, laisse passer sinon.

Le code exact des trois classes est dans **`GuideV2!!!.md` §5.2** : le recopier tel quel plutôt que de le réécrire.

### 5.3 Brancher les 3 services

Pour **chacun** de `customers`, `visits`, `vets` :

1. `pom.xml` — ajouter la dépendance au module :

```xml
<dependency>
    <!-- groupId du parent, PAS celui des services (…petclinic.client) -->
    <groupId>org.springframework.samples</groupId>
    <artifactId>spring-petclinic-security</artifactId>
    <version>${project.version}</version>
</dependency>
```

2. Classe `…ServiceApplication` — ajouter au-dessus de la classe :

```java
@Import(org.springframework.samples.petclinic.security.ServletSecurityConfiguration.class)
```

3. `application.yml` — **obligatoire**, sinon le service refuse de démarrer (aucun `JwtDecoder`) :

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8180/realms/petclinic}
```

4. **Tests métier existants** (`OwnerResourceTest`, `PetResourceTest`, `VetResourceTest`, `VisitResourceTest`) :
   ils testent la logique, pas la sécurité, et vont échouer en 401/403.
   Ajouter sur la classe de test : `@AutoConfigureMockMvc(addFilters = false)`.
   *(Les tests **de sécurité** de P9, eux, gardent les filtres actifs — c'est tout l'intérêt.)*

```bash
./mvnw -B -DskipTests package     # doit passer avant d'aller plus loin
```

### 5.4 Brancher le gateway (WebFlux)

1. `spring-petclinic-api-gateway/pom.xml` : dépendance au module `spring-petclinic-security`
   (pour `KeycloakRoles`) **et** au starter `spring-boot-starter-security-oauth2-resource-server`.
2. `application.yml` du gateway : le même bloc `security.oauth2.resourceserver.jwt.issuer-uri` qu'au §5.3.
3. Réécrire `…/api/config/SecurityConfig.java` selon **`GuideV2!!!.md` §5.4** (`ServerHttpSecurity` →
   `SecurityWebFilterChain`) : `/actuator/health/**` et `/fallback` publics, `GET /api/**` → `viewer` ou `staff`,
   le reste de `/api/**` → `staff`, `GET /**` public (fichiers statiques du front), `anyExchange().denyAll()`.
4. Le gateway **transmet** l'en-tête `Authorization` : chaque service revalide. C'est la défense en profondeur.

### 5.5 Test de la phase

Démarrer dans l'ordre : Keycloak, `discovery-server`, les 3 services, le gateway
(`./scripts/dev-start.sh` ou `./scripts/run_all.sh`, qui exportent déjà les identifiants Eureka).

```bash
# sans jeton → 401
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/vet/vets
# carol (aucun rôle) → 403
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN_CAROL" http://localhost:8080/api/vet/vets
# bob (viewer) en lecture → 200
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN_BOB" http://localhost:8080/api/vet/vets
# bob en écriture → 403
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/customer/owners \
  -H "Authorization: Bearer $TOKEN_BOB" -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","address":"1 rue X","city":"Rabat","telephone":"0612345678"}'
# alice (staff) en écriture → 201
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/customer/owners \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","address":"1 rue X","city":"Rabat","telephone":"0612345678"}'
```

**Résultat attendu** : `401` · `403` · `200` · `403` · `201`.

**Bonus résilience (§5.6)** : dans le `application.yml` du gateway, le filtre `Retry` doit avoir
`methods: GET` et **non** `POST` — rejouer un POST peut créer deux fois la ressource.

### Captures 📸 (`docs/preuves/p05-jwt-rbac/`)

| Fichier | Quoi |
|---|---|
| `01-matrice.png` | les cinq codes dans l'ordre 401/403/200/403/201 |
| `02-servlet-security.png` | extrait de `ServletSecurityConfiguration` (règles + `denyAll`) |
| `03-gateway-security.png` | extrait de `SecurityConfig` du gateway |
| `04-jwt-claims.png` | jeton décodé montrant `realm_access.roles` |

### Critère de sortie

**✅** La matrice du §3 du guide v2 est respectée, `./mvnw -B verify` est vert.
**⛔** `401` avec un jeton valide → `iss` différent de `issuer-uri` (utiliser `localhost` partout).
**⛔** `ConnectException` au démarrage → Keycloak n'est pas lancé (l'émetteur est contacté au premier appel).
**⛔** Le front Vue ne peut plus lire les données : c'est **voulu** (Zero Trust). La démo passe par `curl`.

---

## P6 — mTLS strict + autorisation par identité de service · 🟨 PARTIEL (3 j, 🅱️)

**But** : prouver **quel service appelle** (mTLS) puis **si ce service a le droit** (liste blanche de CN).
C'est la partie « droits du service » du Zero Trust, distincte de l'identité de l'utilisateur (P5).

**Ce qui existe déjà** : profils `tls` dans les 4 applications, HTTPS, mTLS `client-auth: want`,
gateway faisant confiance à la CA, Eureka enregistré par nom d'hôte avec port sécurisé.

**État vérifié dans le code le 14/11/2026** :

| Point | État |
|---|---|
| Profil `tls` + keystore | ✅ dans les 4 applications |
| `client-auth: want` | ✅ sur les trois services — `need` à passer en **§6.4** |
| Confiance en la CA côté gateway | ✅ `trusted-x509-certificates: …/ca.pem` |
| `ServiceIdentityFilter` | ✅ livré avec P5 et branché dans les trois services, avant `BearerTokenAuthenticationFilter` |
| `CLIENT_AUTH`, `MTLS_REQUIRED`, `allowed-callers` | ❌ absents de tous les `application.yml` : le filtre tourne avec ses valeurs par défaut (`api-gateway`, `mtls-required=false`) — **à faire au §6.1** |
| Certificat `rogue-client` | ❌ `scripts/pki/` ne contient que `generate.sh`, `secrets/` est vide — à générer avec **`scripts/pki/generate.sh`** (cf. §6.4) |
| Preuves `docs/preuves/p06-mtls/` | ❌ dossier absent — les 4 captures de la fin de **§6.4** |

> Conséquence : le palier 3 n'est **pas démontrable** aujourd'hui. `mtls-required` vaut `false` et la liste
> blanche n'est pas configurée — le §6.1 est donc le premier travail à faire, pas une option.

### 6.0 Les cinq pièges de ce dépôt (déjà rencontrés)

1. Les services écoutent sur un port aléatoire (`port: 0`) → fixer 8081/8082/8083 via `SERVER_PORT`.
2. Eureka enregistre par **IP** (`prefer-ip-address: true`) → un certificat sans SAN IP échoue.
   D'où `prefer-ip-address: false` + `hostname` dans le profil `tls`.
3. Pour que `lb://` produise du `https://` : `secure-port-enabled: true` et `non-secure-port-enabled: false`.
4. `vets` active le profil `production` (cache) dans son YAML ; `SPRING_PROFILES_ACTIVE` le **remplace** →
   lancer vets avec `production,tls`.
5. `PKI_DIR` doit être un **chemin absolu avec des `/`** (`C:/Users/.../secrets`) : le répertoire de travail
   d'IntelliJ n'est pas toujours la racine du projet.

### 6.1 Rendre la rigueur mTLS paramétrable (≈ 20 min) — **à faire**

Dans le profil `tls` des trois services, remplacer la valeur figée :

```yaml
    client-auth: want                    # AVANT
    client-auth: ${CLIENT_AUTH:need}     # APRÈS
```

et ajouter le bloc de politique de service (utilisé par `ServiceIdentityFilter` de P5) :

```yaml
petclinic:
  security:
    mtls-required: ${MTLS_REQUIRED:true}
    allowed-callers: api-gateway        # CN autorisés, séparés par des virgules
```

Ainsi un seul profil sert aux trois paliers, sans réécrire le YAML : `CLIENT_AUTH=none MTLS_REQUIRED=false`
(palier 1), `want`/`false` (palier 2), valeurs par défaut (palier 3).

### 6.2 Palier 1 — HTTPS côté serveur (déjà fait, à rejouer pour les captures)

```bash
export PKI_PASS='demo-local' PKI_DIR="$(pwd)/secrets"
./mvnw -B -DskipTests package

CLIENT_AUTH=none MTLS_REQUIRED=false SPRING_PROFILES_ACTIVE=tls SERVER_PORT=8081 \
  java -jar spring-petclinic-customers-service/target/spring-petclinic-customers-service-4.0.1.jar
```

```bash
curl --cacert secrets/ca.pem -i https://localhost:8081/owners   # 401 (TLS ok, jeton absent)
curl -i https://localhost:8081/owners                           # échec : certificat non reconnu
curl -i http://localhost:8081/owners                            # échec : le port parle TLS
```

Puis, dans le navigateur : ouvrir `https://localhost:8081` → avertissement de certificat (capture `01`),
puis importer `secrets/ca.pem` dans les autorités de confiance de Windows → recharger → cadenas valide (capture `02`).

### 6.3 Palier 2 — le gateway fait confiance à la CA (déjà fait)

Le bloc de profil `tls` du gateway contient `trusted-x509-certificates: ${PKI_DIR:secrets}/ca.pem`
et le keystore `api-gateway.p12`. Lancer toute la pile avec `tls` et vérifier :

```bash
curl -i http://localhost:8761/eureka/apps -u eureka:"$EUREKA_PASSWORD" | grep -o 'https\?://[^<]*' | head
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/customer/owners    # 200
```

Dans http://localhost:8761, les instances doivent apparaître en **`https`** avec un **nom d'hôte**.

### 6.4 Palier 3 — mTLS strict + liste blanche (le cœur du travail restant)

Relancer les services **sans** `CLIENT_AUTH` ni `MTLS_REQUIRED` (donc `need` / `true`), puis les cinq tests :

```bash
# 1. Aucun certificat client → handshake refusé
curl --cacert secrets/ca.pem -i https://localhost:8081/owners

# 2. Certificat du gateway, sans jeton → TLS accepté, JWT absent → 401
curl --cacert secrets/ca.pem --cert secrets/api-gateway.crt --key secrets/api-gateway.key \
  -i https://localhost:8081/owners

# 3. Certificat du gateway + jeton alice → 200
curl --cacert secrets/ca.pem --cert secrets/api-gateway.crt --key secrets/api-gateway.key \
  -H "Authorization: Bearer $TOKEN" -i https://localhost:8081/owners

# 4. Certificat « intrus » (CA valide, CN non autorisé) + jeton alice → 403
curl --cacert secrets/ca.pem --cert secrets/rogue-client.crt --key secrets/rogue-client.key \
  -H "Authorization: Bearer $TOKEN" -i https://localhost:8081/owners

# 5. Via le gateway → 200
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/customer/owners
```

**Résultat attendu** : échec de handshake · `401` · `200` · `403` · `200`.
Les cas 3 et 4 sont **la** démonstration à montrer au jury : mTLS **authentifie** le service,
la liste blanche de CN l'**autorise**.

### Captures 📸 (`docs/preuves/p06-mtls/`)

| Fichier | Quoi |
|---|---|
| `01-tls.png` | `curl --cacert` qui répond, puis échec sans la CA |
| `02-mtls.png` | les cinq commandes du palier 3 avec leurs codes |
| `03-eureka-https.png` | console Eureka, instances en `https` avec nom d'hôte |
| `04-rogue-403.png` | le cas 4 : CN refusé, journal `Appelant refuse : CN=rogue-client` |

### Critère de sortie

**✅** Les 5 codes du palier 3 sont conformes, et le journal du service affiche le refus de `rogue-client`.
**⛔** `PKIX path building failed` → la CA n'est pas dans le truststore du client qui échoue.
**⛔** `403` alors que le certificat du gateway est présenté → `allowed-callers` ne contient pas `api-gateway`.
**⛔** `handshake_failure` → certificat client absent ou mauvaise CA.
**⛔** Si le palier 3 bloque plus d'une journée : le documenter comme limite (l'agrégation reste en TLS simple)
plutôt que d'y passer le week-end.

---

## P7 — Scénario `dossier` à 3 microservices · ⬜ À FAIRE (1 j, 🅰️ gateway + 🅱️ client mTLS)

**État vérifié dans le code le 14/11/2026** — rien du scénario n'est encore écrit :

- pas de `MtlsHttpClientFactory`, pas de `VetsServiceClient`, pas de `DossierController` ;
- **pas de `ServerBearerExchangeFilterFunction`** dans `ApiGatewayApplication`. C'est le point bloquant :
  le gateway n'appelle pas les services avec le jeton de l'utilisateur, donc l'agrégation existante
  `GET /api/gateway/owners/{id}` renvoie `500` même avec un jeton valide, alors que les routes proxy
  (`/api/vet/**`, `/api/customer/**`) fonctionnent car Spring Cloud Gateway recopie les en-têtes ;
- preuves `docs/preuves/p07-dossier/` : ❌ absentes.

> Ordre de travail conseillé : le filtre `ServerBearerExchangeFilterFunction` (point 3 du §7.1) **avant**
> `MtlsHttpClientFactory`. Sans lui, le §7.3 ne peut pas afficher de `200`.

**But** : l'exigence E3 de l'énoncé — une requête cliente fait collaborer **les trois** services métier.
Le `GET /api/gateway/owners/{id}/dossier` interroge `customers` (propriétaire + animaux), `visits` (visites)
**et** `vets`, puis assemble la réponse. Chaque appel porte **le certificat du gateway** (identité du service)
et **le jeton de l'utilisateur** (identité et rôles).

### 7.1 Client mTLS du gateway (🅱️, ~2 h)

1. Créer `…/api/config/MtlsHttpClientFactory.java` (code dans `GuideV2!!!.md` §7.1) : construit un
   `HttpClient` Reactor qui présente `api-gateway.p12` et fait confiance à `truststore.p12` ;
   si la propriété `petclinic.mtls.key-store` est vide, il renvoie un client standard (utile hors TLS).
2. Ajouter au `application.yml` du gateway, dans le profil `tls` :

```yaml
petclinic:
  mtls:
    key-store: ${PKI_DIR:secrets}/api-gateway.p12
    trust-store: ${PKI_DIR:secrets}/truststore.p12
    password: ${PKI_PASS}
```

3. Dans `ApiGatewayApplication`, **remplacer** le bean `loadBalancedWebClientBuilder` par la version qui
   utilise `MtlsHttpClientFactory` **et** le filtre `ServerBearerExchangeFilterFunction` — c'est ce filtre
   qui relaie le jeton de l'utilisateur vers les services (sinon les services répondent 401).

### 7.2 Endpoint du scénario (🅰️, ~3 h)

1. `…/api/application/VetsServiceClient.java` — `WebClient` `@LoadBalanced` appelant `http://vets-service/vets`.
2. `…/api/dto/OwnerDossier.java` — `record OwnerDossier(OwnerDetails owner, List<Map<String,Object>> vets)`.
3. `…/api/boundary/web/DossierController.java` — **classe distincte** (sinon `ApiGatewayControllerTest` casse) :

```java
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

L'agrégation existante (`ApiGatewayController.getOwnerDetails`) appelle déjà `customers` **et** `visits` :
en ajoutant `vets`, la requête fait collaborer les trois services.

### 7.3 Vérification

```bash
curl -i -H "Authorization: Bearer $TOKEN"       http://localhost:8080/api/gateway/owners/1/dossier  # 200
curl -i                                          http://localhost:8080/api/gateway/owners/1/dossier  # 401
curl -i -H "Authorization: Bearer $TOKEN_CAROL"  http://localhost:8080/api/gateway/owners/1/dossier  # 403
```

Contrôler aussi **les journaux des trois services** : l'appel doit arriver en HTTPS avec le CN `api-gateway`.

### Captures 📸 (`docs/preuves/p07-dossier/`)

| Fichier | Quoi |
|---|---|
| `01-dossier-200.png` | la réponse JSON complète (owner + animaux + visites + vets) |
| `02-sans-jeton.png` | `401` sur le même appel |
| `03-carol-403.png` | `403` avec `carol` |
| `04-journaux.png` | les trois services recevant l'appel (CN `api-gateway`) |
| `05-sequence.png` | diagramme de séquence client → gateway → customers/visits/vets (schéma central du rapport) |

### Critère de sortie

**✅** `200` avec les données des **trois** services, `401` sans jeton, `403` avec `carol`.
**⛔** `200` mais `vets` vide → la route `vets` n'est pas en `https` (revoir P6.3).
**⛔** Tests du gateway cassés → la nouvelle classe `DossierController` doit rester séparée de
`ApiGatewayController`, et les tests existants doivent recevoir un `mockJwt()`.

---

## P8 — Durcissement (Actuator + Eureka) · ✅ TERMINÉ le 10/11/2026

**But** : fermer les deux portes qui permettaient de lire l'infrastructure sans s'authentifier.
Seul ce qui renforce l'authentification interservices a été fait ; Grafana, ZAP et la publication des ports
Docker sont hors périmètre v2.

### Ce qui a été fait (à reproduire seulement sur une nouvelle machine)

**1. Actuator réduit** — sur les **cinq** applications (gateway, customers, vets, visits, discovery),
le bloc `management:` contient désormais :

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

(`discovery-server` n'avait aucun bloc `management:` : il a été ajouté.)

**2. Annuaire Eureka protégé** — `spring-boot-starter-security` ajouté au `discovery-server`, plus la classe
`EurekaSecurityConfig` : CSRF ignoré sur `/eureka/**` (les clients Eureka n'envoient pas de jeton CSRF),
`/actuator/health` et `/actuator/info` publics (les scripts de démarrage attendent ce endpoint),
`anyRequest().authenticated()` + HTTP Basic.

**3. Identifiants** — `spring.security.user.name/password` lus depuis `EUREKA_USER` / `EUREKA_PASSWORD`
(valeurs par défaut `eureka` / `eureka-pass`), et `defaultZone` de chaque client devenu :

```yaml
defaultZone: ${EUREKA_URL:http://${EUREKA_USER:eureka}:${EUREKA_PASSWORD:eureka-pass}@localhost:8761/eureka/}
```

(et la même chose dans le bloc `on-profile: docker`). `scripts/dev-start.sh` et `scripts/run_all.sh`
exportent ces deux variables pour que **tous** les processus en héritent.

### Vérification (rejouable en 2 minutes)

```bash
./mvnw -B -DskipTests package
EUREKA_USER=eureka EUREKA_PASSWORD=eureka-pass \
  java -jar spring-petclinic-discovery-server/target/spring-petclinic-discovery-server-4.0.1.jar
```

Puis, dans un second terminal :

| Commande | Attendu | Mesuré le 10/11 |
|---|---|---|
| `curl -i http://localhost:8761/eureka/apps` | refusé | **401** |
| `curl -i -u eureka:eureka-pass http://localhost:8761/eureka/apps` | autorisé | **200** |
| `curl -i -u eureka:faux http://localhost:8761/eureka/apps` | refusé | **401** |
| `curl -i http://localhost:8761/` (console) | refusé | **401** |
| `curl -i http://localhost:8761/actuator/health` | public | **200** |
| `curl -i -u eureka:eureka-pass http://localhost:8761/actuator/env` | masqué | **404** |

Puis démarrer `customers` et vérifier qu'il **s'enregistre toujours** :
`registration status: 204` dans son log, et dans le log du discovery
`Registered instance CUSTOMERS-SERVICE/… with status UP`.
`curl -i http://localhost:8081/actuator/env` → **404**, `/actuator/health` → **200**.

**Non-régression** : `./mvnw -B verify` → `BUILD SUCCESS` (32 tests, 0 échec).

### Captures 📸 (`docs/preuves/p08-durcissement/`) — **à prendre**

| Fichier | Quoi |
|---|---|
| `01-actuator.png` | `actuator/env` = 404 à côté de `actuator/health` = 200 |
| `02-eureka-auth.png` | `eureka/apps` 401 sans identifiants, 200 avec |
| `03-services-up.png` | le service toujours `UP` dans la console Eureka |

### Pièges et limite à assumer

**⛔** Les services disparaissent d'Eureka → `EUREKA_PASSWORD` non défini pour **un seul** processus,
ou CSRF non exclu de `/eureka/**`.
**⛔** Conséquence assumée : Prometheus/Grafana (hors périmètre) ne peuvent plus scraper `/actuator/prometheus`.
**À écrire dans le rapport** : HTTP Basic sur l'annuaire reste une protection **faible** (identifiants statiques,
sans rotation, sans TLS en local) ; l'idéal serait mTLS vers Eureka.

---

## P9 — Tests automatisés et script de contrôle · ⬜ À FAIRE (1 j, 🅰️ + 🅱️)

**État vérifié dans le code le 14/11/2026** :

| Élément attendu | État |
|---|---|
| `scripts/security-smoke.sh` | ❌ absent (`scripts/` = `dev-start`, `dev-stop`, `run_all`, `stop_all`, `pushImages`, `tagImages`, `pki`) |
| Tests JUnit de sécurité dans les services | ❌ aucun ; seuls existent les 16 tests du module `spring-petclinic-security` et les 14 du gateway |
| `./mvnw -B -P quality verify` | ✅ vert, 48 tests — mais rien ne prouve le RBAC **via HTTP réel** |
| Preuves `docs/preuves/p09-tests/` | ❌ dossier absent |

> La dépendance `spring-boot-starter-security-test` existe bien dans le BOM 4.0.9 (vérifié) : le §9.2
> est applicable tel quel. P9 dépend de P7 (les trois lignes « dossier » du smoke) et du palier 3 de P6
> (les quatre lignes mTLS) : à planifier après eux, sinon le script restera partiellement `SKIP`.

**But** : transformer la matrice de vérification en **preuve reproductible**, utilisable en démonstration
et par le correcteur.

### 9.1 `scripts/security-smoke.sh`

Créer le script (contenu complet dans `GuideV2!!!.md` §9.1). Il vérifie douze points et **sort en code non nul**
si une ligne échoue :

| Groupe | Vérifications |
|---|---|
| RBAC via le gateway | 401 sans jeton · 403 `carol` · 200 `bob` · 403 POST `bob` · 201 POST `alice` |
| Scénario P7 | `dossier` 200 avec `alice`, 401 sans jeton |
| Durcissement P8 | `/actuator/env` = 404 |
| mTLS direct | sans certificat → `000` · certificat gateway sans jeton → 401 · avec jeton → 200 · `rogue-client` → **403** |

```bash
chmod +x scripts/security-smoke.sh
export PKI_DIR="$(pwd)/secrets"
./scripts/security-smoke.sh; echo "code de sortie : $?"
```

Lecture du résultat : chaque ligne `OK ...` est une preuve ; toute ligne `FAIL` indique la valeur attendue
et la valeur obtenue. `000` signifie « pas de réponse HTTP » : c'est le refus de handshake, donc un succès du test.

### 9.2 Tests JUnit de sécurité (au moins un par service)

Ajouter `spring-boot-starter-security-test` en scope `test` (Spring Boot 4), puis, dans un
`@SpringBootTest` + `@AutoConfigureMockMvc` **avec filtres actifs** (contrairement aux tests métier de P5.3) :

```java
mockMvc.perform(get("/owners")).andExpect(status().isUnauthorized());
mockMvc.perform(get("/owners").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_viewer"))))
       .andExpect(status().isOk());
mockMvc.perform(post("/owners").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_viewer")))
       .contentType(APPLICATION_JSON).content(json)).andExpect(status().isForbidden());
```

> Ne jamais réutiliser un test `addFilters = false` pour « prouver » la sécurité : il ne teste rien des filtres.

### Captures 📸 (`docs/preuves/p09-tests/`)

| Fichier | Quoi |
|---|---|
| `01-smoke.png` | le script entièrement `OK` (les 12 lignes visibles) |
| `02-mvn-test.png` | `Tests run: …` + `BUILD SUCCESS` |
| `03-junit-securite.png` | extrait d'un test de sécurité (401/200/403) |

### Critère de sortie

**✅** `./mvnw -B test` vert **et** `security-smoke.sh` sans aucun `FAIL` (code de sortie 0).

---

## P10 — Rapport · ⬜ À FAIRE (17 → 22/11, 🅰️ + 🅱️)

**But** : livrable 1 de l'énoncé (E5). **Neuf chapitres**, exactement ceux qui sont demandés — pas les
douze chapitres de l'ancien plan.

| # | Chapitre | Contenu | Rédige |
|---|---|---|---|
| 1 | Introduction et problématique | authentification **vs** autorisation, Zero Trust, constats « avant » de P2 | A |
| 2 | **Solutions existantes** | mTLS, JWT, OAuth2/OIDC, API keys, service mesh/SPIFFE ; RBAC, ABAC, PBAC, ReBAC ; produits (Keycloak, Spring Authorization Server, Auth0, Cognito, Istio, SPIRE, OPA, Cerbos, OpenFGA, SpiceDB) | A + B |
| 3 | **Critères de décision et comparaison** | les tableaux de `GuideV2!!!.md` §8.3, **vérifiés, complétés et sourcés** | A + B |
| 4 | **Architecture du module** | schéma de P6/P7, diagramme de séquence du `dossier`, découpage `spring-petclinic-security` | A |
| 5 | **Fonctionnement des composants** | Keycloak, validation JWT, filtre d'identité de service, PKI, gateway, Eureka | A (identité) / B (transport) |
| 6 | **Installation et configuration** | le pas-à-pas reproductible de P3 à P9 (c'est le cœur de « mise en place ») | B |
| 7 | **Validation** | tableaux 401/403/200, mTLS accepté/refusé, sortie du script | A + B |
| 8 | **Avantages et inconvénients** | performance (double validation), complexité, limites | A + B |
| 9 | Conclusion et perspectives | — | A + B |

### Méthode de travail (4 jours)

1. **Jour 1** : chapitres 1, 2, 3 (état de l'art et comparaisons) — ce sont les plus longs, commencer par eux.
2. **Jour 2** : chapitres 4 et 5 en s'appuyant sur les figures déjà capturées.
3. **Jour 3** : chapitre 6 (installation) en **rejouant** le pas-à-pas depuis un dossier propre : ce qui manque
   est un bug de la procédure, donc une correction à reporter dans le dépôt.
4. **Jour 4** : chapitre 7 (validation), 8, 9, références, relecture croisée.

### Règles de preuve

- Chaque affirmation de sécurité renvoie à **une figure ou une sortie de commande**.
- Figures **numérotées, légendées et citées** dans le texte.
- **Aucune capture** ne montre un mot de passe, un jeton réel ou une clé (flouter avec `Win + Maj + S` → éditeur).
- Tableau avant/après de `GuideV2!!!.md` §8.2 à reprendre tel quel : c'est le cœur du chapitre 7.
- Limites à assumer (§8.4) : front sans écran de connexion, Keycloak en mode dev, CA auto-signée sans révocation,
  jetons relayés sans contrôle d'audience, Eureka en Basic.

### Critère de sortie

**✅** Les 9 chapitres existent, chaque affirmation renvoie à une preuve, les deux noms et le nom du module
figurent en page de garde.

---

## P11 — Archive importable dans IntelliJ · ⬜ À FAIRE (22/11, 🅰️ + 🅱️)

**But** : livrable 2 (E6) — une archive Java que le correcteur ouvre et fait tourner **sans vous**.

### 11.1 Produire l'archive

```bash
git switch main && git pull
git archive --format=zip --prefix=petclinic-secure/ -o petclinic-secure-NOM1-NOM2.zip HEAD
```

`git archive` n'exporte que les fichiers **suivis** : ni `target/`, ni `secrets/`, ni `.idea/`.
Les modules hors build (config-server, genai, admin) sont exclus par les règles `export-ignore` de P1.

### 11.2 Ajouter `LISEZMOI-RENDU.md` et `scripts/run-secure.sh`

Le `LISEZMOI-RENDU.md` (à la racine, inclus dans le ZIP) doit contenir, dans cet ordre :

```bash
# 1. Importer dans IntelliJ : File > Open > pom.xml > Open as Project
# 2. Construire
./mvnw -B -DskipTests package
# 3. Certificats de démonstration
export PKI_PASS='demo-local'; ./scripts/pki/generate.sh
# 4. Keycloak (realm importé automatiquement)
docker compose -f docker-compose.security.yml up -d
# 5. Pile sécurisée + contrôles
export PKI_DIR="$(pwd)/secrets" EUREKA_PASSWORD='demo-eureka'
./scripts/run-secure.sh
./scripts/security-smoke.sh
```

Comptes : `alice` / `alice-pass` (staff), `bob` / `bob-pass` (viewer), `carol` / `carol-pass` (aucun rôle).

`scripts/run-secure.sh` (contenu dans `GuideV2!!!.md` §9.3) démarre : discovery, puis les 3 services en `tls`
(`production,tls` pour vets), puis le gateway — avec `PKI_PASS` et `EUREKA_PASSWORD` obligatoires.

### 11.3 Test à froid (obligatoire, ~1 h)

1. Décompresser le ZIP dans un **dossier vide** (pas dans le dépôt).
2. IntelliJ : `File → Open…` → sélectionner le `pom.xml` décompressé → *Open as Project* → attendre l'import.
3. Dans IntelliJ, créer **une configuration par module** : `Run → Edit Configurations… → + → Application`
   (ou *Spring Boot* en Ultimate) → classe principale `…ServiceApplication` → onglet *Environment variables* :

```
PKI_PASS=demo-local;PKI_DIR=C:/Users/<vous>/petclinic-secure/secrets;SERVER_PORT=8081;EUREKA_PASSWORD=demo-eureka
```

   (chemin **absolu**, avec des `/`). Pour `vets`, ajouter `SPRING_PROFILES_ACTIVE=tls` → en réalité
   `production,tls` (le profil `production` active le cache).
4. Dérouler le `LISEZMOI-RENDU.md` **tel quel**, sans rien compléter de mémoire.
5. Toute correction faite pendant ce test se fait **dans le dépôt**, jamais dans le ZIP ; on régénère l'archive ensuite.

### Critère de sortie

**✅** L'archive s'ouvre dans IntelliJ, se compile, et `security-smoke.sh` passe **sans intervention manuelle
hors du LISEZMOI**.

---

## P12 — Présentation · ⬜ À FAIRE (date annoncée par l'encadrant, 🅰️ + 🅱️)

**Démonstration de 10 minutes**, dans cet ordre (préparer un terminal avec les variables déjà exportées) :

1. `./scripts/security-smoke.sh` → toutes les lignes `OK` (30 s).
2. Jeton `alice` → `GET /api/gateway/owners/1/dossier` → **200** avec les trois services (1 min).
3. Même appel **sans jeton** → 401 ; avec `carol` → 403 ; `POST` de `bob` → 403 (2 min).
4. Appel **direct** à `customers` : sans certificat (échec), avec le certificat du gateway (200),
   avec `rogue-client` (**403**) (3 min).
5. Schéma d'architecture + tableau des compromis (3 min).

**Plan B** : captures et **vidéo de secours** de la démo, si le réseau ou Docker fait défaut le jour J.

### Questions probables (chacun doit savoir répondre sur la partie de l'autre)

- Pourquoi mTLS **et** JWT ? (le premier authentifie le service, le second l'utilisateur et ses rôles ;
  JWT seul ne protège pas contre l'appel direct à un service.)
- Pourquoi valider le JWT dans **chaque** service et pas seulement au gateway ? (Zero Trust, défense en profondeur.)
- Pourquoi RBAC et pas ABAC/PBAC/ReBAC ? Que changerait ReBAC ici ? (« propriétaire de l'animal » est une relation.)
- Différence entre **authentifier** et **autoriser** un service ? (mTLS vs liste blanche de CN.)
- Pourquoi un SAN dans le certificat ? Que change une CA de projet ?
- Que se passe-t-il si Keycloak tombe ? (les jetons valides restent vérifiables via les clés en cache,
  mais plus aucun nouveau jeton n'est émis.)
- Quelles sont vos limites ? (voir le chapitre 8 du rapport.)

---

## Annexe A — Dépannage rapide

| Symptôme | Cause probable | Action |
|---|---|---|
| Service qui ne démarre pas : `JwtDecoder` introuvable | `issuer-uri` absent du YAML | ajouter le bloc `spring.security.oauth2.resourceserver.jwt` (P5.3) |
| Maven ne résout pas le starter OAuth2 | nom de starter Spring Boot 4 | `spring-boot-starter-security-oauth2-resource-server` |
| `Account is not fully set up` (Keycloak) | prénom/nom/e-mail manquants | le JSON d'import les fournit ; sinon les saisir |
| 401 avec un jeton valide | `iss` ≠ `issuer-uri` | même URL (`localhost`) des deux côtés |
| `openssl req` : sujet déformé, chemin Windows | conversion de chemin Git Bash | `export MSYS_NO_PATHCONV=1` |
| `PKIX path building failed` | CA absente du truststore | `trusted-x509-certificates` ou truststore JVM |
| `handshake_failure` / `certificate_unknown` | pas de certificat client ou mauvaise CA | vérifier `api-gateway.p12` et `truststore.p12` |
| `No subject alternative names matching` | nom d'hôte absent du SAN | régénérer avec le bon `DNS:`/`IP:` |
| `lb://` appelle `http://` | instance sans port sécurisé | `secure-port-enabled: true`, `non-secure-port-enabled: false` |
| 403 alors que le certificat du gateway est présenté | CN absent de `allowed-callers` | vérifier `petclinic.security.allowed-callers` |
| Services disparus d'Eureka | identifiants manquants | `EUREKA_PASSWORD` défini pour **tous** les processus |
| Tests `@WebMvcTest` en 401/403 | filtres actifs | `@AutoConfigureMockMvc(addFilters = false)` sur les tests métier |
| `vets` : profil `production` perdu | `SPRING_PROFILES_ACTIVE` remplace le YAML | lancer avec `production,tls` |
| `PKI_DIR` introuvable sous IntelliJ | répertoire de travail différent | chemin absolu avec des `/` |

---

## Annexe B — Planning restant

| Jour | 🅰️ A | 🅱️ B |
|---|---|---|
| Mar 10 (fait) | P8 (durcissement) ✅ | — |
| Mer 11 | **P4** Keycloak + jetons | **P3** `rogue-client` + P6.1 |
| Jeu 12 | **P5** module + services | **P6** palier 1 sur les 3 services |
| Ven 13 | **P5** gateway | **P6** palier 2 |
| Sam 14 – Dim 15 | tampon · **P5** finitions | **P6** palier 3 + liste blanche |
| Lun 16 | **P7** `dossier` | **P7** client mTLS |
| Mar 17 | **P9** tests | **P9** `security-smoke.sh` |
| Mer 18 → Sam 21 | **P10** rapport (ch. 1-5 puis relecture) | **P10** rapport (ch. 6-9) |
| Dim 22 | **P11** archive + test à froid | relecture croisée |
| Lun 23 | **P12** préparation + **dépôt Moodle** | idem |

---

## Annexe C — Check-list avant dépôt

**Exigences**
- [ ] E1 : OIDC/JWT + mTLS + RBAC implémentés **et justifiés** (P4, P5, P6)
- [ ] E2 : chaque service valide le JWT **et** exige un certificat client autorisé (P5, P6)
- [ ] E3 : scénario `dossier` fonctionnel avec les 3 services (P7)
- [ ] E4 : avantages/inconvénients de **chaque** mécanisme, comparés aux existants, avec critères de décision (P10)
- [ ] E5 : rapport avec solutions existantes, architecture, composants, avantages/inconvénients **et installation** (P10)
- [ ] E6 : archive **ouverte et testée à froid** dans IntelliJ (P11)
- [ ] E7 : dépôt Moodle **avant le 24/11 minuit** (visé : 23 au soir)

**Qualité**
- [ ] `./mvnw -B clean verify` → `BUILD SUCCESS`
- [ ] `./scripts/security-smoke.sh` → aucun `FAIL`
- [ ] `git log -p | grep -iE "password|PRIVATE KEY"` ne renvoie que des mots de passe de démonstration documentés
- [ ] Captures sans secret, rangées dans `docs/preuves/pXX-…/`
- [ ] Keycloak arrêté après la démonstration : `docker compose -f docker-compose.security.yml down`
