# Dishly Smart Backend

Quarkus-Backend für die Rezeptverwaltung. Das aktuelle MVP stellt die
bestehende Recipe-API unter `/recipes` bereit und verwendet PostgreSQL mit
Hibernate ORM/Panache. `POST /recipes`, `PUT /recipes/{id}` und
`DELETE /recipes/{id}` benötigen einen Bearer Token; Recipe-Read-Endpunkte
bleiben für veröffentlichte Rezepte öffentlich.

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

Für Production-CORS wird die erlaubte Frontend-Origin ebenfalls über eine
Environment Variable konfiguriert:

- `CORS_ORIGINS`

Beispiel:

```text
CORS_ORIGINS=https://deine-frontend-url.onrender.com
```

## Lokaler Docker-Start

Das Backend kann als Docker-Container gebaut werden. Der Container verwendet
die Quarkus Fast-JAR-Struktur aus `build/quarkus-app` und startet im
Production-Profil.

Image bauen:

```powershell
docker build -t dishly-backend .
```

Container mit Production-Environment-Variables starten:

```powershell
docker run --rm -p 8080:8080 `
  -e DB_URL="jdbc:postgresql://host.docker.internal:5433/dishly_prod_test" `
  -e DB_USER="dishly" `
  -e DB_PASSWORD="dishly" `
  -e JWT_SECRET="local-prod-test-secret-local-prod-test-secret-123456" `
  -e JWT_ISSUER="dishly-smart" `
  -e JWT_EXPIRES_IN_SECONDS="3600" `
  -e CORS_ORIGINS="http://localhost:5173" `
  dishly-backend
```

Unter Windows und macOS kann `host.docker.internal` verwendet werden, damit der
Container eine PostgreSQL-Instanz auf dem Host erreicht. Der Beispielport
`5433` passt zu einer lokalen Testdatenbank, die außerhalb des Containers läuft.

## Authentifizierung

Das Backend stellt JWT-basierte Authentifizierungsendpunkte bereit:

- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`

Recipe-Endpunkte bleiben im aktuellen Schritt öffentlich.

JWT-Konfiguration:

- `JWT_SECRET`: HS256-Signatur-Secret. In Production ist diese Environment
  Variable Pflicht. Der Wert muss mindestens 32 Zeichen lang sein. Fehlt
  `JWT_SECRET` oder ist der Wert zu kurz, bricht der Backend-Start ab.
- `JWT_ISSUER`: Token-Issuer, Standardwert ist `dishly-smart`.
- `JWT_EXPIRES_IN_SECONDS`: Token-Laufzeit, Standardwert ist `3600` Sekunden.

Im Dev- und Test-Profil ist ein lokales Default-Secret hinterlegt, damit die
Anwendung ohne zusätzliche Konfiguration lokal gestartet werden kann. Dieses
Default-Secret darf nicht für Production verwendet werden.

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

## Shopping-List-Endpunkte

Die Einkaufsliste ist eine persönliche manuelle Liste. Alle Shopping-List-
Endpunkte benötigen einen Bearer Token. Nutzer sehen, ändern und löschen nur
eigene Shopping List Items.

Eigene Shopping List Items abrufen:

```http
GET /shopping-list/items
Authorization: Bearer <jwt>
```

Shopping List Item erstellen:

```http
POST /shopping-list/items
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "name": "Tomatoes",
  "quantity": 3,
  "unit": "piece",
  "category": "Vegetables",
  "checked": false
}
```

Eigenes Shopping List Item aktualisieren:

```http
PUT /shopping-list/items/1
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "name": "Cherry Tomatoes",
  "quantity": 5,
  "unit": "piece",
  "category": "Vegetables",
  "checked": true
}
```

Eigenes Shopping List Item löschen:

```http
DELETE /shopping-list/items/1
Authorization: Bearer <jwt>
```

Fehlende oder ungültige Tokens geben `401 Unauthorized` zurück. Fremde Shopping
List Items geben bei `PUT` und `DELETE` `403 Forbidden` zurück. Nicht vorhandene
Shopping List Items geben `404 Not Found` zurück.

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

Öffentliche Recipe-Read-Endpunkte benötigen keinen Token, liefern aber nur
veröffentlichte Rezepte:

- `GET /recipes`
- `GET /recipes/published`
- `GET /recipes/external`

`GET /recipes/external` lädt externe Rezepte über TheMealDB. Optional kann ein
Suchbegriff übergeben werden:

```http
GET /recipes/external?search=pasta
```

Ohne Suchbegriff verwendet das Backend eine kleine Default-Suche. Die externen
Rezeptdaten sind im MVP zunächst englisch, werden nicht in PostgreSQL
gespeichert und gehören keinem Dishly-User.

`GET /recipes/{id}` ist öffentlich nur für veröffentlichte Rezepte sichtbar.
Private oder unveröffentlichte Rezepte werden für fremde oder anonyme Nutzer als
`404 Not Found` behandelt. Eigene Rezepte des eingeloggten Users können über
`GET /recipes/mine` abgerufen werden.

## Smoke-Test

Eine manuelle Smoke-Test-Checkliste befindet sich im Frontend-Projekt unter:

`../kochbuch-frontend/docs/SMOKE_TEST.md`

Eine lokale Docker-Run-Testanleitung befindet sich unter:

`docs/DOCKER_RUN_TEST.md`

Eine Deployment-Anleitung für Vercel, Koyeb und Neon befindet sich unter:

`docs/DEPLOYMENT_VERCEL_KOYEB_NEON.md`
