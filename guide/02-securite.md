# Partie 2 — Sécurité de la communication

> Prérequis : étapes 1 à 14 terminées, pipeline vert.
> **Ces étapes ne se font pas toutes d'un coup** : après chacune, relance l'application et vérifie le ✅ avant de continuer.
> L'étape 22 (mTLS strict) est la seule optionnelle : si elle bloque plus d'une journée, passe à l'étape 23 et
> documente-la comme limite dans le rapport.

---

## ÉTAPE 15 — Écrire le modèle de menace

**Où :** fichier `SECURITE-modele-de-menace.md` à la racine.

**Fais :** colle ce tableau et complète la dernière colonne au fur et à mesure des étapes 16 à 25.

```markdown
| # | Flux | Protocole actuel | Menace (STRIDE) | Contre-mesure | Étape |
|---|------|------------------|-----------------|---------------|-------|
| 1 | Navigateur → api-gateway (8080) | HTTP clair, aucune authentification | Usurpation, falsification, élévation de privilèges | JWT sur les écritures | 18 |
| 2 | api-gateway → customers / visits / vets | HTTP clair, aucune authentification mutuelle | Usurpation d'un service, interception | TLS puis mTLS | 20-22 |
| 3 | Services → discovery-server (8761) | HTTP clair, annuaire ouvert | Injection d'une fausse instance, déni de service | Authentification basique | 23 |
| 4 | Prometheus → services (`/actuator/prometheus`) | Exposition `include: '*'`, anonyme | Divulgation d'information | Restriction des endpoints | 24 |
| 5 | Image Docker | Processus `root` | Élévation de privilèges dans le conteneur | Utilisateur non privilégié | 25 |
```

**✅ Tu dois voir :** un tableau de 5 lignes avec les colonnes remplies.

**Preuve pour le rapport :** ce tableau, commenté en deux pages (chapitre « Analyse de risque »).

---

## ÉTAPE 16 — Lancer Keycloak

**Fais :** dans Git Bash.

```bash
docker run -d --name keycloak -p 8180:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.0 start-dev
```

Attends 30 secondes, puis ouvre `http://localhost:8180` dans le navigateur.
Connecte-toi avec **admin / admin**.

**Fais ensuite, dans l'interface Keycloak :**
1. En haut à gauche, clique sur `master` → **Create realm** → Realm name : `petclinic` → **Create**.
2. Menu de gauche → **Clients** → **Create client** :
   - Client ID : `petclinic-ui`
   - **Next** → **Client authentication** = OFF, **Direct access grants** = ON → **Next** → **Save**
3. Menu de gauche → **Users** → **Create new user** :
   - Username : `demo` → **Create**
   - Onglet **Credentials** → **Set password** → `demo`, **Temporary** = OFF → **Save**

**✅ Tu dois voir :** le realm `petclinic` sélectionné, un client `petclinic-ui`, un utilisateur `demo`.

**⛔ Si le conteneur s'arrête :** `docker logs keycloak` — sur un poste avec peu de RAM, ajoute `-e KC_HOSTNAME_STRICT=false`.

---

## ÉTAPE 17 — Obtenir un jeton avec `curl`

**Fais :**

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/petclinic/protocol/openid-connect/token \
  -d grant_type=password -d client_id=petclinic-ui \
  -d username=demo -d password=demo \
  | sed 's/.*"access_token":"\([^"]*\)".*/\1/')

echo "${TOKEN:0:40}"
```

**✅ Tu dois voir :** une chaîne qui ressemble à `eyJhbGciOiJSUzI1NiIsInR5cCIg...`

**⛔ Si tu vois `{"error":"invalid_grant"...}` :** l'utilisateur `demo` n'a pas de mot de passe (refais le point 3 de l'étape 16).
**⛔ Si tu vois `{"error":"unauthorized_client"}` :** « Direct access grants » n'est pas activé sur le client.

**Note :** `TOKEN` n'existe que dans cette fenêtre de terminal. Pour le réutiliser plus tard, relance cette commande.

---

## ÉTAPE 18 — Sécuriser les écritures au gateway

**Fais 3 choses.**

**1. Ajouter la dépendance** dans `spring-petclinic-api-gateway/pom.xml`, à l'intérieur du bloc `<dependencies>` :

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
```

