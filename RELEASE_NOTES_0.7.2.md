# Nozio v0.7.2

## Highlights
- Google-Drive-Authentifizierung auf aktuelle Best Practice umgestellt: Credential Manager + Google Authorization API.

## Improvements
- Legacy-`GoogleSignIn`-Flow im Backup-Auth-Handling ersetzt.
- Neuer zweistufiger Auth-Flow:
  - Sign-In ueber Credential Manager (Google-Kontoauswahl)
  - Drive-Berechtigung ueber AuthorizationClient inklusive Resolution-Flow
- Robustere Fehlerpfade bei Anmeldung/Autorisierung:
  - Abbruch durch User
  - Konfigurationsprobleme (OAuth/Client-ID/SHA)
  - Netzwerk-/temporäre Fehler
- Settings-Flow fuer `Verbinden`, `Jetzt sichern`, `Wiederherstellen` auf den neuen Auth-State umgestellt.
- CI-Release-Workflow gegen doppelte Runs gehaertet (Release-Trigger + Concurrency angepasst).

## Notes
- Android-Version auf `0.7.2` angehoben.
- Fuer lokalen Build muss `GOOGLE_WEB_CLIENT_ID` gesetzt sein.
