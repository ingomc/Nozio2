# Nozio

Nozio ist eine Android-App zum einfachen, taeglichen Ernaehrungs-Tracking.  
Fokus: Lebensmittel schnell erfassen, Kalorien/Makros gegen Ziele vergleichen und Aktivitaet/Gewicht im Blick behalten.

## Status

- Version: `0.1.0`
- Plattform: Android (Kotlin + Jetpack Compose)
- Reifegrad: aktive Entwicklung

## Features

- Tages-Dashboard mit:
  - gegessenen Kalorien
  - geschaetzten Aktivitaetskalorien
  - verbleibenden Kalorien
  - Makro-Balken (KH, Eiweiss, Fett)
- Mahlzeiten-Tracking (`Fruehstueck`, `Mittagessen`, `Abendessen`, `Snacks`)
- Lebensmittel-Suche (OpenFoodFacts)
- Barcode-Scan (CameraX + ML Kit)
- Add-Food-Flow mit Mengenwahl und Mahlzeitzuordnung
- Bearbeiten/Loeschen von Eintraegen
- Schritte pro Tag erfassen
- Gewicht pro Tag erfassen
- Profil mit Zielwerten (Kalorien + Makros, Gewicht, KFA)
- Gewichtsverlauf mit Zeitfiltern (7T / 30T / 90T / Alle)

## Tech Stack

- Kotlin
- Jetpack Compose + Material3
- Android Architecture Components (ViewModel, Flow)
- Room (lokale Datenbank)
- DataStore Preferences
- Retrofit + Kotlinx Serialization
- OpenFoodFacts API
- CameraX
- Google ML Kit Barcode Scanning

## Projektstruktur

```text
app/src/main/java/de/ingomc/nozio
├─ data
│  ├─ local        # Room Entities, DAOs, DB
│  ├─ remote       # Retrofit API + DTOs
│  └─ repository   # Fachlogik und Datenzugriff
├─ ui
│  ├─ dashboard    # Home/Tagesueberblick
│  ├─ search       # Suche + Barcode + Add-Food
│  ├─ profile      # Ziele + Gewichtsverlauf
│  └─ theme
├─ MainActivity.kt
└─ NozioApplication.kt
```

## Voraussetzungen

- Android Studio (aktuelle stabile Version empfohlen)
- JDK 11
- Android SDK:
  - `compileSdk 35`
  - `targetSdk 35`
  - `minSdk 26`

## Lokales Setup

1. Repository klonen
2. Projekt in Android Studio oeffnen
3. Gradle-Sync abwarten
4. App auf Emulator oder Geraet starten

Alternative per CLI:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Build-Konfiguration

- `applicationId`: `de.ingomc.nozio`
- `versionName`: `0.1.0`
- `versionCode`: `1`

## Datenquellen und Persistenz

- Extern: OpenFoodFacts (Suche + Barcode)
- Lokal:
  - Room DB (`food_items`, `diary_entries`, `daily_activity`)
  - DataStore (`user_preferences`)
- Bei Netzproblemen nutzt die Suche einen lokalen Fallback (Cache-Suche in Room).

## Aktueller Konzeptfokus

- Daily Nutrition Control (Single-User, local-first)
- Schnelle Eingabe statt komplexer Planungslogik
- Zielorientierte Darstellung im Dashboard

Detailierte Bestandsaufnahme:
- [APP_KONZEPT_AKTUELL.txt](APP_KONZEPT_AKTUELL.txt)

## Roadmap / Releases

- [ROADMAP.md](ROADMAP.md)
- [RELEASE_NOTES_0.1.0.md](RELEASE_NOTES_0.1.0.md)

## Hinweise

- Kamera-Berechtigung wird fuer Barcode-Scan benoetigt.
- Die App ist derzeit nicht auf Multi-Device-Sync ausgelegt (kein Account/Cloud).

## Lizenz

Aktuell keine Lizenzdatei hinterlegt.  
Falls das Repo oeffentlich genutzt werden soll, empfiehlt sich eine `LICENSE` (z. B. MIT).

