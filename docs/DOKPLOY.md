# Dokploy Deploy

Diese Anleitung beschreibt den Deploy des Stacks nach Dokploy unter `noziodb.ingomc.de`.

## Zielbild

- `food-api` ist oeffentlich unter `https://noziodb.ingomc.de` erreichbar
- `meilisearch` bleibt intern
- TLS endet in Dokploy
- der Suchindex `foods` wird nach dem Deploy separat importiert

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
- die Parquet-Datei lokal oder auf einer Build-Maschine fuer den spaeteren Import

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
FOOD_API_KEY=<separates-api-secret>
FOOD_API_PUBLIC_PORT=3000
```

Hinweis:

- `MEILI_URL` muss intern auf `http://meilisearch:7700` zeigen
- `MEILI_MASTER_KEY` muss in beiden Services identisch sein
- `FOOD_API_KEY` ist dein Client-Key fuer die Android-App

### 3. Domain binden

In Dokploy die Domain am `food-api`-Service hinterlegen:

- Domain: `noziodb.ingomc.de`
- Ziel-Port: `3000`
- HTTPS/TLS in Dokploy aktivieren

`meilisearch` bekommt keine oeffentliche Domain.

### 4. Volume

Stelle sicher, dass das Compose-Volume `meili_data` persistent bleibt. Dort liegt dein Suchindex.

## Erster Start

Nach dem ersten Deploy:

1. Stack starten lassen
2. `https://noziodb.ingomc.de/health` pruefen
3. dann erst Daten importieren

Der Healthcheck muss liefern:

```json
{"status":"ok"}
```

## Datenimport

Der Import passiert bewusst separat vom Deploy.

### 1. Import-JSON aus Parquet erzeugen

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

### 2. In den Server-Index importieren

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
- wenn du Meili in Dokploy nicht oeffentlich expose willst, fuehre den Import stattdessen auf dem Server oder in einem internen Kontext aus

## Empfehlung fuer den Import in Dokploy

Wenn `meilisearch` nicht oeffentlich erreichbar ist, hast du zwei saubere Wege:

1. den Import einmal direkt auf dem Server laufen lassen
2. temporaer eine interne Shell/Job gegen denselben Docker-Netzwerk-Kontext verwenden

Fuer den Start ist Weg 1 meist am einfachsten.

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

- `502 MEILI_UNAVAILABLE`
  - `food-api` erreicht `meilisearch` intern nicht
  - `MEILI_URL` oder der interne Service-Name stimmt nicht

- leere Treffer
  - Import wurde noch nicht ausgefuehrt
  - oder der Index `foods` ist leer

- nach Redeploy keine Daten mehr
  - pruefen, ob `meili_data` persistent gemountet bleibt
