import { readFile } from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { config as loadDotenv } from "dotenv";

import { chunkDocuments, transformRecords } from "./lib.js";

loadDotenv({
  path: process.env.DOTENV_CONFIG_PATH || path.resolve(process.cwd(), "infra/food-api.env")
});

type CliConfig = {
  filePath: string;
  meiliUrl: string;
  meiliMasterKey: string;
  indexName: string;
  batchSize: number;
};

function readCliConfig(argv: string[], env: NodeJS.ProcessEnv): CliConfig {
  const args = new Map<string, string>();
  for (let index = 0; index < argv.length; index += 1) {
    const current = argv[index];
    const next = argv[index + 1];
    if (current?.startsWith("--") && next) {
      args.set(current.slice(2), next);
      index += 1;
    }
  }

  return {
    filePath: args.get("file") ?? "data/seed/foods.cleaned.json",
    meiliUrl: env.MEILI_URL ?? "http://127.0.0.1:7700",
    meiliMasterKey: env.MEILI_MASTER_KEY ?? "change-me",
    indexName: env.MEILI_INDEX_NAME ?? "foods",
    batchSize: Number(args.get("batch-size") ?? "1000")
  };
}

async function main() {
  if (typeof globalThis.Headers === "undefined") {
    const undici = await import("undici");
    globalThis.Headers = undici.Headers as typeof globalThis.Headers;
    globalThis.Request = undici.Request as typeof globalThis.Request;
    globalThis.Response = undici.Response as typeof globalThis.Response;
    globalThis.fetch = undici.fetch as typeof globalThis.fetch;
  }

  const { MeiliSearch } = await import("meilisearch");
  const config = readCliConfig(process.argv.slice(2), process.env);
  const resolvedPath = path.resolve(config.filePath);
  const rawJson = await readFile(resolvedPath, "utf8");
  const rawRecords = JSON.parse(rawJson);

  if (!Array.isArray(rawRecords)) {
    throw new Error("Input JSON must be an array of food records.");
  }

  const { documents, stats } = transformRecords(rawRecords);
  console.log(`Input records: ${stats.total}`);
  console.log(`Accepted: ${stats.accepted}`);
  console.log(`Rejected: ${stats.rejected}`);

  const client = new MeiliSearch({
    host: config.meiliUrl,
    apiKey: config.meiliMasterKey
  });
  const index = client.index(config.indexName);

  await index.updateSettings({
    searchableAttributes: ["displayName", "name", "brand", "barcode", "searchTerms"],
    filterableAttributes: ["barcode", "source"]
  });

  for (const batch of chunkDocuments(documents, config.batchSize)) {
    await index.addDocuments(batch, { primaryKey: "id" });
  }

  console.log(`Indexed ${documents.length} documents into ${config.indexName}.`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
