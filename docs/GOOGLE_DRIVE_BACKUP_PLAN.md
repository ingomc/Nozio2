# Google Drive Backup Plan (Fortsetzung)

Stand: 05.03.2026

## Ist-Stand (bereits umgesetzt)

- Google Sign-In fuer Drive AppData (`DRIVE_APPDATA`) ist integriert.
- Manueller Flow in Settings:
  - `Verbinden`
  - `Jetzt sichern`
  - `Wiederherstellen` (mit Confirm-Dialog)
- Periodischer Wochen-Backup-Worker vorhanden.
- JSON-Backup hat Versionierung ueber `BackupPayloadV1.schemaVersion`.
- Unit-Tests fuer Repository, Worker und Settings-ViewModel vorhanden.

## Nächste Umsetzungsstufen

### Stufe 1 (P1): Bedienbarkeit + Kontrolle

1. Persistente User-Option `Auto-Backup aktiv` einfuehren.
2. Worker daran koppeln:
   - bei `aktiv`: Work enqueuen
   - bei `inaktiv`: Work canceln
3. UI-Status in Settings klarer trennen:
   - verbunden/nicht verbunden
   - letzter erfolgreicher Upload
   - letzte Fehlermeldung

Akzeptanzkriterium:
- User kann Auto-Backup explizit steuern und die Einstellung bleibt app-neustartfest.

### Stufe 2 (P1): Zuverlaessigkeit

1. Fehlerklassifikation in Drive-Service verbessern:
   - Auth-Fehler
   - Netzwerk/Timeout
   - API/Quota
2. Worker-Retry-Strategie gezielter machen:
   - nur transiente Fehler -> `Result.retry()`
   - permanente Fehler -> `Result.failure()` oder `Result.success()` mit Status-Flag
3. Logging fuer Backup-Flow erweitern (ohne sensible Daten).

Akzeptanzkriterium:
- Weniger "stumme" Wiederholungen, nachvollziehbare Fehlerursachen in QA.

### Stufe 3 (P2): Datenmodell fuer V2 vorbereiten

1. Metadaten im Payload erweitern:
   - `deviceId`
   - `backupReason` (manual/weekly)
   - `counts` pro Entitaet
2. Migrationspfad fuer `BackupPayloadV2` definieren:
   - `V1 -> V2` lesen
   - `V2` schreiben
3. Regressionstests fuer Versionswechsel ausbauen.

Akzeptanzkriterium:
- Restore bleibt rueckwaertskompatibel, neues Schema kann ohne Datenverlust ausgerollt werden.

### Stufe 4 (P2): Fallback ohne Google-Konto

1. Lokaler JSON Export (Datei teilen/speichern).
2. Lokaler JSON Import mit gleicher Validation wie Drive-Restore.

Akzeptanzkriterium:
- Backup/Restore bleibt auch ohne Google-Account nutzbar.

## Sofort naechster Task (empfohlen)

`Stufe 1, Punkt 1-2`: Auto-Backup Toggle + Worker-Kopplung umsetzen, danach Unit-Tests erweitern.
