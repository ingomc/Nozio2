# Nozio 0.13.0

## Highlights
- Neues Supplements-Feature: taeglicher Standardplan mit Uhrzeit, Tageszeit-Kategorie und Menge/Einheit.
- Dashboard erweitert um horizontale Supplements-Timeline direkt vor den Mahlzeiten inkl. schnellem Abhaken pro gewaehltem Datum.
- Neue Bearbeiten-Seite fuer Supplements (`Vorfrueh`, `Mittag`, `Abend`, `Spaet`) mit Hinzufuegen, Aendern und Loeschen.
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
- Profil-Chart erweitert: Gewicht und KFA jetzt gemeinsam im Verlauf (zweite Linie/Farbe mit separater KFA-Achse).
- Zeitraumauswahl im Profil-Chart auf `14T`, `60T`, `180T`, `1J` umgestellt (aktive Auswahl als solid Chip).
- Diagramm-Logik ueberarbeitet: Initial immer neueste Daten rechts, alle Zeitraeume horizontal scrollbar (3x Fensterbreite).
- Datenverdichtung im Profil-Chart: ab `60T` Wochenmittelwerte, bei `1J` Monatsmittelwerte fuer bessere Lesbarkeit.
- X-Achsen-Beschriftung im Profil-Chart verbessert (regelmaessige Datums-Ticks ohne Label-Ueberlappung).
- Supplements-Timeline visuell verdichtet: kompaktere Zwei-Zeilen-Karten, kleinerer Toggle-Indikator mit animiertem Check-in.

## Notes
- Lokale DB-Migration auf Version 8: neue Tabellen `supplement_plan_items` und `supplement_intakes`.
- Backup/Restore auf Schema-Version 2 erweitert (Supplements inklusive), Restore alter Schema-Version 1 bleibt kompatibel.
- Zusaetzlicher Android-Smoke-Test fuer Dashboard->Supplements-Navigation ergaenzt.
