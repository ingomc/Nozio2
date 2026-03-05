# Nozio Roadmap

Stand: 26.02.2026  
Scope: Produkt- und Umsetzungs-Roadmap fuer die naechsten Entwicklungsschritte von Nozio.

## Zielbild

Nozio soll eine schnelle, verlaessliche Daily-Nutrition-App bleiben:
- moeglichst wenig Reibung bei der Erfassung
- klare Tagessteuerung ueber Ziele und Feedback
- robuste Datenqualitaet fuer aussagekraeftige Trends

## Leitprinzipien

1. Erfassungsgeschwindigkeit vor Feature-Bloat
2. Stabilitaet und Datenkonsistenz vor kosmetischen Erweiterungen
3. Local-first beibehalten, Integrationen schrittweise ergaenzen

---

## Phase 1 - NOW (0-6 Wochen)
Fokus: Bestehende Kernflows robust machen und UX-Reibung reduzieren.

### 1) Barcode-Scanner harden (P0)
- Scan-Overlay mit klarer Zielzone
- Duplicate-Scan-Schutz + Cooldown
- bessere Fehlerpfade:
  - Kamera verweigert
  - kein Barcode erkannt
  - API/Netzwerkfehler
- optional haptisches Feedback bei erfolgreichem Scan

Erfolgskriterium:
- signifikant weniger Fehl-/Doppelscans
- klarere Rueckmeldung in jedem Fehlerfall

### 2) Search UX polish (P0)
- Success-Hinweis als konsistenter Top-Banner mit Auto-Dismiss
- stabileres Verhalten mit Keyboard/Insets auf verschiedenen Geraeten
- bessere Empty/Error-Stati
- weiche Ergebnis-Transitions

Erfolgskriterium:
- Such- und Add-Flow ohne visuelle Spruenge
- weniger Abbrueche zwischen Treffer und Hinzufuegen

### 3) Portion Defaults aus API (P1)
- OpenFoodFacts-Mapping um `serving_size` / `serving_quantity` erweitern
- Add-Food mit intelligentem Default statt starrer `100g`
- Fallback-Regeln:
  - Portion in Gramm vorhanden -> uebernehmen
  - nur Textportion vorhanden -> best effort parsen
  - sonst -> `100g`

Erfolgskriterium:
- weniger manuelle Mengenkorrekturen vor dem Speichern

### 4) Meal Entry "Copy" vervollstaendigen (P1)
- bestehende Copy-Action funktional machen
- Eintrag als neue Instanz fuer gleiches Datum/Mahlzeit anlegen
- Option: direkt in Edit-Menge springen

Erfolgskriterium:
- schnellere Wiederholung typischer Mahlzeiten

---

## Phase 2 - NEXT (6-12 Wochen)
Fokus: Datenqualitaet, Insights und Wiederverwendung.

### 1) Smart Suggestions (P1)
- kontextbezogene Vorschlaege nach Mahlzeit und Uhrzeit
- zuletzt genutzte Mengen pro Lebensmittel merken
- Ranking aus "haeufig + aktuell" statt nur "zuletzt hinzugefuegt"

### 2) Trend-Insights im Dashboard (P1)
- 7-Tage-Schnitt fuer Kalorien und Makros
- Zielabweichungen sichtbar machen (zu hoch/zu niedrig)
- einfache Wochenzusammenfassung

### 3) Datenpflege und Konsistenz (P1)
- Duplikat-Strategie fuer gleiche Produkte (z. B. gleicher Barcode)
- bessere Nahrwertplausibilitaet bei API-Daten
- saubere Migrationstests fuer Room-Versionen

### 4) Testabdeckung ausbauen (P1)
- ViewModel-Tests fuer Dashboard/Search/Profile
- Repository-Tests fuer Fallback- und Mappinglogik
- UI-Smoke-Tests fuer Kernflows

---

## Phase 3 - LATER (3-6 Monate)
Fokus: Plattformausbau ohne Kernkonzept zu verwässern.

### 1) Health-Integrationen (P2)
- Health Connect / Google Fit fuer Schritte (optional bidirektional)
- manuelle Eingabe bleibt immer als Fallback

### 2) Backup / Sync Optionen (P2)
- **Status (05.03.2026): Google Drive Backup/Restore ist als V1 bereits im Produkt**
  - Google Sign-In + Scope `DriveScopes.DRIVE_APPDATA`
  - manuelles Backup (Settings) + manuelle Wiederherstellung mit Confirm-Dialog
  - periodischer Wochen-Backup-Worker (nur bei vorhandenem Sign-In)
  - serialisiertes Payload-Schema `BackupPayloadV1` inkl. Schema-Version
- **Fortsetzungplan (V1 hardening -> V2 readiness)**
  - P1: Settings-Option fuer automatisches Backup (ein/aus) inkl. persistenter User-Preference
  - P1: Worker-Scheduling an diese Option koppeln (bei `aus` Work canceln)
  - P1: Backups robuster machen (Netzfehler/Rate-Limits sauber unterscheiden, Retry-Strategie schaerfen)
  - P1: technische Doku fuer OAuth/Drive-Setup (Debug/Release SHA-1 + Test-Checkliste) in `README`
  - P2: Backup-Metadaten erweitern (`deviceId`, `backupReason`, `recordCount`) fuer bessere Restore-Transparenz
  - P2: optional JSON Export/Import lokal als Fallback ohne Google-Konto
  - P2: Vorbereitung auf `BackupPayloadV2` inkl. Migrationspfad und Regressionstests

### 3) Erweiterte Zielsystematik (P2)
- Zielprofile (Cut, Maintenance, Bulk)
- adaptive Zielvorschlaege auf Basis von Verlauf

### 4) Onboarding & Personalization (P3)
- kurzer Setup-Assistent bei Erststart
- sinnvolle Defaults fuer Ziele und Eingabegewohnheiten

---

## Backlog (Ideenpool)

- Reminder fuer Logging-Zeiten
- Wochen-/Monatsreport als kompakte Ansicht
- Favoriten-Lebensmittel
- "Heute erneut hinzufuegen" fuer wiederkehrende Meals
- Widget fuer Schnellzugriff (Scan/Add)

---

## Nicht-Ziele (aktuell)

- Kein Social Feed / kein Community-Vergleich
- Kein komplexer Trainingsplan-Editor
- Keine sofortige Multi-Plattform-Expansion vor Stabilisierung der Android-Basis

---

## Priorisierungsmethode

- P0 = Blocker/Kern-UX (muss zuerst)
- P1 = hoher Produktwert bei moderatem Aufwand
- P2 = strategische Erweiterung nach Stabilisierung
- P3 = nice-to-have

Entscheidungsregel:
- erst Kernflow stabil + schnell, dann neue Oberflaeche.
