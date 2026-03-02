import { z } from "zod";

export const FOOD_SOURCE = "SELF_HOSTED_OFF" as const;

const numberField = z.number().finite();

export const foodItemSchema = z.object({
  id: z.string().min(1),
  name: z.string().min(1),
  brand: z.string().trim().nullable().optional(),
  displayName: z.string().min(1),
  barcode: z.string().regex(/^\d+$/).nullable().optional(),
  caloriesPer100g: numberField,
  proteinPer100g: numberField,
  fatPer100g: numberField,
  carbsPer100g: numberField,
  servingSize: z.string().trim().nullable().optional(),
  servingQuantity: numberField.nullable().optional(),
  packageSize: z.string().trim().nullable().optional(),
  packageQuantity: numberField.nullable().optional(),
  source: z.literal(FOOD_SOURCE)
});

export const foodSearchResponseSchema = z.object({
  items: z.array(foodItemSchema),
  totalEstimated: z.number().int().nonnegative()
});

export const foodBarcodeResponseSchema = z.object({
  item: foodItemSchema
});

export const apiErrorSchema = z.object({
  error: z.object({
    code: z.string().min(1),
    message: z.string().min(1)
  })
});

export const importInputSchema = z.object({
  id: z.union([z.string(), z.number()]),
  name: z.string().optional(),
  brand: z.string().nullable().optional(),
  barcode: z.union([z.string(), z.number()]).nullable().optional(),
  caloriesPer100g: z.number().optional(),
  proteinPer100g: z.number().optional(),
  fatPer100g: z.number().optional(),
  carbsPer100g: z.number().optional(),
  servingSize: z.string().nullable().optional(),
  servingQuantity: z.number().nullable().optional(),
  packageSize: z.string().nullable().optional(),
  packageQuantity: z.number().nullable().optional()
});

export const meiliFoodDocumentSchema = foodItemSchema.extend({
  searchTerms: z.string().min(1)
});

export type FoodItem = z.infer<typeof foodItemSchema>;
export type FoodSearchResponse = z.infer<typeof foodSearchResponseSchema>;
export type FoodBarcodeResponse = z.infer<typeof foodBarcodeResponseSchema>;
export type ApiError = z.infer<typeof apiErrorSchema>;
export type ImportInput = z.infer<typeof importInputSchema>;
export type MeiliFoodDocument = z.infer<typeof meiliFoodDocumentSchema>;
