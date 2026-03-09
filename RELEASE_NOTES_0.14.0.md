# Nozio 0.14.0

## Highlights
- Dashboard-Mahlzeiten koennen jetzt per Long-Press & Drag-and-Drop direkt zwischen Mahlzeiten verschoben werden.
- Neuer "Eintrag kopieren"-Flow in den Mahlzeiten: Lebensmittel lassen sich in ein anderes Datum, eine andere Mahlzeit und mit angepasster Grammzahl uebernehmen.
- Waehrend des Ziehens wird ein klares Overlay angezeigt und moegliche Ziel-Mahlzeiten werden visuell hervorgehoben.

## Improvements
- Robustere Drag-Overlay-Logik im Dashboard: unsichere Non-Null-Assertions entfernt.
- Ueberfluessiger/ungueltiger Pointer-Import in `MealCard` entfernt.
- Datenzugriff fuer Drag-and-Drop erweitert: Eintraege koennen jetzt atomar auf Datum + Mahlzeit umgebucht werden.

## Notes
- Neue Tests fuer `DiaryRepository` decken das Verschieben von Eintraegen sowie das Kopieren (Insert mit Ziel-Datum/Mahlzeit/Menge) ab.
- Keine DB-Migration erforderlich.
