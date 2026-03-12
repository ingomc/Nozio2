import test from "node:test";
import assert from "node:assert/strict";

import { buildApp } from "../src/app.js";
import { VisionParseError } from "../src/vision.js";

const baseConfig = {
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
  GEMINI_MODEL: "gemini-2.0-flash",
  GEMINI_FALLBACK_MODELS: [],
  VISION_TIMEOUT_MS: 12000,
  VISION_MAX_IMAGE_MB: 4
};

test("health endpoint responds ok", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({ method: "GET", url: "/health" });

  assert.equal(response.statusCode, 200);
  assert.deepEqual(response.json(), { status: "ok" });
});

test("protected endpoints require api key", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({ method: "GET", url: "/v1/foods/search?q=hafer" });

  assert.equal(response.statusCode, 401);
  assert.equal(response.json().error.code, "UNAUTHORIZED");
});

test("short query returns validation error before meili call", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({
    method: "GET",
    url: "/v1/foods/search?q=a",
    headers: { "x-api-key": "secret" }
  });

  assert.equal(response.statusCode, 400);
  assert.equal(response.json().error.code, "INVALID_QUERY");
});

test("admin seed upload requires admin token", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({
    method: "POST",
    url: "/admin/seed-upload",
    payload: []
  });

  assert.equal(response.statusCode, 401);
  assert.equal(response.json().error.code, "UNAUTHORIZED");
});

test("admin barcode preview requires admin token", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({
    method: "GET",
    url: "/admin/foods/barcode/8721082520362"
  });

  assert.equal(response.statusCode, 401);
  assert.equal(response.json().error.code, "UNAUTHORIZED");
});

test("admin barcode delete requires admin token", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({
    method: "DELETE",
    url: "/admin/foods/barcode/8721082520362"
  });

  assert.equal(response.statusCode, 401);
  assert.equal(response.json().error.code, "UNAUTHORIZED");
});

test("admin seed upload validates json array payload", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({
    method: "POST",
    url: "/admin/seed-upload?importNow=false",
    headers: {
      "x-admin-token": "admin-secret"
    },
    payload: { invalid: true }
  });

  assert.equal(response.statusCode, 400);
  assert.equal(response.json().error.code, "INVALID_SEED");
});

test("custom food endpoint validates payload before meili call", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({
    method: "POST",
    url: "/v1/foods/custom",
    headers: { "x-api-key": "secret" },
    payload: {
      name: "",
      caloriesPer100g: -1
    }
  });

  assert.equal(response.statusCode, 400);
  assert.equal(response.json().error.code, "INVALID_BODY");
});

test("admin barcode endpoints validate barcode before meili call", async () => {
  const app = buildApp(baseConfig);
  const headers = { "x-admin-token": "admin-secret" };

  const previewResponse = await app.inject({
    method: "GET",
    url: "/admin/foods/barcode/not-a-barcode",
    headers
  });
  assert.equal(previewResponse.statusCode, 400);
  assert.equal(previewResponse.json().error.code, "INVALID_BARCODE");

  const deleteResponse = await app.inject({
    method: "DELETE",
    url: "/admin/foods/barcode/not-a-barcode",
    headers
  });
  assert.equal(deleteResponse.statusCode, 400);
  assert.equal(deleteResponse.json().error.code, "INVALID_BARCODE");
});

test("vision endpoint requires api key", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({
    method: "POST",
    url: "/v1/vision/nutrition/parse",
    payload: { imageBase64: "abc" }
  });

  assert.equal(response.statusCode, 401);
  assert.equal(response.json().error.code, "UNAUTHORIZED");
});

test("vision endpoint validates payload and mime", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({
    method: "POST",
    url: "/v1/vision/nutrition/parse",
    headers: { "x-api-key": "secret" },
    payload: { imageBase64: "bm90LWFuLWltYWdl" }
  });

  assert.equal(response.statusCode, 400);
  assert.equal(response.json().error.code, "INVALID_BODY");
});

test("vision endpoint rejects too large image", async () => {
  const app = buildApp(
    {
      ...baseConfig,
      VISION_MAX_IMAGE_MB: 0.00001
    },
    {
      parseNutrition: async () => {
        throw new Error("should not be called");
      }
    }
  );
  const jpegHeader = Buffer.from([0xff, 0xd8, 0xff, 0xdb]);
  const body = Buffer.concat([jpegHeader, Buffer.alloc(800)]).toString("base64");
  const response = await app.inject({
    method: "POST",
    url: "/v1/vision/nutrition/parse",
    headers: { "x-api-key": "secret" },
    payload: { imageBase64: body }
  });

  assert.equal(response.statusCode, 413);
  assert.equal(response.json().error.code, "IMAGE_TOO_LARGE");
});

