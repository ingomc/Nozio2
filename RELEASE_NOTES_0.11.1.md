# Nozio - Release 0.11.1 (Patch)

## Highlights
- Produktbilder im Add-Sheet werden jetzt vollständig im Bildrahmen angezeigt, bei beibehaltener Aspect Ratio.

## Improvements
- Lokale Bestandsdaten werden bei fehlender `imageUrl` automatisch per Barcode aus der API nachgezogen (Backfill), damit bereits gespeicherte Produkte ebenfalls Bilder erhalten.
- Beim Öffnen eines Produkts in der Suche wird ein fehlendes Bild gezielt nachgeladen, bevor das Add-Sheet angezeigt wird.

## Notes
- Keine zusätzlichen manuellen Schritte nötig; der Bild-Backfill läuft automatisch.
