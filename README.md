# OraVisit — Backend

API REST interne de gestion des visiteurs d'Orabank Togo. L'agent d'accueil
enregistre les visiteurs qui se présentent, leur attribue un badge, suit les
visites en cours, effectue les check-out et consulte l'historique.
L'administrateur gère en plus les comptes du personnel et dispose
d'indicateurs statistiques.

## Stack

- Java 21, Spring Boot 3.5
- Spring Web, Spring Data JPA, Spring Security (JWT HS256), Bean Validation
- PostgreSQL + Flyway
- Apache POI pour les exports `.xlsx`

## Prérequis

- JDK 21 ou plus
- PostgreSQL 13 ou plus (développé et testé sur 18)
- Aucune installation de Maven : le wrapper `./mvnw` est fourni

## Démarrage rapide

```bash
# 1. Créer la base (Flyway crée les tables, pas la base)
createdb oravisit

# 2. Configurer l'environnement
cp .env.example .env   # puis renseigner DB_USERNAME / DB_PASSWORD

# 3. Lancer en profil dev
./mvnw spring-boot:run
```

L'API écoute sur `http://localhost:8080`. Le profil `dev` applique un jeu de
données de démonstration et crée ces comptes :

| Matricule | Mot de passe     | Rôle      |
|-----------|------------------|-----------|
| `ADM001`  | `Admin@2026`     | ADMIN     |
| `REC001`  | `Reception@2026` | RECEPTION |
| `EMP001`  | `Default@2026`   | DEFAULT   |

Ces mots de passe sont publics : ils ne doivent jamais servir hors développement.

```bash
# Vérifier que tout fonctionne
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"matricule":"ADM001","password":"Admin@2026"}'
```

## Configuration

Toutes les valeurs sensibles viennent de l'environnement — voir
[.env.example](.env.example) pour la liste complète et commentée.
Les essentielles :

| Variable                            | Défaut (dev)                     | Rôle                                      |
|-------------------------------------|----------------------------------|-------------------------------------------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | base `oravisit` en local     | Connexion PostgreSQL                      |
| `APP_JWT_SECRET`                    | secret de dev                    | Signature HS256, **obligatoire en prod**  |
| `APP_JWT_EXPIRATION`                | `PT8H`                           | Durée de vie du jeton                     |
| `APP_CORS_ALLOWED_ORIGINS`          | `http://localhost:4200`          | Origines autorisées, séparées par virgule |
| `APP_TIMEZONE`                      | `Africa/Lome`                    | Fuseau des bornes de journée              |
| `APP_BOOTSTRAP_ADMIN_*`             | compte de démo                   | Amorçage du premier ADMIN (hash BCrypt)   |

Deux profils : `dev` (par défaut, jeu de démonstration, logs DEBUG) et `prod`
(aucune valeur par défaut sensible, pas de données de démonstration).

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## Rôles

| Rôle        | Accès                                                          |
|-------------|----------------------------------------------------------------|
| `ADMIN`     | Tout, dont la gestion des employés et les statistiques          |
| `RECEPTION` | Visiteurs, visites, check-out, tableau de bord opérationnel     |
| `DEFAULT`   | Employé visitable, aucun accès fonctionnel à l'application      |

## Endpoints

Toutes les routes exigent un en-tête `Authorization: Bearer <jeton>`, sauf la
connexion.

### Authentification — `/api/v1/auth`

| Méthode | Route       | Accès       | Description                        |
|---------|-------------|-------------|------------------------------------|
| POST    | `/login`    | public      | Connexion, renvoie le jeton (8 h)  |
| GET     | `/me`       | authentifié | Profil de l'utilisateur courant    |
| PATCH   | `/password` | authentifié | Changer son propre mot de passe    |

### Visiteurs — `/api/v1/visitors` (ADMIN, RECEPTION)

| Méthode | Route            | Description                                          |
|---------|------------------|------------------------------------------------------|
| GET     | `/`              | Recherche paginée (`q`, `includeArchived`, `archivedOnly`) |
| GET     | `/export`        | Export `.xlsx` avec les mêmes filtres                |
| GET     | `/{id}`          | Détail d'une fiche                                   |
| POST    | `/`              | Créer une fiche                                      |
| PUT     | `/{id}`          | Modifier une fiche                                   |
| DELETE  | `/{id}`          | Archiver (suppression logique)                       |
| PATCH   | `/{id}/restore`  | Désarchiver                                          |
| GET     | `/{id}/visits`   | Historique des visites du visiteur                   |

