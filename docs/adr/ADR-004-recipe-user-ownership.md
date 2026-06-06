# ADR-004: User Ownership und Schutz von Recipe-Write-Endpunkten

## Status

Akzeptiert

## Datum

2026-06-05

## Kontext

Dishly Smart hat eine JWT-basierte Authentifizierung. Rezepte sind aktuell
nicht einem User zugeordnet und alle Recipe-Endpunkte sind öffentlich.
Schreiboperationen sollen schrittweise geschützt werden, ohne bestehende
öffentliche Read-Endpunkte zu brechen.

## Entscheidung

- `GET /recipes`, `GET /recipes/published`, `GET /recipes/{id}` und
  `GET /recipes/external` bleiben zunächst öffentlich.
- `POST /recipes` wird als erster Schritt geschützt.
- Neue Rezepte gehören dem eingeloggten User.
- `PUT /recipes/{id}` und `DELETE /recipes/{id}` bleiben in diesem Schritt
  noch unverändert.
- Owner-basierte Ändern-/Löschen-Regeln folgen in einem späteren Schritt.
- Admin-Logik bleibt optional für später.

## Konsequenzen

- `Recipe` erhält eine nullable Beziehung `owner` zu `AppUser`.
- Bestehende Rezepte ohne Owner bleiben lesbar.
- Create-Requests benötigen einen Bearer Token.
- Die JSON-Struktur der Recipe API bleibt kompatibel.

## Risiken

- Bestehende Rezepte ohne Owner müssen später bei PUT/DELETE bewusst
  behandelt werden.
- Frontend muss für Create später einen Token mitsenden.
- Eine produktionsreife Datenmigration sollte später mit Flyway/Liquibase
  erfolgen.
