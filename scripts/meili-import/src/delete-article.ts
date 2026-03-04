import path from "node:path";
import process from "node:process";
import { existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { config as loadDotenv } from "dotenv";
import { createInterface } from "node:readline/promises";

function resolveDotenvPath(): string {
  if (process.env.DOTENV_CONFIG_PATH) {
    return process.env.DOTENV_CONFIG_PATH;
  }

  const scriptDir = path.dirname(fileURLToPath(import.meta.url));
  const candidates = [
    path.resolve(process.cwd(), "infra/food-api.env"),
    path.resolve(process.cwd(), "../../infra/food-api.env"),
    path.resolve(scriptDir, "../../../infra/food-api.env")
  ];

  const found = candidates.find((candidate) => existsSync(candidate));
  return found ?? candidates[0];
}

loadDotenv({
  path: resolveDotenvPath()
});

type CliConfig = {
  barcode: string;
  baseUrl: string;
  adminToken: string;
  yes: boolean;
};

type ApiErrorPayload = {
  error?: {
    code?: string;
    message?: string;
  };
};

type BarcodeItem = {
  id: string;
  name: string;
  brand: string | null;
  displayName: string;
  barcode: string | null;
  caloriesPer100g: number;
  proteinPer100g: number;
  fatPer100g: number;
  carbsPer100g: number;
  sugarPer100g: number;
  servingSize: string | null;
  servingQuantity: number | null;
  packageSize: string | null;
  packageQuantity: number | null;
  source: string;
};

type BarcodeResponse = {
  item: BarcodeItem;
};

type DeleteResponse = {
  deleted: boolean;
  item: BarcodeItem;
};

function normalizeBaseUrl(value: string): string {
  return value.trim().replace(/\/+$/, "");
}

function parseCliConfig(argv: string[], env: NodeJS.ProcessEnv): CliConfig {
  const args = new Map<string, string>();
  const positionals: string[] = [];

  for (let index = 0; index < argv.length; index += 1) {
    const current = argv[index];
    if (!current) {
      continue;
    }
    if (current === "--") {
      continue;
    }
    if (current === "--yes" || current === "-y") {
      args.set("yes", "true");
      continue;
    }
    if (current.startsWith("--")) {
      const key = current.slice(2);
      if (!key) {
        continue;
      }
      const next = argv[index + 1];
      if (next && !next.startsWith("--")) {
        args.set(key, next);
        index += 1;
      }
      continue;
    }
    positionals.push(current);
  }

  const barcode = (args.get("barcode") ?? positionals[0] ?? "").trim();
  if (!/^\d+$/.test(barcode)) {
    throw new Error("Usage: pnpm delete:article <barcode> [--base-url <url>] [--yes]");
  }

  const defaultBaseUrl = env.FOOD_API_BASE_URL ?? env.FOOD_API_URL ?? `http://127.0.0.1:${env.PORT ?? "3000"}`;
  const baseUrl = normalizeBaseUrl(args.get("base-url") ?? defaultBaseUrl);
  const adminToken = (env.ADMIN_API_TOKEN ?? "").trim();
  if (!adminToken) {
    throw new Error("Missing ADMIN_API_TOKEN in environment.");
  }

  return {
    barcode,
    baseUrl,
    adminToken,
    yes: args.get("yes") === "true"
  };
}

async function readJson<T>(response: Response): Promise<T | null> {
  const text = await response.text();
  if (!text) {
    return null;
  }
  return JSON.parse(text) as T;
}

function printItem(item: BarcodeItem) {
  console.log("Article preview:");
  console.log(`- id: ${item.id}`);
  console.log(`- displayName: ${item.displayName}`);
  console.log(`- barcode: ${item.barcode ?? "-"}`);
  console.log(`- source: ${item.source}`);
  console.log(
    `- macros/100g: kcal ${item.caloriesPer100g}, protein ${item.proteinPer100g}g, carbs ${item.carbsPer100g}g, sugar ${item.sugarPer100g}g, fat ${item.fatPer100g}g`
  );
  console.log(
    `- serving: ${item.servingSize ?? "-"} (${item.servingQuantity ?? "-"}) | package: ${item.packageSize ?? "-"} (${item.packageQuantity ?? "-"})`
  );
}

async function confirmDelete(): Promise<boolean> {
  const rl = createInterface({
    input: process.stdin,
    output: process.stdout
  });
  try {
    const answer = await rl.question('Delete this article now? Type "delete" to confirm: ');
    return answer.trim().toLowerCase() === "delete";
  } finally {
    rl.close();
  }
}

async function requestPreview(config: CliConfig): Promise<BarcodeItem> {
  const response = await fetch(`${config.baseUrl}/admin/foods/barcode/${config.barcode}`, {
    method: "GET",
    headers: {
      "x-admin-token": config.adminToken
    }
  });

  if (!response.ok) {
    const payload = await readJson<ApiErrorPayload>(response);
    const reason = payload?.error?.message ?? response.statusText;
    throw new Error(`Preview failed (${response.status}): ${reason}`);
  }

  const payload = await readJson<BarcodeResponse>(response);
  if (!payload?.item) {
    throw new Error("Preview failed: empty response payload.");
  }
  return payload.item;
}

async function requestDelete(config: CliConfig): Promise<DeleteResponse> {
  const response = await fetch(`${config.baseUrl}/admin/foods/barcode/${config.barcode}`, {
    method: "DELETE",
    headers: {
      "x-admin-token": config.adminToken
    }
  });

  if (!response.ok) {
    const payload = await readJson<ApiErrorPayload>(response);
    const reason = payload?.error?.message ?? response.statusText;
    throw new Error(`Delete failed (${response.status}): ${reason}`);
  }

  const payload = await readJson<DeleteResponse>(response);
  if (!payload?.deleted || !payload.item) {
    throw new Error("Delete failed: unexpected response payload.");
  }
  return payload;
}

async function main() {
  const config = parseCliConfig(process.argv.slice(2), process.env);
  const item = await requestPreview(config);
  printItem(item);

  if (!config.yes) {
    const confirmed = await confirmDelete();
    if (!confirmed) {
      console.log("Aborted. Nothing deleted.");
      return;
    }
  }

  const deleted = await requestDelete(config);
  console.log(`Deleted article: ${deleted.item.displayName} (${deleted.item.id})`);
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
