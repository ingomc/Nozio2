# Nozio - Next Release Draft

## Highlights
- Add-Food Bottom Sheet unterstuetzt jetzt Mengen in `g`, `ml`, `Portion` und `Packung`, wenn die Produktdaten dafuer vorhanden sind.
- Portionen und Packungen koennen direkt als Anzahl erfasst werden, inklusive korrekter Umrechnung fuer Naehrwerte und Speicherung.
- Neue Android-Homescreen-Widgets zeigen Kalorien und Makros direkt auf dem Startbildschirm an.
- Neue Einstellungen-Seite fuer App-Theme und taegliche Meal-Reminder.

## Improvements
- Einheiten-Auswahl im Add-Food Flow auf natives Material3-Select-Verhalten umgestellt.
- Sinnvolle Defaults beim Einheitenwechsel:
  - `100g` / `100ml`
  - `1 Portion`
  - `1 Packung`
- Schnellere Mengeneingabe fuer Portion/Packung ueber `-` / `+` Buttons.
- Quick-Chips im Add-Food Flow auf realistische Standardwerte reduziert:
  - `100g` oder `100ml`
  - `1 Portion`
  - `1 Packung`
- Ausgewaehlte Quick-Chips und Mahlzeit-Chips sind visuell deutlicher hervorgehoben.
- Zwei Android-Widget-Varianten verfuegbar: kompakt und gross.
- Widget-Aktionen oeffnen direkt Suche mit Fokus oder den Barcode-Scanner.
- Widget-Daten aktualisieren sich nach Eintraegen und Aenderungen in der App.
- Einstellungen erlauben jetzt die Auswahl von `System`, `Hell` oder `Dunkel` als Theme-Modus.
- Meal-Reminder koennen direkt in der App aktiviert, zeitlich konfiguriert und beim App-Start wiederhergestellt werden.
- In den Einstellungen gibt es einen Test-Reminder zum direkten Pruefen der Benachrichtigung.
- Ein Tap auf die Meal-Reminder-Benachrichtigung oeffnet die App direkt.

## Notes
- Portion- und Packungsoptionen erscheinen nur, wenn `servingQuantity` bzw. `packageQuantity` fuer das Lebensmittel verfuegbar sind.
- Android-Widgets stehen nur auf Android-Homescreens mit Widget-Unterstuetzung zur Verfuegung.
