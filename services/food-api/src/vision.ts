import {
  visionNutritionParseResponseSchema,
  type VisionNutritionParseResponse
} from "@nozio/food-contracts";

import type { AppConfig } from "./config.js";

const geminiPayloadSchema = {
  type: "object",
  properties: {
    name: { type: "string", nullable: true },
    brand: { type: "string", nullable: true },
    caloriesPer100g: { type: "number", nullable: true },
    proteinPer100g: { type: "number", nullable: true },
    carbsPer100g: { type: "number", nullable: true },
    fatPer100g: { type: "number", nullable: true },
    sugarPer100g: { type: "number", nullable: true },
    servingSize: { type: "string", nullable: true },
    servingQuantity: { type: "number", nullable: true },
    caloriesPerServing: { type: "number", nullable: true },
    proteinPerServing: { type: "number", nullable: true },
    carbsPerServing: { type: "number", nullable: true },
    fatPerServing: { type: "number", nullable: true },
    sugarPerServing: { type: "number", nullable: true },
    confidence: { type: "number" },
    model: { type: "string" },
    warnings: {
      type: "array",
      items: { type: "string" }
    }
  },
  required: ["confidence", "model", "warnings"]
};

type GeminiGenerateResponse = {
  candidates?: Array<{
    content?: {
      parts?: Array<{
        text?: string;
      }>;
    };
  }>;
};

type GeminiErrorResponse = {
  error?: {
    code?: number;
    message?: string;
    status?: string;
  };
};

function extractJsonObject(text: string): string | null {
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start < 0 || end <= start) return null;
  return text.slice(start, end + 1);
}

function normalizeParsedResponse(
  parsed: VisionNutritionParseResponse,
  model: string
): VisionNutritionParseResponse {
  const warnings = [...(parsed.warnings ?? [])];
  let sugarPer100 = parsed.sugarPer100g ?? null;
  let sugarPerServing = parsed.sugarPerServing ?? null;

  if (
    sugarPer100 != null &&
    parsed.carbsPer100g != null &&
    sugarPer100 > parsed.carbsPer100g
  ) {
    sugarPer100 = null;
    warnings.push("Zuckerwert war höher als Kohlenhydrate und wurde verworfen.");
  }

  if (
    sugarPerServing != null &&
    parsed.carbsPerServing != null &&
    sugarPerServing > parsed.carbsPerServing
  ) {
    sugarPerServing = null;
    warnings.push("Portions-Zuckerwert war höher als Portions-Kohlenhydrate und wurde verworfen.");
  }

  if (
    parsed.proteinPer100g != null &&
    parsed.carbsPer100g != null &&
    parsed.fatPer100g != null
  ) {
    const sum = parsed.proteinPer100g + parsed.carbsPer100g + parsed.fatPer100g;
    if (sum > 100) {
      warnings.push("Makrosumme über 100g erkannt. Werte bitte prüfen.");
    }
  }

  return visionNutritionParseResponseSchema.parse({
    name: parsed.name ?? null,
    brand: parsed.brand ?? null,
    caloriesPer100g: parsed.caloriesPer100g ?? null,
    proteinPer100g: parsed.proteinPer100g ?? null,
    carbsPer100g: parsed.carbsPer100g ?? null,
    fatPer100g: parsed.fatPer100g ?? null,
    sugarPer100g: sugarPer100,
    servingSize: parsed.servingSize ?? null,
    servingQuantity: parsed.servingQuantity ?? null,
    caloriesPerServing: parsed.caloriesPerServing ?? null,
    proteinPerServing: parsed.proteinPerServing ?? null,
    carbsPerServing: parsed.carbsPerServing ?? null,
    fatPerServing: parsed.fatPerServing ?? null,
    sugarPerServing: sugarPerServing,
    confidence: parsed.confidence,
    model,
    warnings
  });
}

export class VisionUnavailableError extends Error {}
export class VisionParseError extends Error {}

export async function parseNutritionWithGemini(
  config: AppConfig,
  input: {
    imageBase64: string;
    mimeType: string;
    locale: string;
  }
): Promise<VisionNutritionParseResponse> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), config.VISION_TIMEOUT_MS);

  try {
    const prompt = [
      "Extrahiere Nährwerte aus dem Bild als JSON.",
      `Sprache/Locale: ${input.locale}`,
      "Regeln:",
      "- Primär Werte pro 100g oder 100ml extrahieren.",
      "- Wenn vorhanden, zusätzlich Portionswerte und Portionsmenge extrahieren.",
      "- servingQuantity nur als numerischen Wert (g/ml) zurückgeben.",
      "- Felder ohne klaren Wert als null zurückgeben.",
      "- Keine zusätzlichen Texte außerhalb von JSON.",
      "- confidence zwischen 0 und 1.",
      "- warnings als Liste von knappen Hinweisen."
    ].join("\n");

    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(config.GEMINI_MODEL)}:generateContent?key=${encodeURIComponent(config.GEMINI_API_KEY)}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          contents: [
            {
              parts: [
                { text: prompt },
                {
                  inlineData: {
                    mimeType: input.mimeType,
                    data: input.imageBase64
                  }
                }
              ]
            }
          ],
          generationConfig: {
            temperature: 0,
            responseMimeType: "application/json",
            responseSchema: geminiPayloadSchema
          }
        }),
        signal: controller.signal
      }
    );

    if (!response.ok) {
      let detail = "";
      try {
        const errorPayload = (await response.json()) as GeminiErrorResponse;
        const apiMessage = errorPayload.error?.message?.trim();
        if (apiMessage) {
          detail = apiMessage;
        }
      } catch {
        // Ignore parse failure and keep generic status text.
      }

      const statusPart = `HTTP ${response.status}`;
      const message = detail
        ? `Gemini request failed (${statusPart}): ${detail}`
        : `Gemini request failed (${statusPart}).`;
      throw new VisionUnavailableError(message);
    }

    const payload = (await response.json()) as GeminiGenerateResponse;
    const rawText = payload.candidates?.[0]?.content?.parts?.map((part) => part.text ?? "").join("\n")?.trim() ?? "";

    if (!rawText) {
      throw new VisionParseError("Gemini returned empty response.");
    }

    const jsonText = extractJsonObject(rawText);
    if (!jsonText) {
      throw new VisionParseError("Gemini response did not contain JSON object.");
    }

    let parsedUnknown: unknown;
    try {
      parsedUnknown = JSON.parse(jsonText);
    } catch {
      throw new VisionParseError("Gemini JSON could not be parsed.");
    }

    const parsed = visionNutritionParseResponseSchema.parse(parsedUnknown);
    return normalizeParsedResponse(parsed, config.GEMINI_MODEL);
  } catch (error) {
    if (error instanceof VisionParseError || error instanceof VisionUnavailableError) {
      throw error;
    }
    throw new VisionUnavailableError(error instanceof Error ? error.message : "Gemini unavailable");
  } finally {
    clearTimeout(timeout);
  }
}
