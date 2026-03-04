# Nozio - Next Release Draft

## Highlights
- Neue detailreiche In-App-Benachrichtigung nach dem Hinzufuegen von Lebensmitteln im Search-Flow.
- Rueckgaengig-Aktion direkt in der Benachrichtigung: der zuletzt hinzugefuegte Eintrag kann sofort entfernt werden.
- Vorauswahl der Mahlzeit im Add-Food- und Quick-Add-Bottom-Sheet erfolgt jetzt kontextuell anhand der Uhrzeit, wenn keine Mahlzeit vorgegeben ist.

## Improvements
- Add-Bestaetigung erscheint als Top-Banner mit Material-You-Animation, echter Elevation und klaren Naehrwert-Infos (kcal, EW, KH, F).
- Der 5-Sekunden-Countdown der Add-Bestaetigung laeuft als kapselfoermiger Progress und blendet den Banner sauber wieder aus.
- Add-Bestaetigung und Undo sind fuer normale Food-Adds und Quick Add konsistent umgesetzt.
- Die Add-Confirmation-Logik wurde im Search-State strukturiert modelliert, wodurch UI und Undo robuster zusammenarbeiten.
- Systemleisten (Status-/Navigationsleiste) passen ihre Icon-Farben jetzt an das in der App ausgewaehlte Theme an, auch wenn das Systemtheme abweicht.
- Die Ring-Fuellung im Widget wurde an die Dashboard-Logik angeglichen und basiert jetzt konsistent auf gegessen/goal.

## Notes
- Undo gilt immer fuer den zuletzt hinzugefuegten Eintrag, solange die Benachrichtigung sichtbar ist.
- Zeitfenster fuer automatische Mahlzeit-Vorauswahl ohne Vorgabe:
  - Fruehstueck: 05:00-10:59
  - Mittagessen: 11:00-14:59
  - Abendessen: 18:00-21:59
  - sonst: Snacks
- Der Widget-Kreis zeigt den Verbrauchsfortschritt; der Mittelpunkt zeigt weiterhin die verbleibenden Kalorien.
