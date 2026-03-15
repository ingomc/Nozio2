package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.DiaryDao
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.FoodDao
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.local.MealTemplateDao
import de.ingomc.nozio.data.local.MealTemplateEntity
import de.ingomc.nozio.data.local.MealTemplateIngredientEntity
import de.ingomc.nozio.data.local.MealTemplateIngredientWithFood
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.local.RecipeAmountUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MealTemplateRepositoryTest {

    private val milchId = 10L
    private val eiweissId = 11L
    private val blaubeererId = 12L

    private val milch = FoodItem(
        id = milchId,
        name = "Milch",
        caloriesPer100g = 64.0,
        proteinPer100g = 3.3,
        fatPer100g = 3.5,
        carbsPer100g = 4.8,
        servingQuantity = 250.0,
        packageQuantity = 1000.0,
        source = FoodSource.OPEN_FOOD_FACTS
    )
    private val eiweiss = FoodItem(
        id = eiweissId,
        name = "Eiweiss",
        caloriesPer100g = 375.0,
        proteinPer100g = 80.0,
        fatPer100g = 3.0,
        carbsPer100g = 7.0,
        source = FoodSource.CUSTOM
    )
    private val blaubeeren = FoodItem(
        id = blaubeererId,
        name = "Blaubeeren",
        caloriesPer100g = 57.0,
        proteinPer100g = 0.7,
        fatPer100g = 0.3,
        carbsPer100g = 14.0,
        source = FoodSource.OPEN_FOOD_FACTS
    )

    private fun buildRepo(
        foodDao: FakeFoodDao = FakeFoodDao(mutableListOf(milch, eiweiss, blaubeeren)),
        diaryDao: FakeDiaryDao = FakeDiaryDao(),
        mealTemplateDao: FakeMealTemplateDao = FakeMealTemplateDao()
    ): MealTemplateRepository {
        mealTemplateDao.seedFoods(foodDao.getAllFoodsSync())
        return MealTemplateRepository(mealTemplateDao, diaryDao, foodDao)
    }

    @Test
    fun saveTemplate_storesNameMealTypeAndIngredients() = runTest {
        val repo = buildRepo()

        val templateId = repo.saveTemplate(
            SaveMealTemplateInput(
                name = "Proteinshake",
                defaultMealType = MealType.BREAKFAST,
                ingredients = listOf(
                    SaveIngredientInput(milchId, 300.0, RecipeAmountUnit.MILLILITER),
                    SaveIngredientInput(eiweissId, 50.0, RecipeAmountUnit.GRAM)
                )
            )
        )

        val detail = repo.getTemplateDetail(templateId)!!
        assertEquals("Proteinshake", detail.name)
        assertEquals(MealType.BREAKFAST, detail.defaultMealType)
        assertEquals(2, detail.ingredients.size)
        assertEquals(milchId, detail.ingredients[0].foodItemId)
        assertEquals(0, detail.ingredients[0].position)
        assertEquals(eiweissId, detail.ingredients[1].foodItemId)
        assertEquals(1, detail.ingredients[1].position)
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveTemplate_rejectsBlankName() = runTest {
        val repo = buildRepo()
        repo.saveTemplate(
            SaveMealTemplateInput(
                name = "   ",
                defaultMealType = MealType.LUNCH,
                ingredients = listOf(SaveIngredientInput(milchId, 100.0, RecipeAmountUnit.GRAM))
            )
        )
    }

    @Test
    fun trackTemplate_createsOneDiaryEntryPerIngredient() = runTest {
        val diaryDao = FakeDiaryDao()
        val repo = buildRepo(diaryDao = diaryDao)

        val templateId = repo.saveTemplate(
            SaveMealTemplateInput(
                name = "Shake",
                defaultMealType = MealType.BREAKFAST,
                ingredients = listOf(
                    SaveIngredientInput(milchId, 300.0, RecipeAmountUnit.MILLILITER),
                    SaveIngredientInput(eiweissId, 50.0, RecipeAmountUnit.GRAM),
                    SaveIngredientInput(blaubeererId, 80.0, RecipeAmountUnit.GRAM)
                )
            )
        )

        val detail = repo.getTemplateDetail(templateId)!!
        val result = repo.trackTemplate(
            TrackMealTemplateInput(
                date = LocalDate.parse("2026-03-15"),
                mealType = MealType.BREAKFAST,
                ingredients = detail.ingredients.map { ing ->
                    TrackIngredientInput(
                        foodItemId = ing.foodItemId,
                        amountValue = ing.defaultAmountValue,
                        amountUnit = ing.amountUnit,
                        servingQuantity = ing.servingQuantity,
                        packageQuantity = ing.packageQuantity
                    )
                }
            )
        )

        assertTrue(result is TrackMealTemplateResult.Success)
        assertEquals(3, (result as TrackMealTemplateResult.Success).trackedCount)
        assertEquals(3, diaryDao.entries.size)
    }

    @Test
    fun trackTemplate_unitConversion_gram() = runTest {
        val diaryDao = FakeDiaryDao()
        val repo = buildRepo(diaryDao = diaryDao)

        repo.trackTemplate(
            TrackMealTemplateInput(
                date = LocalDate.parse("2026-03-15"),
                mealType = MealType.LUNCH,
                ingredients = listOf(
                    TrackIngredientInput(eiweissId, 50.0, RecipeAmountUnit.GRAM, null, null)
                )
            )
        )

        assertEquals(50.0, diaryDao.entries.first().amountInGrams, 0.001)
    }

    @Test
    fun trackTemplate_unitConversion_milliliter() = runTest {
        val diaryDao = FakeDiaryDao()
        val repo = buildRepo(diaryDao = diaryDao)

        repo.trackTemplate(
            TrackMealTemplateInput(
                date = LocalDate.parse("2026-03-15"),
                mealType = MealType.LUNCH,
                ingredients = listOf(
                    TrackIngredientInput(milchId, 300.0, RecipeAmountUnit.MILLILITER, null, null)
                )
            )
        )

        assertEquals(300.0, diaryDao.entries.first().amountInGrams, 0.001)
    }

    @Test
    fun trackTemplate_unitConversion_portion_withServingQuantity() = runTest {
        val diaryDao = FakeDiaryDao()
        val repo = buildRepo(diaryDao = diaryDao)

        repo.trackTemplate(
            TrackMealTemplateInput(
                date = LocalDate.parse("2026-03-15"),
                mealType = MealType.LUNCH,
                ingredients = listOf(
                    TrackIngredientInput(milchId, 2.0, RecipeAmountUnit.PORTION, 250.0, null)
                )
            )
        )

        assertEquals(500.0, diaryDao.entries.first().amountInGrams, 0.001)
    }

    @Test
    fun trackTemplate_unitConversion_portion_fallback100g() = runTest {
        val diaryDao = FakeDiaryDao()
        val repo = buildRepo(diaryDao = diaryDao)

        repo.trackTemplate(
            TrackMealTemplateInput(
                date = LocalDate.parse("2026-03-15"),
                mealType = MealType.LUNCH,
                ingredients = listOf(
                    TrackIngredientInput(eiweissId, 1.0, RecipeAmountUnit.PORTION, null, null)
                )
            )
        )

        assertEquals(100.0, diaryDao.entries.first().amountInGrams, 0.001)
    }

    @Test
    fun trackTemplate_unitConversion_package_withPackageQuantity() = runTest {
        val diaryDao = FakeDiaryDao()
        val repo = buildRepo(diaryDao = diaryDao)

        repo.trackTemplate(
            TrackMealTemplateInput(
                date = LocalDate.parse("2026-03-15"),
                mealType = MealType.LUNCH,
                ingredients = listOf(
                    TrackIngredientInput(milchId, 1.0, RecipeAmountUnit.PACKAGE, null, 1000.0)
                )
            )
        )

        assertEquals(1000.0, diaryDao.entries.first().amountInGrams, 0.001)
    }

    @Test
    fun trackTemplate_unitConversion_package_fallback100g() = runTest {
        val diaryDao = FakeDiaryDao()
        val repo = buildRepo(diaryDao = diaryDao)

        repo.trackTemplate(
            TrackMealTemplateInput(
                date = LocalDate.parse("2026-03-15"),
                mealType = MealType.LUNCH,
                ingredients = listOf(
                    TrackIngredientInput(eiweissId, 1.0, RecipeAmountUnit.PACKAGE, null, null)
                )
            )
        )

        assertEquals(100.0, diaryDao.entries.first().amountInGrams, 0.001)
    }

    @Test
    fun trackTemplate_zeroAmountIngredients_areIgnored() = runTest {
        val diaryDao = FakeDiaryDao()
        val repo = buildRepo(diaryDao = diaryDao)

        val result = repo.trackTemplate(
            TrackMealTemplateInput(
                date = LocalDate.parse("2026-03-15"),
                mealType = MealType.DINNER,
                ingredients = listOf(
                    TrackIngredientInput(milchId, 300.0, RecipeAmountUnit.GRAM, null, null),
                    TrackIngredientInput(eiweissId, 0.0, RecipeAmountUnit.GRAM, null, null),
                    TrackIngredientInput(blaubeererId, -10.0, RecipeAmountUnit.GRAM, null, null)
                )
            )
        )

        assertTrue(result is TrackMealTemplateResult.Success)
        assertEquals(1, (result as TrackMealTemplateResult.Success).trackedCount)
    }

    @Test
    fun trackTemplate_emptyIngredients_returnsError() = runTest {
        val repo = buildRepo()

        val result = repo.trackTemplate(
            TrackMealTemplateInput(
                date = LocalDate.parse("2026-03-15"),
                mealType = MealType.SNACK,
                ingredients = emptyList()
            )
        )

        assertTrue(result is TrackMealTemplateResult.Error)
    }

    @Test
    fun trackTemplate_allZeroAmounts_returnsError() = runTest {
        val repo = buildRepo()

        val result = repo.trackTemplate(
            TrackMealTemplateInput(
                date = LocalDate.parse("2026-03-15"),
                mealType = MealType.SNACK,
                ingredients = listOf(
                    TrackIngredientInput(milchId, 0.0, RecipeAmountUnit.GRAM, null, null)
                )
            )
        )

        assertTrue(result is TrackMealTemplateResult.Error)
    }

    @Test
    fun deleteTemplate_removesTemplate() = runTest {
        val mealTemplateDao = FakeMealTemplateDao()
        val repo = buildRepo(mealTemplateDao = mealTemplateDao)

        val templateId = repo.saveTemplate(
            SaveMealTemplateInput(
                name = "Test",
                defaultMealType = MealType.LUNCH,
                ingredients = listOf(SaveIngredientInput(milchId, 100.0, RecipeAmountUnit.GRAM))
            )
        )

        repo.deleteTemplate(templateId)

        val templates = mealTemplateDao.observeAllTemplates().first()
        assertTrue(templates.isEmpty())
    }

    @Test
    fun observeTemplates_returnsCorrectSummaries() = runTest {
        val foodDao = FakeFoodDao(mutableListOf(milch, eiweiss))
        val repo = buildRepo(foodDao = foodDao)

        repo.saveTemplate(
            SaveMealTemplateInput(
                name = "Shake",
                defaultMealType = MealType.BREAKFAST,
                ingredients = listOf(
                    SaveIngredientInput(milchId, 100.0, RecipeAmountUnit.GRAM),
                    SaveIngredientInput(eiweissId, 50.0, RecipeAmountUnit.GRAM)
                )
            )
        )

        val summaries = repo.observeTemplates().first()
        assertEquals(1, summaries.size)
        assertEquals("Shake", summaries[0].name)
        assertEquals(MealType.BREAKFAST, summaries[0].defaultMealType)
        assertEquals(2, summaries[0].ingredientCount)
        assertTrue(summaries[0].totalCalories > 0)
    }
}

// ── Fakes ────────────────────────────────────────────────────────

private class FakeMealTemplateDao : MealTemplateDao {
    private var nextTemplateId = 1L
    private var nextIngredientId = 1L
    private val templatesById = linkedMapOf<Long, MealTemplateEntity>()
    private val ingredientsByTemplateId = mutableMapOf<Long, MutableList<MealTemplateIngredientEntity>>()
    private val state = MutableStateFlow(emptyList<MealTemplateEntity>())
    private val foodsById = mutableMapOf<Long, FoodItem>()

    fun seedFoods(foods: List<FoodItem>) {
        foods.forEach { foodsById[it.id] = it }
    }

    override suspend fun getIngredientsWithFood(templateId: Long): List<MealTemplateIngredientWithFood> {
        return (ingredientsByTemplateId[templateId] ?: emptyList()).mapNotNull { ing ->
            val food = foodsById[ing.foodItemId] ?: return@mapNotNull null
            MealTemplateIngredientWithFood(
                ingredientId = ing.id,
                templateId = ing.templateId,
                foodItemId = ing.foodItemId,
                position = ing.position,
                defaultAmountValue = ing.defaultAmountValue,
                amountUnit = ing.amountUnit,
                foodName = food.name,
                caloriesPer100g = food.caloriesPer100g,
                proteinPer100g = food.proteinPer100g,
                fatPer100g = food.fatPer100g,
                carbsPer100g = food.carbsPer100g,
                servingQuantity = food.servingQuantity,
                packageQuantity = food.packageQuantity
            )
        }
    }

    override fun observeAllTemplates(): Flow<List<MealTemplateEntity>> = state

    override suspend fun getTemplateById(templateId: Long): MealTemplateEntity? = templatesById[templateId]

    override suspend fun insertTemplate(template: MealTemplateEntity): Long {
        val id = nextTemplateId++
        templatesById[id] = template.copy(id = id)
        emitCurrent()
        return id
    }

    override suspend fun updateTemplate(template: MealTemplateEntity) {
        templatesById[template.id] = template
        emitCurrent()
    }

    override suspend fun deleteTemplate(templateId: Long) {
        templatesById.remove(templateId)
        ingredientsByTemplateId.remove(templateId)
        emitCurrent()
    }

    override suspend fun deleteIngredientsForTemplate(templateId: Long) {
        ingredientsByTemplateId.remove(templateId)
    }

    override suspend fun insertIngredients(ingredients: List<MealTemplateIngredientEntity>) {
        ingredients.forEach { ing ->
            val id = nextIngredientId++
            val saved = ing.copy(id = id)
            ingredientsByTemplateId.getOrPut(saved.templateId) { mutableListOf() }.add(saved)
        }
    }

    override suspend fun getAllRaw(): List<MealTemplateEntity> = templatesById.values.toList()

    override suspend fun getAllIngredientsRaw(): List<MealTemplateIngredientEntity> =
        ingredientsByTemplateId.values.flatten()

    override suspend fun insertAllTemplates(templates: List<MealTemplateEntity>) {
        templates.forEach { templatesById[it.id] = it }
        emitCurrent()
    }

    override suspend fun insertAllIngredients(ingredients: List<MealTemplateIngredientEntity>) {
        ingredients.forEach { ing ->
            ingredientsByTemplateId.getOrPut(ing.templateId) { mutableListOf() }.add(ing)
        }
    }

    override suspend fun deleteAllIngredients() {
        ingredientsByTemplateId.clear()
    }

    override suspend fun deleteAllTemplates() {
        templatesById.clear()
        ingredientsByTemplateId.clear()
        emitCurrent()
    }

    private fun emitCurrent() {
        state.value = templatesById.values.sortedByDescending { it.updatedAtEpochMs }
    }
}

private class FakeFoodDao(
    private val foods: MutableList<FoodItem> = mutableListOf()
) : FoodDao {
    fun getAllFoodsSync(): List<FoodItem> = foods.toList()

    override suspend fun insert(foodItem: FoodItem): Long {
        val existing = foods.indexOfFirst { it.id == foodItem.id }
        if (existing >= 0) {
            foods[existing] = foodItem
        } else {
            foods.add(foodItem)
        }
        return foodItem.id
    }

    override suspend fun insertAll(foodItems: List<FoodItem>): List<Long> {
        foodItems.forEach { foods.add(it) }
        return foodItems.map { it.id }
    }

    override suspend fun searchByName(query: String): List<FoodItem> =
        foods.filter { it.name.contains(query, ignoreCase = true) }.take(10)

    override suspend fun getById(id: Long): FoodItem? = foods.find { it.id == id }

    override suspend fun getByBarcode(barcode: String): FoodItem? =
        foods.find { it.barcode == barcode }

    override suspend fun getAll(): List<FoodItem> = foods.toList()

    override suspend fun getFavorites(limit: Int): List<FoodItem> =
        foods.filter { it.isFavorite }.take(limit)

    override suspend fun getFoodsMissingImageUrl(limit: Int): List<FoodItem> = emptyList()

    override suspend fun setFavorite(foodId: Long, isFavorite: Boolean) {
        val idx = foods.indexOfFirst { it.id == foodId }
        if (idx >= 0) {
            foods[idx] = foods[idx].copy(isFavorite = isFavorite)
        }
    }

    override suspend fun insertAllWithIds(foodItems: List<FoodItem>) {
        foodItems.forEach { item ->
            val idx = foods.indexOfFirst { it.id == item.id }
            if (idx >= 0) foods[idx] = item else foods.add(item)
        }
    }

    override suspend fun deleteAll() {
        foods.clear()
    }
}

private class FakeDiaryDao : DiaryDao {
    val entries = mutableListOf<DiaryEntry>()
    private var nextId = 1L

    override suspend fun insert(entry: DiaryEntry): Long {
        val id = nextId++
        entries.add(entry.copy(id = id))
        return id
    }

    override suspend fun deleteById(entryId: Long) {
        entries.removeAll { it.id == entryId }
    }

    override suspend fun updateAmountInGrams(entryId: Long, amountInGrams: Double) {
        val idx = entries.indexOfFirst { it.id == entryId }
        if (idx >= 0) entries[idx] = entries[idx].copy(amountInGrams = amountInGrams)
    }

    override suspend fun updateDateAndMealType(entryId: Long, date: LocalDate, mealType: MealType) {
        val idx = entries.indexOfFirst { it.id == entryId }
        if (idx >= 0) entries[idx] = entries[idx].copy(date = date, mealType = mealType)
    }

    override fun getEntriesWithFoodForDate(date: LocalDate): Flow<List<DiaryEntryWithFood>> =
        flowOf(emptyList())

    override suspend fun getRecentlyAddedFoods(limit: Int): List<FoodItem> = emptyList()

    override suspend fun getFrequentlyAddedFoods(limit: Int): List<FoodItem> = emptyList()

    override suspend fun getAllRaw(): List<DiaryEntry> = entries.toList()

    override suspend fun insertAll(entries: List<DiaryEntry>) {
        this.entries.addAll(entries)
    }

    override suspend fun deleteAll() {
        entries.clear()
    }
}
