import test from "node:test";
import assert from "node:assert/strict";

import type { AppConfig } from "../src/config.js";
import { parseNutritionWithGemini, VisionUnavailableError } from "../src/vision.js";

const baseConfig: AppConfig = {
  PORT: 3000,
  HOST: "127.0.0.1",
  MEILI_URL: "http://127.0.0.1:7700",
  MEILI_MASTER_KEY: "change-me",
  MEILI_INDEX_NAME: "foods",
  FOOD_API_KEY: "secret",
  ADMIN_API_TOKEN: "admin-secret",
  MAX_SEED_UPLOAD_MB: 100,
  MEILI_AUTO_SEED: true,
  MEILI_SEED_FILE: "/tmp/nozio-test-seed.json",
  MEILI_SEED_FALLBACK_FILE: "data/seed/foods.de.sample.json",
  GEMINI_API_KEY: "gemini-test-key",
  GEMINI_MODEL: "gemini-2.5-flash",
  GEMINI_FALLBACK_MODELS: ["gemini-2.0-flash"],
  VISION_TIMEOUT_MS: 12000,
  VISION_MAX_IMAGE_MB: 4
};

test("falls back to next model when primary model is rate-limited", async () => {
  const originalFetch = globalThis.fetch;
  const calledModels: string[] = [];

  globalThis.fetch = (async (input) => {
    const url = String(input);
    const modelMatch = url.match(/\/models\/([^:]+):generateContent/);
    const model = decodeURIComponent(modelMatch?.[1] ?? "");
    calledModels.push(model);

    if (model === "gemini-2.5-flash") {
      return new Response(
        JSON.stringify({
          error: {
            code: 429,
            status: "RESOURCE_EXHAUSTED",
            message: "Quota exceeded"
          }
        }),
        { status: 429, headers: { "content-type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        candidates: [
          {
            content: {
              parts: [
                {
                  text: JSON.stringify({
                    name: "Test",
                    confidence: 0.92,
                    model: "ignored-by-backend",
                    warnings: []
                  })
                }
              ]
            }
          }
        ]
      }),
      { status: 200, headers: { "content-type": "application/json" } }
    );
  }) as typeof fetch;

  try {
    const result = await parseNutritionWithGemini(baseConfig, {
      imageBase64: "dGVzdA==",
      mimeType: "image/png",
      locale: "de"
    });

    assert.equal(result.model, "gemini-2.0-flash");
    assert.deepEqual(calledModels, ["gemini-2.5-flash", "gemini-2.0-flash"]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("does not fall back for non-rate-limit errors", async () => {
  const originalFetch = globalThis.fetch;
  const calledModels: string[] = [];

  globalThis.fetch = (async (input) => {
    const url = String(input);
    const modelMatch = url.match(/\/models\/([^:]+):generateContent/);
    const model = decodeURIComponent(modelMatch?.[1] ?? "");
    calledModels.push(model);

    return new Response(
      JSON.stringify({
        error: {
          code: 400,
          status: "INVALID_ARGUMENT",
          message: "Bad request"
        }
      }),
      { status: 400, headers: { "content-type": "application/json" } }
    );
  }) as typeof fetch;

  try {
    await assert.rejects(
      () =>
        parseNutritionWithGemini(baseConfig, {
          imageBase64: "dGVzdA==",
          mimeType: "image/png",
          locale: "de"
        }),
      VisionUnavailableError
    );
    assert.deepEqual(calledModels, ["gemini-2.5-flash"]);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
