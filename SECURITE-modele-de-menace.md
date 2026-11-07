# Modèle de menace — PetClinic microservices

> Méthode : STRIDE appliqué **par flux**. Les constats de la section 3 sont issus de la lecture du code du dépôt (fichier + ligne de configuration), pas d'hypothèses.
> Les colonnes « Statut » et « Preuve » sont à compléter au fil des phases 3 à 10.

Légende STRIDE : **S**poofing (usurpation) · **T**ampering (falsification) · **R**epudiation · **I**nformation disclosure · **D**enial of service · **E**levation of privilege.

---

## 1. Périmètre et actifs

| Composant | Rôle | Port (profil `docker`) |
|---|---|---|
| api-gateway | Point d'entrée, sert aussi le front | 8080 |
| customers-service | Propriétaires et animaux (données personnelles) | 8081 |
| visits-service | Visites vétérinaires | 8082 |
| vets-service | Liste des vétérinaires (lecture seule) | 8083 |
| discovery-server (Eureka) | Annuaire des services | 8761 |
| Zipkin / Prometheus / Grafana | Observabilité | 9411 / 9091 / 3030 |

**Actifs à protéger**

| # | Actif | Pourquoi |
|---|---|---|
| A1 | Données des propriétaires (nom, adresse, téléphone) et des animaux | Données personnelles |
| A2 | Historique des visites | Intégrité (écritures non autorisées) |
| A3 | Annuaire Eureka | Qui est enregistré = vers qui le trafic est routé |
| A4 | Métriques, traces, logs | Révèlent la topologie et les chemins internes |
| A5 | Secrets et clés (futurs : Sonar, AWS, RDS, keystores) | Compromission de la chaîne |
| A6 | Pipeline CI/CD et images | Supply chain |

**Attaquants considérés** : (1) utilisateur anonyme sur Internet, (2) utilisateur authentifié malveillant, (3) machine compromise dans le réseau interne, (4) dépendance ou action CI compromise.

---

## 2. Schéma des flux

```
 Navigateur ──(F1)──► api-gateway:8080 ──(F2)──► customers:8081
                          │                 ├──► visits:8082
                          │                 └──► vets:8083
                          │
 services & gateway ──(F3)──► discovery-server:8761
 Prometheus ──(F4)──► /actuator/prometheus de chaque service
 services & gateway ──(F5)──► Zipkin:9411
 Navigateur/attaquant ──(F6)──► ports des services publiés directement sur l'hôte
 Grafana (F7) : accès anonyme
 CI/CD GitHub ──(F8)──► registre / cloud
```

---

## 3. Constats vérifiés dans le dépôt

