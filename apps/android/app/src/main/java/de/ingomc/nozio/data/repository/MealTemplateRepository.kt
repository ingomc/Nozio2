package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.DiaryDao
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.FoodDao
import de.ingomc.nozio.data.local.MealTemplateDao
import de.ingomc.nozio.data.local.MealTemplateEntity
import de.ingomc.nozio.data.local.MealTemplateIngredientEntity
import de.ingomc.nozio.data.local.MealTemplateIngredientWithFood
import de.ingomc.nozio.data.local.MealTemplateWithIngredients
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.local.RecipeAmountUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class MealTemplateSummary(
    val id: Long,
    val name: String,
    val defaultMealType: MealType,
    val ingredientCount: Int,
    val totalCalories: Double,
    val ingredientPreview: String?
)

data class MealTemplateDetail(
    val id: Long,
    val name: String,
    val defaultMealType: MealType,
    val ingredients: List<MealTemplateIngredientDetail>
)

data class MealTemplateIngredientDetail(
    val ingredientId: Long,
    val foodItemId: Long,
    val foodName: String,
    val position: Int,
    val defaultAmountValue: Double,
    val amountUnit: RecipeAmountUnit,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val servingQuantity: Double?,
    val packageQuantity: Double?
) {
    fun normalizedAmountInGrams(): Double = normalizeToGrams(defaultAmountValue, amountUnit, servingQuantity, packageQuantity)
    val calories: Double get() = caloriesPer100g * normalizedAmountInGrams() / 100.0
    val protein: Double get() = proteinPer100g * normalizedAmountInGrams() / 100.0
    val fat: Double get() = fatPer100g * normalizedAmountInGrams() / 100.0
    val carbs: Double get() = carbsPer100g * normalizedAmountInGrams() / 100.0
}

data class SaveMealTemplateInput(
    val id: Long = 0,
    val name: String,
    val defaultMealType: MealType,
    val ingredients: List<SaveIngredientInput>
)

data class SaveIngredientInput(
    val foodItemId: Long,
    val amountValue: Double,
    val amountUnit: RecipeAmountUnit
)

data class TrackMealTemplateInput(
    val date: LocalDate,
    val mealType: MealType,
    val ingredients: List<TrackIngredientInput>
)

data class TrackIngredientInput(
    val foodItemId: Long,
    val amountValue: Double,
    val amountUnit: RecipeAmountUnit,
    val servingQuantity: Double?,
    val packageQuantity: Double?
)

sealed interface TrackMealTemplateResult {
    data class Success(val trackedCount: Int) : TrackMealTemplateResult
    data class Error(val message: String) : TrackMealTemplateResult
}

fun normalizeToGrams(
    amountValue: Double,
    amountUnit: RecipeAmountUnit,
    servingQuantity: Double?,
    packageQuantity: Double?
): Double {
    return when (amountUnit) {
        RecipeAmountUnit.GRAM -> amountValue
        RecipeAmountUnit.MILLILITER -> amountValue
        RecipeAmountUnit.PORTION -> {
            val multiplier = servingQuantity ?: 100.0
            amountValue * multiplier
        }
        RecipeAmountUnit.PACKAGE -> {
            val multiplier = packageQuantity ?: 100.0
            amountValue * multiplier
        }
    }
}

