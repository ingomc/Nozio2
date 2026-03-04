import test from "node:test";
import assert from "node:assert/strict";

import { buildApp } from "../src/app.js";

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
  MEILI_SEED_FALLBACK_FILE: "data/seed/foods.de.sample.json"
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
