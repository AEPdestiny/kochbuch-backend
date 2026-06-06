# Dishly Smart Backend

Quarkus-Backend für die Rezeptverwaltung. Das aktuelle MVP stellt die
bestehende Recipe-API unter `/recipes` bereit und verwendet PostgreSQL mit
Hibernate ORM/Panache. `POST /recipes`, `PUT /recipes/{id}` und
`DELETE /recipes/{id}` benötigen einen Bearer Token; Recipe-Read-Endpunkte
bleiben öffentlich.

## Tests

Alle Backend-Tests ausführen:

```powershell
.\gradlew.bat test
```

Die normalen Unit- und Resource-Tests laufen ohne manuell konfigurierte lokale
Datenbank. Die Datenbank-Integrationstests verwenden Quarkus Dev Services, um
eine temporäre PostgreSQL-Datenbank für das Testprofil zu starten.

## PostgreSQL-Integrationstests

Die Integrationstests sind:

```text
src/test/java/de/htwberlin/webtech/recipe/RecipeRepositoryIntegrationTest.java
src/test/java/de/htwberlin/webtech/user/AppUserRepositoryIntegrationTest.java
```

Der `RecipeRepositoryIntegrationTest` prüft, dass Quarkus, Panache, PostgreSQL
und die Entity `Recipe` für folgende Fälle zusammenarbeiten:

- Rezept speichern
- Rezept anhand der ID lesen
- veröffentlichte Rezepte filtern
- Rezept löschen

Der `AppUserRepositoryIntegrationTest` prüft:

- AppUser speichern
- AppUser anhand der E-Mail lesen
- AppUser anhand des Usernamens lesen
- doppelte E-Mails über eine PostgreSQL-Unique-Constraint ablehnen
- doppelte Usernames über eine PostgreSQL-Unique-Constraint ablehnen

Voraussetzung:

- Docker Desktop muss installiert sein und laufen

Quarkus Dev Services/Testcontainers startet und stoppt die PostgreSQL-
Testdatenbank automatisch. Für diesen Test ist keine manuelle lokale
PostgreSQL-Instanz erforderlich.

## Lokale Entwicklung

Im Dev-Modus kann Quarkus Dev Services PostgreSQL ebenfalls automatisch
bereitstellen, wenn Docker Desktop läuft:

```powershell
.\gradlew.bat quarkusDev
```

Für Produktion wird die Datenbank ausschließlich über Environment Variables
konfiguriert:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

## Authentifizierung

Das Backend stellt JWT-basierte Authentifizierungsendpunkte bereit:

- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`

Recipe-Endpunkte bleiben im aktuellen Schritt öffentlich.

JWT-Konfiguration:

- `JWT_SECRET`: HS256-Signatur-Secret. In Produktion einen langen, zufälligen
  Wert verwenden.
- `JWT_ISSUER`: Token-Issuer, Standardwert ist `dishly-smart`.
- `JWT_EXPIRES_IN_SECONDS`: Token-Laufzeit, Standardwert ist `3600` Sekunden.

Die Token-Laufzeit im MVP beträgt 1 Stunde.

### Registrierung

```http
POST /auth/register
Content-Type: application/json

{
  "username": "salma",
  "email": "salma@example.com",
  "password": "supersecret"
}
```

Eine erfolgreiche Registrierung gibt `201 Created` zurück und loggt den User
direkt ein:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "salma",
    "email": "salma@example.com",
    "role": "USER",
    "createdAt": "2026-06-05T12:00:00Z"
  }
}
```

### Login

```http
POST /auth/login
Content-Type: application/json

{
  "email": "salma@example.com",
  "password": "supersecret"
}
```

Ein erfolgreicher Login gibt `200 OK` mit derselben `AuthResponse`-Struktur
zurück.

### Aktueller User

```http
GET /auth/me
Authorization: Bearer <jwt>
```

Gibt `200 OK` mit dem aktuellen User zurück. Fehlende oder ungültige Tokens
geben `401 Unauthorized` zurück.

## Pantry-Endpunkte

Die Pantry ist eine persönliche Vorratsverwaltung. Alle Pantry-Endpunkte
benötigen einen Bearer Token. Nutzer sehen, ändern und löschen nur eigene
Pantry Items.

Eigene Pantry Items abrufen:

```http
GET /pantry/items
Authorization: Bearer <jwt>
```

Pantry Item erstellen:

```http
POST /pantry/items
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "name": "Rice",
  "quantity": 2,
  "unit": "kg",
  "category": "Grains"
}
```

Eigenes Pantry Item aktualisieren:

```http
PUT /pantry/items/1
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "name": "Basmati Rice",
  "quantity": 1.5,
  "unit": "kg",
  "category": "Grains"
}
```

Eigenes Pantry Item löschen:

```http
DELETE /pantry/items/1
Authorization: Bearer <jwt>
```

Fehlende oder ungültige Tokens geben `401 Unauthorized` zurück. Fremde Pantry
Items geben bei `PUT` und `DELETE` `403 Forbidden` zurück. Nicht vorhandene
Pantry Items geben `404 Not Found` zurück.

## Recipe-Write-Endpunkte

Das Erstellen von Rezepten erfordert jetzt einen authentifizierten User. Das
erstellte Rezept gehört dem User aus dem Bearer Token.

```http
POST /recipes
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "title": "Pasta",
  "imageUrl": "",
  "prepTimeMinutes": 10,
  "cookTimeMinutes": 20,
  "servings": 2,
  "difficulty": "easy",
  "category": "Italian",
  "rating": 4.5,
  "ingredients": "noodles",
  "instructions": "cook",
  "favorite": false,
  "published": true
}
```

Das Aktualisieren von Rezepten erfordert ebenfalls Authentifizierung. Nur der
Owner des Rezepts darf es ändern:

```http
PUT /recipes/1
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "title": "Updated Pasta",
  "imageUrl": "",
  "prepTimeMinutes": 10,
  "cookTimeMinutes": 20,
  "servings": 2,
  "difficulty": "easy",
  "category": "Italian",
  "rating": 4.5,
  "ingredients": "noodles",
  "instructions": "cook",
  "favorite": false,
  "published": true
}
```

Das Löschen von Rezepten erfordert ebenfalls Authentifizierung. Nur der Owner
darf das Rezept löschen:

```http
DELETE /recipes/1
Authorization: Bearer <jwt>
```

Eigene Rezepte des eingeloggten Users können über einen geschützten
Read-Endpunkt abgerufen werden:

```http
GET /recipes/mine
Authorization: Bearer <jwt>
```

Öffentliche Recipe-Read-Endpunkte benötigen keinen Token:

- `GET /recipes`
- `GET /recipes/published`
- `GET /recipes/{id}`
- `GET /recipes/external`

## Smoke-Test

Eine manuelle Smoke-Test-Checkliste befindet sich im Frontend-Projekt unter:

`../kochbuch-frontend/docs/SMOKE_TEST.md`
