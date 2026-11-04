# Guide opérationnel — de zéro à la soutenance

Ce guide se lit **dans l'ordre**. Chaque étape contient : où aller, quoi faire, quoi coller, quelle commande lancer,
et **ce que tu dois voir** pour savoir que c'est bon. Ne passe à l'étape suivante que si la case ✅ est vérifiée.

---

## Où travailler

| Quoi | Valeur |
|---|---|
| Dossier du projet | `C:\Users\aitel\Downloads\PetClinic` |
| Terminal à ouvrir | Git Bash (pas cmd.exe) |
| Commande de départ, à chaque ouverture de terminal | `cd /c/Users/aitel/Downloads/PetClinic` |
| Build Maven | `./mvnw` (ne jamais utiliser `mvn` seul) |
| JDK | 21, déjà installé |
| Ports utilisés | gateway 8080 · customers 8081 · visits 8082 · vets 8083 · Eureka 8761 · Keycloak 8180 · HTTPS services 8443/8444/8445 |

---

## État de départ (vérifié)

- Le projet compile : 5 modules (3 métier + gateway + Eureka).
- Les 3 routes métier répondent en 200 via le gateway.
- Les tests existent et passent : **14 tests** sur les 3 services (`customers` 10, `vets` 2, `visits` 2).
  - `OwnerResourceTest` (3 tests), `VetResourceTest` (2), `VisitResourceTest` (2) ont été ajoutés ;
    `PetResourceTest` (7) existait déjà.
- Il n'y a **pas encore** de dépôt Git, pas de JaCoCo, pas de Sonar, pas de TLS, pas de JWT.

---

## Ordre des étapes

### Fichier 1 — `01-cadrage-et-pipeline.md`
| Étape | Contenu | Durée |
|---|---|---|
| 1 | Vérifier les outils | 5 min |
| 2 | Créer le dépôt Git local | 10 min |
| 3 | Créer le dépôt sur GitHub et pousser | 10 min |
| 4 | Créer la branche `dev` | 5 min |
| 5 | Protéger `main` | 10 min |
| 6 | Créer le compte SonarCloud et le projet | 15 min |
| 7 | Récupérer `SONAR_TOKEN` | 5 min |
| 8 | Ajouter les secrets GitHub | 5 min |
| 9 | Ajouter JaCoCo au pom parent | 15 min |
| 10 | Lancer la couverture et lire le rapport | 10 min |
| 11 | Ajouter Sonar au pom parent | 15 min |
| 12 | Créer le workflow GitHub Actions | 20 min |
| 13 | Faire une PR et voir le pipeline | 20 min |
| 14 | Ajouter Dependabot + Trivy | 20 min |

### Fichier 2 — `02-securite.md`
| Étape | Contenu | Durée |
|---|---|---|
| 15 | Écrire le modèle de menace | 1 h |
| 16 | Lancer Keycloak | 15 min |
| 17 | Obtenir un jeton avec `curl` | 15 min |
| 18 | Sécuriser les écritures au gateway | 1 h |
| 19 | Tester 401 / 201 | 15 min |
| 20 | Activer TLS sur les 3 services | 1 h |
| 21 | Faire confiance à la CA au gateway | 1 h |
| 22 | mTLS strict (option avancée) | 1 j |
| 23 | Protéger Eureka | 45 min |
| 24 | Réduire Actuator | 30 min |
| 25 | Durcir les Dockerfile | 45 min |
| 26 | Scan ZAP | 30 min |

### Fichier 3 — `03-donnees-et-rapport.md`
| Étape | Contenu | Durée |
|---|---|---|
| 27 | Créer l'instance RDS | 45 min |
| 28 | Créer les 3 schémas | 20 min |
| 29 | Ajouter le profil `prod` | 1 h |
| 30 | Vérifier la persistance | 30 min |
| 31 | Tableau avant/après TP04 | 30 min |
| 32 | Observabilité (Prometheus/Grafana/Zipkin) | 2 h |
| 33 | Squelette du rapport LaTeX | 1 h |
| 34 | Compiler et vérifier la pagination | 30 min |

---

## Règles à ne jamais enfreindre

1. **Un secret ne se colle jamais dans un fichier du dépôt.** Il va dans GitHub → Settings → Secrets.
2. **Toujours `./mvnw`, jamais `mvn`.**
3. Après chaque modification du `pom.xml` ou d'un fichier Java, relancer `./mvnw -B -DskipTests package`
   et vérifier `BUILD SUCCESS` avant de continuer.
4. Après chaque modification d'un `application.yml`, redémarrer le service concerné et vérifier son port.
5. Si une commande échoue, **ne passe pas à l'étape suivante** : chaque étape a une rubrique ⛔ avec les causes fréquentes.

---

## Démarrer / arrêter l'application (rappel)

Démarrage complet (5 services), dans Git Bash :

```bash
cd /c/Users/aitel/Downloads/PetClinic
./mvnw -B -DskipTests package
java -jar spring-petclinic-discovery-server/target/spring-petclinic-discovery-server-4.0.1.jar &
java -jar spring-petclinic-customers-service/target/spring-petclinic-customers-service-4.0.1.jar &
java -jar spring-petclinic-vets-service/target/spring-petclinic-vets-service-4.0.1.jar &
java -jar spring-petclinic-visits-service/target/spring-petclinic-visits-service-4.0.1.jar &
java -jar spring-petclinic-api-gateway/target/spring-petclinic-api-gateway-4.0.1.jar &
sleep 45
curl -s -o /dev/null -w "gateway=%{http_code}\n" http://localhost:8080/
```

Arrêt : `Ctrl+C` dans le terminal, ou fermer la fenêtre Git Bash.

Interface web : ouvrir `http://localhost:8080/` dans le navigateur.