test("vision endpoint returns parsed payload from parser dependency", async () => {
  const app = buildApp(baseConfig, {
    parseNutrition: async () => ({
      name: "Protein Bar",
      brand: "Nozio",
      caloriesPer100g: 420,
      proteinPer100g: 24,
      carbsPer100g: 38,
      fatPer100g: 16,
      sugarPer100g: 6,
      confidence: 0.88,
      model: "gemini-2.0-flash",
      warnings: []
    })
  });
  const jpegHeader = Buffer.from([0xff, 0xd8, 0xff, 0xdb]).toString("base64");
  const response = await app.inject({
    method: "POST",
    url: "/v1/vision/nutrition/parse",
    headers: { "x-api-key": "secret" },
    payload: { imageBase64: jpegHeader, locale: "de" }
  });

  assert.equal(response.statusCode, 200);
  assert.equal(response.json().name, "Protein Bar");
  assert.equal(response.json().confidence, 0.88);
});

test("vision endpoint maps parser format errors to VISION_PARSE_FAILED", async () => {
  const app = buildApp(baseConfig, {
    parseNutrition: async () => {
      throw new VisionParseError("bad payload");
    }
  });
  const jpegHeader = Buffer.from([0xff, 0xd8, 0xff, 0xdb]).toString("base64");
  const response = await app.inject({
    method: "POST",
    url: "/v1/vision/nutrition/parse",
    headers: { "x-api-key": "secret" },
    payload: { imageBase64: jpegHeader }
  });

  assert.equal(response.statusCode, 422);
  assert.equal(response.json().error.code, "VISION_PARSE_FAILED");
});

test("food analyze endpoint requires api key", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({
    method: "POST",
    url: "/v1/vision/food/analyze",
    payload: { imageBase64: "abc" }
  });

  assert.equal(response.statusCode, 401);
  assert.equal(response.json().error.code, "UNAUTHORIZED");
});

test("food analyze endpoint validates payload and mime", async () => {
  const app = buildApp(baseConfig);
  const response = await app.inject({
    method: "POST",
    url: "/v1/vision/food/analyze",
    headers: { "x-api-key": "secret" },
    payload: { imageBase64: "bm90LWFuLWltYWdl" }
  });

  assert.equal(response.statusCode, 400);
  assert.equal(response.json().error.code, "INVALID_BODY");
});

test("food analyze endpoint returns parsed payload from analyzer dependency", async () => {
  const app = buildApp(baseConfig, {
    analyzeFood: async () => ({
      name: "Haehnchenbrust mit Reis",
      caloriesPer100g: 150,
      proteinPer100g: 22,
      carbsPer100g: 15,
      fatPer100g: 3,
      sugarPer100g: 0,
      servingSize: "1 Teller",
      servingQuantity: 350,
      caloriesPerServing: 525,
      proteinPerServing: 77,
      carbsPerServing: 52,
      fatPerServing: 10,
      confidence: 0.72,
      model: "gemini-2.0-flash",
      warnings: ["Geschaetzte Werte basierend auf Bilderkennung"]
    })
  });
  const jpegHeader = Buffer.from([0xff, 0xd8, 0xff, 0xdb]).toString("base64");
  const response = await app.inject({
    method: "POST",
    url: "/v1/vision/food/analyze",
    headers: { "x-api-key": "secret" },
    payload: { imageBase64: jpegHeader, locale: "de", portionSize: "medium", hints: ["Haehnchen"] }
  });

  assert.equal(response.statusCode, 200);
  assert.equal(response.json().name, "Haehnchenbrust mit Reis");
  assert.equal(response.json().confidence, 0.72);
  assert.equal(response.json().servingSize, "1 Teller");
  assert.equal(response.json().warnings.length, 1);
});

test("food analyze endpoint maps parse errors to VISION_PARSE_FAILED", async () => {
  const app = buildApp(baseConfig, {
    analyzeFood: async () => {
      throw new VisionParseError("bad payload");
    }
  });
  const jpegHeader = Buffer.from([0xff, 0xd8, 0xff, 0xdb]).toString("base64");
  const response = await app.inject({
    method: "POST",
    url: "/v1/vision/food/analyze",
    headers: { "x-api-key": "secret" },
    payload: { imageBase64: jpegHeader }
  });

  assert.equal(response.statusCode, 422);
  assert.equal(response.json().error.code, "VISION_PARSE_FAILED");
});
