import { access, mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";

import {
  FOOD_SOURCE,
  importInputSchema,
  meiliFoodDocumentSchema,
  type ImportInput,
  type MeiliFoodDocument
} from "@nozio/food-contracts";

import type { AppConfig } from "./config.js";
import { createMeiliClient } from "./meili.js";

const SEARCHABLE_ATTRIBUTES = ["displayName", "name", "brand", "barcode", "searchTerms"];
const FILTERABLE_ATTRIBUTES = ["barcode", "source"];
const DEFAULT_BATCH_SIZE = 1000;

function normalizeBarcode(value: string | number | null | undefined): string | null {
  if (value == null) {
    return null;
  }

  const digits = String(value).replace(/\D/g, "");
  return digits.length > 0 ? digits : null;
}

function normalizeNumber(value: number | undefined): number {
  return typeof value === "number" && Number.isFinite(value) ? value : 0;
}

function normalizeNullableNumber(value: number | null | undefined): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function normalizeNullableText(value: string | null | undefined): string | null {
  const trimmed = value?.trim();
  return trimmed ? trimmed : null;
}

function buildDisplayName(name: string, brand?: string | null): string {
  const trimmedBrand = brand?.trim();
  return trimmedBrand ? `${name} (${trimmedBrand})` : name;
}

function buildSearchTerms(input: { name: string; brand?: string | null; barcode?: string | null }) {
  return [input.name, input.brand ?? "", input.barcode ?? ""]
    .map((part) => part.trim())
    .filter(Boolean)
    .join(" ");
}

function transformRecord(raw: unknown): MeiliFoodDocument | null {
  const parsed = importInputSchema.safeParse(raw);
  if (!parsed.success) {
    return null;
  }

  const value: ImportInput = parsed.data;
  const name = value.name?.trim();
  if (!name) {
    return null;
  }

  if (typeof value.caloriesPer100g !== "number" || !Number.isFinite(value.caloriesPer100g)) {
    return null;
  }

  const barcode = normalizeBarcode(value.barcode);
  const brand = value.brand?.trim() || null;

  return meiliFoodDocumentSchema.parse({
    id: String(value.id),
    name,
    brand,
    displayName: buildDisplayName(name, brand),
    barcode,
    searchTerms: buildSearchTerms({ name, brand, barcode }),
    caloriesPer100g: value.caloriesPer100g,
    proteinPer100g: normalizeNumber(value.proteinPer100g),
    fatPer100g: normalizeNumber(value.fatPer100g),
    carbsPer100g: normalizeNumber(value.carbsPer100g),
    servingSize: normalizeNullableText(value.servingSize),
    servingQuantity: normalizeNullableNumber(value.servingQuantity),
    packageSize: normalizeNullableText(value.packageSize),
    packageQuantity: normalizeNullableNumber(value.packageQuantity),
    source: FOOD_SOURCE
  });
}

function chunkDocuments(documents: MeiliFoodDocument[], size: number): MeiliFoodDocument[][] {
  const chunks: MeiliFoodDocument[][] = [];
  for (let index = 0; index < documents.length; index += size) {
    chunks.push(documents.slice(index, index + size));
  }
  return chunks;
}

function isTaskError(error: unknown, code: string): boolean {
  return error instanceof Error && "code" in error && error.code === code;
}

export function resolveConfiguredPath(value: string): string {
  return path.isAbsolute(value) ? value : path.resolve(process.cwd(), value);
}

export async function fileExists(filePath: string): Promise<boolean> {
  try {
    await access(filePath);
    return true;
  } catch {
    return false;
  }
}

export function parseSeedPayload(payload: unknown): unknown[] {
  if (!Array.isArray(payload)) {
    throw new Error("Seed payload must be a JSON array.");
  }

  return payload;
}

export function transformSeedRecords(rawRecords: unknown[]) {
  const documents = rawRecords
    .map((entry) => transformRecord(entry))
    .filter((entry): entry is MeiliFoodDocument => entry !== null);

  return {
    documents,
    stats: {
      total: rawRecords.length,
      accepted: documents.length,
      rejected: rawRecords.length - documents.length
    }
  };
}

export async function loadSeedRecordsFromFile(filePath: string): Promise<unknown[]> {
  const rawJson = await readFile(filePath, "utf8");
  return parseSeedPayload(JSON.parse(rawJson));
}

export async function saveSeedRecordsToFile(filePath: string, rawRecords: unknown[]) {
  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, `${JSON.stringify(rawRecords)}\n`, "utf8");
}

export async function waitForMeili(config: AppConfig, timeoutMs = 60_000, pollMs = 2_000) {
  const client = createMeiliClient(config);
  const startedAt = Date.now();

  while (Date.now() - startedAt < timeoutMs) {
    try {
      await client.health();
      return;
    } catch {
      await new Promise((resolve) => setTimeout(resolve, pollMs));
    }
  }

  throw new Error(`Meilisearch did not become ready within ${timeoutMs}ms.`);
}

export async function getSeedSourcePath(config: AppConfig): Promise<string | null> {
  const candidates = [
    resolveConfiguredPath(config.MEILI_SEED_FILE),
    resolveConfiguredPath(config.MEILI_SEED_FALLBACK_FILE)
  ];

  for (const candidate of candidates) {
    if (await fileExists(candidate)) {
      return candidate;
    }
  }

  return null;
}

export async function importSeedRecords(
  config: AppConfig,
  rawRecords: unknown[],
  options: { resetIndex?: boolean } = {}
) {
  const { documents, stats } = transformSeedRecords(rawRecords);
  if (documents.length === 0) {
    throw new Error("Seed payload contains no valid food documents.");
  }

  const client = createMeiliClient(config);
  const index = client.index(config.MEILI_INDEX_NAME);

  try {
    await client.createIndex(config.MEILI_INDEX_NAME, { primaryKey: "id" });
  } catch (error) {
    if (!isTaskError(error, "index_already_exists")) {
      throw error;
    }
  }

  const settingsTask = await index.updateSettings({
    searchableAttributes: SEARCHABLE_ATTRIBUTES,
    filterableAttributes: FILTERABLE_ATTRIBUTES
  });
  await client.tasks.waitForTask(settingsTask.taskUid);

  if (options.resetIndex) {
    const deleteTask = await index.deleteAllDocuments();
    await client.tasks.waitForTask(deleteTask.taskUid);
  }

  for (const batch of chunkDocuments(documents, DEFAULT_BATCH_SIZE)) {
    const task = await index.addDocuments(batch, { primaryKey: "id" });
    await client.tasks.waitForTask(task.taskUid);
  }

  return {
    imported: documents.length,
    ...stats
  };
}

export async function isIndexEmpty(config: AppConfig): Promise<boolean> {
  const client = createMeiliClient(config);
  const index = client.index(config.MEILI_INDEX_NAME);

  try {
    const stats = await index.getStats();
    return stats.numberOfDocuments === 0;
  } catch (error) {
    if (isTaskError(error, "index_not_found")) {
      return true;
    }
    throw error;
  }
}
