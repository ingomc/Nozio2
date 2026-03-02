import path from "node:path";
import { config as loadDotenv } from "dotenv";
import { z } from "zod";

loadDotenv({
  path: process.env.DOTENV_CONFIG_PATH || path.resolve(process.cwd(), "infra/food-api.env")
});

const configSchema = z.object({
  PORT: z.coerce.number().int().positive().default(3000),
  HOST: z.string().min(1).default("0.0.0.0"),
  MEILI_URL: z.string().url().default("http://127.0.0.1:7700"),
  MEILI_MASTER_KEY: z.string().min(1).default("change-me"),
  MEILI_INDEX_NAME: z.string().min(1).default("foods"),
  FOOD_API_KEY: z.string().min(1).default("dev-change-me"),
  ADMIN_API_TOKEN: z.string().default(""),
  MAX_SEED_UPLOAD_MB: z.coerce.number().int().positive().default(100),
  MEILI_AUTO_SEED: z
    .enum(["true", "false"])
    .default("true")
    .transform((value) => value === "true"),
  MEILI_SEED_FILE: z.string().min(1).default("/data/seeds/foods.json"),
  MEILI_SEED_FALLBACK_FILE: z.string().min(1).default("data/seed/foods.de.sample.json")
});

export type AppConfig = z.infer<typeof configSchema>;

export function readConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  return configSchema.parse(env);
}
