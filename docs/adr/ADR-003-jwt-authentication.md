# ADR-003: Einfuehrung JWT-basierter Authentifizierung

## Status

Akzeptiert

## Datum

2026-06-05

## Kontext

Dishly Smart benoetigt eine produktionsnahe Authentifizierung. Bisher sind die
Recipe-Endpunkte oeffentlich und es existiert kein User-Modell.
Authentifizierung soll eingefuehrt werden, ohne die Recipe-API sofort
user-bezogen umzubauen.

## Entscheidung

Wir fuehren eine eigene Auth-Schicht mit Registrierung, Login,
BCrypt-Passwort-Hashing und JWT Access Tokens ein.

- Login erfolgt per Email und Passwort.
- Registrierung loggt den User direkt ein und gibt ein JWT zurueck.
- JWT Access Tokens sind fuer 1 Stunde gueltig.
- Fuer das MVP wird HS256 mit `JWT_SECRET` aus Environment Variables verwendet.
- Der aktuelle User ist ueber `GET /auth/me` abrufbar.
- Recipe-Endpunkte bleiben in diesem Schritt oeffentlich.
- Recipe-User-Verknuepfung folgt in einem spaeteren Schritt.

## Konsequenzen

- Neue Backend-Bereiche `auth`, `user` und `security`.
- Passwords werden nie gespeichert, sondern nur BCrypt Hashes.
- JWT-Konfiguration erfolgt ueber Environment Variables.
- Spaetere user-bezogene Datenzugriffe koennen auf `AppUser` aufbauen.

## Nicht Teil dieser Entscheidung

- Keine Refresh Tokens
- Keine OAuth/OIDC-Integration
- Keine Frontend-Aenderungen
- Keine Recipe-User-Verknuepfung
- Keine geschuetzten Recipe-Endpunkte in diesem Schritt
