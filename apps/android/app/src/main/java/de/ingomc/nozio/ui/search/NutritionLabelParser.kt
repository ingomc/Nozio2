package de.ingomc.nozio.ui.search

import kotlin.math.roundToInt

enum class NutritionFieldKey {
    CALORIES,
    PROTEIN,
    CARBS,
    FAT,
    SUGAR
}

data class NutritionFieldValue(
    val value: Double,
    val confidence: Float,
    val sourceTag: String
)

data class NutritionScanResult(
    val fields: Map<NutritionFieldKey, NutritionFieldValue>,
    val rawText: String,
    val productName: String? = null,
    val brand: String? = null,
    val warnings: List<String> = emptyList()
)

data class CustomFoodDraft(
    val name: String? = null,
    val brand: String? = null,
    val caloriesPer100g: Double? = null,
    val proteinPer100g: Double? = null,
    val carbsPer100g: Double? = null,
    val fatPer100g: Double? = null,
    val sugarPer100g: Double? = null
)

object NutritionLabelParser {
    private val lineSplitRegex = Regex("[\\n\\r]+")
    private val numericRegex = Regex("([0-9][0-9oO]{0,3}(?:[\\.,][0-9oO]{1,2})?)")
    private val kcalRegex = Regex("([0-9][0-9oO]{0,3}(?:[\\.,][0-9oO]{1,2})?)\\s*kcal")
    private val gramsRegex = Regex("([0-9][0-9oO]{0,3}(?:[\\.,][0-9oO]{1,2})?)\\s*g")