| # | Constat | Où le vérifier | Gravité |
|---|---|---|---|
| C1 | **Aucune dépendance Spring Security** dans les 5 `pom.xml` : aucune authentification nulle part | `grep -l security spring-petclinic-*/pom.xml pom.xml` → vide | Critique |
| C2 | `management.endpoints.web.exposure.include: '*'` sur gateway, customers, visits, vets : tous les endpoints Actuator exposés sans authentification (`env`, `beans`, `heapdump`, `loggers`, etc.) | `application.yml` de chaque service | Critique |
| C3 | Les ports 8081/8082/8083 sont **publiés sur l'hôte** par `docker-compose.yml` : on peut contourner le gateway | `ports:` dans `docker-compose.yml` | Élevée |
| C4 | Tout le trafic est en **HTTP clair** : gateway → services (`lb://`), services → Eureka, services → Zipkin | `application.yml`, profil `docker` | Élevée |
| C5 | Eureka (8761) sans authentification : n'importe qui peut enregistrer une fausse instance ou lire l'annuaire | `discovery-server/application.yml` | Élevée |
| C6 | **Grafana en accès anonyme avec le rôle Admin** | `docker/grafana/grafana.ini`, section `[auth.anonymous]` | Critique |
| C7 | Image Docker : conteneur exécuté en **root**, base `eclipse-temurin:17` (JDK complet) | `docker/Dockerfile` | Moyenne |
| C8 | Le compose utilise des images externes `springcommunity/...` (non construites ni scannées par vous) | `docker-compose.yml` | Moyenne |
| C9 | Le gateway rejoue les requêtes **POST** (`Retry`, `methods: POST`, 1 essai sur 503) : risque de **doublon d'écriture** (non idempotent) | `api-gateway/application.yml`, `default-filters` | Moyenne |
| C10 | `tracing.sampling.probability: 1` : 100 % des requêtes tracées vers Zipkin non authentifié | `application.yml` | Faible |
| C11 | Pas de limitation de débit ni d'en-têtes de sécurité (HSTS, X-Frame-Options, CSP) sur le gateway | Absence de configuration | Moyenne |
| C12 | `.gitignore` ne protège pas `*.pem`, `*.p12`, `*.key`, `.env` : risque de fuite de clés dès la phase 4 | `.gitignore` | Élevée (préventif) |
| C13 | Workflow `maven-build.yml` : pas de tests de sécurité, pas de `permissions:` restreintes, distribution `adopt` (obsolète) | `.github/workflows/maven-build.yml` | Moyenne |
| C14 | Données en HSQLDB en mémoire par défaut : perte au redémarrage, pas de contrôle d'accès (sera remplacé par RDS) | `spring.sql.init`, `db/hsqldb` | Information |

> Le fichier de plan du dépôt (`PLAN-PROJET.md`, étape C.1) anticipait déjà C2, C4 et C7 ; les points C1, C3, C6, C9 et C12 sont des constats supplémentaires à valoriser dans le rapport.

---

## 4. Analyse STRIDE par flux

