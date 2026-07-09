# Dishly Smart — From Recipe to Reality

„Was koche ich diese Woche — und was habe ich eigentlich noch zu Hause?" Zwischen Rezeptsuche, Vorratschaos, spontanen Einkäufen und der Frage,
ob man heute überhaupt selbst kocht oder essen geht, geht im Alltag meist genau das verloren, was Planung eigentlich erleichtern sollte: der Überblick.
Klassische Rezeptplattformen lösen das nicht — sie zeigen Gerichte, aber sie planen nichts mit.

**Dishly Smart** verbindet Rezeptsuche, Ernährungsprofil, Vorratsverwaltung, Wochenplanung, Einkaufsliste und Restaurantsuche zu einem durchgängigen Workflow:
von der Rezeptidee bis zur Umsetzung — kochen oder essen gehen, geplant statt spontan.

## Inhaltsverzeichnis

- [Zielsetzung und Problemstellung](#zielsetzung-und-problemstellung)
- [Fachliches Konzept](#fachliches-konzept)
- [Technologiestack](#technologiestack)
- [Komponentenarchitektur](#komponentenarchitektur)
- [Wichtigste Komponenten](#wichtigste-komponenten)
- [Wichtigste Funktionalitäten](#wichtigste-funktionalitäten)
- [Datenfluss / Zusammenspiel der Module](#datenfluss--zusammenspiel-der-module)
- [Setup und Installation](#setup-und-installation)
- [Projektstruktur](#projektstruktur)
- [Qualitätssicherung](#qualitätssicherung)
- [Bekannte Einschränkungen](#bekannte-einschränkungen)
- [Fazit](#fazit)

## Zielsetzung und Problemstellung

Die ursprüngliche Idee hinter Dishly Smart war es, eine bestehende, isolierte Rezeptverwaltung zu einem intelligenten Entscheidungssystem weiterzuentwickeln:
statt Rezeptsuche, Einkaufsliste und Wochenplanung getrennt zu organisieren, sollten Nutzer:innen einen einzigen Workflow von der Rezeptauswahl bis zur
konkreten Umsetzung durchlaufen - inklusive Personalisierung über ein Ernährungsprofil, Abgleich mit vorhandenen Zutaten und der Option, statt zu kochen
ein passendes Restaurant zu finden.

Als technischer Ausgangspunkt diente ein bestehendes Recipe-CRUD-Projekt: ein Spring-Boot-Backend mit Recipe-Modell, Repository/Service/Controller-Schicht und
externer Rezept-Integration sowie ein Vue-Frontend mit Routing, Suche und Formularen für eigene Rezepte. Geplant war, dieses Fundament zu übernehmen und um
Login, Profilverwaltung, Pantry, Einkaufsliste, Wochenplaner, Nährwert-/Zielorientierung und Restaurantsuche zu erweitern - bei gleichzeitiger Migration des
Backends von Spring Boot zu Quarkus.

**Diese Kernidee wurde nicht nur vollständig umgesetzt, sondern im Projektverlauf fachlich sinnvoll erweitert.**

Aus dem Konzept realisiert:

| Geplant im Konzept | Umgesetzt im Code |
|---|---|
| Migration Spring Boot → Quarkus | Vollständig auf Quarkus 3.35.2 (Java 21) umgestellt |
| Login / Profilverwaltung | JWT-Auth (`auth`) + Präferenzverwaltung (`profile`, `UserPreferences`) |
| Intelligente Filterung & Personalisierung | Filter nach Diät, Allergie, Kalorien, Zubereitungszeit; profilbasierte Empfehlungen im Frontend |
| Pantry-/Bestandslogik | Eigenständiges `pantry`-Modul mit CRUD |
| Einkaufslistenverwaltung | Eigenständiges `shopping`-Modul, inkl. automatischer Generierung aus dem Wochenplan |
| Wochenplaner | `mealplan`-Modul mit Tages-Slots, Verschieben/Tauschen von Einträgen |
| Nährwert- und Zielorientierung | Kalorien-/Protein-Snapshots im Wochenplan, Kalorienziel im Profil |
| Restaurantsuche | `restaurant`-Modul mit Geocoding (Geoapify) und Web-Suche (Tavily) |

Darüber hinausgehend - im ursprünglichen Konzept nicht vorgesehen, aber im Projektverlauf ergänzt:

- **Mehrsprachigkeit**: Rezeptdaten und Oberfläche konsequent in Deutsch und Englisch, inklusive kuratierter zweisprachiger Seed-Daten.
- **Zwei komplementäre Planungsmodi** im Wochenplaner (manuell und Swipe-basiert) statt einer einzelnen Zuordnungslogik.
- **Import/Export von Rezepten** als JSON, um Rezepte außerhalb der Plattform zu sichern oder zu teilen.
- **PDF-/Druckexport** für Vorrat, Einkaufsliste und Wochenplan, um diese auch offline nutzbar zu machen.

## Fachliches Konzept

Die fachliche Idee hinter Dishly Smart ist, dass eine Mahlzeitenentscheidung selten isoliert getroffen wird - sie hängt von dem ab, was im Vorrat ist, was zum
Ernährungsprofil passt, was diese Woche schon geplant ist und was am Ende auf der Einkaufsliste landet. Genau dieses Zusammenspiel bildet die Anwendung ab:

- **Rezepte** sind der gemeinsame Nenner: sowohl eigene, veröffentlichte Rezepte als auch extern bezogene (Spoonacular) werden im selben Datenmodell behandelt
  und lassen sich gleichermaßen planen, favorisieren oder in die Einkaufsliste überführen.
- **Profil & Präferenzen** filtern und personalisieren diesen Rezeptbestand: Allergien schließen Ergebnisse aus, Diätformen (vegan, glutenfrei, …) und Ziele
  (Kalorienbudget, Proteinfokus) priorisieren sie.
- **Wochenplan** überführt eine Rezeptauswahl in konkrete Tage und Mahlzeiten-Slots (Frühstück, Mittag, Abend, Snack) - inklusive Kalorien-Snapshot pro Tag.
- **Einkaufsliste** entsteht entweder manuell oder automatisch aus dem Wochenplan: Zutaten mehrerer geplanter Rezepte werden zusammengeführt und kategorisiert.
- **Vorrat (Pantry)** reduziert Redundanz - Zutaten, die bereits vorhanden sind, müssen nicht erneut eingeplant oder gekauft werden.
- **Restaurantsuche** greift die ursprüngliche Rezeptidee auf, wenn Selberkochen keine Option ist, und sucht standortbasiert nach passenden Alternativen.

Der Mehrwert gegenüber einer isolierten Rezept- oder Einkaufslisten-App liegt genau in dieser Verzahnung: Eine Entscheidung in einem Modul
(z. B. „dieses Rezept diese Woche kochen") wirkt sich unmittelbar auf ein anderes Modul aus (Einkaufsliste), statt dass Nutzer:innen den
Abgleich manuell vornehmen müssen.

## Technologiestack

### Backend

| Bereich | Technologie | Zweck im Projekt |
|---|---|---|
| Framework | Quarkus 3.35.2 | Cloud-natives REST-Backend, Basis für alle fachlichen Module |
| Sprache | Java 21 | Implementierungssprache des Backends |
| Build | Gradle (Wrapper) | Build, Test-Ausführung, Docker-Build |
| REST/JSON | `quarkus-rest-jackson` | REST-Endpunkte inkl. JSON-(De-)Serialisierung |
| ORM / Persistenz | Hibernate ORM mit Panache | Entity-Repository-Zugriff auf PostgreSQL |
| Datenbank | PostgreSQL | Persistenz für User, Rezepte, Wochenplan, Pantry, Einkaufsliste, Präferenzen, Favoriten |
| Migrationen | Flyway (`quarkus-flyway`) | Versionierte Schema-Migration (`V1__initial_schema.sql`) |
| Validation | `quarkus-hibernate-validator` | Bean-Validation auf DTO-Ebene |
| Authentifizierung | `quarkus-smallrye-jwt`, `smallrye-jwt-build` | Ausstellung und Prüfung von JWTs (HS256) |
| Security | `quarkus-security`, `quarkus-elytron-security-common` | Security-Infrastruktur, Passwort-Hashing (PBKDF2) |
| API-Dokumentation | `quarkus-smallrye-openapi` | OpenAPI-Spezifikation unter `/q/openapi`, Swagger UI |
| Testing | JUnit 5, Mockito, REST-Assured, Quarkus Dev Services/Testcontainers | Unit-, Service- und Integrationstests inkl. echter PostgreSQL-Instanz |
| Externe API — Rezepte | Spoonacular | Externe Rezeptsuche und -details |
| Externe API — Web-Suche | Tavily | Anleitungs-/Bildsuche für Rezepte, Restaurant-Suche |
| Externe API — Geocoding | Geoapify | Umwandlung von GPS-Koordinaten in Adressdaten für die Restaurantsuche |
| Externe API — Object Storage | Supabase Storage | Upload und Hosting von Rezeptbildern |
| Container / Deployment | Docker (Multi-Stage: `gradle:jdk21-jammy` → `eclipse-temurin:21-jre-jammy`) | Produktions-Image, Quarkus-Fast-JAR |

### Frontend

| Bereich | Technologie | Zweck im Projekt |
|---|---|---|
| Framework | Vue 3 (Composition API) | Single-Page-Application |
| Sprache | TypeScript | Durchgängige Typisierung von Views, Stores, API-Clients |
| State Management | Pinia | `authStore`, `searchStore`, `toastStore` |
| Routing | Vue Router 4 | Client-seitiges Routing mit Auth-Guards |
| HTTP-Kommunikation | Axios | Zentraler `apiClient` mit Request-/Response-Interceptors |
| UI / Styling | Bootstrap 5, eigene CSS-Design-Tokens | Layout und Basiskomponenten |
| Build-Tool | Vite | Dev-Server und Produktions-Build |
| Internationalisierung | Vue I18n | Deutsch/Englisch, umschaltbar zur Laufzeit |
| Barcode-Scanning | ZXing (`@zxing/browser`, `@zxing/library`) | Kamerabasierte Erkennung im Pantry |
| Testing | Vitest, Jest, Cypress, `@vue/test-utils` | Unit-, Komponenten- und End-to-End-Tests |
| Codequalität | ESLint, Prettier, `vue-tsc` | Linting, Formatierung, Typprüfung |
| Deployment | `vercel.json` (SPA-Rewrite) | Konfiguration für statisches Hosting |

## Komponentenarchitektur

### Systemüberblick

Dishly Smart folgt einer klassischen Drei-Schichten-Architektur mit klarer Trennung von Präsentation, Anwendungslogik und Persistenz, ergänzt um mehrere
externe Dienste:

```
┌─────────────────────────────┐
│   Frontend (Vue 3 SPA)      │
│   Views · Components ·      │
│   Pinia-Stores · Router     │
└──────────────┬──────────────┘
               │ REST (HTTP/JSON), Axios, Bearer-Token
┌──────────────▼──────────────┐
│   Backend (Quarkus)         │
│   Resource-Schicht (REST)   │
│   Service-Schicht           │
│   Repository-Schicht        │
└──────┬────────────────┬─────┘
       │                │
┌──────▼──────┐  ┌──────▼───────────────────────────┐
│ PostgreSQL  │  │ Externe Dienste (HTTP-Clients)    │
│ (Flyway-    │  │ Spoonacular · Tavily · Geoapify · │
│  verwaltet) │  │ Supabase Storage · Groq           │
└─────────────┘  └────────────────────────────────────┘
```

Ein Request durchläuft dabei immer denselben Pfad: Das Frontend ruft über den zentralen `apiClient` einen REST-Endpunkt auf und hängt bei geschützten Routen
automatisch den Bearer-Token aus dem `authStore` an. Im Backend nimmt eine **Resource**-Klasse den Request entgegen, validiert Eingaben über
Bean-Validation-Annotationen auf den DTOs und delegiert an die zuständige **Service**-Klasse. Die Service-Schicht enthält die eigentliche Geschäftslogik
etwa Owner-Prüfungen, Aggregation von Einkaufslisten-Positionen oder das Ansprechen externer APIs — und greift über ein **Repository** (Panache) auf PostgreSQL
zu oder ruft einen dedizierten HTTP-Client für einen externen Dienst auf. Die Antwort wird über einen **Mapper** von der Entity in ein Response-DTO überführt,
bevor sie an das Frontend zurückgeht. Fehler werden nicht in den Resource-Klassen behandelt, sondern zentral über `ExceptionMapper`-Klassen im `shared`-Package
in konsistente HTTP-Fehlerantworten (`404`, `401`, `403`, `409`) übersetzt.

Diese Trennung ist bewusst gewählt: Die Resource-Schicht bleibt dünn und rein transport-orientiert, die Service-Schicht bündelt fachliche Regeln testbar an
einer Stelle, und externe Abhängigkeiten (Spoonacular, Tavily, Geoapify, Supabase, Groq) sind hinter eigenen Client-Klassen gekapselt — ein Ausfall oder Wechsel
eines externen Dienstes betrifft damit nur eine klar abgegrenzte Stelle im Code, nicht die gesamte Anwendung.

### Package-by-Feature im Backend

Statt nach technischer Schicht (`controllers/`, `services/`, `repositories/`) ist das Backend **nach fachlichem Modul** organisiert - jedes Modul unter
`de.htwberlin.webtech` bringt seine komplette Schichtenlogik selbst mit:

```
de.htwberlin.webtech/
├── auth/        Registrierung, Login, aktueller User (JWT-Ausstellung)
├── user/        AppUser-Entity, Rollen (USER, ADMIN)
├── recipe/      Eigene & externe Rezepte, Bild-Upload, Seed-Import
├── mealplan/    Wochenplaner inkl. automatischer Einkaufslisten-Ableitung
├── pantry/      Vorratsverwaltung
├── shopping/    Manuelle Einkaufsliste
├── favorite/    Favoriten für externe Rezepte
├── profile/     Ernährungsprofil & Präferenzen
├── restaurant/  Standortbasierte Restaurantsuche
├── security/    JWT-Validierung, Passwort-Hashing, UserContext
└── shared/      Zentrale Exceptions & ExceptionMapper
```

Ein typisches fachliches Modul (z. B. `pantry`) folgt demselben Muster:

- **Resource** (`PantryResource`) — REST-Endpunkte, nimmt Requests entgegen, ruft den Service auf.
- **Service** (`PantryService`) — Geschäftslogik, u. a. Owner-Prüfung.
- **Repository** (`PantryItemRepository`) — Panache-Repository für den Datenbankzugriff.
- **Entity** (`PantryItem`) — JPA-Entity, bildet die Datenbanktabelle ab.
- **DTOs** (`PantryItemRequest`, `PantryItemResponse`) — Transportobjekte für eingehende/ausgehende Daten, getrennt von der Entity.
- **Mapper** (`PantryItemMapper`) — Übersetzung zwischen Entity und DTO.
- **Exception** (`PantryItemNotFoundException`) — modul-spezifische Fehlerklasse, zentral in `shared` auf eine HTTP-Antwort gemappt.

Module mit externen Abhängigkeiten erweitern dieses Muster um dedizierte **Client**-Klassen
(z. B. `SpoonacularExternalRecipeClient`, `GeoapifyClient`, `TavilyRestaurantSearchClient`, `GroqClient`, `SupabaseStorageService`),
die die HTTP-Kommunikation mit dem jeweiligen Drittanbieter kapseln.

### Struktur im Frontend

Auch das Frontend ist fachlich statt rein technisch gegliedert:

- **`views/`** — 16 Seiten, jede einem fachlichen Bereich zugeordnet: `HomeView` (Rezept-Feed), `MyRecipesView`/`NewRecipeView`/`EditRecipeView`/`RecipeDetailView`
  (eigene Rezepte), `MealPlanView` (Wochenplan), `ShoppingListView`/`ShoppingListRecipesView` (Einkaufsliste), `PantryView` (Vorrat), `ProfileView` (Präferenzen),
  `LoginView`/`RegisterView` (Auth), `DashboardView`, `AboutView`, `ContactView`.
- **`components/`** — wiederverwendbare, fachlich zugeschnittene Bausteine: `ApiRecipeList`/`RecipeList` (Rezeptdarstellung), `SuggestInput`/`TagInput` (Formulareingaben),
  `AppToast`/`ConfirmDialog` (App-weite UI-Bausteine), `LanguageSwitcher`.
- **`stores/`** — Pinia-Stores für querschnittlichen Zustand: `authStore` (Token, aktueller User, Session-Status), `searchStore` (Filter der Startseite),
  `toastStore` (Benachrichtigungen).
- **`shared/api/`** — je Backend-Modul ein eigener API-Client (`recipeApi`, `mealPlanApi`, `pantryApi`, `shoppingListApi`, `favoriteApi`, `profileApi`,
  `restaurantApi`, `authApi`), alle aufbauend auf dem zentralen `apiClient`.
- **`router/`** — eine zentrale Router-Konfiguration mit `beforeEach`-Guard: Routen mit `meta.requiresAuth` prüfen das Vorhandensein eines Tokens und
  leiten andernfalls zu `/login` um.
- **`types/`** — TypeScript-Typen parallel zu den Backend-DTOs, ein Typmodul pro fachlichem Bereich.

## Wichtigste Komponenten

**Auth (`auth`, `user`, `security`)** — Registrierung, Login und Token-Validierung. Ausgabe eines JWT (HS256) mit konfigurierbarer Gültigkeit
(Standard: 1 Stunde), Passwort-Hashing über PBKDF2. `UserContext` extrahiert den aktuellen User aus dem Bearer-Token und wird von allen geschützten Endpunkten
für Owner-Prüfungen genutzt.

**Rezepte (`recipe`)** — Zentrales Modul für eigenes CRUD sowie die Integration externer Rezepte. Enthält neben der klassischen Resource/Service/Repository-Kette
einen Spoonacular-Client mit API-Key-Rotation (`SpoonacularKeyRotator`) für höhere Rate-Limits, einen Tavily-Client zur Suche nach Anleitungsbildern, einen Supabase-Client
für Bild-Uploads (max. 5 MB) sowie einen JSON-basierten Seed-Importer für kuratierte deutsche und englische Startrezepte.

**Profil (`profile`)** — Verwaltet `UserPreferences`: Vorlieben, Abneigungen und Allergien als Mengen, Diätformen (vegan, vegetarisch, glutenfrei, laktosefrei),
Ziele (proteinreich, kalorienbewusst, budgetfreundlich), maximale Zubereitungszeit sowie ein tägliches Kalorienziel. Steuert damit direkt Filterung und
Personalisierung im Frontend.

**Vorrat (`pantry`)** — Persönliche Liste vorhandener Zutaten mit Menge, Einheit und Kategorie. Dient als Grundlage, um Rezeptvorschläge und Einkaufslisten um
bereits vorhandene Zutaten zu bereinigen.

**Einkaufsliste (`shopping`)** — Manuell pflegbare Liste mit Kategorisierung und Abhak-Status; wird ergänzt durch eine automatische Ableitung aus dem Wochenplan.

**Wochenplan (`mealplan`)** — Ordnet Rezepte (eigene, externe oder frei eingetragene) einzelnen Tagen und Mahlzeiten-Slots (`BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`) zu,
mit Kalorien-/Protein-Snapshot je Eintrag. Unterstützt Verschieben und Tauschen von Einträgen zwischen Tagen.

**Favoriten (`favorite`)** — Merkliste für extern bezogene Rezepte (Spoonacular), unabhängig vom eigenen Rezeptbestand, mit eindeutiger Zuordnung pro Nutzer, Quelle und externer ID.

**Restaurantsuche (`restaurant`)** — Zwei komplementäre Suchwege: Geoapify wandelt GPS-Koordinaten in Ortsdaten um, Tavily sucht darauf aufbauend im Web nach Restaurants, die zu einem Rezepttitel passen.

## Wichtigste Funktionalitäten

- **Personalisierter Rezept-Feed** — Die Startseite kombiniert externe Rezepte (Spoonacular) mit Profil-Filtern (Diät, Allergien, Kalorien, Zubereitungszeit)
  und einer Shuffle-Funktion.
- **Gast- vs. eingeloggte Nutzung** — Rezeptübersicht, Rezeptdetails, About und Contact sind ohne Login erreichbar; Dashboard, eigene Rezepte, Profil, Pantry,
  Einkaufsliste und Wochenplan erfordern Authentifizierung (Router-Guard + Backend-Autorisierung).
- **Profilverwaltung** — Vorlieben, Abneigungen, Allergien, Diätform und Kalorienziel werden über `TagInput`-Komponenten gepflegt und fließen in Filterung
  und Empfehlungen ein.
- **Wochenplanung (manuell & Swipe)** — Neben der klassischen Tages-/Slot-Zuordnung existiert ein Swipe-Modus zur schnelleren Planung sowie Drag & Drop
  zum Verschieben und Tauschen von Einträgen.
- **Vorrat mit Barcode-Scan** — Zutaten lassen sich per Kamera (ZXing) über den Barcode erfassen, alternativ manuell eintragen.
- **Einkaufsliste** — Manuell pflegbar oder automatisch aus dem aktuellen Wochenplan generiert, mit Zusammenführung gleicher Zutaten und Kategorisierung.
- **Eigene Rezepte** — Vollständiges CRUD inklusive Bild-Upload per Drag & Drop, Veröffentlichungs-Status und Sichtbarkeit auf der Startseite.
- **Favoriten** — Sowohl für eigene als auch für externe Rezepte, mit eigener Favoriten-Ansicht.
- **Restaurant-Finder** — Standortbasierte Suche nach Restaurants passend zu einem Rezepttitel.
- **Import/Export & PDF** — JSON-Import/Export für eigene Rezepte sowie Druck-/PDF-Export für Wochenplan, Einkaufsliste und Vorrat.
- **Externe Rezeptdaten** — Rezeptsuche und -details über Spoonacular, inklusive Nährwertangaben, Diät-Kennzeichnungen (vegan, vegetarisch, glutenfrei,
  laktosefrei) und Anleitungsschritten.
- **Mehrsprachigkeit** — Durchgängige Lokalisierung der Oberfläche und der Seed-Rezepte in Deutsch und Englisch.

## Setup und Installation

### Voraussetzungen

- **Backend:** JDK 21, Docker Desktop (für Quarkus Dev Services / Testcontainers)
- **Frontend:** Node.js `^20.19.0` oder `>=22.12.0`
- Kein manuelles PostgreSQL-Setup nötig — Quarkus startet im Dev-Modus automatisch eine temporäre Datenbank über Docker

### 1. Backend starten

```bash
cd kochbuch-backend
./gradlew quarkusDev
```

Im Dev-Modus stellt Quarkus Dev Services automatisch eine PostgreSQL-Instanz bereit (Voraussetzung: Docker Desktop läuft) und wendet die Flyway-Migration (`V1__initial_schema.sql`) an.
Beim ersten Start werden zusätzlich die kuratierten deutschen und englischen Rezept-Seeds importiert. Das Backend läuft anschließend unter `http://localhost:8080`,
die OpenAPI-Dokumentation unter `http://localhost:8080/q/openapi` bzw. die Swagger UI unter `http://localhost:8080/q/swagger-ui`.

Für Funktionen, die externe APIs nutzen (externe Rezepte, Restaurantsuche, Bild-Upload), müssen die entsprechenden Keys als Umgebungsvariablen bzw.
`application.properties`-Werte gesetzt sein (siehe Tabelle unten). Ohne diese Keys starten Backend und die übrigen Endpunkte trotzdem — die jeweilige Funktion liefert
dann Fehler oder eine Konfigurationsmeldung.

### 2. Frontend starten

```bash
cd kochbuch-frontend
npm install
npm run dev
```

Das Frontend läuft unter `http://localhost:5173` und ist über `.env.development` (`VITE_BACKEND_BASE_URL=http://localhost:8080`) bereits auf das lokale Backend
konfiguriert. CORS ist im Backend-Dev-Profil standardmäßig für genau diesen Origin freigegeben.

### Umgebungsvariablen (Produktion / optionale Funktionen)

| Variable | Modul | Pflicht in Produktion |
|---|---|---|
| `DATABASE_URL` / `DB_URL`, `DB_USER`, `DB_PASSWORD` | Datenbank | Ja |
| `JWT_SECRET` (mind. 32 Zeichen) | Auth | Ja — Start bricht sonst ab |
| `JWT_ISSUER`, `JWT_EXPIRES_IN_SECONDS` | Auth | Nein (Defaults vorhanden) |
| `CORS_ORIGINS` | CORS | Ja |
| `spoonacular.api.keys` / `spoonacular.api.key` | Externe Rezepte | Nein, aber ohne Key keine externen Rezepte |
| `tavily.api.key` | Anleitungssuche, Restaurantsuche | Nein, aber ohne Key keine Tavily-Suche |
| `GEOAPIFY_API_KEY` | Restaurantsuche (Geocoding) | Nein, aber ohne Key kein Geocoding |
| `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_BUCKET` | Bild-Upload | Nein, aber ohne Werte kein Bild-Upload |

## Projektstruktur

- **Backend:** `kochbuch-backend/src/main/java/de/htwberlin/webtech/` — ein Ordner pro fachlichem Modul (siehe [Komponentenarchitektur](#komponentenarchitektur)). Konfiguration
  in `src/main/resources/application.properties`, Migrationen in `src/main/resources/db/migration/`, Seed-Rezepte in `src/main/resources/recipes/{de,en}/`.
- **Frontend:** `kochbuch-frontend/src/` — `views/` für Seiten, `components/` für wiederverwendbare Bausteine, `stores/` für Pinia-State, `shared/api/` für
  die Backend-Kommunikation, `router/` für die Navigationslogik, `types/` für die TypeScript-Modelle.

## Qualitätssicherung

**Backend:** 45 Testdateien über Unit-, Service-, Resource- und Repository-Ebene (JUnit 5, Mockito, REST-Assured). Repository-Integrationstests
(u. a. `RecipeRepositoryIntegrationTest`, `AppUserRepositoryIntegrationTest`, `MealPlanRepositoryIntegrationTest`)
laufen gegen eine echte, von Quarkus Dev Services bereitgestellte PostgreSQL-Instanz statt gegen Mocks. Ein eigener `FlywayMigrationTest` prüft die
Schema-Migration. Bean-Validation über `quarkus-hibernate-validator` sichert Eingaben auf DTO-Ebene ab, zentrale `ExceptionMapper`-Klassen sorgen für
konsistente Fehlerantworten.

**Frontend:** Unit- und Store-Tests mit Vitest sowie parallel mit Jest, Komponententests mit `@vue/test-utils`, End-to-End-Smoke-Test mit Cypress
(`cypress/e2e/smoke.cy.ts`). Codequalität wird über ESLint (inkl. Vue-, TypeScript- und Cypress-Regeln) und Checkstyle durchgesetzt, `vue-tsc` prüft die
Typkorrektheit vor jedem Produktions-Build.

## Fazit

Dishly Smart zeigt, wie aus einem einfachen Recipe-CRUD-Fundament durch konsequente fachliche Erweiterung ein zusammenhängendes System entstehen kann:
Rezepte, Profil, Vorrat, Wochenplan, Einkaufsliste und Restaurantsuche greifen nicht nebeneinander her, sondern über klar geschnittene Services ineinander.
Die Package-by-Feature-Struktur im Backend hält jedes fachliche Modul in sich abgeschlossen und testbar, während externe Abhängigkeiten konsequent hinter
eigenen Client-Klassen gekapselt sind — ein Architekturentscheid, der sich bereits daran zeigt, dass fünf unterschiedliche Drittanbieter-APIs
(Spoonacular, Tavily, Geoapify, Supabase) integriert wurden, ohne die Kernlogik der einzelnen Module zu verwässern.
Die 45 Backend-Tests inklusive echter Datenbank-Integrationstests und die durchgängige Trennung von Entity, DTO und Mapper unterstreichen, dass hier nicht nur
Features aneinandergereiht, sondern mit Blick auf Wartbarkeit gebaut wurde. Genau darin liegt die Stärke des Projekts: Es hat die ursprüngliche Konzeptidee
nicht nur erfüllt, sondern zu einem System ausgebaut, das über die Summe seiner Einzelteile hinausgeht.