    fun parse(rawText: String): NutritionScanResult? {
        val lines = rawText
            .split(lineSplitRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return null

        val hasHundredBase = lines.any(::containsHundredBase)
        val firstHundredBaseIndex = lines.indexOfFirst(::containsHundredBase)
        val fields = linkedMapOf<NutritionFieldKey, NutritionFieldValue>()

        parseCalories(lines, hasHundredBase, firstHundredBaseIndex)?.let { fields[NutritionFieldKey.CALORIES] = it }
        parseNutrient(lines, hasHundredBase, firstHundredBaseIndex, aliases = PROTEIN_ALIASES)?.let {
            fields[NutritionFieldKey.PROTEIN] = it
        }
        parseNutrient(lines, hasHundredBase, firstHundredBaseIndex, aliases = CARBS_ALIASES)?.let {
            fields[NutritionFieldKey.CARBS] = it
        }
        parseNutrient(lines, hasHundredBase, firstHundredBaseIndex, aliases = FAT_ALIASES)?.let {
            fields[NutritionFieldKey.FAT] = it
        }
        parseNutrient(lines, hasHundredBase, firstHundredBaseIndex, aliases = SUGAR_ALIASES)?.let {
            fields[NutritionFieldKey.SUGAR] = it
        }

        if (fields.isEmpty()) return null

        val nonNutritionLines = lines
            .take(8)
            .filterNot(::isNutritionContextLine)
        val productName = nonNutritionLines.getOrNull(0)
        val brand = nonNutritionLines.getOrNull(1)

        return NutritionScanResult(
            fields = fields,
            rawText = rawText,
            productName = productName,
            brand = brand
        )
    }

    private fun parseCalories(
        lines: List<String>,
        hasHundredBase: Boolean,
        firstHundredBaseIndex: Int
    ): NutritionFieldValue? {
        val candidate = findBestLine(lines, hasHundredBase, firstHundredBaseIndex, CALORIE_ALIASES)
            ?: return null

        val normalized = normalize(candidate.value)
        val kcalValue = kcalRegex.find(normalized)?.groupValues?.getOrNull(1)?.toDoubleOrNullNormalized()
            ?: numericRegex.find(normalized)?.groupValues?.getOrNull(1)?.toDoubleOrNullNormalized()
            ?: findNearbyValue(lines, candidate.index, preferKcal = true)
            ?: return null

        return NutritionFieldValue(
            value = kcalValue,
            confidence = confidenceForLine(normalized, hasHundredBase, prefersUnit = true),
            sourceTag = sourceTag(normalized, hasHundredBase)
        )
    }

    private fun parseNutrient(
        lines: List<String>,
        hasHundredBase: Boolean,
        firstHundredBaseIndex: Int,
        aliases: Set<String>
    ): NutritionFieldValue? {
        val candidate = findBestLine(lines, hasHundredBase, firstHundredBaseIndex, aliases)
            ?: return null

        val normalized = normalize(candidate.value)
        val value = gramsRegex.find(normalized)?.groupValues?.getOrNull(1)?.toDoubleOrNullNormalized()
            ?: numericRegex.find(normalized)?.groupValues?.getOrNull(1)?.toDoubleOrNullNormalized()
            ?: findNearbyValue(lines, candidate.index, preferKcal = false)
            ?: return null

        return NutritionFieldValue(
            value = value,
            confidence = confidenceForLine(normalized, hasHundredBase, prefersUnit = true),
            sourceTag = sourceTag(normalized, hasHundredBase)
        )
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace("ö", "oe")
            .replace("ä", "ae")
            .replace("ü", "ue")
            .replace("ß", "ss")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun containsAlias(line: String, aliases: Set<String>): Boolean {
        return aliases.any { alias ->
            val regex = Regex("\\b${Regex.escape(alias)}\\b")
            regex.containsMatchIn(line)
        }
    }

    private fun containsHundredBase(line: String): Boolean {
        val compact = line.replace(" ", "")
        return compact.contains("100g") || compact.contains("100ml")
    }

    private fun isPortionLine(line: String): Boolean {
        return PORTION_ALIASES.any { line.contains(it) } && !containsHundredBase(line)
    }

    private fun isCandidateLine(line: String, hasHundredBase: Boolean): Boolean {
        if (isPortionLine(line)) return false
        return containsHundredBase(line) || hasHundredBase
    }

    private fun findBestLine(
        lines: List<String>,
        hasHundredBase: Boolean,
        firstHundredBaseIndex: Int,
        aliases: Set<String>
    ): LineCandidate? {
        val candidates = lines.withIndex()
            .filter { indexed ->
                val normalized = normalize(indexed.value)
                containsAlias(normalized, aliases) && isCandidateLine(normalized, hasHundredBase)
            }
            .sortedByDescending { indexed ->
                val normalized = normalize(indexed.value)
                var score = 0
                if (containsHundredBase(normalized)) score += 100
                if (firstHundredBaseIndex >= 0 && indexed.index > firstHundredBaseIndex) score += 60
                if (normalized.contains("kcal") || normalized.contains(" g")) score += 20
                score - indexed.index
            }
            .map { LineCandidate(index = it.index, value = it.value) }
        return candidates.firstOrNull()
    }

    private fun findNearbyValue(lines: List<String>, anchorIndex: Int, preferKcal: Boolean): Double? {
        val candidateLines = buildList {
            for (offset in -1..2) {
                val idx = anchorIndex + offset
                if (idx in lines.indices) add(lines[idx])
            }
        }
        val extracted = candidateLines.mapNotNull { raw ->
            val normalized = normalize(raw)
            val fromUnit = if (preferKcal) {
                kcalRegex.find(normalized)?.groupValues?.getOrNull(1)?.toDoubleOrNullNormalized()
            } else {
                gramsRegex.find(normalized)?.groupValues?.getOrNull(1)?.toDoubleOrNullNormalized()
            }
            fromUnit ?: numericRegex.find(normalized)?.groupValues?.getOrNull(1)?.toDoubleOrNullNormalized()
        }
        if (extracted.isEmpty()) return null
        return if (preferKcal) {
            extracted.filter { it in 1.0..2000.0 }.maxOrNull() ?: extracted.first()
        } else {
            extracted.filter { it in 0.0..100.0 }.maxOrNull() ?: extracted.first()
        }
    }

    private fun isNutritionContextLine(line: String): Boolean {
        return containsHundredBase(line) ||
            isPortionLine(line) ||
            containsAlias(line, CALORIE_ALIASES) ||
            containsAlias(line, PROTEIN_ALIASES) ||
            containsAlias(line, CARBS_ALIASES) ||
            containsAlias(line, FAT_ALIASES) ||
            containsAlias(line, SUGAR_ALIASES)
    }

    private fun confidenceForLine(line: String, hasHundredBase: Boolean, prefersUnit: Boolean): Float {
        val hasBase = containsHundredBase(line)
        val unitBonus = if (prefersUnit && (line.contains("kcal") || line.contains(" g"))) 0.06f else 0f
        val base = when {
            hasBase -> 0.9f
            hasHundredBase -> 0.78f
            else -> 0.62f
        }
        return (base + unitBonus).coerceAtMost(0.98f)
    }

    private fun sourceTag(line: String, hasHundredBase: Boolean): String {
        return when {
            containsHundredBase(line) -> "line_100"
            hasHundredBase -> "table_100"
            else -> "fallback"
        }
    }

    private fun String.toDoubleOrNullNormalized(): Double? {
        val normalized = replace('O', '0')
            .replace('o', '0')
            .replace(',', '.')
            .replace(" ", "")
        return normalized.toDoubleOrNull()
    }

    fun formatValue(value: Double): String {
        if (value % 1.0 == 0.0) return value.roundToInt().toString()
        return value.toString()
    }

    private val CALORIE_ALIASES = setOf("kcal", "kalorien", "energie", "brennwert")
    private val PROTEIN_ALIASES = setOf("protein", "proteine", "eiweiss", "eiweiß", "eiwess")
    private val CARBS_ALIASES = setOf("kohlenhydrat", "kohlenhydrate", "carbohydrate", "carbohydrates", "carbs")
    private val FAT_ALIASES = setOf("fett", "fat", "lipid")
    private val SUGAR_ALIASES = setOf("zucker", "sugar", "davon zucker")
    private val PORTION_ALIASES = setOf("portion", "serving", "pro port", "proportion", "per serving")
}
    private data class LineCandidate(val index: Int, val value: String)
