package de.ingomc.nozio.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionLabelParserTest {

    @Test
    fun parse_extractsTypicalGermanValuesPer100g() {
        val raw = """
            Protein Riegel
            FitBrand
            Nährwerte pro 100 g
            Energie 450 kcal
            Fett 18 g
            Kohlenhydrate 36 g
            davon Zucker 12 g
            Eiweiß 30 g
        """.trimIndent()

        val result = NutritionLabelParser.parse(raw)

        assertNotNull(result)
        assertEquals(450.0, result?.fields?.get(NutritionFieldKey.CALORIES)?.value)
        assertEquals(18.0, result?.fields?.get(NutritionFieldKey.FAT)?.value)
        assertEquals(36.0, result?.fields?.get(NutritionFieldKey.CARBS)?.value)
        assertEquals(12.0, result?.fields?.get(NutritionFieldKey.SUGAR)?.value)
        assertEquals(30.0, result?.fields?.get(NutritionFieldKey.PROTEIN)?.value)
    }

    @Test
    fun parse_prefers100gOverServingValues() {
        val raw = """
            Nutrition Facts
            per serving 40g
            Calories 220 kcal
            Protein 8 g
            Carbohydrates 10 g
            per 100 g
            Calories 500 kcal
            Protein 20 g
            Carbohydrates 25 g
            Fat 15 g
            Sugar 5 g
        """.trimIndent()

        val result = NutritionLabelParser.parse(raw)

        assertNotNull(result)
        assertEquals(500.0, result?.fields?.get(NutritionFieldKey.CALORIES)?.value)
        assertEquals(20.0, result?.fields?.get(NutritionFieldKey.PROTEIN)?.value)
        assertEquals(25.0, result?.fields?.get(NutritionFieldKey.CARBS)?.value)
        assertEquals(15.0, result?.fields?.get(NutritionFieldKey.FAT)?.value)
        assertEquals(5.0, result?.fields?.get(NutritionFieldKey.SUGAR)?.value)
    }

    @Test
    fun parse_handlesOcrNoiseCommaAndLetterO() {
        val raw = """
            Nährwerte pro 100g
            Energie 4O5 kcal
            Fett 9,5 g
            Kohlenhydrate 55,O g
            Davon Zucker 12,0 g
            Eiweiss 22,5 g
        """.trimIndent()

        val result = NutritionLabelParser.parse(raw)

        assertNotNull(result)
        assertEquals(405.0, result?.fields?.get(NutritionFieldKey.CALORIES)?.value)
        assertEquals(9.5, result?.fields?.get(NutritionFieldKey.FAT)?.value)
        assertEquals(55.0, result?.fields?.get(NutritionFieldKey.CARBS)?.value)
        assertEquals(12.0, result?.fields?.get(NutritionFieldKey.SUGAR)?.value)
        assertEquals(22.5, result?.fields?.get(NutritionFieldKey.PROTEIN)?.value)
    }

    @Test
    fun parse_returnsNullWhenNoNutritionTableWasDetected() {
        val raw = "Produktname\nZutaten: Wasser, Salz\nOhne Nährwertangaben"
        assertNull(NutritionLabelParser.parse(raw))
    }
}
