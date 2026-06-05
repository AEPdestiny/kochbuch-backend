# ADR-000: Dokumentationssprache

## Status

Akzeptiert

## Datum

2026-06-05

## Kontext

Das Projekt besteht aus Backend, Frontend, API, Architekturentscheidungen und
weiterer Projektdokumentation. Damit die Dokumentation einheitlich bleibt, muss
festgelegt werden, welche Sprache fuer Dokumente und welche Sprache fuer
Quellcode-Artefakte verwendet wird.

## Entscheidung

- README-Dateien werden auf Deutsch gefuehrt.
- ADR-Dokumente werden auf Deutsch gefuehrt.
- Architektur- und Projektdokumentation werden auf Deutsch gefuehrt.
- Quellcode bleibt Englisch.
- API-Endpunkte bleiben Englisch.
- Klassen-, Methoden-, DTO-, Entity- und Variablennamen bleiben Englisch.

## Konsequenzen

- Fachliche und architektonische Entscheidungen sind fuer das Projektteam auf
  Deutsch dokumentiert.
- Technische Namen im Code bleiben konsistent mit gaengigen Framework- und
  API-Konventionen.
- Neue Dokumentation soll auf Deutsch erstellt werden.
- Bestehende englische Dokumentation wird schrittweise ins Deutsche ueberfuehrt.

## Nicht Teil dieser Entscheidung

- Keine Umbenennung von Code-Artefakten
- Keine Aenderung von API-Pfaden
- Keine Aenderung fachlicher Funktionalitaet
