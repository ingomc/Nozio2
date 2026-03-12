# Nozio - Next Release Draft

## Highlights
- Neues Supplements-Feature: taeglicher Standardplan mit Uhrzeit, Tageszeit-Kategorie und Menge/Einheit.
- Dashboard erweitert um horizontale Supplements-Timeline direkt vor den Mahlzeiten inkl. schnellem Abhaken pro gewaehltem Datum.
- Neue Bearbeiten-Seite fuer Supplements (`Vorfrueh`, `Mittag`, `Abend`, `Spaet`) mit Hinzufuegen, Aendern und Loeschen.
- Dashboard-Gewichtskachel erweitert: Gewicht und KFA koennen jetzt gemeinsam pro Tag eingetragen werden.
- Fallback-Anzeige verbessert: Fehlt am gewaehlten Tag ein Wert, wird der letzte bekannte Wert bis zu diesem Datum angezeigt (inkl. kleinem Stand-Datum, visuell subdued).

## Improvements
- Profilbereich "Meine Ziele" bereinigt: aktuelles Gewicht und KFA aus Ziel-Ansicht/Bearbeitung entfernt.
- Ziele fokussieren jetzt auf Kalorien, Makros sowie Start- und Zielgewicht.
- Profilbild kann jetzt per Tap auf den Avatar geaendert werden (Bildauswahl, Zuschneiden und optimierte Speicherung).
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
- Dashboard-Drag-and-Drop fuer Mahlzeit-Eintraege verbessert: Das Original-Item wird entlang der Y-Achse gezogen (kein dupliziertes Overlay mehr), inklusive sauberem Layering ueber den Karten.
- Clipping in den Mahlzeit-Karten ist jetzt zustandsabhaengig: normal sauber geclippt, waehrend aktivem Drag-and-Drop bewusst offen fuer fluesiges Umordnen.
- Copy-BottomSheet optimiert: Mahlzeit-Chips sind gleich breit, einzeilig und umbrechen nicht mehr unruhig.
- Home-Screen-Widgets nutzen die Breite besser: reduzierte Seitenabstaende sowie groessere Scan-/Plus-Buttons fuer bessere Touchbarkeit.
- Add-Product-BottomSheet zeigt beim Laden von Produktbildern jetzt einen Shimmer-Placeholder im Bildcontainer (mit sauberem Fallback bei fehlendem/fehlerhaftem Bild).
- Supplements-Timeline ueberarbeitet: Uhrzeit als sticky Zeitlabel plus incoming Zeitlabels fuer kommende Gruppen; Badge-/Card-Layout und Layering wurden gegen Clipping/Versatz stabilisiert.
- Dashboard-Makro-Balken zeigen Ueberziel jetzt anteilig in Rot (rechter Anteil), waehrend die Fuellung als ein durchgehender Balken animiert.
- Kompaktes Widget-Makro-Layout neu aufgeteilt: links kurze Labels (`KH`, `EW`, `F`), rechts die jeweiligen Balken fuer bessere Lesbarkeit.
- App-weites Material3-Refresh gestartet: konsistentere expressive Tokens fuer Farben, Shapes, TopBars, BottomSheets und Navigation.
- Suche auf Android-Primary-Tabs umgestellt (`Zuletzt`, `Haeufig`, `Favoriten`) inkl. Icon+Label pro Tab bei unveraenderter Ergebnis-Logik.
- Suchleiste im Suchscreen visuell harmonisiert (subdued-primary statt lila Stich) und als klarere, zusammengehoerige Suche+Scanner-Aktionseinheit gestaltet.
- QR-Scanner-BottomSheet verbessert: quadratischer Kameraausschnitt, dynamischere Sheet-Hoehe nach Inhalt und neuer Blitz-Toggle fuer dunkle Umgebungen.
- Dashboard-Mahlzeitzeilen verdichtet: weniger vertikaler Verbrauch, staerkere Full-Width-Anmutung, Swipe-Actions mit progressivem Einblenden und stabilerem Button-Hintergrund-Kontrast.
- Swipe-Actions in Mahlzeitzeilen weiter verfeinert: groesserer Reveal-Bereich fuer Gesten sowie klarere Copy/Delete-Button-Farben fuer bessere Erkennbarkeit.
- Swipe/Drag-Layering in Mahlzeiten weiter verfeinert: gezogenes Item liegt per erhoehtem z-index sichtbar ueber Nachbarinhalten.
- Einstellungen visuell entschlackt: Row-Hierarchie klarer, weniger "Kachel-Overload", besserer Fit zum restlichen App-Chrome.
- Nährwert-Scan deutlich erweitert: eigener Foto-Scan-Flow (separat vom Barcode-Scanner) mit serverseitiger Vision-Erkennung und Review vor der Übernahme in Quick Add oder Eigenes Produkt.
- Scanner-Transparenz verbessert: nach Fotoaufnahme bleibt das Scanner-BottomSheet sichtbar, Eingaben werden deaktiviert und ein klarer Analyse-Loading-State wird angezeigt.
- Fehlerkommunikation verbessert: Scan- und Save-Fehler werden als Snackbar angezeigt, inklusive konkreterer Gründe aus Backend/API (z. B. `UNAUTHORIZED`, `VISION_UNAVAILABLE`, `MEILI_UNAVAILABLE`).
- Eigenes Produkt erweitert: Barcode-Scan direkt im Formular möglich, um den Barcode schneller vorzubefüllen.
- Suche/Quick-Action UX aktualisiert: CTA-Struktur konsolidiert und Buttons in den zentralen Widgets größer für bessere Touchbarkeit.
- Texteingaben verbessert: in Name-/Marke-Feldern startet die Tastatur jetzt mit Großschreibung am Wortanfang.

## Notes
- Lokale DB-Migration auf Version 8: neue Tabellen `supplement_plan_items` und `supplement_intakes`.
- Backup/Restore auf Schema-Version 2 erweitert (Supplements inklusive), Restore alter Schema-Version 1 bleibt kompatibel.
- Zusaetzlicher Android-Smoke-Test fuer Dashboard->Supplements-Navigation ergaenzt.
- Neuer Vision-Endpoint im `food-api`: `POST /v1/vision/nutrition/parse` (Gemini-basiert, Bilder nur transient verarbeitet, keine Persistenz).
