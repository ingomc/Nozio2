# Nozio v0.8.0

## Highlights
- Backup & Wiederherstellung laufen jetzt ohne Google-Setup direkt lokal auf dem Geraet.

## Improvements
- Neuer lokaler Backup-Service statt Cloud-Abhaengigkeit fuer den Kernflow.
- Backup-Datei wird komprimiert als `nozio-backup-v1.json.gz` gespeichert.
- Wiederherstellung ist rueckwaertskompatibel:
  - zuerst aus `.json.gz`
  - fallback auf bestehende `.json`
- Backup-UI auf lokalen Speicher angepasst.

## Notes
- Android-Version auf `0.8.0` angehoben.
