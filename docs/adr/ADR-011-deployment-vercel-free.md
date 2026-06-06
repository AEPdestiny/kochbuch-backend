# ADR-011: Kostenlose Deployment-Strategie mit Vercel

Status: vorgeschlagen

## Kontext

Dishly Smart soll gemeinsam mit einem Partner möglichst kostenlos betrieben
werden. Render wurde bisher als Deployment-Ziel betrachtet, soll aber durch
eine kostenlose oder sehr kostenarme Kombination ersetzt werden.

Die bestehende Architektur bleibt erhalten:

- Vue/Vite Frontend
- Quarkus Backend
- PostgreSQL
- JWT-basierte Authentifizierung
- CORS zwischen Frontend und Backend

Das Frontend und Backend werden weiterhin getrennt deployt. Das Backend bleibt
ein eigenständiger Java-Service und wird nicht in Vercel Serverless Functions
umgebaut.

## Entscheidung

Für das MVP wird folgende kostenlose Zielarchitektur empfohlen:

- Frontend: Vercel
- Backend: Koyeb als kostenloser Container-Host, sofern die aktuellen
  Free-Tier-Bedingungen zum Projekt passen
- Datenbank: Neon PostgreSQL

Diese Kombination passt gut zur bestehenden Architektur, weil das Frontend als
statische Vite-Anwendung sehr gut auf Vercel läuft, während das Quarkus-Backend
als separater Container-Service betrieben werden kann.

Für das Backend wird Docker als MVP-Deployment-Variante gewählt. Der Container
verwendet die Quarkus Fast-JAR-Struktur aus `build/quarkus-app`. Der HTTP-Port
bleibt lokal bei `8080`, kann aber über die Environment Variable `PORT`
überschrieben werden.

## Warum Vercel gut für das Frontend geeignet ist

Vercel ist für statische Frontends und moderne JavaScript-Frameworks sehr gut
geeignet:

- Vite-Builds können direkt aus dem Git-Repository gebaut werden.
- Das Ergebnis liegt als statisches `dist`-Verzeichnis vor.
- Vercel stellt globale Auslieferung über CDN bereit.
- Preview Deployments pro Branch oder Pull Request sind für UI-Arbeit
  hilfreich.
- Die benötigte Frontend-Variable `VITE_API_URL` kann pro Deployment gesetzt
  werden.

Für Dishly Smart ist das Frontend damit ein idealer Kandidat für Vercel.

## Warum Vercel nicht ideal für das Quarkus-Backend ist

Das Backend ist eine Quarkus/JVM-Anwendung mit PostgreSQL-Verbindung,
Hibernate/Panache und JWT-Security. Vercel ist primär für Frontends,
Serverless Functions und Edge-nahe JavaScript-/TypeScript-Workloads optimiert.

Für dieses Backend wäre Vercel nicht ideal, weil:

- ein dauerhaft startbarer JVM-Service benötigt wird,
- Datenbankverbindungen zu PostgreSQL stabil verwaltet werden müssen,
- Quarkus als JAR oder Container natürlicher auf einem Java-/Container-Host
  läuft,
- ein Umbau auf Vercel Functions ein fachlich unnötiger Architekturbruch wäre,
- Cold Starts und Laufzeitmodell für eine klassische Quarkus REST API nicht
  optimal sind.

Das Backend bleibt deshalb außerhalb von Vercel.

## Datenbank: Neon vs. Supabase

### Neon

Vorteile:

- PostgreSQL-fokussiert.
- Serverless-Ansatz mit Scale-to-zero.
- Free Plan ist gut für kleine, intermittierende Projekte geeignet.
- Connection Pooling ist verfügbar.
- Passt gut, wenn nur PostgreSQL gebraucht wird.

Nachteile:

- Bei Inaktivität oder Scale-to-zero können erste Requests langsamer sein.
- Für echte Production-Last müssen Quotas und Kosten beobachtet werden.
- Keine integrierte App-Auth nötig, da Dishly Smart eigene JWT-Auth verwendet.

### Supabase

Vorteile:

- PostgreSQL plus zusätzliche Plattformfunktionen wie Auth, Storage und APIs.
- Free Plan ist für kleine Projekte attraktiv.
- Gutes Dashboard und einfache Verwaltung.

Nachteile:

