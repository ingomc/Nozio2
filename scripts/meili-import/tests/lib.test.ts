import test from "node:test";
import assert from "node:assert/strict";

import { chunkDocuments, transformRecord, transformRecords } from "../src/lib.ts";

test("transformRecord rejects entries without name", () => {
  assert.equal(
    transformRecord({
      id: "1",
      caloriesPer100g: 100
    }),
    null
  );
});

test("transformRecord normalizes barcode and default macros", () => {
  const result = transformRecord({
    id: "1",
    name: "Haferflocken",
    brand: "Ja",
    barcode: "12-34 56",
    caloriesPer100g: 370,
    packageSize: "500 g",
    packageQuantity: 500
  });

  assert.ok(result);
  assert.equal(result.barcode, "123456");
  assert.equal(result.displayName, "Haferflocken (Ja)");
  assert.equal(result.proteinPer100g, 0);
  assert.equal(result.packageSize, "500 g");
});

test("transformRecords tracks rejects", () => {
  const result = transformRecords([
    { id: "1", name: "A", caloriesPer100g: 1 },
    { id: "2", caloriesPer100g: 2 }
  ]);

  assert.equal(result.stats.total, 2);
  assert.equal(result.stats.accepted, 1);
  assert.equal(result.stats.rejected, 1);
});

test("chunkDocuments splits arrays into batches", () => {
  const chunks = chunkDocuments(
    [
      { id: "1" },
      { id: "2" },
      { id: "3" }
    ] as never[],
    2
  );

  assert.equal(chunks.length, 2);
  assert.equal(chunks[0].length, 2);
  assert.equal(chunks[1].length, 1);
});
