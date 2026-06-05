# ADR-001: Migration von Spring Boot nach Quarkus

## Status

Akzeptiert

## Datum

2026-06-05

## Kontext

Das Backend soll von Spring Boot auf Quarkus migriert werden. Die bestehende
Recipe-Funktionalitaet soll dabei erhalten bleiben. Bestehende REST-Endpunkte
unter `/recipes` muessen kompatibel bleiben, damit das Frontend und vorhandene
Clients weiterhin funktionieren.

## Entscheidung

Das Backend wird auf eine Quarkus-Grundlage umgestellt. Die bestehende
Recipe-Funktionalitaet wird 1:1 portiert:

- REST Controller werden zu Quarkus REST Resources migriert.
- Repositories werden mit Hibernate ORM Panache umgesetzt.
- Die Service-Schicht bleibt fachlich erhalten und wird als CDI-Service
  betrieben.
- PostgreSQL wird ueber Quarkus JDBC und Hibernate ORM angebunden.
- OpenAPI/Swagger wird ueber SmallRye OpenAPI integriert.
- Bestehende Tests werden auf Quarkus, JUnit 5 und RestAssured migriert.

## Konsequenzen

- Spring Boot-Abhaengigkeiten werden durch Quarkus-Abhaengigkeiten ersetzt.
- Jakarta-Namespaces werden verwendet.
- Die API-Pfade unter `/recipes` bleiben kompatibel.
- Tests laufen ueber die Quarkus-Testinfrastruktur.
- Quarkus Dev Services kann lokale Testdatenbanken bereitstellen.

## Nicht Teil dieser Entscheidung

- Keine Authentifizierung
- Keine neuen Fachmodule
- Keine neuen Datenmodelle ausser `Recipe`
- Keine Frontend-Aenderungen