| # | Flux | Menace (STRIDE) | Scénario concret | Constat | Gravité | Contre-mesure | Phase | Statut | Preuve |
|---|---|---|---|---|---|---|---|---|---|
| F1-a | Navigateur → gateway | **S** | Création/modification d'un propriétaire sans identité (`POST /api/customer/owners`) | C1 | Critique | JWT (Keycloak) obligatoire sur POST/PUT/DELETE | 7 | ⬜ | 401 puis 201 |
| F1-b | Navigateur → gateway | **I** | Écoute du réseau : données personnelles en clair | C4 | Élevée | HTTPS en bordure + HSTS | 6, 10 | ⬜ | `curl -I` |
| F1-c | Navigateur → gateway | **T** | Modification du corps/URL en transit | C4 | Élevée | TLS | 6 | ⬜ | |
| F1-d | Navigateur → gateway | **D** | Requêtes en rafale sur `/api/**` | C11 | Moyenne | Limitation de débit (RequestRateLimiter) | 10 | ⬜ | |
| F1-e | Navigateur → gateway | **R** | Aucune trace de *qui* a modifié une donnée | C1 | Moyenne | `sub` du JWT dans les logs + `traceId` | 7 | ⬜ | log |
| F2-a | Gateway → services | **S** | Un attaquant du réseau interne se fait passer pour le gateway | C1, C4 | Élevée | TLS, puis mTLS (certificats signés par la CA du TP1) | 4, 6 | ⬜ | `curl` sans cert → échec |
| F2-b | Gateway → services | **I / T** | Interception ou altération entre gateway et services | C4 | Élevée | TLS | 6 | ⬜ | |
| F2-c | Gateway → services | **T** | Doublon d'écriture suite à un retry POST | C9 | Moyenne | Retirer `POST` du Retry, ou clé d'idempotence | 10 | ⬜ | |
| F3-a | Services → Eureka | **S / T** | Enregistrement d'une fausse instance `customers-service` qui reçoit le trafic | C5 | Élevée | Authentification basique Eureka + TLS | 10 | ⬜ | `curl` 401 |
| F3-b | Services → Eureka | **I** | Lecture de l'annuaire (topologie interne) | C5 | Moyenne | Idem | 10 | ⬜ | |
| F3-c | Services → Eureka | **D** | Saturation du registre | C5 | Moyenne | Auth + réseau interne seulement | 10 | ⬜ | |
| F4-a | Prometheus → Actuator | **I** | `/actuator/env`, `/heapdump`, `/configprops` lisibles par tous | C2 | Critique | `include: health,info,prometheus`, `show-details: never` | 10 | ⬜ | `/actuator/env` → 404 |
| F4-b | Actuator | **E** | `loggers`/`refresh` modifiables → altération du comportement | C2 | Élevée | Même mesure + port de management séparé | 10 | ⬜ | |
| F5 | Services → Zipkin | **I** | Traces (chemins, identifiants) lisibles sans auth | C10 | Faible | Réseau interne, échantillonnage réduit en prod | 10 | ⬜ | |
| F6 | Hôte → ports 8081-8083 | **S / E** | Appel direct d'un service en **contournant** la validation JWT du gateway | C3 | Élevée | Ne plus publier les ports ; réseau Docker interne ; mTLS | 6, 9 | ⬜ | `curl` direct refusé |
| F7 | Grafana | **E / I** | Accès anonyme Admin : modification des dashboards, lecture des métriques, ajout de sources | C6 | Critique | `auth.anonymous.enabled=false`, mot de passe admin fort | 10 | ⬜ | |
| F8-a | CI/CD | **I** | Token Sonar, clés AWS ou mot de passe RDS committés ou affichés dans les logs | C12 | Élevée | GitHub Secrets, `.gitignore`, gitleaks, rotation | 3, 5 | ⬜ | `git log -p` propre |
| F8-b | CI/CD | **T / E** | Action tierce ou dépendance compromise (ex. `@master`) | C13 | Moyenne | Versions épinglées, Dependabot, `permissions: contents: read` | 3, 5 | ⬜ | |
| F8-c | Images | **E** | Évasion/élévation depuis un conteneur root | C7 | Moyenne | Utilisateur non-root, JRE minimale, scan Trivy | 9, 10 | ⬜ | `id` → non-root |
| F8-d | Images | **T** | Image externe non maîtrisée | C8 | Moyenne | Construire et scanner ses propres images, ECR + scan on push | 9 | ⬜ | |
| F9 | Base de données (RDS) | **I** | Port 3306 ouvert à `0.0.0.0/0`, sans chiffrement (config du TP4) | — | Élevée | SG restreint, chiffrement, utilisateur à droits limités | 8 | ⬜ | |
| F10 | Navigateur → gateway | **E** | Un utilisateur authentifié modifie les données d'un autre propriétaire en changeant l'identifiant dans l'URL (`/api/customer/owners/42`) | C1 | Élevée | Contrôle d'accès par objet + rôles/scopes | 7 | ⬜ | requête sur l'ID d'autrui → 403 |
| F11 | Navigateur → gateway | **I** | Messages d'erreur trop détaillés (stack trace, requête SQL) aidant l'attaquant | C1, C13 | Moyenne | Gestionnaire d'erreurs générique, logs côté serveur | 10 | ⬜ | réponse 500 sans détail |
---

## 5. Priorisation

Score simple : **Gravité (1-3) × Probabilité (1-3)**.

| Priorité | Menaces | Pourquoi |
|---|---|---|
| 1 (à traiter en premier) | F1-a, F4-a, F7 | Exploitables par un anonyme, impact direct sur les données |
| 2 | F2-a/b, F6, F3-a | Nécessitent un accès réseau interne ; couverts par TLS/mTLS |
| 3 | F8-a/b/c/d, F9 | Chaîne de livraison et cloud |
| 4 | F1-d, F2-c, F5, F4-b | Durcissement complémentaire |

### 5.1 Cotation DREAD des menaces principales (méthode du cours, ch. 4)

