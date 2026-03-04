# Nozio - Next Release Draft

## Highlights
- Add-Food Bottom Sheet unterstuetzt jetzt Mengen in `g`, `ml`, `Portion` und `Packung`, wenn die Produktdaten dafuer vorhanden sind.
- Portionen und Packungen koennen direkt als Anzahl erfasst werden, inklusive korrekter Umrechnung fuer Naehrwerte und Speicherung.

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

## Notes
- Portion- und Packungsoptionen erscheinen nur, wenn `servingQuantity` bzw. `packageQuantity` fuer das Lebensmittel verfuegbar sind.
