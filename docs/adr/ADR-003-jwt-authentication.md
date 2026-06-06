# ADR-003: Einführung JWT-basierter Authentifizierung

## Status

Akzeptiert

## Datum

2026-06-05

## Kontext

Dishly Smart benötigt eine produktionsnahe Authentifizierung. Bisher sind die
Recipe-Endpunkte öffentlich und es existiert kein User-Modell.
Authentifizierung soll eingeführt werden, ohne die Recipe-API sofort
user-bezogen umzubauen.

## Entscheidung

Wir führen eine eigene Auth-Schicht mit Registrierung, Login,
BCrypt-Passwort-Hashing und JWT Access Tokens ein.

- Login erfolgt per Email und Passwort.
- Registrierung loggt den User direkt ein und gibt ein JWT zurück.
- JWT Access Tokens sind für 1 Stunde gültig.
- Für das MVP wird HS256 mit `JWT_SECRET` aus Environment Variables verwendet.
- Der aktuelle User ist über `GET /auth/me` abrufbar.
- Recipe-Endpunkte bleiben in diesem Schritt öffentlich.
- Recipe-User-Verknüpfung folgt in einem späteren Schritt.

## Konsequenzen

- Neue Backend-Bereiche `auth`, `user` und `security`.
- Passwords werden nie gespeichert, sondern nur BCrypt Hashes.
- JWT-Konfiguration erfolgt über Environment Variables.
- Spätere user-bezogene Datenzugriffe können auf `AppUser` aufbauen.

## Nicht Teil dieser Entscheidung

- Keine Refresh Tokens
- Keine OAuth/OIDC-Integration
- Keine Frontend-Änderungen
- Keine Recipe-User-Verknüpfung
- Keine geschützten Recipe-Endpunkte in diesem Schritt
