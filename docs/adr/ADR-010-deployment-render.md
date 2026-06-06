# ADR-010: Deployment-Strategie für Render

Status: vorgeschlagen

## Kontext

Dishly Smart besteht aktuell aus einem Quarkus-Backend, PostgreSQL, JWT-basierter
Authentifizierung und einem Vue/Vite-Frontend. Für ein späteres MVP-Deployment
soll Render als Zielplattform verwendet werden.

Backend, Frontend und Datenbank werden getrennt betrieben:

- Backend als Render Web Service
- Frontend als Render Static Site
- Datenbank als Render PostgreSQL
- Secrets und umgebungsspezifische Werte über Render Environment Variables

Die lokale Entwicklung verwendet weiterhin Quarkus Dev Services/Testcontainers
oder lokale Entwicklungsserver. Production darf keine lokalen Dev-Services-
Annahmen verwenden.

## Entscheidung

Das Backend wird als eigenständiger Quarkus-Service auf Render deployt. Das
Frontend wird als statisches Vite-Build-Artefakt auf Render veröffentlicht. Die
PostgreSQL-Datenbank wird über Render PostgreSQL bereitgestellt.

Das Backend verwendet für Production bewusst die bestehenden Environment
Variables:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

Diese Variablen werden in `application.properties` auf die Quarkus
Datasource-Konfiguration gemappt. Die Quarkus-Standardnamen
`QUARKUS_DATASOURCE_*` werden aktuell nicht direkt verwendet.

## Backend Deployment auf Render

Render-Service:

- Typ: Web Service
- Runtime: Java
- Java-Version: passend zur Projektkonfiguration prüfen, bevorzugt Java 21

Build Command:

```bash
./gradlew build
```

Start Command:

```bash
java -jar build/quarkus-app/quarkus-run.jar
```

Benötigte Environment Variables:

```text
DB_URL=<render-postgres-jdbc-url>
DB_USER=<render-postgres-user>
DB_PASSWORD=<render-postgres-password>
JWT_SECRET=<starkes-production-secret>
JWT_ISSUER=dishly-smart
JWT_EXPIRES_IN_SECONDS=3600
CORS_ORIGINS=https://deine-frontend-url.onrender.com
```

CORS wird profilbasiert getrennt. Dev verwendet `http://localhost:5173`.
Production verwendet `CORS_ORIGINS`, damit die Render-Frontend-URL ohne
Codeänderung konfiguriert werden kann.

## Frontend Deployment auf Render

Render-Service:

- Typ: Static Site
- Framework: Vue/Vite

Build Command:

```bash
npm install && npm run build
```

Publish Directory:

```text
dist
```

Benötigte Environment Variable:

```text
VITE_API_URL=<production-backend-url>
```

Beispiel:

```text
VITE_API_URL=https://dishly-smart-api.onrender.com
```

Die Backend-URL darf nicht hart codiert werden. Das Frontend soll weiterhin den
zentralen API-Client verwenden, der die Vite-Environment-Variable nutzt.

## Datenbank-Konfiguration

Render PostgreSQL stellt die Production-Datenbank bereit.

Für Production verwendet das Backend:

```text
DB_URL
DB_USER
DB_PASSWORD
```

Das Backend mappt diese Variablen in `application.properties` auf die Quarkus
Datasource-Konfiguration:

```properties
%prod.quarkus.datasource.jdbc.url=${DB_URL}
%prod.quarkus.datasource.username=${DB_USER}
%prod.quarkus.datasource.password=${DB_PASSWORD}
```

Lokal werden Quarkus Dev Services/Testcontainers oder lokale Entwicklungsserver
verwendet. Production verwendet ausschließlich die konfigurierte Render
PostgreSQL-Datenbank.

## JWT-Konfiguration

JWT wird über Environment Variables konfiguriert:

```text
JWT_SECRET
JWT_ISSUER
JWT_EXPIRES_IN_SECONDS
```

Für Production gilt:

- `JWT_SECRET` muss ein langer, zufälliger und geheimer Wert sein.
- `JWT_SECRET` darf nicht im Repository gespeichert werden.
- `JWT_ISSUER` sollte stabil bleiben, zum Beispiel `dishly-smart`.
- `JWT_EXPIRES_IN_SECONDS` bleibt für das MVP bei `3600` Sekunden.

## CORS-Konfiguration

Production-CORS darf nur die echte Frontend-URL erlauben und wird über
`CORS_ORIGINS` gesetzt.

