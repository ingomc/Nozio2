# Dokploy Deploy

Diese Anleitung beschreibt den Deploy des Stacks nach Dokploy unter `noziodb.ingomc.de`.

## Zielbild

- `food-api` ist oeffentlich unter `https://noziodb.ingomc.de` erreichbar
- `meilisearch` bleibt intern
- TLS endet in Dokploy
- hochgeladene Seed-Dateien bleiben persistent im Server-Volume erhalten
- der Suchindex `foods` wird bei leerem Index automatisch aus `/data/seeds/foods.json` oder dem Repo-Fallback befuellt

## Dateien

- Compose: [infra/docker-compose.prod.yml](/Users/andre/Documents/dev/Nozio2/infra/docker-compose.prod.yml)
- API-Env-Beispiel: [infra/food-api.prod.env.example](/Users/andre/Documents/dev/Nozio2/infra/food-api.prod.env.example)
- Meili-Env-Beispiel: [infra/meilisearch.prod.env.example](/Users/andre/Documents/dev/Nozio2/infra/meilisearch.prod.env.example)
- API-Dockerfile: [services/food-api/Dockerfile](/Users/andre/Documents/dev/Nozio2/services/food-api/Dockerfile)

## Vor dem Deploy

Du brauchst:

- DNS-Eintrag fuer `noziodb.ingomc.de` auf deinen Server
- ein langes `MEILI_MASTER_KEY`
- ein separates langes `FOOD_API_KEY`
- ein separates langes `ADMIN_API_TOKEN`
- optional deine grosse JSON-Datei lokal fuer den spaeteren Upload

## Dokploy Setup

### 1. Compose-App anlegen

In Dokploy:

- `Create Application`
- Typ `Docker Compose`
- Repository dieses Projekts verbinden
- Branch `main`
- Compose-Datei: `infra/docker-compose.prod.yml`

### 2. Env-Dateien in Dokploy setzen

Lege in Dokploy die Variablen aus den Beispiel-Dateien an.

Fuer `meilisearch`:

```env
MEILI_MASTER_KEY=<langes-zufaelliges-secret>
```

Fuer `food-api`:

```env
PORT=3000
HOST=0.0.0.0
MEILI_URL=http://meilisearch:7700
MEILI_MASTER_KEY=<gleiches-meili-secret>
MEILI_INDEX_NAME=foods
ADMIN_API_TOKEN=<separates-admin-secret>
MAX_SEED_UPLOAD_MB=100
MEILI_AUTO_SEED=true
MEILI_SEED_FILE=/data/seeds/foods.json
MEILI_SEED_FALLBACK_FILE=data/seed/foods.de.sample.json
FOOD_API_KEY=<separates-api-secret>
FOOD_API_PUBLIC_PORT=3000
```

Hinweis:

- `MEILI_URL` muss intern auf `http://meilisearch:7700` zeigen
- `MEILI_MASTER_KEY` muss in beiden Services identisch sein
- `FOOD_API_KEY` ist dein Client-Key fuer die Android-App
- `ADMIN_API_TOKEN` schuetzt den Upload-Endpoint
- `MEILI_SEED_FILE` zeigt auf die persistent gespeicherte Server-Seed-Datei
- `MEILI_SEED_FALLBACK_FILE` bleibt dein Repo-Fallback fuer leere Volumes

### 3. Domain binden

In Dokploy die Domain am `food-api`-Service hinterlegen:

- Domain: `noziodb.ingomc.de`
- Ziel-Port: `3000`
- HTTPS/TLS in Dokploy aktivieren

`meilisearch` bekommt keine oeffentliche Domain.

### 4. Volumes

Stelle sicher, dass beide Compose-Volumes persistent bleiben:

- `meili_data` fuer den Suchindex
- `seed_data` fuer hochgeladene Seed-Dateien unter `/data/seeds`

## Erster Start

Nach dem ersten Deploy:

1. Stack starten lassen
2. `https://noziodb.ingomc.de/health` pruefen
3. optional deine echte Seed-Datei per Admin-Endpoint hochladen
4. danach Such-Endpunkte testen

Der Healthcheck muss liefern:

```json
{"status":"ok"}
```

## Seed-Daten

Standardmaessig nutzt der Container zuerst `/data/seeds/foods.json` aus dem persistenten `seed_data`-Volume. Wenn dort noch nichts liegt, faellt er auf `data/seed/foods.de.sample.json` aus dem Repo zurueck.

Wichtig:

- der Auto-Seed laeuft nur, wenn `MEILI_AUTO_SEED=true` ist
- importiert wird nur, wenn `foods` noch nicht existiert oder `0` Dokumente hat
- bestehende Daten werden nicht ueberschrieben

### Seed-Datei per API hochladen

