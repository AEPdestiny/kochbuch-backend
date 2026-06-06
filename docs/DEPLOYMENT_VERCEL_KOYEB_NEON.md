# Deployment mit Vercel, Koyeb und Neon

Diese Anleitung beschreibt die kostenlose MVP-Zielarchitektur für Dishly Smart.

## Zielarchitektur

```text
Frontend:   Vercel
Backend:    Koyeb mit Dockerfile
Datenbank:  Neon PostgreSQL
```

Das Frontend wird als statische Vue/Vite-Anwendung auf Vercel deployed. Das
Backend läuft als Quarkus-Docker-Container auf Koyeb. PostgreSQL wird über Neon
bereitgestellt.

## Neon PostgreSQL erstellen

1. Neon öffnen: `https://neon.tech`
2. Account erstellen oder einloggen.
3. Neues Projekt erstellen.
4. Beispielwerte:

```text
Project name: dishly-smart
Database name: dishly
Region: möglichst nah am Backend-Host wählen
```

5. Nach dem Erstellen den Connection String kopieren.

Neon zeigt typischerweise einen Connection String in dieser Form:

```text
postgresql://dishly_owner:PASSWORT@ep-example.eu-central-1.aws.neon.tech/dishly?sslmode=require
```

## `DB_URL` aus Neon Connection String ableiten

Das Backend erwartet eine JDBC-URL in `DB_URL`.

Aus:

```text
postgresql://dishly_owner:PASSWORT@ep-example.eu-central-1.aws.neon.tech/dishly?sslmode=require
```

wird:

```text
jdbc:postgresql://ep-example.eu-central-1.aws.neon.tech/dishly?sslmode=require
```

Die Werte werden getrennt als Backend-Environment-Variables gesetzt:

```text
DB_URL=jdbc:postgresql://ep-example.eu-central-1.aws.neon.tech/dishly?sslmode=require
DB_USER=dishly_owner
DB_PASSWORD=PASSWORT
```

Wichtig: `sslmode=require` muss für Neon beibehalten werden.

## Koyeb Backend Deployment mit Dockerfile

1. Koyeb öffnen: `https://www.koyeb.com`
2. Account erstellen oder einloggen.
3. Neue App oder neuen Service erstellen.
4. GitHub als Quelle verbinden.
5. Backend-Repository auswählen.
6. Branch auswählen, zum Beispiel `main`.
7. Deployment-Methode auswählen:

```text
Dockerfile
```

8. Dockerfile-Pfad:

```text
Dockerfile
```

9. Service-Port:

```text
8080
```

Das Backend kann optional über die Environment Variable `PORT` einen anderen
Port verwenden. Ohne gesetztes `PORT` läuft es auf `8080`.

## Koyeb Env Vars

Im Koyeb Backend-Service folgende Environment Variables setzen:

```text
DB_URL=jdbc:postgresql://ep-example.eu-central-1.aws.neon.tech/dishly?sslmode=require
DB_USER=dishly_owner
DB_PASSWORD=<neon-passwort>
JWT_SECRET=<langes-zufaelliges-secret>
JWT_ISSUER=dishly-smart
JWT_EXPIRES_IN_SECONDS=3600
CORS_ORIGINS=https://platzhalter.vercel.app
```

`CORS_ORIGINS` wird später durch die echte Vercel-Frontend-URL ersetzt.

Für `JWT_SECRET` einen langen, zufälligen Wert verwenden. Der Wert muss stabil
bleiben und darf nicht ins Repository geschrieben werden.

Beispiel für einen lokalen Testwert, nicht für Production:

```text
JWT_SECRET=local-prod-test-secret-local-prod-test-secret-123456
```

Nach erfolgreichem Koyeb-Deployment die Backend-URL notieren, zum Beispiel:

```text
https://dishly-backend.example.koyeb.app
```

Backend direkt prüfen:

```text
https://dishly-backend.example.koyeb.app/q/swagger-ui
https://dishly-backend.example.koyeb.app/recipes
```

## Vercel Frontend Deployment