Beispiel:

```text
CORS_ORIGINS=https://dishly-smart.onrender.com
```

Dev nutzt `http://localhost:5173`. Diese lokale Origin gilt nicht global für
Production.

## Risiken

Hibernate Schema Update in Production:

- Das automatische Aktualisieren des Schemas ist für Production riskant.
- Entity-Änderungen können unbeabsichtigt Tabellen oder Spalten verändern.
- Für ein kleines MVP kann es kurzfristig toleriert werden, sollte aber bewusst
  überprüft werden.

Fehlende Migrationen:

- Flyway oder Liquibase sind noch nicht eingeführt.
- Ohne Migrationstool sind Production-Schemaänderungen schwer kontrollierbar.

CORS-Fehler:

- Eine falsche Frontend-Origin führt zu blockierten Browser-Requests.
- Render-URLs stehen oft erst nach dem ersten Deployment endgültig fest.

Falsche API-URL im Frontend:

- `VITE_API_URL` wird zur Build-Zeit eingebettet.
- Nach einer Änderung der API-URL muss das Frontend neu gebaut und deployt
  werden.

Secrets im Repository:

- Datenbankpasswörter und JWT-Secrets dürfen nicht committed werden.
- README-Beispiele dürfen keine echten Secrets enthalten.

Cold Starts:

- Render Free Tier kann Services schlafen legen.
- Erste Requests nach Inaktivität können langsam sein.

## MVP-Empfehlung

Für das MVP wird ein möglichst kleiner Deployment-Weg empfohlen:

1. Render PostgreSQL anlegen.
2. Backend als Render Web Service deployen.
3. `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ISSUER`,
   `JWT_EXPIRES_IN_SECONDS` und `CORS_ORIGINS` in Render setzen.
4. Frontend als Render Static Site deployen.
5. `VITE_API_URL` auf die Render-Backend-URL setzen.
6. CORS auf die Render-Frontend-URL begrenzen.
7. Production Smoke-Test durchführen.

Vor öffentlicher Nutzung sollten Flyway oder Liquibase sowie eine saubere
Production-CORS-Konfiguration eingeplant werden.

## Schritt-für-Schritt-Plan

1. Backend vorbereiten:
   - Java-Version prüfen.
   - Lokalen Gradle-Build ausführen.
   - Sicherstellen, dass Production über `DB_URL`, `DB_USER` und `DB_PASSWORD`
     konfiguriert wird.
   - JWT-Environment-Variables prüfen.
   - `CORS_ORIGINS` für Production planen.

2. Datenbank anlegen:
   - Render PostgreSQL erstellen.
   - JDBC-URL, User und Passwort übernehmen.
   - Werte als `DB_URL`, `DB_USER` und `DB_PASSWORD` im Backend-Service setzen.

3. Backend deployen:
   - Render Web Service konfigurieren.
   - Build Command `./gradlew build` setzen.
   - Start Command `java -jar build/quarkus-app/quarkus-run.jar` setzen.
   - Environment Variables inklusive `CORS_ORIGINS` setzen.
   - Logs prüfen.

4. Frontend deployen:
   - Render Static Site konfigurieren.
   - Build Command `npm install && npm run build` setzen.
   - Publish Directory `dist` setzen.
   - `VITE_API_URL` auf die Production-Backend-URL setzen.

5. CORS finalisieren:
   - Finale Frontend-URL als `CORS_ORIGINS` setzen.
   - Backend neu deployen, falls nötig.
   - Browser-Requests prüfen.

6. Production Smoke-Test durchführen.

## Production Smoke-Test

Nach dem Deployment wird die bestehende Smoke-Test-Checkliste gegen die
Production-URLs ausgeführt:

- Registrierung testen
- Login testen
- `GET /auth/me` mit Bearer Token prüfen
- Rezept erstellen
- `GET /recipes/mine` prüfen
- Recipe Ownership für `PUT` und `DELETE` grob prüfen
- Pantry CRUD testen
- Einkaufsliste CRUD testen
- Ohne Login geschützte Bereiche prüfen

Erwartetes Ergebnis:

- Authentifizierung funktioniert mit Production-JWT-Konfiguration.
- Backend erreicht die Render PostgreSQL-Datenbank.
- Frontend verwendet die korrekte Production-API-URL.
- CORS blockiert keine legitimen Frontend-Requests.
- Geschützte Endpunkte liefern ohne Token `401 Unauthorized`.
