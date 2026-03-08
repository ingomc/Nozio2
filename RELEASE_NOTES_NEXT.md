# Nozio - Next Release Draft

## Highlights
- Dashboard-Gewichtskachel erweitert: Gewicht und KFA koennen jetzt gemeinsam pro Tag eingetragen werden.
- Fallback-Anzeige verbessert: Fehlt am gewaehlten Tag ein Wert, wird der letzte bekannte Wert bis zu diesem Datum angezeigt (inkl. kleinem Stand-Datum, visuell subdued).

## Improvements
- Profilbereich "Meine Ziele" bereinigt: aktuelles Gewicht und KFA aus Ziel-Ansicht/Bearbeitung entfernt.
- Ziele fokussieren jetzt auf Kalorien, Makros sowie Start- und Zielgewicht.
- Aktivkalorien-Berechnung (Dashboard + Widget) nutzt Tages-Gewicht/KFA mit sauberer Fallback-Kette.
- Backup/Restore erweitert um taeglichen KFA-Wert.

## Notes
- Lokale DB-Migration auf Version 7: `daily_activity.bodyFatPercent` hinzugefuegt.