**2. Ajouter ces lignes** dans `spring-petclinic-api-gateway/src/main/resources/application.yml`,
sous la clé `spring:` existante (attention à l'indentation, 2 espaces) :

```yaml
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/petclinic
```

**3. Créer le fichier** `spring-petclinic-api-gateway/src/main/java/org/springframework/samples/petclinic/api/SecurityConfig.java` :

```java
package org.springframework.samples.petclinic.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Le gateway reste libre en lecture (l'interface web n'a pas d'écran de connexion)
 * mais exige un jeton JWT valide pour toute écriture.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/**").authenticated()
                .pathMatchers(HttpMethod.PUT, "/api/**").authenticated()
                .pathMatchers(HttpMethod.DELETE, "/api/**").authenticated()
                .anyExchange().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

**Commande :**

```bash
./mvnw -B -DskipTests -pl spring-petclinic-api-gateway package
```

**✅ Tu dois voir :** `BUILD SUCCESS`.

**Décision assumée (à écrire dans le rapport) :** la lecture reste publique pour que l'interface de démonstration
fonctionne sans écran de connexion ; les écritures sont authentifiées. L'ajout d'un écran de connexion au front est
une perspective, pas une exigence du sujet.

---

## ÉTAPE 19 — Tester 401 / 201

**Fais :** relance le gateway, puis :

```bash
echo "--- sans jeton :"
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/customer/owners \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"Jwt","address":"1 rue","city":"Sale","telephone":"0600000000"}'

echo "--- avec jeton :"
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/customer/owners \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"firstName":"Test","lastName":"Jwt","address":"1 rue","city":"Sale","telephone":"0600000000"}'
```

**✅ Tu dois voir :** `401` puis `201`.

**⛔ Si tu vois 200 au lieu de 401 :** le gateway n'a pas été reconstruit ou l'ancien processus tourne encore
(ferme-le, puis relance).

**⛔ Si tu vois 401 dans les deux cas :** le `TOKEN` a expiré (5 minutes) → relance l'étape 17.

**Preuve pour le rapport :** capture des deux codes.

---

## ÉTAPE 20 — Activer TLS sur les 3 services

**Fais 1 :** générer les certificats (une seule fois, depuis la racine du projet).

```bash
cd /c/Users/aitel/Downloads/PetClinic
mkdir -p docker/certs && cd docker/certs

keytool -genkeypair -alias petclinic-ca -keyalg RSA -keysize 2048 -validity 3650 \
  -dname "CN=PetClinic CA, OU=ENSIAS, O=PetClinic" -ext bc:c \
  -keystore ca.p12 -storetype PKCS12 -storepass changeit
keytool -exportcert -alias petclinic-ca -keystore ca.p12 -storepass changeit -rfc -file ca.crt

for svc in customers visits vets; do
  keytool -genkeypair -alias $svc -keyalg RSA -keysize 2048 -validity 3650 \
    -dname "CN=${svc}-service, OU=ENSIAS, O=PetClinic" \
    -keystore ${svc}.p12 -storetype PKCS12 -storepass changeit
  keytool -certreq -alias $svc -keystore ${svc}.p12 -storepass changeit -file ${svc}.csr
  keytool -gencert -alias petclinic-ca -keystore ca.p12 -storepass changeit \
    -infile ${svc}.csr -outfile ${svc}.crt -rfc -validity 3650 \
    -ext san=dns:localhost,dns:${svc}-service
  keytool -importcert -alias petclinic-ca -keystore ${svc}.p12 -storepass changeit -file ca.crt -noprompt
  keytool -importcert -alias $svc -keystore ${svc}.p12 -storepass changeit -file ${svc}.crt
  cp ${svc}.p12 ../../spring-petclinic-${svc}-service/src/main/resources/keystore.p12
done

keytool -importcert -alias petclinic-ca -keystore truststore.p12 -storetype PKCS12 \
  -storepass changeit -file ca.crt -noprompt
cp truststore.p12 ../../spring-petclinic-api-gateway/src/main/resources/truststore.p12
ls -l
```

**✅ Tu dois voir :** `ca.p12`, `ca.crt`, les fichiers `customers/visits/vets` (`.p12`, `.crt`, `.csr`) et `truststore.p12`.

**Fais 2 — ajouter ces lignes au `.gitignore`** (les clés ne se versionnent jamais) :

```gitignore
*.p12
*.csr
*.crt
!docker/certs/.gitkeep
```

**Fais 3 — modifier les 3 `application.yml`.** Pour `customers`, remplace la section `server:` existante par :

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: customers
```

Puis, sous `eureka: instance:`, ajoute :

