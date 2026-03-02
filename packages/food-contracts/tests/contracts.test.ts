import test from "node:test";
import assert from "node:assert/strict";

import { foodItemSchema, foodSearchResponseSchema } from "../src/index.ts";

test("food item schema accepts valid payload", () => {
  const parsed = foodItemSchema.parse({
    id: "off-123",
    name: "Haferflocken",
    brand: "Ja",
    displayName: "Haferflocken (Ja)",
    barcode: "1234567890123",
    caloriesPer100g: 370,
    proteinPer100g: 13.5,
    fatPer100g: 7,
    carbsPer100g: 58,
    servingSize: "50 g",
    servingQuantity: 50,
    packageSize: "500 g",
    packageQuantity: 500,
    source: "SELF_HOSTED_OFF"
  });

  assert.equal(parsed.id, "off-123");
  assert.equal(parsed.packageQuantity, 500);
});

test("search response schema requires totalEstimated", () => {
  assert.throws(
    () => foodSearchResponseSchema.parse({ items: [] }),
    /totalEstimated/
  );
});