class MealTemplateRepository(
    private val mealTemplateDao: MealTemplateDao,
    private val diaryDao: DiaryDao,
    private val foodDao: FoodDao
) {
    fun observeTemplates(): Flow<List<MealTemplateSummary>> {
        return mealTemplateDao.observeAllTemplates().map { templates ->
            templates.map { template ->
                val ingredients = mealTemplateDao.getIngredientsWithFood(template.id)
                val totalCalories = ingredients.sumOf { ing ->
                    val grams = normalizeToGrams(ing.defaultAmountValue, ing.amountUnit, ing.servingQuantity, ing.packageQuantity)
                    ing.caloriesPer100g * grams / 100.0
                }
                MealTemplateSummary(
                    id = template.id,
                    name = template.name,
                    defaultMealType = template.defaultMealType,
                    ingredientCount = ingredients.size,
                    totalCalories = totalCalories,
                    ingredientPreview = ingredients.toIngredientPreview()
                )
            }
        }
    }

    fun observeTemplateDetail(templateId: Long): Flow<MealTemplateDetail?> {
        return mealTemplateDao.observeAllTemplates().map {
            val template = mealTemplateDao.getTemplateById(templateId) ?: return@map null
            val ingredients = mealTemplateDao.getIngredientsWithFood(templateId)
            MealTemplateDetail(
                id = template.id,
                name = template.name,
                defaultMealType = template.defaultMealType,
                ingredients = ingredients.map { it.toDetail() }
            )
        }
    }

    suspend fun getTemplateDetail(templateId: Long): MealTemplateDetail? {
        val template = mealTemplateDao.getTemplateById(templateId) ?: return null
        val ingredients = mealTemplateDao.getIngredientsWithFood(templateId)
        return MealTemplateDetail(
            id = template.id,
            name = template.name,
            defaultMealType = template.defaultMealType,
            ingredients = ingredients.map { it.toDetail() }
        )
    }

    suspend fun saveTemplate(input: SaveMealTemplateInput): Long {
        val normalizedName = input.name.trim()
        require(normalizedName.isNotBlank()) { "Recipe name cannot be blank." }
        require(input.ingredients.all { it.foodItemId > 0 }) { "All ingredients must have a valid foodItemId." }

        val now = System.currentTimeMillis()

        val templateId = if (input.id == 0L) {
            mealTemplateDao.insertTemplate(
                MealTemplateEntity(
                    name = normalizedName,
                    defaultMealType = input.defaultMealType,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
        } else {
            val existing = mealTemplateDao.getTemplateById(input.id)
            mealTemplateDao.updateTemplate(
                MealTemplateEntity(
                    id = input.id,
                    name = normalizedName,
                    defaultMealType = input.defaultMealType,
                    createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                    updatedAtEpochMs = now
                )
            )
            input.id
        }

        mealTemplateDao.deleteIngredientsForTemplate(templateId)
        val ingredientEntities = input.ingredients.mapIndexed { index, ingredient ->
            MealTemplateIngredientEntity(
                templateId = templateId,
                foodItemId = ingredient.foodItemId,
                position = index,
                defaultAmountValue = ingredient.amountValue,
                amountUnit = ingredient.amountUnit
            )
        }
        mealTemplateDao.insertIngredients(ingredientEntities)

        return templateId
    }

    suspend fun deleteTemplate(templateId: Long) {
        mealTemplateDao.deleteTemplate(templateId)
    }

    suspend fun trackTemplate(input: TrackMealTemplateInput): TrackMealTemplateResult {
        val validIngredients = input.ingredients.filter { it.amountValue > 0 }
        if (validIngredients.isEmpty()) {
            return TrackMealTemplateResult.Error("Keine gueltigen Zutaten zum Tracken.")
        }

        var trackedCount = 0
        for (ingredient in validIngredients) {
            val food = foodDao.getById(ingredient.foodItemId) ?: continue
            val grams = normalizeToGrams(
                ingredient.amountValue,
                ingredient.amountUnit,
                ingredient.servingQuantity ?: food.servingQuantity,
                ingredient.packageQuantity ?: food.packageQuantity
            )
            if (grams > 0) {
                diaryDao.insert(
                    DiaryEntry(
                        date = input.date,
                        mealType = input.mealType,
                        foodItemId = ingredient.foodItemId,
                        amountInGrams = grams
                    )
                )
                trackedCount++
            }
        }

        return if (trackedCount > 0) {
            TrackMealTemplateResult.Success(trackedCount)
        } else {
            TrackMealTemplateResult.Error("Keine Zutaten konnten getrackt werden.")
        }
    }
}

private fun MealTemplateIngredientWithFood.toDetail() = MealTemplateIngredientDetail(
    ingredientId = ingredientId,
    foodItemId = foodItemId,
    foodName = foodName,
    position = position,
    defaultAmountValue = defaultAmountValue,
    amountUnit = amountUnit,
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    fatPer100g = fatPer100g,
    carbsPer100g = carbsPer100g,
    servingQuantity = servingQuantity,
    packageQuantity = packageQuantity
)

private fun List<MealTemplateIngredientWithFood>.toIngredientPreview(): String? {
    if (isEmpty()) return null
    val visibleNames = take(3).map { it.foodName }
    val hiddenCount = size - visibleNames.size
    val base = visibleNames.joinToString(", ")
    return if (hiddenCount > 0) "$base +$hiddenCount" else base
}