```yaml
    secure-port-enabled: true
    non-secure-port-enabled: false
    secure-port: 8443
```

**Répète** pour `visits` (port **8444**, alias `visits`) et `vets` (port **8445**, alias `vets`).
Attention : change bien `key-alias` et `secure-port` en même temps que `port`.

**Commande :**

```bash
./mvnw -B -DskipTests -pl spring-petclinic-customers-service,spring-petclinic-visits-service,spring-petclinic-vets-service package
```

**✅ Tu dois voir :** `BUILD SUCCESS`, puis, services relancés :

```bash
curl -k -s -o /dev/null -w "customers=%{http_code}\n" https://localhost:8443/owners
```

doit répondre `200`.

**⛔ Si `PKIX path building failed` :** le certificat n'a pas été signé par la CA (refais la boucle `for svc in ...` en entier).
**⛔ Si `Port 8443 already in use` :** un ancien processus tourne encore ; ferme-le.

---

## ÉTAPE 21 — Faire confiance à la CA depuis le gateway

**Fais :** dans `spring-petclinic-api-gateway/src/main/resources/application.yml`, ajoute (sous `spring:`) :

```yaml
  cloud:
    gateway:
      httpclient:
        ssl:
          trustedX509Certificates:
            - file:C:/Users/aitel/Downloads/PetClinic/docker/certs/ca.crt
```

Reconstruis et relance le gateway, puis :

```bash
curl -s -o /dev/null -w "via gateway=%{http_code}\n" http://localhost:8080/api/customer/owners
```

**✅ Tu dois voir :** `200` (le gateway appelle maintenant les services en HTTPS).

**⛔ Si tu vois `502` ou `SSLHandshakeException` :** deux causes possibles.
1. Les services n'ont pas publié leur port sécurisé : vérifie `secure-port-enabled: true` et que Eureka
   affiche bien `https` pour l'instance (ouvre `http://localhost:8761`).
2. Le chemin du fichier `ca.crt` est faux : mets un chemin absolu et vérifie que le fichier existe.

**Solution de repli (à documenter comme provisoire si elle est utilisée)** — accepter n'importe quel certificat,
**uniquement en local** :

```yaml
  cloud:
    gateway:
      httpclient:
        ssl:
          use-insecure-trust-manager: true
```

---

## ÉTAPE 22 — mTLS strict (option avancée)

> **Objectif :** le gateway présente lui aussi un certificat ; un service refuse toute connexion sans certificat client.
> **Budget :** 1 journée. Si ça bloque, va à l'étape 23 et note la limite dans le rapport.

**Fais 1 — exiger un certificat client sur chaque service**, dans les 3 `application.yml`, sous `server.ssl:` :

```yaml
    client-auth: need
    trust-store: classpath:truststore.p12
    trust-store-password: changeit
```

et copie le truststore dans chaque service :

```bash
cp docker/certs/truststore.p12 spring-petclinic-customers-service/src/main/resources/
cp docker/certs/truststore.p12 spring-petclinic-visits-service/src/main/resources/
cp docker/certs/truststore.p12 spring-petclinic-vets-service/src/main/resources/
```

**Fais 2 — créer un certificat client pour le gateway :**

```bash
cd /c/Users/aitel/Downloads/PetClinic/docker/certs
keytool -genkeypair -alias gateway -keyalg RSA -keysize 2048 -validity 3650 \
  -dname "CN=api-gateway, OU=ENSIAS, O=PetClinic" \
  -keystore gateway.p12 -storetype PKCS12 -storepass changeit
keytool -certreq -alias gateway -keystore gateway.p12 -storepass changeit -file gateway.csr
keytool -gencert -alias petclinic-ca -keystore ca.p12 -storepass changeit \
  -infile gateway.csr -outfile gateway.crt -rfc -validity 3650 -ext san=dns:localhost
keytool -importcert -alias petclinic-ca -keystore gateway.p12 -storepass changeit -file ca.crt -noprompt
keytool -importcert -alias gateway -keystore gateway.p12 -storepass changeit -file gateway.crt
cp gateway.p12 ../../spring-petclinic-api-gateway/src/main/resources/
```

**Fais 3 — dire au gateway d'utiliser ce certificat.** Crée
`spring-petclinic-api-gateway/src/main/java/org/springframework/samples/petclinic/api/HttpClientConfig.java` :

