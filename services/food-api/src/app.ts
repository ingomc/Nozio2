import Fastify from "fastify";
import type { FastifyInstance } from "fastify";
import {
  createCustomFoodRequestSchema,
  createCustomFoodResponseSchema,
  foodBarcodeResponseSchema,
  foodItemSchema,
  foodSearchResponseSchema,
  visionNutritionParseRequestSchema,
  visionNutritionParseResponseSchema
} from "@nozio/food-contracts";
import { z } from "zod";

import type { AppConfig } from "./config.js";
import { sendApiError } from "./errors.js";
import { createMeiliClient } from "./meili.js";
import {
  importSeedRecords,
  parseSeedPayload,
  persistCustomFood,
  saveSeedRecordsToFile,
  waitForMeili
} from "./seed.js";
import {
  parseNutritionWithGemini,
  VisionParseError,
  VisionUnavailableError
} from "./vision.js";

const searchQuerySchema = z.object({
  q: z.string().trim().min(2),
  limit: z.coerce.number().int().min(1).max(50).default(20)
});

const barcodeParamsSchema = z.object({
  barcode: z.string().regex(/^\d+$/)
});

const seedUploadQuerySchema = z.object({
  importNow: z.coerce.boolean().default(true),
  resetIndex: z.coerce.boolean().default(false)
});

type VisionParser = typeof parseNutritionWithGemini;

function isMeiliUnavailable(error: unknown): boolean {
  if (!(error instanceof Error)) {
    return false;
  }

  const message = error.message.toLowerCase();
  return message.includes("fetch failed") || message.includes("connect") || message.includes("network");
}

