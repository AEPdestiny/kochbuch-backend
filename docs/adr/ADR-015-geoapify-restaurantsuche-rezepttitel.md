# ADR-015: Geoapify-Restaurantsuche anhand Rezepttitel

## Status

Vorgeschlagen

## Kontext

Dishly Smart unterstützt bereits persönliche Rezepte, veröffentlichte Rezepte, externe Rezeptdaten, Vorrat, Einkaufsliste und Wochenplanung. Eine Restaurantsuche ergänzt diesen Nutzungspfad sinnvoll: Nutzer können bei einem Rezept entscheiden, ob sie selbst kochen oder ein passendes Restaurant in der Nähe suchen möchten.

Der Zusammenhang mit bestehenden Funktionen ist fachlich naheliegend:

- Rezepte liefern den Suchkontext.
- Der Wochenplan zeigt geplante Mahlzeiten.
- Die Restaurantsuche bietet eine Alternative zum Kochen.
- Nutzer können anhand ihrer Situation entscheiden: kochen, einkaufen oder auswärts essen.

Für das MVP soll diese Funktion bewusst klein bleiben. Es wird keine Kartenansicht gebaut und es werden keine Standortdaten gespeichert. Die Suche wird nur durch eine aktive Nutzeraktion ausgelöst.

## Entscheidung

Dishly Smart soll eine Restaurantsuche anhand des Rezepttitels erhalten.

Für das MVP gilt:

- Die Suche wird durch einen Button bei einem Rezept ausgelöst.
- Der Suchbegriff ist der Rezepttitel.
- Das Frontend fragt den Browser-Standort über `navigator.geolocation.getCurrentPosition` ab.
- Das Frontend sendet Suchbegriff, Latitude und Longitude an das Backend.
- Das Backend ruft die Geoapify Places API auf.
- Ergebnisse werden als einfache Liste angezeigt.
- Pro Restaurant wird ein Google-Maps-Link erzeugt.
- Es wird keine Kartenansicht umgesetzt.
- Standortdaten werden nicht gespeichert.
- Es werden keine API-Keys im Frontend verwendet.

## Backend-Architektur

Geplante Backend-Struktur:

```text
restaurant/
  client/
    GeoapifyClient.java
  dto/
    RestaurantSearchRequest.java
    RestaurantResponse.java
    GeoapifyPlacesResponse.java
    GeoapifyFeature.java
  resource/
    RestaurantResource.java
  service/
    RestaurantService.java
```

### RestaurantResource

Die Resource stellt den API-Endpunkt für das Frontend bereit.

Aufgaben:

- Request entgegennehmen
- Request validieren
- Service aufrufen
- kontrollierte HTTP-Antwort liefern

### RestaurantService

Der Service enthält die fachliche Logik.

Aufgaben:

- Query normalisieren
- Radius und Limit für das MVP setzen
- GeoapifyClient aufrufen
- Geoapify-Antwort auf `RestaurantResponse` mappen
- Google-Maps-Link erzeugen
- Fehler kontrolliert behandeln

### GeoapifyClient

Der Client kapselt den externen HTTP-Aufruf.

Aufgaben:

- Geoapify Places API aufrufen
- API-Key aus Konfiguration verwenden
- Timeouts und Fehler kontrolliert behandeln
- keine Secrets in Fehlerantworten offenlegen

## Backend-Endpoint

### POST `/restaurants/search`

Der Endpunkt sucht Restaurants in der Nähe einer Position.

Request:

```json
{
  "query": "Pasta Carbonara",
  "latitude": 52.52,
  "longitude": 13.405
}
```

Response:

```json
[
  {
    "name": "Restaurant Beispiel",
    "address": "Musterstraße 1, Berlin",
    "distanceMeters": 850,
    "googleMapsUrl": "https://www.google.com/maps/search/?api=1&query=52.5201,13.4052",
    "latitude": 52.5201,
    "longitude": 13.4052
  }
]
```

Validierung:

- `query` darf nicht leer sein.
- `latitude` muss vorhanden sein.
- `longitude` muss vorhanden sein.
- Ungültige Requests führen zu `400 Bad Request`.

## Geoapify-Konzept

Geoapify wird ausschließlich vom Backend aufgerufen.

Konfiguration:

```text
GEOAPIFY_API_KEY=<secret>
```

MVP-Parameter:

- Radius: `5000` Meter
- Limit: `5`
- Kategorie: `catering.restaurant`
- Freitextsuche: Rezepttitel

