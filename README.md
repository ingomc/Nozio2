# Nozio Monorepo

Nozio ist jetzt als Monorepo organisiert. Die bestehende Android-App lebt unter `apps/android`, dazu kommen ein self-hosted Food API auf Basis von Fastify + Meilisearch sowie ein Importskript fuer dein OpenFoodFacts-Parquet.

## App installieren

Wenn du Nozio einfach auf einem Android-Geraet installieren willst:

1. Oeffne die aktuelle Release-Seite auf GitHub und lade die neueste `.apk` aus den Assets herunter.
2. Oeffne die heruntergeladene APK auf dem Geraet.
3. Falls Android blockiert, erlaube fuer den verwendeten Downloader oder Dateimanager kurz `Apps aus unbekannten Quellen`.
4. Bestaetige die Installation und oeffne danach die App.

Typische Hinweise:

- Je nach Android-Version erscheint die Freigabe unter `Einstellungen > Sicherheit`, `Datenschutz` oder direkt im Installationsdialog.
- Wenn beim Update `App nicht installiert` erscheint, zuerst die bestehende Version deinstallieren und dann die neue APK installieren.
- Releases findest du im GitHub-Repo unter `Releases`.

## Struktur

```text
.
├─ apps/
│  └─ android/
├─ data/
│  ├─ examples/
│  └─ seed/
├─ infra/
├─ packages/
│  └─ food-contracts/
├─ scripts/
│  └─ meili-import/
└─ services/
   └─ food-api/
```

## Schnellstart

### 1. Meilisearch starten

```bash
cp infra/meilisearch.env.example infra/meilisearch.env
docker compose -f infra/docker-compose.yml --env-file infra/meilisearch.env up -d meilisearch
```

### 2. Node-Dependencies installieren

```bash
pnpm install
```

### 3. Beispieldaten oder eigenes JSON importieren

```bash
cp infra/food-api.env.example infra/food-api.env
pnpm import:meili --file data/examples/foods.sample.json
```

Fuer dein eigenes JSON lege z. B. `data/seed/foods.cleaned.json` an und uebergib den Pfad per `--file`.

Wenn du mit dem OpenFoodFacts-Parquet `food.parquet` arbeitest, extrahierst du daraus zuerst ein schlankes JSON:

```bash
python3 -m venv .venv-parquet
.venv-parquet/bin/pip install -r scripts/meili-import/requirements-parquet.txt
corepack pnpm extract:off --input /Users/andre/Documents/dev/Nozio2/food.parquet --output /Users/andre/Documents/dev/Nozio2/data/seed/foods.de.cleaned.json --country Germany --min-completeness 0.2
corepack pnpm import:meili --file /Users/andre/Documents/dev/Nozio2/data/seed/foods.de.cleaned.json
```

Default-Verhalten des Extractors:

- `product_name`, `code` und `energy-kcal` aus `nutriments` sind Pflicht
- Makros pro 100g sind standardmaessig ebenfalls Pflicht
- `completeness >= 0.2`
- Bild und Serving-Angaben sind optional

Unterstuetzte Flags:

- `--input <pfad>`
- `--output <pfad>`
- `--country Germany`
- `--min-completeness 0.2`
- `--require-image true`
- `--require-full-macros false`
- `--limit 10000`

### 4. Food API starten

```bash
pnpm dev:food-api
```

### 5. Android-App starten

```bash
cd apps/android
./gradlew assembleDebug
```

Die Android-App erwartet standardmaessig:

- `FOOD_API_BASE_URL = http://10.0.2.2:3000/`
- `FOOD_API_KEY = dev-change-me`

Fuer echte Geraete musst du die Base-URL auf deine LAN-IP anpassen.

## Services

### Android

- Pfad: `apps/android`
- Stack: Kotlin, Jetpack Compose, Room, Retrofit
- Die App spricht nur noch mit dem self-hosted Food API.

### Food API