export function buildApp(
  config: AppConfig,
  dependencies: {
    parseNutrition?: VisionParser;
  } = {}
): FastifyInstance {
  const parseNutrition = dependencies.parseNutrition ?? parseNutritionWithGemini;
  const app = Fastify({
    logger: true,
    bodyLimit: config.MAX_SEED_UPLOAD_MB * 1024 * 1024
  });
  const meili = createMeiliClient(config);
  const index = meili.index(config.MEILI_INDEX_NAME);
  const findFoodByBarcode = async (barcode: string) => {
    const result = await index.search("", {
      filter: [`barcode = "${barcode}"`],
      limit: 1
    });
    const hit = result.hits[0];
    return hit ? foodItemSchema.parse(hit) : null;
  };

  app.addHook("onRequest", async (request, reply) => {
    if (request.url.startsWith("/v1/")) {
      const apiKey = request.headers["x-api-key"];
      if (apiKey !== config.FOOD_API_KEY) {
        return sendApiError(reply, 401, "UNAUTHORIZED", "Missing or invalid API key.");
      }
      return;
    }

    if (request.url.startsWith("/admin/")) {
      if (!config.ADMIN_API_TOKEN) {
        return sendApiError(reply, 503, "ADMIN_DISABLED", "Admin upload endpoint is disabled.");
      }

      const adminToken = request.headers["x-admin-token"];
      if (adminToken !== config.ADMIN_API_TOKEN) {
        return sendApiError(reply, 401, "UNAUTHORIZED", "Missing or invalid admin token.");
      }
    }
  });

  app.get("/health", async () => ({ status: "ok" }));

  app.post("/admin/seed-upload", async (request, reply) => {
    const parsedQuery = seedUploadQuerySchema.safeParse(request.query);
    if (!parsedQuery.success) {
      return sendApiError(reply, 400, "INVALID_QUERY", "Invalid admin seed upload options.");
    }

    try {
      const rawRecords = parseSeedPayload(request.body);
      await saveSeedRecordsToFile(config.MEILI_SEED_FILE, rawRecords);

      let importSummary = null;
      if (parsedQuery.data.importNow) {
        await waitForMeili(config);
        importSummary = await importSeedRecords(config, rawRecords, {
          resetIndex: parsedQuery.data.resetIndex
        });
      }

      return reply.send({
        storedAt: config.MEILI_SEED_FILE,
        importNow: parsedQuery.data.importNow,
        resetIndex: parsedQuery.data.resetIndex,
        importSummary
      });
    } catch (error) {
      if (error instanceof SyntaxError) {
        return sendApiError(reply, 400, "INVALID_JSON", "Request body must be valid JSON.");
      }
      if (error instanceof Error && error.message.includes("Seed payload must be a JSON array")) {
        return sendApiError(reply, 400, "INVALID_SEED", error.message);
      }
      if (error instanceof Error && error.message.includes("contains no valid food documents")) {
        return sendApiError(reply, 400, "INVALID_SEED", error.message);
      }
      if (isMeiliUnavailable(error)) {
        return sendApiError(reply, 502, "MEILI_UNAVAILABLE", "Search backend is unavailable.");
      }
      request.log.error({ err: error }, "seed upload failed");
      return sendApiError(reply, 500, "INTERNAL_ERROR", "Unexpected server error.");
    }
  });

  app.get("/admin/foods/barcode/:barcode", async (request, reply) => {
    const parsedParams = barcodeParamsSchema.safeParse(request.params);
    if (!parsedParams.success) {
      return sendApiError(reply, 400, "INVALID_BARCODE", "Barcode must contain only digits.");
    }

    try {
      const item = await findFoodByBarcode(parsedParams.data.barcode);
      if (!item) {
        return sendApiError(reply, 404, "NOT_FOUND", "No product found for barcode.");
      }

      return foodBarcodeResponseSchema.parse({ item });
    } catch (error) {
      if (isMeiliUnavailable(error)) {
        return sendApiError(reply, 502, "MEILI_UNAVAILABLE", "Search backend is unavailable.");
      }
      request.log.error({ err: error }, "admin barcode lookup failed");
      return sendApiError(reply, 500, "INTERNAL_ERROR", "Unexpected server error.");
    }
  });

  app.delete("/admin/foods/barcode/:barcode", async (request, reply) => {
    const parsedParams = barcodeParamsSchema.safeParse(request.params);
    if (!parsedParams.success) {
      return sendApiError(reply, 400, "INVALID_BARCODE", "Barcode must contain only digits.");
    }

    try {
      const item = await findFoodByBarcode(parsedParams.data.barcode);
      if (!item) {
        return sendApiError(reply, 404, "NOT_FOUND", "No product found for barcode.");
      }

      await index.deleteDocument(item.id);

      return reply.send({
        deleted: true,
        item
      });
    } catch (error) {
      if (isMeiliUnavailable(error)) {
        return sendApiError(reply, 502, "MEILI_UNAVAILABLE", "Search backend is unavailable.");
      }
      request.log.error({ err: error }, "admin barcode delete failed");
      return sendApiError(reply, 500, "INTERNAL_ERROR", "Unexpected server error.");
    }
  });

  app.get("/v1/foods/search", async (request, reply) => {
    const parsedQuery = searchQuerySchema.safeParse(request.query);
    if (!parsedQuery.success) {
      return sendApiError(reply, 400, "INVALID_QUERY", "Query must be at least 2 characters.");
    }

    try {
      const result = await index.search(parsedQuery.data.q, {
        limit: parsedQuery.data.limit
      });

      const payload = {
        items: result.hits.map((hit) => foodItemSchema.parse(hit)),
        totalEstimated: result.estimatedTotalHits ?? result.hits.length
      };

      return foodSearchResponseSchema.parse(payload);
    } catch (error) {
      if (isMeiliUnavailable(error)) {
        return sendApiError(reply, 502, "MEILI_UNAVAILABLE", "Search backend is unavailable.");
      }
      request.log.error({ err: error }, "search failed");
      return sendApiError(reply, 500, "INTERNAL_ERROR", "Unexpected server error.");
    }
  });

  app.get("/v1/foods/barcode/:barcode", async (request, reply) => {
    const parsedParams = barcodeParamsSchema.safeParse(request.params);
    if (!parsedParams.success) {
      return sendApiError(reply, 400, "INVALID_BARCODE", "Barcode must contain only digits.");
    }

    try {
      const item = await findFoodByBarcode(parsedParams.data.barcode);
      if (!item) {
        return sendApiError(reply, 404, "NOT_FOUND", "No product found for barcode.");
      }

      return foodBarcodeResponseSchema.parse({ item });
    } catch (error) {
      if (isMeiliUnavailable(error)) {
        return sendApiError(reply, 502, "MEILI_UNAVAILABLE", "Search backend is unavailable.");
      }
      request.log.error({ err: error }, "barcode lookup failed");
      return sendApiError(reply, 500, "INTERNAL_ERROR", "Unexpected server error.");
    }
  });

  app.post("/v1/foods/custom", async (request, reply) => {
    const parsedBody = createCustomFoodRequestSchema.safeParse(request.body);
    if (!parsedBody.success) {
      return sendApiError(reply, 400, "INVALID_BODY", "Custom food payload is invalid.");
    }

    try {
      const document = await persistCustomFood(config, parsedBody.data);
      return createCustomFoodResponseSchema.parse({
        item: foodItemSchema.parse(document)
      });
    } catch (error) {
      if (isMeiliUnavailable(error)) {
        return sendApiError(reply, 502, "MEILI_UNAVAILABLE", "Search backend is unavailable.");
      }
      request.log.error({ err: error }, "custom food create failed");
      return sendApiError(reply, 500, "INTERNAL_ERROR", "Unexpected server error.");
    }
  });

  app.post("/v1/vision/nutrition/parse", async (request, reply) => {
    const parsedBody = visionNutritionParseRequestSchema.safeParse(request.body);
    if (!parsedBody.success) {
      return sendApiError(reply, 400, "INVALID_BODY", "Vision payload is invalid.");
    }

    const stripped = stripDataUrlPrefix(parsedBody.data.imageBase64);
    const imageBuffer = decodeBase64Image(stripped);
    if (!imageBuffer) {
      return sendApiError(reply, 400, "INVALID_BODY", "Image payload must be valid base64.");
    }

    const maxBytes = Math.floor(config.VISION_MAX_IMAGE_MB * 1024 * 1024);
    if (imageBuffer.length > maxBytes) {
      return sendApiError(reply, 413, "IMAGE_TOO_LARGE", "Image exceeds configured size limit.");
    }

    const mimeType = detectImageMimeType(imageBuffer);
    if (!mimeType) {
      return sendApiError(reply, 400, "INVALID_BODY", "Only JPEG and PNG images are supported.");
    }

    try {
      const parsed = await parseNutrition(config, {
        imageBase64: stripped,
        mimeType,
        locale: parsedBody.data.locale
      });
      return visionNutritionParseResponseSchema.parse(parsed);
    } catch (error) {
      if (error instanceof VisionParseError) {
        return sendApiError(reply, 422, "VISION_PARSE_FAILED", "Vision response could not be parsed.");
      }
      if (error instanceof VisionUnavailableError) {
        return sendApiError(reply, 502, "VISION_UNAVAILABLE", "Vision backend is unavailable.");
      }
      request.log.error({ err: error }, "vision parse failed");
      return sendApiError(reply, 500, "INTERNAL_ERROR", "Unexpected server error.");
    }
  });

  return app;
}

function stripDataUrlPrefix(value: string): string {
  const marker = ";base64,";
  const markerIndex = value.indexOf(marker);
  if (markerIndex < 0) return value.trim();
  return value.slice(markerIndex + marker.length).trim();
}

function decodeBase64Image(value: string): Buffer | null {
  try {
    const normalized = value.replace(/\s+/g, "");
    if (!normalized) return null;
    const decoded = Buffer.from(normalized, "base64");
    if (decoded.length === 0) return null;
    return decoded;
  } catch {
    return null;
  }
}

function detectImageMimeType(buffer: Buffer): string | null {
  if (buffer.length >= 3 && buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff) {
    return "image/jpeg";
  }
  if (
    buffer.length >= 8 &&
    buffer[0] === 0x89 &&
    buffer[1] === 0x50 &&
    buffer[2] === 0x4e &&
    buffer[3] === 0x47 &&
    buffer[4] === 0x0d &&
    buffer[5] === 0x0a &&
    buffer[6] === 0x1a &&
    buffer[7] === 0x0a
  ) {
    return "image/png";
  }
  return null;
}