Wenn du deine grosse JSON auf den Server pushen willst, kannst du sie direkt an Fastify senden:

```bash
curl -X POST \
  'https://noziodb.ingomc.de/admin/seed-upload?importNow=true&resetIndex=false' \
  -H 'x-admin-token: <dein-admin-secret>' \
  -H 'Content-Type: application/json' \
  --data-binary @/pfad/zu/foods.json
```

Das macht drei Dinge:

- speichert die Datei persistent unter `/data/seeds/foods.json`
- importiert sie optional sofort in Meili
- nutzt dieselbe Datei spaeter wieder fuer leere Neu-Deployments

Fuer eine komplette Neu-Befuellung des Index kannst du `resetIndex=true` setzen:

```bash
curl -X POST \
  'https://noziodb.ingomc.de/admin/seed-upload?importNow=true&resetIndex=true' \
  -H 'x-admin-token: <dein-admin-secret>' \
  -H 'Content-Type: application/json' \
  --data-binary @/pfad/zu/foods.json
```

### Eigenes Start-Dataset aus Parquet erzeugen

Lokal:

```bash
python3 -m venv .venv-parquet
.venv-parquet/bin/pip install -r scripts/meili-import/requirements-parquet.txt
PATH="$(pwd)/.venv-parquet/bin:$PATH" corepack pnpm extract:off \
  --input "$(pwd)/food.parquet" \
  --output "$(pwd)/data/seed/foods.de.cleaned.json" \
  --country Germany \
  --min-completeness 0.2
```

### Optional: manuell in den Server-Index importieren

Das ist nur noch der Fallback, falls du bewusst am Admin-Upload vorbei direkt gegen Meili arbeiten willst.

Setze vor dem Import die produktiven Variablen:

```bash
export MEILI_URL="https://noziodb.ingomc.de"
export MEILI_MASTER_KEY="<dein-meili-secret>"
export MEILI_INDEX_NAME="foods"
```

Dann:

```bash
corepack pnpm import:meili --file "$(pwd)/data/seed/foods.de.cleaned.json"
```

Wichtig:

- dieser Befehl spricht direkt gegen die Meili-API-URL aus `MEILI_URL`
- in deinem Dokploy-Setup ist Meili normalerweise nicht oeffentlich erreichbar
- daher funktioniert dieser Weg nur auf dem Server selbst oder in einem internen Docker-/Dokploy-Kontext
- fuer kuenftige Neu-Deploys ohne bestehendes Volume bleibt der Repo-Fallback unter `MEILI_SEED_FALLBACK_FILE` bestehen

## Empfehlung fuer Seed und Import in Dokploy

Wenn `meilisearch` nicht oeffentlich erreichbar ist, hast du zwei saubere Wege:

1. fuer den initialen Grundbestand den Admin-Upload nach `/data/seeds/foods.json` verwenden
2. fuer spaetere Updates denselben Endpoint erneut aufrufen
3. den direkten Meili-Import nur als Sonderfall fuer interne Wartung nutzen

Fuer deinen Fall mit kleiner Datenbasis ist Weg 1 der passende Default.

## Android danach

Nach dem Deploy in der Android-App setzen:

- `FOOD_API_BASE_URL=https://noziodb.ingomc.de/`
- `FOOD_API_KEY=<dein-api-secret>`

## Checks nach dem Deploy

Diese drei Checks sollten funktionieren:

1. Health

```bash
curl https://noziodb.ingomc.de/health
```

2. Suche

```bash
curl -H 'x-api-key: <dein-api-secret>' \
  'https://noziodb.ingomc.de/v1/foods/search?q=Deit%20Cola-Mix&limit=1'
```

3. Barcode

```bash
curl -H 'x-api-key: <dein-api-secret>' \
  'https://noziodb.ingomc.de/v1/foods/barcode/4104450003188'
```

## Troubleshooting

- `401 Unauthorized`
  - `FOOD_API_KEY` in App/Client passt nicht zum Server

- `401 Unauthorized` beim Upload
  - `x-admin-token` passt nicht zu `ADMIN_API_TOKEN`

- `503 ADMIN_DISABLED`
  - `ADMIN_API_TOKEN` ist in Dokploy nicht gesetzt

- `502 MEILI_UNAVAILABLE`
  - `food-api` erreicht `meilisearch` intern nicht
  - `MEILI_URL` oder der interne Service-Name stimmt nicht

- leere Treffer
  - Auto-Seed-Datei wurde nicht gefunden oder war leer
  - oder ein manueller Import wurde noch nicht ausgefuehrt
  - oder der Index `foods` ist leer

- nach Redeploy keine Daten mehr
  - pruefen, ob `meili_data` persistent gemountet bleibt
  - pruefen, ob `seed_data` persistent gemountet bleibt
