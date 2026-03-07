# Nozio v0.10.0

## Highlights
- App-Navigation wurde auf Jetpack `NavHost` umgestellt und verhaelt sich jetzt bei Android-Back-Gesten nativer.

## Improvements
- Native Route-Hierarchie eingefuehrt: `home`, `search`, `profile`, `profile/legal`, `settings/main`, `settings/reminder`, `settings/backup`.
- Back-Fallback korrigiert: Von Suche/Einstellungen/Profil fuehrt Back jetzt zum Dashboard statt zum Android-Homescreen.
- Dashboard-Back zeigt nun eine Exit-Bestaetigung (Ja/Nein), bevor die App geschlossen wird.
- Native-aehnliche Shared-Axis-X-Transitions fuer Vor/Zurueck zwischen den Routen integriert.
- Settings-Unterseiten (`Erinnerung`, `Backup & Wiederherstellung`) sind jetzt echte Routen mit nativer Up-Navigation.
- Such-Screen priorisiert Back korrekt auf modale UI (Scanner/BottomSheets werden zuerst geschlossen).

## Notes
- Android-Version auf `0.10.0` angehoben.
- Build geprueft mit `./gradlew :app:compileDebugKotlin`.