### Visites — `/api/v1/visits` (ADMIN, RECEPTION)

| Méthode | Route             | Description                                              |
|---------|-------------------|----------------------------------------------------------|
| POST    | `/`               | Enregistrer une visite (statut `EN_COURS`)               |
| GET     | `/`               | Recherche paginée (`q`, `status`, `direction`, `hostEmployeeId`, `dateFrom`, `dateTo`) |
| GET     | `/export`         | Export `.xlsx` avec les mêmes filtres                    |
| GET     | `/{id}`           | Détail d'une visite                                      |
| PATCH   | `/{id}/checkout`  | Check-out : `EN_COURS` → `TERMINEE`                      |
| PATCH   | `/{id}/cancel`    | Annulation avec motif : `EN_COURS` → `ANNULEE`           |

### Employés — `/api/v1/employees` (ADMIN)

| Méthode | Route               | Description                                     |
|---------|---------------------|-------------------------------------------------|
| GET     | `/`                 | Recherche paginée (`q`, `role`, `direction`, `isActive`) |
| GET     | `/lookup`           | Autocomplétion des employés actifs — **aussi RECEPTION** |
| GET     | `/{id}`             | Détail d'un employé                             |
| POST    | `/`                 | Créer un compte                                 |
| PUT     | `/{id}`             | Modifier un compte                              |
| PATCH   | `/{id}/activate`    | Réactiver                                       |
| PATCH   | `/{id}/deactivate`  | Désactiver (suppression logique)                |
| PATCH   | `/{id}/password`    | Réinitialiser le mot de passe                   |

### Tableau de bord — `/api/v1/dashboard` (ADMIN, RECEPTION)

`GET /` renvoie les indicateurs du jour (visites, visiteurs présents,
check-out, durée moyenne). Pour un ADMIN, la réponse ajoute les statistiques
de la période `[from, to]` — 30 derniers jours par défaut : répartition par
direction, courbe d'évolution et cinq employés les plus visités.

## Règles métier

- Un numéro de badge ne peut équiper qu'une seule visite `EN_COURS`.
- Un visiteur ne peut avoir qu'une seule visite `EN_COURS`.
- La direction d'une visite est copiée depuis l'employé hôte par le serveur ;
  elle n'est jamais saisie par le client, pas plus que le statut, les
  horodatages ou l'agent enregistreur.
- L'employé hôte doit être actif, le visiteur non archivé.
- Depuis `TERMINEE` ou `ANNULEE`, aucune transition n'est possible.
- Rien n'est jamais supprimé physiquement : employés désactivés, visiteurs
  archivés, pour préserver l'historique des visites.
- Il reste toujours au moins un ADMIN actif, et un ADMIN ne peut pas
  désactiver son propre compte.

Les deux premières règles sont doublées d'index uniques partiels en base : la
vérification applicative donne un message clair, l'index protège des accès
concurrents.

## Réponses

Les listes sont paginées (`page`, `size`, `sort`) et enveloppées :

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 137,
  "totalPages": 7, "first": true, "last": false }
```

Les erreurs suivent le format RFC 7807, enrichi d'un `code` stable destiné au
frontend et, le cas échéant, du détail par champ :

```json
{ "status": 409, "detail": "Ce numero de badge est deja attribue a une visite en cours.",
  "code": "BADGE_ALREADY_IN_USE" }
```

## Structure

```
src/main/java/com/orabank/backend/
├── config/       AppProperties, SecurityConfig
├── controller/   5 contrôleurs REST
├── dto/          records d'entrée/sortie, avec leurs fabriques from(entity)
├── entity/       Employee, Visitor, Visit et les énumérations
├── exception/    ApiException, GlobalExceptionHandler
├── export/       génération des classeurs .xlsx
├── repository/   3 repositories Spring Data
├── security/     JWT, UserDetails, utilisateur courant
└── service/      5 services (règles métier)

src/main/resources/db/migration/   schéma géré par Flyway
```

Le schéma appartient à Flyway : Hibernate est en `ddl-auto: validate` et ne
génère rien. Toute évolution passe par une nouvelle migration `V<n>__*.sql`.

## Bon à savoir

- Le projet ne contient pas de tests automatisés.
- Il n'expose ni Swagger UI ni endpoint Actuator : les seules routes sont
  celles listées ci-dessus.
