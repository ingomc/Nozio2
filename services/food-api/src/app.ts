import Fastify from "fastify";
import type { FastifyInstance } from "fastify";
import {
  foodBarcodeResponseSchema,
  foodItemSchema,
  foodSearchResponseSchema
} from "@nozio/food-contracts";
import { z } from "zod";

import type { AppConfig } from "./config.js";
import { sendApiError } from "./errors.js";
import { createMeiliClient } from "./meili.js";

const searchQuerySchema = z.object({
  q: z.string().trim().min(2),
  limit: z.coerce.number().int().min(1).max(50).default(20)
});

const barcodeParamsSchema = z.object({
  barcode: z.string().regex(/^\d+$/)
});

function isMeiliUnavailable(error: unknown): boolean {
  if (!(error instanceof Error)) {
    return false;
  }

  const message = error.message.toLowerCase();
  return message.includes("fetch failed") || message.includes("connect") || message.includes("network");
}

export function buildApp(config: AppConfig): FastifyInstance {
  const app = Fastify({ logger: true });
  const meili = createMeiliClient(config);
  const index = meili.index(config.MEILI_INDEX_NAME);

  app.addHook("onRequest", async (request, reply) => {
    if (!request.url.startsWith("/v1/")) {
      return;
    }

    const apiKey = request.headers["x-api-key"];
    if (apiKey !== config.FOOD_API_KEY) {
      return sendApiError(reply, 401, "UNAUTHORIZED", "Missing or invalid API key.");
    }
  });

  app.get("/health", async () => ({ status: "ok" }));

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
      const result = await index.search("", {
        filter: [`barcode = "${parsedParams.data.barcode}"`],
        limit: 1
      });
      const hit = result.hits[0];
      if (!hit) {
        return sendApiError(reply, 404, "NOT_FOUND", "No product found for barcode.");
      }

      return foodBarcodeResponseSchema.parse({
        item: foodItemSchema.parse(hit)
      });
    } catch (error) {
      if (isMeiliUnavailable(error)) {
        return sendApiError(reply, 502, "MEILI_UNAVAILABLE", "Search backend is unavailable.");
      }
      request.log.error({ err: error }, "barcode lookup failed");
      return sendApiError(reply, 500, "INTERNAL_ERROR", "Unexpected server error.");
    }
  });

  return app;
}