Das Backend kombiniert die Kategorie- und Standortsuche mit dem Rezepttitel als Suchtext. Wenn Geoapify keine passenden Restaurants liefert, gibt das Backend eine leere Liste zurück.

Fehlerbehandlung:

- Geoapify nicht erreichbar: kontrollierte Antwort, kein Stacktrace an den Client.
- API-Key fehlt oder ist ungültig: kontrollierter Fehler, Secret wird nicht geleakt.
- keine Ergebnisse: `200 OK` mit leerer Liste.

## Google Maps Link

Für das MVP wird kein Google API-Key benötigt.

Das Backend erzeugt pro Restaurant einen Link im Format:

```text
https://www.google.com/maps/search/?api=1&query=LAT,LON
```

Beispiel:

```text
https://www.google.com/maps/search/?api=1&query=52.5201,13.4052
```

Der Link öffnet Google Maps mit der Restaurantposition. Eine eingebettete Karte wird bewusst nicht umgesetzt.

## Frontend-Architektur

Geplante Frontend-Dateien:

```text
src/types/restaurant.ts
src/shared/api/restaurantApi.ts
src/components/RestaurantSearchModal.vue
```

Alternativ kann die erste UI klein im bestehenden Rezept-Overlay integriert werden, sofern dadurch kein großer Refactoring-Schritt entsteht.

### UI-Verhalten

Bei einem Rezept wird ein Button ergänzt:

```text
Restaurant in der Nähe finden
```

Ablauf:

1. Nutzer klickt auf den Button.
2. Frontend fragt Browser-Geolocation ab.
3. Bei erfolgreichem Standortzugriff sendet das Frontend `query`, `latitude` und `longitude` an `POST /restaurants/search`.
4. Ergebnisse werden als Liste angezeigt.
5. Jeder Treffer enthält einen Link zu Google Maps.

### Fehlerfälle im Frontend

Das Frontend soll folgende Fälle nutzerfreundlich behandeln:

- Browser unterstützt Geolocation nicht.
- Nutzer verweigert Standortzugriff.
- Standort konnte nicht ermittelt werden.
- Backend liefert keine Restaurants.
- Backend oder Geoapify ist nicht erreichbar.

## i18n

Alle neuen UI-Texte werden über `vue-i18n` vorbereitet.

Mögliche Keys:

- `restaurants.actions.findNearby`
- `restaurants.loading.location`
- `restaurants.loading.search`
- `restaurants.empty`
- `restaurants.errors.locationDenied`
- `restaurants.errors.geolocationUnsupported`
- `restaurants.errors.searchFailed`
- `restaurants.openInMaps`

Nicht übersetzt werden:

- Rezepttitel
- Restaurantnamen
- Adressen
- externe Geoapify-Daten

Das folgt der bestehenden Trennung zwischen UI-i18n und Daten-i18n.

## Datenschutz

Für das MVP gelten klare Datenschutzregeln:

- Standort wird nur nach aktiver Nutzeraktion abgefragt.
- Standort wird nicht in PostgreSQL gespeichert.
- Standort wird nicht im User-Profil gespeichert.
- Standort wird nicht historisiert.
- Latitude und Longitude werden nur für die konkrete Restaurantsuche ans Backend gesendet.
- Das Backend verwendet die Koordinaten nur für den Geoapify-Aufruf.

## Sicherheit

Sicherheitsregeln:

- `GEOAPIFY_API_KEY` wird nur als Backend Environment Variable gesetzt.
- Der API-Key wird niemals im Frontend ausgeliefert.
- Der API-Key wird nicht ins Repository geschrieben.
- Fehlerantworten dürfen keine Secrets enthalten.
- Logs dürfen keine vollständigen Secrets ausgeben.
- Geoapify Free-Tier und Rate-Limits müssen beachtet werden.

Der Endpunkt kann im MVP öffentlich bleiben, weil die eigentliche Schutzmaßnahme der serverseitige API-Key-Schutz ist. Falls Missbrauch oder Limits ein Problem werden, kann später Authentifizierung für `POST /restaurants/search` ergänzt werden.

## Risiken

### Rezepttitel als Suchbegriff

Rezepttitel sind nicht immer gute Restaurant-Suchbegriffe. Beispiele wie `Omas Auflauf` oder sehr kreative Rezeptnamen können unpassende Ergebnisse liefern.

Mögliche spätere Verbesserung:

- Kategorie oder Küche zusätzlich berücksichtigen
- Query vereinfachen
- Nutzer kann Suchbegriff manuell anpassen

### Standortberechtigung

Nutzer können die Standortfreigabe verweigern. Die UI muss diesen Fall sauber erklären und darf nicht kaputtgehen.

### Geoapify-Limits

Geoapify kann Free-Tier-Limits haben. Häufige Suchanfragen können das Limit belasten.

Mögliche spätere Verbesserung:

- kleines Backend-Caching pro Query und Standort-Raster
- Rate-Limit im Backend
- Auth-Pflicht für den Endpunkt

### Ergebnisqualität

Restaurants können unpassend, veraltet oder unvollständig sein. Dishly sollte Ergebnisse als externe Vorschläge darstellen.

### Standortgenauigkeit

Browser-Geolocation kann ungenau sein, besonders auf Desktop-Geräten ohne GPS. Ergebnisse können daher weiter entfernt oder unpassend sein.

## Teststrategie

### Backend

Backend-Tests:

- `RestaurantService` mit gemocktem `GeoapifyClient`
- Mapping von Geoapify-Antwort auf `RestaurantResponse`
- Google-Maps-Link wird korrekt erzeugt
- leere Geoapify-Antwort ergibt leere Liste
- Geoapify-Fehler wird kontrolliert behandelt
- `RestaurantResource` validiert Request
- `POST /restaurants/search` liefert erwartete Response
- API-Key wird nicht in Fehlerantworten geleakt

### Frontend

Frontend-Tests:

- `restaurantApi.ts` ruft `POST /restaurants/search` auf
- Geolocation-Erfolg wird gemockt
- Standort verweigert wird gemockt
- Geolocation nicht verfügbar wird getestet
- Ergebnisliste wird angezeigt
- Empty-State wird angezeigt
- Fehlerzustände werden angezeigt
- Google-Maps-Link ist vorhanden
- i18n-Texte werden verwendet

## Schritt-für-Schritt-Plan

1. ADR-015 akzeptieren.
2. Backend: `GEOAPIFY_API_KEY` in Konfiguration und README dokumentieren.
3. Backend: DTOs für Request und Response anlegen.
4. Backend: `GeoapifyClient` mit externer Places-API-Anbindung erstellen.
5. Backend: `RestaurantService` mit Mapping und Google-Maps-Link implementieren.
6. Backend: `RestaurantResource` mit `POST /restaurants/search` implementieren.
7. Backend: Service- und Resource-Tests ergänzen.
8. Frontend: `restaurant.ts` Typen anlegen.
9. Frontend: `restaurantApi.ts` anlegen.
10. Frontend: Button im Rezept-Overlay oder auf Rezeptkarte ergänzen.
11. Frontend: Geolocation und Ergebnisliste umsetzen.
12. Frontend: i18n-Keys ergänzen.
13. Frontend: Tests mit Geolocation-Mocks ergänzen.
14. Smoke-Test lokal und in Production durchführen.

## Bewusst ausgeschlossene Punkte

Nicht Teil des MVP:

- keine Kartenansicht
- keine Google Maps API
- kein Google API-Key
- keine Speicherung von Standortdaten
- keine Speicherung von Restaurantdaten
- keine Favoriten für Restaurants
- keine Restaurant-Bewertungen in Dishly
- keine Reservierungsfunktion
- keine automatische Entscheidung, ob gekocht oder auswärts gegessen wird
- keine Übersetzung von Restaurantnamen oder Adressen

## Konsequenzen

Die Geoapify-Restaurantsuche erweitert Dishly Smart um eine alltagsnahe Entscheidungshilfe, ohne die bestehende Architektur stark zu verändern.

Die Lösung bleibt MVP-tauglich:

- Frontend bleibt gegen das eigene Backend gekoppelt.
- Backend kapselt Geoapify und den API-Key.
- Keine neuen Datenbanktabellen sind nötig.
- Keine Standortdaten werden gespeichert.
- Die UI kann klein und schrittweise ergänzt werden.

## Empfehlung

Die erste Umsetzung sollte mit dem Backend beginnen:

1. `POST /restaurants/search` mit Geoapify-Anbindung bauen.
2. `GEOAPIFY_API_KEY` dokumentieren.
3. Backend-Tests mit gemocktem GeoapifyClient schreiben.

Danach kann das Frontend eine kleine Modal- oder Overlay-Erweiterung erhalten, die Geolocation abfragt und die Ergebnisliste mit Google-Maps-Links anzeigt.
