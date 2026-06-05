# ADR-002: Einfuehrung von DTOs fuer die API-Schicht

## Status

Akzeptiert

## Datum

2026-06-05

## Kontext

Die Recipe-API verwendet bisher die JPA-Entity `Recipe` direkt als Request- und
Response-Modell. Das koppelt die REST-Schnittstelle an Persistenzdetails und
erschwert spaetere Aenderungen am Datenmodell, ohne externe Clients zu
beeinflussen.

## Entscheidung

Die API-Schicht verwendet eigene DTOs:

- `RecipeRequest` fuer `POST /recipes` und `PUT /recipes/{id}`
- `RecipeResponse` fuer alle Recipe-Responses
- `RecipeMapper` fuer das Mapping zwischen DTOs und Entity

Die interne Persistenz bleibt unveraendert bei der Entity `Recipe`. API-Pfade,
JSON-Feldnamen und fachliche Funktionalitaet bleiben kompatibel.

## Konsequenzen

- REST Resources geben keine Entities mehr direkt aus.
- Bean Validation wird auf dem Request-DTO angewendet.
- OpenAPI beschreibt die API-DTOs statt der Entity.
- Die Entity bleibt intern und kann spaeter unabhaengiger weiterentwickelt
  werden.

## Nicht Teil dieser Entscheidung

- Keine Authentifizierung
- Keine neuen Fachmodule
- Keine User-Modelle
- Keine Frontend-Aenderungen
- Keine vollstaendige Domain-Refaktorierung