1. Vercel öffnen: `https://vercel.com`
2. Account erstellen oder einloggen.
3. Neues Projekt erstellen.
4. GitHub als Quelle verbinden.
5. Frontend-Repository auswählen.
6. Framework Preset:

```text
Vite
```

7. Build Command:

```text
npm run build
```

8. Output Directory:

```text
dist
```

## `VITE_API_URL`

In Vercel folgende Environment Variable setzen:

```text
VITE_API_URL=https://dishly-backend.example.koyeb.app
```

`VITE_API_URL` wird bei Vite zur Build-Zeit eingebettet. Wenn die Backend-URL
geändert wird, muss das Frontend neu deployed werden.

Nach dem Vercel-Deployment die Frontend-URL notieren, zum Beispiel:

```text
https://dishly-smart.vercel.app
```

## `CORS_ORIGINS`

Die finale Vercel-URL muss im Koyeb Backend-Service als `CORS_ORIGINS` gesetzt
werden:

```text
CORS_ORIGINS=https://dishly-smart.vercel.app
```

Danach Backend-Service neu starten oder neu deployen.

Wichtig:

- Kein Slash am Ende.
- Nur die echte Production-Frontend-URL setzen.
- Vercel Preview-URLs sind andere Origins und werden nicht automatisch erlaubt.

## Production Smoke-Test

Frontend öffnen:

```text
https://dishly-smart.vercel.app
```

Prüfen:

1. Startseite lädt.
2. Registrierung funktioniert.
3. Login funktioniert.
4. Eingeloggter User ist sichtbar.
5. `/auth/me` funktioniert indirekt über den App-Start.
6. Rezept erstellen funktioniert.
7. „Meine Rezepte“ zeigt eigene Rezepte.
8. Fremde Rezepte sind unter „Meine Rezepte“ nicht sichtbar.
9. Pantry CRUD funktioniert.
10. Einkaufsliste CRUD funktioniert.
11. Logout funktioniert.
12. Geschützte Bereiche zeigen ohne Login einen Login-Hinweis.
13. Öffentliche Recipe-Read-Endpunkte bleiben ohne Login nutzbar.

Backend direkt prüfen:

```text
https://dishly-backend.example.koyeb.app/q/swagger-ui
https://dishly-backend.example.koyeb.app/recipes
```

## Typische Fehler

### Neon SSL

Problem:

- Backend kann keine Verbindung zur Datenbank herstellen.

Prüfen:

- `DB_URL` beginnt mit `jdbc:postgresql://`.
- `DB_URL` enthält `sslmode=require`.
- `DB_USER` und `DB_PASSWORD` passen zum Neon Connection String.

### CORS

Problem:

- Browser blockiert API-Requests.

Prüfen:

- `CORS_ORIGINS` ist exakt die Vercel-Frontend-URL.
- Kein Slash am Ende.
- Backend wurde nach Änderung von `CORS_ORIGINS` neu gestartet.

Beispiel:

```text
Richtig: https://dishly-smart.vercel.app
Falsch:  https://dishly-smart.vercel.app/
```

### Vite Env Vars

Problem:

- Frontend ruft eine falsche oder alte Backend-URL auf.

Prüfen:

- `VITE_API_URL` ist in Vercel korrekt gesetzt.
- Nach Änderung von `VITE_API_URL` wurde das Frontend neu deployed.

### Koyeb Port

Problem:

- Backend startet, ist aber nicht erreichbar.

Prüfen:

- Koyeb Service-Port ist `8080`.
- Falls Koyeb `PORT` setzt, akzeptiert das Backend diese Variable.

### Cold Starts

Problem:

- Erster Request ist sehr langsam oder läuft in einen Timeout.

Hinweis:

- Kostenlose Backend-Hosts und serverless Datenbanken können schlafen.
- Nach Inaktivität einige Sekunden warten und erneut versuchen.

### JWT Secret

Problem:

- Login funktioniert, aber Tokens werden nach Redeploy ungültig.

Prüfen:

- `JWT_SECRET` ist gesetzt.
- `JWT_SECRET` bleibt zwischen Deployments stabil.
- `JWT_SECRET` ist lang und zufällig.
- Das Secret steht nicht im Repository.