- Viele Supabase-Funktionen würden im aktuellen MVP nicht genutzt.
- Supabase Auth wird nicht benötigt, weil Dishly Smart eigene JWT-Auth hat.
- Free Projects können bei Inaktivität pausiert werden.
- Es entsteht schneller konzeptionelle Verwechslungsgefahr zwischen eigener
  App-Auth und Supabase Auth.

### Bewertung

Für Dishly Smart ist Neon im MVP die schlankere Wahl, weil wir nur PostgreSQL
benötigen. Supabase bleibt eine gute Alternative, falls später Storage,
Realtime oder Plattform-Auth bewusst genutzt werden sollen.

## Backend-Hosting: Koyeb vs. Railway vs. Fly.io vs. andere Optionen

### Koyeb

Vorteile:

- Geeignet für Container- oder Web-Service-Deployments.
- Passt grundsätzlich zu Quarkus als JAR oder Container.
- Kostenlose Instanz ist für kleine MVPs interessant.

Nachteile:

- Free-Tier-Bedingungen und Zahlungsanforderungen müssen vor dem Einsatz
  geprüft werden.
- Cold Starts oder Sleep-Verhalten können auftreten.

### Railway

Vorteile:

- Sehr gute Developer Experience.
- Backend und Datenbank sind einfach zu deployen.
- Für Experimente schnell und bequem.

Nachteile:

- Der kostenlose Einstieg ist eher ein Trial-/Credit-Modell.
- Für dauerhaft kostenlosen Betrieb mit Partner ist Railway weniger planbar.

### Fly.io

Vorteile:

- Sehr gut für Container und regionale Deployments.
- Technisch gut passend für Quarkus.

Nachteile:

- Kein klassischer dauerhaft kostenloser Account.
- Free Allowances sind keine harte Kostenbremse.
- Für ein bewusst kostenloses Partnerprojekt daher riskanter.

### Weitere Optionen

Oracle Cloud Always Free oder ähnliche Cloud-Angebote können dauerhaft
kostenlos sein, sind aber deutlich ops-lastiger. Für ein kleines MVP mit
Partnerbetrieb ist der Verwaltungsaufwand höher als bei Vercel plus Koyeb plus
Neon.

## Empfohlene MVP-Kombination

Empfohlen wird:

- Frontend: Vercel Hobby
- Backend: Koyeb Free Instance oder vergleichbarer kostenloser Container-Host
- Datenbank: Neon Free PostgreSQL

Diese Kombination minimiert Architekturänderungen:

- Vue/Vite bleibt unverändert.
- Quarkus bleibt ein eigener Docker-basierter Backend-Service.
- PostgreSQL bleibt PostgreSQL.
- JWT bleibt im Backend.
- CORS verbindet Vercel-Frontend und Backend sauber.

## Environment Variables

### Backend

```text
DB_URL=<postgres-jdbc-url>
DB_USER=<postgres-user>
DB_PASSWORD=<postgres-password>
JWT_SECRET=<starkes-secret>
JWT_ISSUER=dishly-smart
JWT_EXPIRES_IN_SECONDS=3600
CORS_ORIGINS=https://dein-frontend.vercel.app
```

Das Backend mappt `DB_URL`, `DB_USER` und `DB_PASSWORD` weiterhin in
`application.properties` auf die Quarkus Datasource-Konfiguration.
Der Backend-Port kann über `PORT` gesetzt werden. Ohne gesetzte Variable nutzt
das Backend `8080`.

### Frontend

```text
VITE_API_URL=https://dein-backend-host.example.com
```

`VITE_API_URL` wird bei Vite zur Build-Zeit eingebettet. Nach Änderungen an
dieser Variable muss das Frontend neu gebaut und deployed werden.

## CORS-Konzept

Das Vercel-Frontend erhält eine feste Production-URL, zum Beispiel:

```text
https://dishly-smart.vercel.app
```

Diese URL wird im Backend als `CORS_ORIGINS` gesetzt:

```text
CORS_ORIGINS=https://dishly-smart.vercel.app
```

Lokale Entwicklung bleibt getrennt über:

```properties
%dev.quarkus.http.cors.origins=http://localhost:5173
```

Production erlaubt nicht pauschal `localhost`.

## Schritt-für-Schritt-Deploymentplan

