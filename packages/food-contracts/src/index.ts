import { z } from "zod";

export const FOOD_SOURCE_VALUES = ["SELF_HOSTED_OFF", "CUSTOM"] as const;
export const FOOD_SOURCE = "SELF_HOSTED_OFF" as const;
export const CUSTOM_FOOD_SOURCE = "CUSTOM" as const;
export const foodSourceSchema = z.enum(FOOD_SOURCE_VALUES);

const numberField = z.number().finite();

export const foodItemSchema = z.object({
  id: z.string().min(1),
  name: z.string().min(1),
  brand: z.string().trim().nullable().optional(),
  displayName: z.string().min(1),
  imageUrl: z.string().trim().nullable().optional(),
  barcode: z.string().regex(/^\d+$/).nullable().optional(),
  caloriesPer100g: numberField,
  proteinPer100g: numberField,
  fatPer100g: numberField,
  carbsPer100g: numberField,
  sugarPer100g: numberField.nonnegative().optional().default(0),
  servingSize: z.string().trim().nullable().optional(),
  servingQuantity: numberField.nullable().optional(),
  packageSize: z.string().trim().nullable().optional(),
  packageQuantity: numberField.nullable().optional(),
  source: foodSourceSchema
});

export const foodSearchResponseSchema = z.object({
  items: z.array(foodItemSchema),
  totalEstimated: z.number().int().nonnegative()
});

export const foodBarcodeResponseSchema = z.object({
  item: foodItemSchema
});

export const createCustomFoodRequestSchema = z.object({
  name: z.string().trim().min(1),
  brand: z.string().trim().nullable().optional(),
  barcode: z
    .string()
    .trim()
    .regex(/^\d+$/)
    .nullable()
    .optional(),
  caloriesPer100g: numberField.nonnegative(),
  proteinPer100g: numberField.nonnegative().default(0),
  fatPer100g: numberField.nonnegative().default(0),
  carbsPer100g: numberField.nonnegative().default(0),
  sugarPer100g: numberField.nonnegative(),
  servingSize: z.string().trim().nullable().optional(),
  servingQuantity: numberField.nonnegative().nullable().optional(),
  packageSize: z.string().trim().nullable().optional(),
  packageQuantity: numberField.nonnegative().nullable().optional()
});

export const createCustomFoodResponseSchema = z.object({
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
  imageUrl: z.string().nullable().optional(),
  barcode: z.union([z.string(), z.number()]).nullable().optional(),
  caloriesPer100g: z.number().optional(),
  proteinPer100g: z.number().optional(),
  fatPer100g: z.number().optional(),
  carbsPer100g: z.number().optional(),
  sugarPer100g: z.number().optional(),
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
export type CreateCustomFoodRequest = z.infer<typeof createCustomFoodRequestSchema>;
export type CreateCustomFoodResponse = z.infer<typeof createCustomFoodResponseSchema>;
export type ApiError = z.infer<typeof apiErrorSchema>;
export type ImportInput = z.infer<typeof importInputSchema>;
export type MeiliFoodDocument = z.infer<typeof meiliFoodDocumentSchema>;
