import {
  FOOD_SOURCE,
  importInputSchema,
  meiliFoodDocumentSchema,
  type ImportInput,
  type MeiliFoodDocument
} from "@nozio/food-contracts";

export type ImportStats = {
  total: number;
  accepted: number;
  rejected: number;
};

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

export function transformRecord(raw: unknown): MeiliFoodDocument | null {
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
  const document = {
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
    sugarPer100g: normalizeNumber(value.sugarPer100g),
    servingSize: normalizeNullableText(value.servingSize),
    servingQuantity: normalizeNullableNumber(value.servingQuantity),
    packageSize: normalizeNullableText(value.packageSize),
    packageQuantity: normalizeNullableNumber(value.packageQuantity),
    source: FOOD_SOURCE
  };

  return meiliFoodDocumentSchema.parse(document);
}

export function transformRecords(rawRecords: unknown[]): { documents: MeiliFoodDocument[]; stats: ImportStats } {
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

export function chunkDocuments(documents: MeiliFoodDocument[], size: number): MeiliFoodDocument[][] {
  const chunks: MeiliFoodDocument[][] = [];
  for (let index = 0; index < documents.length; index += size) {
    chunks.push(documents.slice(index, index + size));
  }
  return chunks;
}