- Pfad: `services/food-api`
- Stack: Node.js, TypeScript, Fastify, Meilisearch
- Endpunkte:
  - `GET /health`
  - `GET /v1/foods/search?q=<query>&limit=<n>`
  - `GET /v1/foods/barcode/:barcode`
  - `POST /v1/foods/custom`

### Importskript

- Pfad: `scripts/meili-import`
- Validiert und transformiert dein JSON in indexierbare Meilisearch-Dokumente.

## Umgebung

### `infra/meilisearch.env`

```env
MEILI_MASTER_KEY=change-me
MEILI_HOST_PORT=7700
```

### `infra/food-api.env`

```env
PORT=3000
HOST=0.0.0.0
MEILI_URL=http://127.0.0.1:7700
MEILI_MASTER_KEY=change-me
MEILI_INDEX_NAME=foods
ADMIN_API_TOKEN=change-me-admin
MAX_SEED_UPLOAD_MB=100
MEILI_AUTO_SEED=true
MEILI_SEED_FILE=data/seed/runtime/foods.json
MEILI_SEED_FALLBACK_FILE=data/seed/foods.de.sample.json
FOOD_API_KEY=dev-change-me
FOOD_API_HOST_PORT=3000
GEMINI_API_KEY=change-me-gemini
GEMINI_MODEL=gemini-2.5-flash
GEMINI_FALLBACK_MODELS=gemini-3.1-flash-lite-preview,gemini-3-flash-preview
VISION_TIMEOUT_MS=25000
VISION_MAX_IMAGE_MB=4
```

Fuer Docker Compose wird `MEILI_URL` automatisch auf `http://meilisearch:7700` gesetzt. Der Wert in `food-api.env` ist fuer den lokalen Non-Docker-Start per `pnpm dev:food-api`.

## Android-Hinweise

- Fuer lokale HTTP-Entwicklung ist `usesCleartextTraffic="true"` gesetzt.
- Room bleibt als lokaler Cache/Fallback fuer Suchdaten erhalten.
- Barcode- und Textsuche laufen ueber dein Backend statt direkt ueber OpenFoodFacts.

## JSON-Anforderungen

Jeder Datensatz sollte diese Felder liefern:

- `id`
- `name`
- `brand`
- `barcode`
- `caloriesPer100g`
- `proteinPer100g`
- `fatPer100g`
- `carbsPer100g`

Fehlende Makros werden beim Import auf `0` gesetzt. Datensaetze ohne `name` oder ohne numerische `caloriesPer100g` werden verworfen.

## Tests

Node:

```bash
pnpm test
```

Android:

```bash
cd apps/android
./gradlew testDebugUnitTest
```

## Android Release Shortcut

```bash
pnpm release:android -- 0.11.2
```

Optional direkt pushen:

```bash
pnpm release:android -- 0.11.2 --push
```

Der Command:
- bumpt `versionName` und `versionCode` in `apps/android/app/build.gradle.kts`
- erstellt den Commit `release: v<version>`
- erstellt den Tag `v<version>`
- pusht mit `--push` Branch und Tag nach `origin`

Checks vor dem Lauf:
- Git-Working-Tree muss sauber sein
- `RELEASE_NOTES_<version>.md` muss existieren
- Tag darf lokal/remote noch nicht existieren

## Release Notes

- [RELEASE_NOTES_0.1.0.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.1.0.md)
- [RELEASE_NOTES_0.3.0.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.3.0.md)
- [RELEASE_NOTES_0.4.0.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.4.0.md)
- [RELEASE_NOTES_0.5.0.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.5.0.md)
- [RELEASE_NOTES_0.5.1.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.5.1.md)
- [RELEASE_NOTES_0.6.0.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.6.0.md)
- [RELEASE_NOTES_0.7.0.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.7.0.md)
- [RELEASE_NOTES_0.7.1.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.7.1.md)
- [RELEASE_NOTES_0.7.2.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.7.2.md)
- [RELEASE_NOTES_0.8.0.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.8.0.md)
- [RELEASE_NOTES_0.8.1.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.8.1.md)
- [RELEASE_NOTES_0.8.2.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.8.2.md)
- [RELEASE_NOTES_0.9.0.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_0.9.0.md)
- [RELEASE_NOTES_0.10.0.md](/Users/andre/Documents/dev/Nozio2/RELEASE_NOTES_0.10.0.md)
- [RELEASE_NOTES_NEXT.md](/Users/ap4716/AndroidStudioProjects/Nozio2/RELEASE_NOTES_NEXT.md)

