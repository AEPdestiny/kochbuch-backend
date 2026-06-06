# ADR-000: Dokumentationssprache

## Status

Akzeptiert

## Datum

2026-06-05

## Kontext

Das Projekt besteht aus Backend, Frontend, API, Architekturentscheidungen und
weiterer Projektdokumentation. Damit die Dokumentation einheitlich bleibt, muss
festgelegt werden, welche Sprache für Dokumente und welche Sprache für
Quellcode-Artefakte verwendet wird.

## Entscheidung

- README-Dateien werden auf Deutsch geführt.
- ADR-Dokumente werden auf Deutsch geführt.
- Architektur- und Projektdokumentation werden auf Deutsch geführt.
- Quellcode bleibt Englisch.
- API-Endpunkte bleiben Englisch.
- Klassen-, Methoden-, DTO-, Entity- und Variablennamen bleiben Englisch.

## Konsequenzen

- Fachliche und architektonische Entscheidungen sind für das Projektteam auf
  Deutsch dokumentiert.
- Technische Namen im Code bleiben konsistent mit gängigen Framework- und
  API-Konventionen.
- Neue Dokumentation soll auf Deutsch erstellt werden.
- Bestehende englische Dokumentation wird schrittweise ins Deutsche überführt.

## Nicht Teil dieser Entscheidung

- Keine Umbenennung von Code-Artefakten
- Keine Änderung von API-Pfaden
- Keine Änderung fachlicher Funktionalität