```java
package org.springframework.samples.petclinic.api;

import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import reactor.netty.http.client.HttpClient;

@Configuration
public class HttpClientConfig {

    @Bean
    HttpClient httpClient(@Value("classpath:gateway.p12") Resource keyStore,
                          @Value("classpath:truststore.p12") Resource trustStore) throws Exception {
        var ssl = SslContextBuilder.forClient()
            .keyManager(keyStore.getInputStream(), "changeit")
            .trustManager(trustStore.getInputStream())
            .build();
        return HttpClient.create().secure(spec -> spec.sslContext(ssl));
    }
}
```

**Commande :**

```bash
./mvnw -B -DskipTests -pl spring-petclinic-api-gateway package
```

**✅ Tu dois voir :** le gateway répond `200` sur `/api/customer/owners`, **et** un appel direct sans certificat est refusé :

```bash
curl -s -o /dev/null -w "sans certificat=%{http_code}\n" https://localhost:8443/owners
```

doit échouer (code `000` ou erreur de handshake).

**⛔ Si tout casse d'un coup :** remets `client-auth: none` dans les 3 services, reconstruis, et garde TLS seul.
C'est un résultat acceptable : documente « mTLS applicatif non abouti, TLS + JWT en place » dans les limites.

---

## ÉTAPE 23 — Protéger Eureka

**Fais 1 :** dans `spring-petclinic-discovery-server/pom.xml`, ajoute dans `<dependencies>` :

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
```

**Fais 2 :** dans `spring-petclinic-discovery-server/src/main/resources/application.yml`, ajoute :

```yaml
spring:
  security:
    user:
      name: eureka
      password: ${EUREKA_PASSWORD:changeit}
```

**Fais 3 :** dans les 3 services et dans le gateway, remplace la valeur de `defaultZone` par :

```yaml
      defaultZone: http://eureka:changeit@${EUREKA_HOST:localhost}:8761/eureka/
```

**Fais 4 :** reconstruis tout et relance :

```bash
./mvnw -B -DskipTests package
```

**✅ Tu dois voir :** `http://localhost:8761` demande un identifiant, et les services réapparaissent quand même
dans l'annuaire après connexion (`eureka` / `changeit`).

**⛔ Si les services ne s'enregistrent plus :** la console du service affiche `401` →
vérifie le format `http://eureka:changeit@localhost:8761/eureka/` (pas d'espace, pas de guillemets).

---

## ÉTAPE 24 — Réduire la surface Actuator

**Fais :** dans les 3 services **et** le gateway, remplace la ligne d'exposition des endpoints par :

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: never
```

**Commande :**

```bash
./mvnw -B -DskipTests package
```

**✅ Tu dois voir, après relance :**

```bash
curl -s -o /dev/null -w "env=%{http_code}\n" http://localhost:8080/actuator/env
curl -s -o /dev/null -w "health=%{http_code}\n" http://localhost:8080/actuator/health
```

`env` doit répondre **404** et `health` **200**.

**Preuve pour le rapport :** la capture des deux codes (avant : `env` = 200).

---

## ÉTAPE 25 — Durcir l'image Docker

**Fais :** dans `docker/Dockerfile`, ajoute **avant** la ligne `ENTRYPOINT` :

```dockerfile
RUN groupadd --system spring && useradd --system --gid spring spring \
    && chown -R spring:spring /application
USER spring:spring
```

Vérifie que le fichier se termine bien par ces deux blocs (`RUN` puis `USER`) avant `ENTRYPOINT`.

**Commande :**

```bash
./mvnw -B -DskipTests -P buildDocker install
docker run --rm springcommunity/spring-petclinic-customers-service whoami
```

**✅ Tu dois voir :** `spring` (et non `root`).

**⛔ Si le build Docker échoue :** le démon n'est pas lancé (ouvre Docker Desktop).

---

## ÉTAPE 26 — Scan dynamique ZAP

**Fais :** application démarrée sur le port 8080, puis :

```bash
docker run --rm -v "$(pwd)/zap:/zap/wrk:rw" -t ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py -t http://host.docker.internal:8080 -r zap-report.html
```

**✅ Tu dois voir :** un résumé (`FAIL-NEW`, `WARN-NEW`) et un fichier `zap/zap-report.html`.

**Preuve pour le rapport :** capture du résumé ZAP et des 3 alertes principales avec leur correction.

**⛔ Si `host.docker.internal` n'est pas résolu :** ajoute `--add-host=host.docker.internal:host-gateway` à la commande.
