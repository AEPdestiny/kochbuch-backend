# Lokaler Docker-Run-Test

Diese Anleitung beschreibt, wie das Backend lokal als Docker-Container mit
einer PostgreSQL-Testdatenbank gestartet und gegen die wichtigsten API-Endpunkte
geprüft werden kann.

## PostgreSQL-Testdatenbank starten

In Windows PowerShell:

```powershell
docker run --name dishly-local-postgres `
  -e POSTGRES_DB=dishly_prod_test `
  -e POSTGRES_USER=dishly `
  -e POSTGRES_PASSWORD=dishly `
  -p 5433:5432 `
  -d postgres:16
```

Verwendete Werte:

- Containername: `dishly-local-postgres`
- Datenbank: `dishly_prod_test`
- User: `dishly`
- Passwort: `dishly`
- Host-Port: `5433`

Container prüfen:

```powershell
docker ps
```

## Backend-Image bauen

Im Backend-Projekt:

```powershell
docker build -t dishly-backend .
```

## Backend-Container starten

Der Backend-Container läuft im Production-Profil und benötigt die Production-
Environment-Variables.

```powershell
docker run --name dishly-backend-local `
  -p 8080:8080 `
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
Backend-Container die PostgreSQL-Datenbank über den Host-Port `5433` erreicht.

## Wichtige Endpunkte testen

Swagger UI im Browser öffnen:

```text
http://localhost:8080/q/swagger-ui
```

Öffentliche Rezepte abrufen:

```powershell
Invoke-RestMethod -Method GET -Uri "http://localhost:8080/recipes"
```

User registrieren:

```powershell
$register = Invoke-RestMethod -Method POST `
  -Uri "http://localhost:8080/auth/register" `
  -ContentType "application/json" `
  -Body '{
    "username": "dockeruser",
    "email": "dockeruser@example.com",
    "password": "supersecret"
  }'

$token = $register.accessToken
```

Login testen:

```powershell
$login = Invoke-RestMethod -Method POST `
  -Uri "http://localhost:8080/auth/login" `
  -ContentType "application/json" `
  -Body '{
    "email": "dockeruser@example.com",
    "password": "supersecret"
  }'

$token = $login.accessToken
```

Aktuellen User prüfen:

```powershell
Invoke-RestMethod -Method GET `
  -Uri "http://localhost:8080/auth/me" `
  -Headers @{ Authorization = "Bearer $token" }
```

Pantry ohne Token prüfen:

```powershell
Invoke-WebRequest -Method GET -Uri "http://localhost:8080/pantry/items"
```

Pantry mit Token prüfen:

```powershell
Invoke-RestMethod -Method GET `
  -Uri "http://localhost:8080/pantry/items" `
  -Headers @{ Authorization = "Bearer $token" }
```

Einkaufsliste mit Token prüfen:

```powershell
Invoke-RestMethod -Method GET `
  -Uri "http://localhost:8080/shopping-list/items" `
  -Headers @{ Authorization = "Bearer $token" }
```

## Erwartete Ergebnisse

Öffentliche Endpunkte:

- `GET /q/swagger-ui` ist im Browser erreichbar.
- `GET /recipes` liefert `200 OK`.

Authentifizierung:

- `POST /auth/register` liefert `201 Created` und ein JWT.
- `POST /auth/login` liefert `200 OK` und ein JWT.
- `GET /auth/me` ohne Token liefert `401 Unauthorized`.
- `GET /auth/me` mit Token liefert `200 OK`.

Geschützte Endpunkte:

- `GET /pantry/items` ohne Token liefert `401 Unauthorized`.
- `GET /pantry/items` mit Token liefert `200 OK`.
- `GET /shopping-list/items` ohne Token liefert `401 Unauthorized`.
- `GET /shopping-list/items` mit Token liefert `200 OK`.

## Container aufräumen

Falls das Backend im Vordergrund läuft, zuerst mit `Ctrl+C` stoppen.

Backend-Container entfernen:

```powershell
docker rm dishly-backend-local
```

PostgreSQL stoppen und entfernen:

```powershell
docker stop dishly-local-postgres
docker rm dishly-local-postgres
```

Optional Backend-Image entfernen:

```powershell
docker rmi dishly-backend
```

## Typische Fehlerquellen

PostgreSQL ist noch nicht bereit:

- Nach dem Start des PostgreSQL-Containers einige Sekunden warten.
- Danach den Backend-Container erneut starten.

Backend erreicht die Datenbank nicht:

- Prüfen, ob `DB_URL` `host.docker.internal` verwendet.
- Prüfen, ob PostgreSQL auf Host-Port `5433` veröffentlicht wurde.

Registrierung liefert `409 Conflict`:

- Der Test-User existiert bereits.
- Andere E-Mail oder neuen PostgreSQL-Testcontainer verwenden.

JWT schlägt fehl:

- Prüfen, ob `JWT_SECRET`, `JWT_ISSUER` und `JWT_EXPIRES_IN_SECONDS` gesetzt
  sind.
- Der lokale `JWT_SECRET` ist nur ein Testwert und darf nicht für Production
  verwendet werden.

CORS schlägt im Browser fehl:

- Prüfen, ob `CORS_ORIGINS` zur Frontend-Origin passt.
- Für lokale Frontend-Tests ist typischerweise `http://localhost:5173` korrekt.
