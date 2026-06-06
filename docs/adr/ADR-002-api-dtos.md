# ADR-002: Einführung von DTOs für die API-Schicht

## Status

Akzeptiert

## Datum

2026-06-05

## Kontext

Die Recipe-API verwendet bisher die JPA-Entity `Recipe` direkt als Request- und
Response-Modell. Das koppelt die REST-Schnittstelle an Persistenzdetails und
erschwert spätere Änderungen am Datenmodell, ohne externe Clients zu
beeinflussen.

## Entscheidung

Die API-Schicht verwendet eigene DTOs:

- `RecipeRequest` für `POST /recipes` und `PUT /recipes/{id}`
- `RecipeResponse` für alle Recipe-Responses
- `RecipeMapper` für das Mapping zwischen DTOs und Entity

Die interne Persistenz bleibt unverändert bei der Entity `Recipe`. API-Pfade,
JSON-Feldnamen und fachliche Funktionalität bleiben kompatibel.

## Konsequenzen

- REST Resources geben keine Entities mehr direkt aus.
- Bean Validation wird auf dem Request-DTO angewendet.
- OpenAPI beschreibt die API-DTOs statt der Entity.
- Die Entity bleibt intern und kann später unabhängiger weiterentwickelt
  werden.

## Nicht Teil dieser Entscheidung

- Keine Authentifizierung
- Keine neuen Fachmodule
- Keine User-Modelle
- Keine Frontend-Änderungen
- Keine vollständige Domain-Refaktorierung
