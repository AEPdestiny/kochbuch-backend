# ADR-014: Wochenplaner / Meal Planner

## Status

Vorgeschlagen

## Kontext

Dishly Smart enthält bereits persönliche Rezepte, Vorrat und Einkaufsliste. Ein Wochenplaner verbindet diese Bereiche fachlich sinnvoll: Nutzer können eigene Rezepte nicht nur speichern, sondern konkret für die kommende Woche einplanen. Dadurch entsteht ein klarer nächster Schritt von der Rezeptsammlung hin zur Alltagsplanung.

Langfristig kann der Wochenplaner als Grundlage dienen, um fehlende Zutaten aus geplanten Rezepten zu erkennen, mit dem Vorrat abzugleichen und daraus Einträge für die Einkaufsliste vorzuschlagen. Für das MVP wird diese Automatisierung bewusst noch nicht umgesetzt, weil Zutaten aktuell als Freitext gespeichert werden und dadurch keine zuverlässige Mengen- oder Zutatenlogik möglich ist.

## Entscheidung

Dishly Smart erhält einen einfachen Wochenplaner für eingeloggte Nutzer.

Für das MVP gilt:

- Es wird die aktuelle Woche angezeigt.
- Die Woche besteht aus Montag bis Sonntag.
- Pro Tag kann maximal ein eigenes Rezept geplant werden.
- Es sind nur Rezepte planbar, die dem eingeloggten Nutzer gehören.
- Es wird keine automatische Einkaufsliste erzeugt.
- Externe Rezepte werden im MVP nicht direkt planbar gemacht.

## Datenmodell

Es wird eine neue Entity `MealPlan` eingeführt.

Geplante Felder:

- `id`
- `owner` / `AppUser`
- `recipe`
- `plannedDate`
- `createdAt`
- `updatedAt`

Das Ownership-Konzept folgt der bestehenden AppUser-Strategie: Jeder MealPlan-Eintrag gehört genau einem Nutzer. Ein Nutzer sieht und verändert ausschließlich eigene Einträge.

Für das MVP soll ein Unique-Konzept gelten:

- ein Eintrag pro User und `plannedDate`

Dadurch kann ein Rezept für einen Tag gesetzt, ersetzt oder entfernt werden, ohne mehrere parallele Einträge für denselben Tag zu erzeugen.

## Backend-Struktur

Geplante Struktur:

```text
mealplan/
  entity/
    MealPlan.java
  dto/
    MealPlanEntryRequest.java
    MealPlanEntryResponse.java
    MealPlanWeekResponse.java
  mapper/
    MealPlanMapper.java
  repository/
    MealPlanRepository.java
  service/
    MealPlanService.java
  resource/
    MealPlanResource.java
```

Der Service ist verantwortlich für:

- Laden der Woche des aktuellen Nutzers
- Setzen oder Ersetzen eines Rezepts für einen Tag
- Entfernen eines geplanten Rezepts
- Prüfung, ob das gewählte Rezept dem aktuellen Nutzer gehört
- Prüfung, ob der MealPlan-Eintrag dem aktuellen Nutzer gehört

## API

Alle Endpunkte benötigen einen gültigen Bearer Token.

### GET `/meal-plan/week?startDate=YYYY-MM-DD`

Liefert die Wochenplanung des aktuellen Nutzers.

Verhalten:

- Wenn `startDate` gesetzt ist, wird die Woche zu diesem Datum geladen.
- Wenn `startDate` fehlt, wird die aktuelle Woche geladen.
- Die Response enthält die Woche von Montag bis Sonntag.
- Es werden nur Einträge des aktuellen Nutzers geliefert.

### PUT `/meal-plan/days/{date}`

Setzt oder ersetzt das Rezept für einen konkreten Tag.

Request Body:

```json
{
  "recipeId": 123
}
```

Verhalten:

- Wenn für diesen Tag noch kein Eintrag existiert, wird ein neuer Eintrag erstellt.
- Wenn bereits ein Eintrag existiert, wird das Rezept ersetzt.
- Das Rezept muss dem aktuellen Nutzer gehören.

### DELETE `/meal-plan/days/{date}`

Entfernt das geplante Rezept für einen konkreten Tag.

Verhalten:

- Eigener Eintrag wird gelöscht.
- Nicht vorhandener Eintrag kann als `404 Not Found` behandelt werden.
- Fremde Einträge sind nicht sichtbar.

## Sicherheitsregeln

- Alle MealPlan-Endpunkte benötigen Bearer Token.
- Ohne Token wird `401 Unauthorized` zurückgegeben.
- Nutzer sehen nur eigene MealPlan-Einträge.
- Nutzer dürfen nur eigene Rezepte planen.
- Fremdes Rezept planen führt zu `403 Forbidden`.
- Unbekanntes Rezept führt zu `404 Not Found`.
- Fremde MealPlan-Einträge werden nicht sichtbar gemacht.

