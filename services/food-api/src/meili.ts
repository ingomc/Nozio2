import { MeiliSearch } from "meilisearch";

import type { AppConfig } from "./config.js";

export function createMeiliClient(config: AppConfig) {
  return new MeiliSearch({
    host: config.MEILI_URL,
    apiKey: config.MEILI_MASTER_KEY
  });
}