1. Datenbank anlegen:
   - Neon-Projekt erstellen.
   - PostgreSQL-Datenbank anlegen.
   - JDBC-URL, User und Passwort ermitteln.
   - Werte für `DB_URL`, `DB_USER` und `DB_PASSWORD` vorbereiten.

2. Backend deployen:
   - Koyeb-Service oder vergleichbaren kostenlosen Container-Host anlegen.
   - Backend aus GitHub per Dockerfile deployen.
   - Dockerfile baut die Quarkus Fast-JAR-Struktur und startet
     `quarkus-run.jar` im Production-Profil.
   - Port `8080` verwenden oder über `PORT` vom Deployment-Host setzen lassen.
   - Backend-Environment-Variables setzen:
     - `DB_URL`
     - `DB_USER`
     - `DB_PASSWORD`
     - `JWT_SECRET`
     - `JWT_ISSUER`
     - `JWT_EXPIRES_IN_SECONDS`
     - `CORS_ORIGINS`
   - Logs prüfen.
   - `/q/swagger-ui` und `/recipes` prüfen.

3. Frontend auf Vercel deployen:
   - Vercel-Projekt aus dem Frontend-Repository erstellen.
   - Build Command `npm run build` verwenden.
   - Output Directory `dist` verwenden.
   - `VITE_API_URL` auf die Backend-URL setzen.
   - Frontend deployen.

4. CORS finalisieren:
   - Finale Vercel-URL als `CORS_ORIGINS` im Backend setzen.
   - Backend neu deployen, falls nötig.
   - Browser-Requests vom Vercel-Frontend prüfen.

5. Production Smoke-Test durchführen:
   - Registrierung testen.
   - Login testen.
   - `GET /auth/me` mit Bearer Token prüfen.
   - Rezept erstellen.
   - `GET /recipes/mine` prüfen.
   - Pantry CRUD testen.
   - Einkaufsliste CRUD testen.
   - Geschützte Endpunkte ohne Token prüfen.

## Risiken

Free-Tier Sleep und Cold Starts:

- Kostenlose Backend-Hosts und serverless Datenbanken können schlafen oder auf
  null skalieren.
- Erste Requests nach Inaktivität können langsam sein.

Datenbank-Verbindung:

- Serverless PostgreSQL kann Verbindungen schließen oder verzögert aufwachen.
- Connection Pooling sollte bevorzugt werden, falls Neon eine passende pooled
  JDBC-URL bereitstellt.

CORS:

- Die Vercel-Frontend-URL muss exakt in `CORS_ORIGINS` stehen.
- Preview-Deployments haben andere URLs und sind nicht automatisch erlaubt.

Vite Env Vars:

- `VITE_API_URL` wird zur Build-Zeit eingebettet.
- Eine falsche Backend-URL führt zu einem funktionierenden UI-Build mit
  kaputten API-Requests.

Kostenrisiko:

- Free-Tier-Bedingungen können sich ändern.
- Einige Anbieter verlangen Zahlungsdaten.
- Free Allowances sind nicht immer harte Kostenlimits.

Hibernate Schema Update:

- Production verwendet aktuell noch automatische Schema-Aktualisierung.
- Vor ernsthafter Nutzung sollten Flyway oder Liquibase eingeführt werden.

## Konsequenzen

Diese Strategie vermeidet einen Umbau der bestehenden Architektur. Vercel wird
für das genutzt, worin es stark ist: Frontend-Hosting. Das Quarkus-Backend
bleibt ein eigenständiger Service, und PostgreSQL wird über einen spezialisierten
kostenlosen Datenbankanbieter bereitgestellt.

Die wichtigsten nächsten technischen Schritte sind:

- Backend-Deploymentfähigkeit als Docker-Container lokal prüfen.
- Neon-Verbindungsdaten mit dem bestehenden `DB_*`-Schema testen.
- Vercel-Frontend mit `VITE_API_URL` gegen ein deployed Backend testen.
- Production Smoke-Test dokumentiert durchführen.

## Quellen

- Vercel Pricing: https://vercel.com/pricing
- Neon Pricing: https://neon.com/pricing
- Supabase Pricing: https://supabase.com/pricing
- Koyeb Pricing FAQ: https://www.koyeb.com/docs/faqs/pricing
- Railway Free Trial: https://docs.railway.com/pricing/free-trial
- Fly.io Cost Management: https://fly.io/docs/about/cost-management/
