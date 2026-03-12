import process from "node:process";

import { buildApp } from "./app.js";
import { readConfig } from "./config.js";
import {
  getSeedSourcePath,
  importSeedRecords,
  isIndexEmpty,
  loadConfiguredSeedRecords,
  waitForMeili
} from "./seed.js";

async function seedIndexIfEmpty() {
  const config = readConfig();
  if (!config.MEILI_AUTO_SEED) {
    return;
  }

  const sourcePath = await getSeedSourcePath(config);
  if (!sourcePath) {
    return;
  }

  await waitForMeili(config);

  if (!(await isIndexEmpty(config))) {
    return;
  }

  const rawRecords = await loadConfiguredSeedRecords(config);
  await importSeedRecords(config, rawRecords);
}

async function main() {
  const config = readConfig();
  await seedIndexIfEmpty();

  const app = buildApp(config);
  app.log.info(
    {
      host: config.HOST,
      port: config.PORT,
      geminiModel: config.GEMINI_MODEL,
      geminiFallbackModels: config.GEMINI_FALLBACK_MODELS
    },
    "Starting food-api"
  );
  await app.listen({
    host: config.HOST,
    port: config.PORT
  });
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
