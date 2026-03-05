# Nozio - Next Release Draft

## Highlights
- Google Drive Backup & Restore ist integriert (manuell in den Einstellungen).
- Woechentlicher Auto-Backup-Worker verfuegbar und per Toggle steuerbar.

## Improvements
- Google-Login-Flow fuer Drive AppData (`DRIVE_APPDATA`) direkt in den Einstellungen.
- Manueller Flow: `Verbinden`, `Jetzt sichern`, `Wiederherstellen` (mit Bestaetigungsdialog).
- Auto-Backup und manuelle Aktionen sind sauber getrennt:
  - Auto-Backup aus = kein geplanter Wochen-Worker
  - manuelles Sichern/Wiederherstellen bleibt weiterhin moeglich
- Backup-Payload als versioniertes JSON-Schema (`BackupPayloadV1`) umgesetzt.
- Fehler- und Statusmeldungen fuer Backup/Restore in der UI verbessert (letzter Erfolg, Fehlermeldung, Laufstatus).
- Unit-Tests fuer Backup-Repository, Worker und Settings-ViewModel erweitert.

## Notes
- Build-Packaging fuer Google/HTTP-Dependencies bereinigt (`META-INF/INDEX.LIST`, `META-INF/DEPENDENCIES` ausgeschlossen).