## Frontend-Auswirkungen

Geplante neue Route:

- `/meal-plan`
- `meta.requiresAuth: true`

Geplante Dateien:

```text
src/views/MealPlanView.vue
src/shared/api/mealPlanApi.ts
src/types/mealPlan.ts
```

Navigation:

- Der Menüpunkt für den Wochenplan wird nur für eingeloggte Nutzer angezeigt.

UI-Konzept:

- Wochenansicht mit Karten für Montag bis Sonntag
- jede Karte zeigt Datum und geplantes Rezept
- Rezept-Auswahl aus `recipeApi.getMyRecipes()`
- Aktion zum Hinzufügen oder Ersetzen eines Rezepts
- Aktion zum Entfernen eines Rezepts
- responsive Darstellung für Desktop und Mobile

## i18n

Alle neuen sichtbaren UI-Texte werden von Anfang an über `vue-i18n` geführt.

Rezeptdaten selbst werden nicht übersetzt:

- Rezepttitel
- Zutaten
- Kategorien
- Anleitungen
- externe Rezeptdaten

Das folgt der bestehenden Trennung zwischen UI-i18n und Daten-i18n.

## Verbindung zu Vorrat und Einkaufsliste

Später kann der Wochenplaner genutzt werden, um Zutaten aus geplanten Rezepten zu sammeln und mit dem Vorrat abzugleichen. Daraus könnten Einkaufsliste-Vorschläge entstehen.

Im MVP wird dies bewusst nicht umgesetzt, weil Zutaten aktuell als Freitext vorliegen. Eine belastbare Automatisierung erfordert später strukturierte Zutaten, Mengen und Einheiten.

## Risiken

- Datums- und Zeitzonenlogik kann zu falschen Wochen oder Tagesgrenzen führen.
- Rezeptlöschung muss sauber behandelt werden, wenn ein Rezept bereits im Wochenplan verwendet wird.
- Ein Rezept pro Tag ist MVP-tauglich, aber langfristig möglicherweise zu eingeschränkt.
- Freitext-Zutaten erschweren spätere Automatisierung Richtung Einkaufsliste.
- Es fehlen weiterhin Flyway-/Liquibase-Migrationen für kontrollierte Schemaänderungen.

## Teststrategie

Backend:

- `GET /meal-plan/week` ohne Token → `401`
- Woche für aktuellen Nutzer laden → `200`
- Einträge fremder Nutzer werden nicht geliefert
- eigenes Rezept für Tag planen → `200`
- fremdes Rezept planen → `403`
- unbekanntes Rezept planen → `404`
- geplantes Rezept ersetzen → `200`
- geplantes Rezept entfernen → `204`
- unbekannten Tageseintrag entfernen → `404`

Frontend:

- Route Guard für `/meal-plan`
- Wochenansicht zeigt Montag bis Sonntag
- eigene Rezepte werden geladen
- Rezept kann einem Tag zugeordnet werden
- Rezept kann entfernt werden
- Fehlerfälle werden verständlich angezeigt
- i18n-Texte werden in der View verwendet

## Schritt-für-Schritt-Implementierungsplan

1. Backend Schritt 1:
   - `MealPlan` Entity anlegen
   - DTOs und Mapper ergänzen
   - Repository, Service und Resource implementieren
   - Ownership-Regeln testen

2. Frontend API/Typen:
   - `mealPlan.ts` anlegen
   - `mealPlanApi.ts` anlegen
   - API-Tests ergänzen

3. Frontend View:
   - `/meal-plan` Route ergänzen
   - `MealPlanView.vue` bauen
   - Navigation für eingeloggte Nutzer ergänzen
   - i18n-Keys ergänzen

4. Smoke-Test:
   - User registrieren oder einloggen
   - eigene Rezepte erstellen
   - Rezept für Wochentag planen
   - Rezept ersetzen
   - Rezept entfernen
   - zweiter User sieht keine fremden Einträge

5. Spätere Erweiterungen:
   - mehrere Mahlzeiten pro Tag
   - Planung externer Rezepte
   - Zutatenstrukturierung
   - Einkaufsliste aus Wochenplan erzeugen
   - Pantry-Abgleich

## Konsequenzen

Der Wochenplaner erweitert Dishly Smart sinnvoll, ohne bestehende Module umzubauen. Das MVP bleibt klein, testbar und kompatibel mit der bestehenden Architektur aus Quarkus, PostgreSQL, JWT, AppUser Ownership und Vue/Vite.

Die Einkaufsliste-Integration wird bewusst später umgesetzt, sobald Zutaten strukturierter vorliegen.

## Empfehlung

Der Wochenplaner sollte als nächster MVP-Schritt umgesetzt werden. Die erste Implementierung sollte mit Backend `GET/PUT/DELETE` für eigene Wochenplan-Einträge beginnen. Danach kann das Frontend eine einfache Wochenansicht mit Rezeptauswahl erhalten.