## Docker Start

Beide Services zusammen lokal:

```bash
docker compose -p nozio-local -f infra/docker-compose.yml --env-file infra/meilisearch.env up -d --build
```

Danach ist das API unter `http://127.0.0.1:3000` erreichbar. Den Datenimport machst du weiter separat vom Host aus:

```bash
python3 -m venv .venv-parquet
.venv-parquet/bin/pip install -r /Users/andre/Documents/dev/Nozio2/scripts/meili-import/requirements-parquet.txt
.venv-parquet/bin/python /Users/andre/Documents/dev/Nozio2/scripts/meili-import/src/extract-off.py \
  --input /Users/andre/Documents/dev/Nozio2/food.parquet \
  --output /Users/andre/Documents/dev/Nozio2/data/seed/foods.de.cleaned.json \
  --country Germany \
  --min-completeness 0.2

curl -sS -X POST 'http://127.0.0.1:7700/indexes/foods/documents?primaryKey=id' \
  -H 'Authorization: Bearer change-me' \
  -H 'Content-Type: application/json' \
  --data-binary @/Users/andre/Documents/dev/Nozio2/data/seed/foods.de.cleaned.json
```

Fuer Dokploy ist der relevante Service das Image aus [Dockerfile](/Users/andre/Documents/dev/Nozio2/services/food-api/Dockerfile) plus eine laufende Meilisearch-Instanz mit denselben Env-Variablen.

Eine konkrete Dokploy-Anleitung fuer `noziodb.ingomc.de` liegt in [DOKPLOY.md](/Users/andre/Documents/dev/Nozio2/docs/DOKPLOY.md).

## Production Compose

Fuer Serverbetrieb liegt eine getrennte Compose-Datei unter [docker-compose.prod.yml](/Users/andre/Documents/dev/Nozio2/infra/docker-compose.prod.yml). Sie trennt lokale Dev-Einstellungen von dem, was du in Dokploy oder auf einem Server deployen willst.

Vorbereitung:

```bash
cp infra/meilisearch.prod.env.example infra/meilisearch.prod.env
cp infra/food-api.prod.env.example infra/food-api.prod.env
```

Dann Secrets setzen:

- `MEILI_MASTER_KEY` lang und zufaellig
- `FOOD_API_KEY` separat und ebenfalls lang

Start:

```bash
docker compose -p nozio-prod -f infra/docker-compose.prod.yml up -d --build
```

Eigenschaften des Production-Setups:

- `meilisearch` ist nicht oeffentlich per Host-Port exposed
- `food-api` ist der einzige nach aussen freigegebene Service
- `food-api` spricht intern ueber `http://meilisearch:7700`
- Daten liegen persistent im Volume `meili_data`
- Seed-Dateien fuer Uploads liegen persistent im Volume `seed_data`
- bei leerem Index importiert der Container zuerst `MEILI_SEED_FILE`, sonst `MEILI_SEED_FALLBACK_FILE`

Empfehlung fuer Dokploy:

- `food-api` hinter deine Domain oder Reverse Proxy legen
- `meilisearch` nur intern erreichbar lassen
- TLS und oeffentliche Exposition nicht auf Compose-Ebene, sondern ueber Dokploy/Proxy terminieren
- fuer Uploads den Admin-Endpoint mit `x-admin-token` und dem persistenten `seed_data`-Volume nutzen
