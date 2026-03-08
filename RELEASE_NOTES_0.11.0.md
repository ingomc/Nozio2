# Nozio - Release 0.11.0

## Highlights
- Suche: Swipebare Tabs für Vorschläge (`Zuletzt hinzugefügt`, `Häufig`, `Favoriten`) mit gleichmäßiger Breite über die volle Tab-Leiste.
- Produkte können jetzt direkt im Produkt-Bottomsheet per Stern als Favorit markiert oder entmarkiert werden.
- Add-Sheet zeigt jetzt das Produktbild oberhalb des Produktnamens (inkl. Fallback, wenn kein Bild verfügbar ist).

## Improvements
- Suchvorschläge sind jetzt aufgeteilt in zuletzt hinzugefügt, häufig genutzt und Favoriten.
- Favoriten werden lokal gespeichert und in Backups mitgesichert.
- Dashboard-Header zeigt bei aktuellem bzw. vorherigem Tag jetzt `Heute` bzw. `Gestern`, sonst weiterhin das normale Datum.
- Food-API/Import-Pipeline unterstützt jetzt `imageUrl` in Seed- und API-Daten.

## Notes
- Datenbank-Migrationen ergänzt (`food_items.isFavorite`, `food_items.imageUrl`).
