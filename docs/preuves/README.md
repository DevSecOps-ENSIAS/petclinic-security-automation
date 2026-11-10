# Preuves — comment nommer et où ranger

Un dossier par phase, nommé d'après **la numérotation du guide v2** (`P0` à `P12`) :
`docs/preuves/p<NN>-<sujet>/<NN>-<description>.png`.

- Les travaux **hors périmètre v2** (CI/JaCoCo, Sonar/SCA, chaîne logicielle) sont rangés sous `bonus-…`
  pour ne pas être confondus avec une phase : `p03-ci` aurait laissé croire qu'il s'agissait de P3 (la PKI).
- Une capture se prend **au moment où la preuve existe** (le 401 avant de corriger, la PR avant de la fusionner).
- Jamais de mot de passe, de jeton réel ni de clé sur une capture : recadrer ou flouter.

## Correspondance

| Dossier | Phase | Contenu | Source |
|---|---|---|---|
| `p01-depot/` | P1 | PR fusionnée, branches, protection de `main` | à créer |
| `p02-menace/` | P2 | 5 constats « avant » (constats, actuator, contournement, Eureka, Grafana) | présent |
| `p03-pki/` | P3 | CA, `openssl verify`, keystore | absent (chez B) |
| `p05-jwt-rbac/` | P5 | matrice RBAC 401/403/200/403/201 | à créer |
| `p06-mtls/` | P6 | 5 tests mTLS + Eureka en `https` | absent (chez B) |
| `p07-dossier/` | P7 | scénario `dossier` + diagramme de séquence | à créer |
| `p08-durcissement/` | P8 | Actuator réduit, Eureka protégé, service `UP` | captures à prendre |
| `p09-tests/` | P9 | `security-smoke.sh`, `mvn test`, tests JUnit | à créer |
| `bonus-ci/` | — (ancienne phase 3) | JaCoCo, run Actions, protection | présent |
| `bonus-qualite-sca/` | — (ancienne phase 4) | Sonar, Trivy, Dependabot | présent |

Les phases **P4, P10, P11, P12** ne produisent pas de captures dédiées : P4 alimente `p05-jwt-rbac/`,
P10 cite les figures existantes, P11 et P12 se prouvent par l'archive et la démonstration.