`Risque = (Damage + Reproducibility + Exploitability + Affected users + Discoverability) / 5`

| Menace | D | R | E | A | Dsc | Score /10 | Rang |
|---|---|---|---|---|---|---|---|
| F4-a (Actuator) | 9 | 10 | 10 | 10 | 10 | **9,8** | 1 |
| F1-a (écriture sans identité) | 8 | 10 | 10 | 10 | 10 | **9,6** | 2 |
| F1-b (HTTP en clair) | 8 | 10 | 9 | 10 | 9 | **9,2** | 3 |
| F6 (contournement du gateway) | 8 | 10 | 10 | 9 | 9 | **9,2** | 3 |
| F7 (Grafana anonyme Admin) | 8 | 10 | 10 | 8 | 9 | **9,0** | 5 |
| F2-b (interception est-ouest) | 8 | 9 | 9 | 8 | 8 | **8,4** | 6 |
| F1-c (falsification en transit) | 6 | 9 | 9 | 9 | 8 | **8,2** | 7 |
| F1-d (rafale sur /api) | 5 | 9 | 9 | 9 | 9 | **8,2** | 7 |
| F1-e (non-répudiation) | 5 | 8 | 9 | 10 | 8 | **8,0** | 9 |
| F2-a (faux service) | 9 | 7 | 6 | 10 | 7 | **7,8** | 10 |
| F4-b (loggers/refresh) | 8 | 8 | 8 | 8 | 7 | **7,8** | 10 |
| F3-a (empoisonnement Eureka) | 9 | 7 | 6 | 10 | 6 | **7,6** | 12 |
| F9 (RDS exposée) | 9 | 7 | 6 | 9 | 7 | **7,6** | 12 |
| F8-a (fuite de secret) | 8 | 6 | 5 | 10 | 6 | **7,0** | 14 |
| F2-c (doublon POST) | 6 | 7 | 7 | 8 | 6 | **6,8** | 15 |
| F10 | Navigateur → gateway | **E** | Un utilisateur authentifié modifie les données d'un autre propriétaire en changeant l'identifiant dans l'URL (`/api/customer/owners/42`) | C1 | Élevée | Contrôle d'accès par objet + rôles/scopes | 7 | ⬜ | requête sur l'ID d'autrui → 403 |
| F11 | Navigateur → gateway | **I** | Messages d'erreur trop détaillés (stack trace, requête SQL) aidant l'attaquant | C1, C13 | Moyenne | Gestionnaire d'erreurs générique, logs côté serveur | 10 | ⬜ | réponse 500 sans détail |

> Les notes sont une appréciation d'équipe : justifiez chaque colonne en une phrase dans le rapport
> (par exemple, *Discoverability* = 10 parce que `/actuator/env` est lisible par un simple `curl` anonyme).
> L'ordre DREAD confirme les priorités de la section 5 : F4-a, F1-a, puis F1-b/F6, puis F7.

---

## 6. Risques résiduels (à assumer dans le rapport)

- Certificats auto-signés : pas de CA publique, rotation manuelle.
- Keycloak en `start-dev` : acceptable pour le projet, **pas pour la production**.
- Trivy en mode `exit-code: 0` : informe mais ne bloque pas encore.
- mTLS strict avec `lb://` : point technique délicat, documenté comme limite si non atteint.

---

## 7. Références à citer

- Microsoft, *STRIDE threat model* (Threat Modeling Tool).
- OWASP Top 10 (2021) : A01 Broken Access Control, A02 Cryptographic Failures, A05 Security Misconfiguration, A06 Vulnerable Components, A08 Software and Data Integrity Failures.
- OWASP API Security Top 10 (2023) : API2 Broken Authentication, API8 Security Misconfiguration.
- Spring Boot Reference, *Actuator – Endpoints / Security*.
- NIST SP 800-204 (sécurité des microservices).