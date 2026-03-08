# Nozio - Next Release Draft

## Highlights
- Dashboard-Gewichtskachel erweitert: Gewicht und KFA koennen jetzt gemeinsam pro Tag eingetragen werden.
- Fallback-Anzeige verbessert: Fehlt am gewaehlten Tag ein Wert, wird der letzte bekannte Wert bis zu diesem Datum angezeigt (inkl. kleinem Stand-Datum, visuell subdued).

## Improvements
- Profilbereich "Meine Ziele" bereinigt: aktuelles Gewicht und KFA aus Ziel-Ansicht/Bearbeitung entfernt.
- Ziele fokussieren jetzt auf Kalorien, Makros sowie Start- und Zielgewicht.
- Aktivkalorien-Berechnung (Dashboard + Widget) nutzt Tages-Gewicht/KFA mit sauberer Fallback-Kette.
- Backup/Restore erweitert um taeglichen KFA-Wert.
- Add-/Edit-BottomSheets sind jetzt bei geoeffneter Tastatur robust: Inhalte bleiben scrollbar und das aktive Eingabefeld wird automatisch in den sichtbaren Bereich gescrollt.
- "Rechtliche Hinweise" wurde von der Profilseite in die Einstellungen verschoben.
- Android-Release-Workflow lokal vereinfacht: neuer Shortcut fuer Version-Bump, Tagging und optionalen Push.
- Aktivitaetskarte im Dashboard visuell an die Gewichtskarte angeglichen (ohne Icons, konsistente Label/Wert-Darstellung).
- Aktivitaetskalorien konservativer geschaetzt (reduzierter Faktor) und optional per Toggle vom Tagesbudget entkoppelbar.
- Toggle "Aktivitaetskalorien anrechnen" jetzt zusaetzlich direkt im Aktivitaets-BottomSheet verfuegbar.
- Gewicht/KFA-BottomSheet speichert nur tatsaechlich geaenderte Werte; unveraenderte Felder bleiben unberuehrt (inkl. Punkt-Indikator fuer ungespeicherte Aenderungen).

## Notes
- Lokale DB-Migration auf Version 7: `daily_activity.bodyFatPercent` hinzugefuegt.
- Build-Version in der App-Konfiguration auf den aktuellen Stand angehoben (`versionName 0.11.1`, `versionCode 17`).
