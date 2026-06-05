# Dishly Smart Backend

Quarkus-Backend fuer die Rezeptverwaltung. Das aktuelle MVP stellt die
bestehende Recipe-API unter `/recipes` bereit und verwendet PostgreSQL mit
Hibernate ORM/Panache. `POST /recipes`, `PUT /recipes/{id}` und
`DELETE /recipes/{id}` benoetigen einen Bearer Token; Recipe-Read-Endpunkte
bleiben oeffentlich.

## Tests

Alle Backend-Tests ausfuehren:

```powershell
.\gradlew.bat test
```

Die normalen Unit- und Resource-Tests laufen ohne manuell konfigurierte lokale
Datenbank. Die Datenbank-Integrationstests verwenden Quarkus Dev Services, um
eine temporaere PostgreSQL-Datenbank fuer das Testprofil zu starten.

## PostgreSQL-Integrationstests

Die Integrationstests sind:

```text
src/test/java/de/htwberlin/webtech/recipe/RecipeRepositoryIntegrationTest.java
src/test/java/de/htwberlin/webtech/user/AppUserRepositoryIntegrationTest.java
```

Der `RecipeRepositoryIntegrationTest` prueft, dass Quarkus, Panache, PostgreSQL
und die Entity `Recipe` fuer folgende Faelle zusammenarbeiten:

- Rezept speichern
- Rezept anhand der ID lesen
- veroeffentlichte Rezepte filtern
- Rezept loeschen

Der `AppUserRepositoryIntegrationTest` prueft:

- AppUser speichern
- AppUser anhand der E-Mail lesen
- AppUser anhand des Usernamens lesen
- doppelte E-Mails ueber eine PostgreSQL-Unique-Constraint ablehnen
- doppelte Usernames ueber eine PostgreSQL-Unique-Constraint ablehnen

Voraussetzung:

- Docker Desktop muss installiert sein und laufen

Quarkus Dev Services/Testcontainers startet und stoppt die PostgreSQL-
Testdatenbank automatisch. Fuer diesen Test ist keine manuelle lokale
PostgreSQL-Instanz erforderlich.

## Lokale Entwicklung

Im Dev-Modus kann Quarkus Dev Services PostgreSQL ebenfalls automatisch
bereitstellen, wenn Docker Desktop laeuft:

```powershell
.\gradlew.bat quarkusDev
```

Fuer Produktion wird die Datenbank ausschliesslich ueber Environment Variables
konfiguriert:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

## Authentifizierung

Das Backend stellt JWT-basierte Authentifizierungsendpunkte bereit:

- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`

Recipe-Endpunkte bleiben im aktuellen Schritt oeffentlich.

JWT-Konfiguration:

- `JWT_SECRET`: HS256-Signatur-Secret. In Produktion einen langen, zufaelligen
  Wert verwenden.
- `JWT_ISSUER`: Token-Issuer, Standardwert ist `dishly-smart`.
- `JWT_EXPIRES_IN_SECONDS`: Token-Laufzeit, Standardwert ist `3600` Sekunden.

Die Token-Laufzeit im MVP betraegt 1 Stunde.

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

Eine erfolgreiche Registrierung gibt `201 Created` zurueck und loggt den User
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
zurueck.

### Aktueller User

```http
GET /auth/me
Authorization: Bearer <jwt>
```

Gibt `200 OK` mit dem aktuellen User zurueck. Fehlende oder ungueltige Tokens
geben `401 Unauthorized` zurueck.

## Recipe-Write-Endpunkte

Das Erstellen von Rezepten erfordert jetzt einen authentifizierten User. Das
erstellte Rezept gehoert dem User aus dem Bearer Token.

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
Owner des Rezepts darf es aendern:

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

Das Loeschen von Rezepten erfordert ebenfalls Authentifizierung. Nur der Owner
darf das Rezept loeschen:

```http
DELETE /recipes/1
Authorization: Bearer <jwt>
```

Oeffentliche Recipe-Read-Endpunkte benoetigen keinen Token:

- `GET /recipes`
- `GET /recipes/published`
- `GET /recipes/{id}`
- `GET /recipes/external`
