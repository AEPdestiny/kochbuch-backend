# ADR-004: User Ownership und Schutz von Recipe-Write-Endpunkten

## Status

Akzeptiert

## Datum

2026-06-05

## Kontext

Dishly Smart hat eine JWT-basierte Authentifizierung. Rezepte sind aktuell
nicht einem User zugeordnet und alle Recipe-Endpunkte sind oeffentlich.
Schreiboperationen sollen schrittweise geschuetzt werden, ohne bestehende
oeffentliche Read-Endpunkte zu brechen.

## Entscheidung

- `GET /recipes`, `GET /recipes/published`, `GET /recipes/{id}` und
  `GET /recipes/external` bleiben zunaechst oeffentlich.
- `POST /recipes` wird als erster Schritt geschuetzt.
- Neue Rezepte gehoeren dem eingeloggten User.
- `PUT /recipes/{id}` und `DELETE /recipes/{id}` bleiben in diesem Schritt
  noch unveraendert.
- Owner-basierte Aendern-/Loeschen-Regeln folgen in einem spaeteren Schritt.
- Admin-Logik bleibt optional fuer spaeter.

## Konsequenzen

- `Recipe` erhaelt eine nullable Beziehung `owner` zu `AppUser`.
- Bestehende Rezepte ohne Owner bleiben lesbar.
- Create-Requests benoetigen einen Bearer Token.
- Die JSON-Struktur der Recipe API bleibt kompatibel.

## Risiken

- Bestehende Rezepte ohne Owner muessen spaeter bei PUT/DELETE bewusst
  behandelt werden.
- Frontend muss fuer Create spaeter einen Token mitsenden.
- Eine produktionsreife Datenmigration sollte spaeter mit Flyway/Liquibase
  erfolgen.
