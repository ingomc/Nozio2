package de.ingomc.nozio.ui.search

import de.ingomc.nozio.data.local.DiaryDao
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.FoodDao
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.remote.CreateCustomFoodRequestDto
import de.ingomc.nozio.data.remote.CreateCustomFoodResponseDto
import de.ingomc.nozio.data.remote.FoodApi
import de.ingomc.nozio.data.remote.FoodBarcodeResponseDto
import de.ingomc.nozio.data.remote.FoodSearchResponseDto
import de.ingomc.nozio.data.repository.DiaryRepository
import de.ingomc.nozio.data.repository.FoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addFood_setsDetailedConfirmationAndRefreshesWidget() = runTest(dispatcher) {
        val foodDao = FakeFoodDao()
        val diaryDao = FakeDiaryDao(foodLookup = foodDao::getStoredById)
        val refreshCounter = RefreshCounter()
        val viewModel = SearchViewModel(
            foodRepository = FoodRepository(FakeFoodApi(), foodDao),
            diaryRepository = DiaryRepository(diaryDao),
            widgetRefreshDelegate = WidgetRefreshDelegate { refreshCounter.count += 1 }
        )
        val food = FoodItem(
            name = "Haferflocken",
            caloriesPer100g = 370.0,
            proteinPer100g = 13.5,
            fatPer100g = 7.0,
            carbsPer100g = 58.0,
            source = FoodSource.SELF_HOSTED_OFF
        )

        advanceUntilIdle()
        viewModel.selectFood(food)
        viewModel.addFood(MealType.LUNCH, amountInGrams = 150.0, amountLabel = "150g")
        advanceUntilIdle()

        val confirmation = viewModel.uiState.value.activeAddConfirmation

        assertNotNull(confirmation)
        assertEquals(1L, confirmation?.entryId)
        assertEquals("Haferflocken", confirmation?.title)
        assertEquals(MealType.LUNCH, confirmation?.mealType)
        assertEquals("150g", confirmation?.amountLabel)
        assertEquals(555, confirmation?.calories)
        assertEquals(20, confirmation?.protein)
        assertEquals(11, confirmation?.fat)
        assertEquals(87, confirmation?.carbs)
        assertEquals(AddConfirmationSource.STANDARD, confirmation?.source)
        assertEquals(1, refreshCounter.count)
        assertEquals(listOf("Haferflocken"), viewModel.uiState.value.recentSuggestions.map { it.name })
    }

    @Test
    fun addQuickEntry_setsQuickAddConfirmationWithoutAmountLabel() = runTest(dispatcher) {
        val foodDao = FakeFoodDao()
        val diaryDao = FakeDiaryDao(foodLookup = foodDao::getStoredById)
        val viewModel = SearchViewModel(
            foodRepository = FoodRepository(FakeFoodApi(), foodDao),
            diaryRepository = DiaryRepository(diaryDao),
            widgetRefreshDelegate = WidgetRefreshDelegate {}
        )

        advanceUntilIdle()
        viewModel.addQuickEntry(
            mealType = MealType.SNACK,
            calories = 320.0,
            protein = 26.0,
            fat = 12.0,
            carbs = 18.0,
            name = "Shake"
        )
        advanceUntilIdle()

        val confirmation = viewModel.uiState.value.activeAddConfirmation

        assertNotNull(confirmation)
        assertEquals("Shake", confirmation?.title)
        assertEquals(MealType.SNACK, confirmation?.mealType)
        assertNull(confirmation?.amountLabel)
        assertEquals(320, confirmation?.calories)
        assertEquals(26, confirmation?.protein)
        assertEquals(12, confirmation?.fat)
        assertEquals(18, confirmation?.carbs)
        assertEquals(AddConfirmationSource.QUICK_ADD, confirmation?.source)
    }

    @Test
    fun undoLastAddedFood_deletesEntryClearsBannerAndReloadsSuggestions() = runTest(dispatcher) {
        val foodDao = FakeFoodDao()
        val diaryDao = FakeDiaryDao(foodLookup = foodDao::getStoredById)
        val refreshCounter = RefreshCounter()
        val viewModel = SearchViewModel(
            foodRepository = FoodRepository(FakeFoodApi(), foodDao),
            diaryRepository = DiaryRepository(diaryDao),
            widgetRefreshDelegate = WidgetRefreshDelegate { refreshCounter.count += 1 }
        )

        advanceUntilIdle()
        viewModel.selectFood(
            FoodItem(
                name = "Skyr",
                caloriesPer100g = 60.0,
                proteinPer100g = 11.0,
                fatPer100g = 0.2,
                carbsPer100g = 3.5
            )
        )
        viewModel.addFood(MealType.BREAKFAST, amountInGrams = 200.0, amountLabel = "200g")
        advanceUntilIdle()
        viewModel.undoLastAddedFood()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeAddConfirmation)
        assertEquals(emptyList<FoodItem>(), viewModel.uiState.value.recentSuggestions)
        assertEquals(listOf(1L), diaryDao.deletedEntryIds)
        assertEquals(2, refreshCounter.count)
    }

    @Test
    fun dismissAddConfirmation_clearsBannerWithoutDeletingEntry() = runTest(dispatcher) {
        val foodDao = FakeFoodDao()
        val diaryDao = FakeDiaryDao(foodLookup = foodDao::getStoredById)
        val viewModel = SearchViewModel(
            foodRepository = FoodRepository(FakeFoodApi(), foodDao),
            diaryRepository = DiaryRepository(diaryDao),
            widgetRefreshDelegate = WidgetRefreshDelegate {}
        )

        advanceUntilIdle()
        viewModel.selectFood(
            FoodItem(
                name = "Banane",
                caloriesPer100g = 89.0,
                proteinPer100g = 1.1,
                fatPer100g = 0.3,
                carbsPer100g = 23.0
            )
        )
        viewModel.addFood(MealType.SNACK, amountInGrams = 120.0, amountLabel = "120g")
        advanceUntilIdle()

        viewModel.dismissAddConfirmation()

        assertNull(viewModel.uiState.value.activeAddConfirmation)
        assertEquals(emptyList<Long>(), diaryDao.deletedEntryIds)
    }

    private data class RefreshCounter(var count: Int = 0)

    private class FakeFoodApi : FoodApi {
        override suspend fun searchFoods(query: String, limit: Int): FoodSearchResponseDto = FoodSearchResponseDto()

        override suspend fun getFoodByBarcode(barcode: String): FoodBarcodeResponseDto {
            throw IllegalStateException("Not needed for this test")
        }

        override suspend fun createCustomFood(request: CreateCustomFoodRequestDto): CreateCustomFoodResponseDto {
            throw IllegalStateException("Not needed for this test")
        }
    }

    private class FakeFoodDao : FoodDao {
        private val storage = linkedMapOf<Long, FoodItem>()
        private var nextId = 1L

        override suspend fun insert(foodItem: FoodItem): Long {
            val id = if (foodItem.id > 0) foodItem.id else nextId++
            storage[id] = foodItem.copy(id = id)
            return id
        }

        override suspend fun insertAll(foodItems: List<FoodItem>): List<Long> {
            return foodItems.map { insert(it) }
        }

        override suspend fun searchByName(query: String): List<FoodItem> {
            return storage.values.filter { it.name.contains(query, ignoreCase = true) }
        }

        override suspend fun getById(id: Long): FoodItem? = storage[id]

        override suspend fun getByBarcode(barcode: String): FoodItem? {
            return storage.values.firstOrNull { it.barcode == barcode }
        }

        override suspend fun getAll(): List<FoodItem> = storage.values.toList()

        override suspend fun getFavorites(limit: Int): List<FoodItem> {
            return storage.values.filter { it.isFavorite }.take(limit)
        }

        override suspend fun getFoodsMissingImageUrl(limit: Int): List<FoodItem> {
            return storage.values
                .filter { it.imageUrl.isNullOrBlank() && !it.barcode.isNullOrBlank() }
                .take(limit)
        }

        override suspend fun setFavorite(foodId: Long, isFavorite: Boolean) {
            val existing = storage[foodId] ?: return
            storage[foodId] = existing.copy(isFavorite = isFavorite)
        }

        override suspend fun insertAllWithIds(foodItems: List<FoodItem>) {
            foodItems.forEach { food ->
                val id = if (food.id > 0) food.id else nextId++
                storage[id] = food.copy(id = id)
            }
        }

        override suspend fun deleteAll() {
            storage.clear()
        }

        fun getStoredById(id: Long): FoodItem? = storage[id]
    }

    private class FakeDiaryDao(
        private val foodLookup: (Long) -> FoodItem?
    ) : DiaryDao {
        private val entries = mutableListOf<DiaryEntry>()
        private val dayEntriesFlow = MutableStateFlow<List<DiaryEntryWithFood>>(emptyList())
        private var nextEntryId = 1L
        val deletedEntryIds = mutableListOf<Long>()

        override suspend fun insert(entry: DiaryEntry): Long {
            val stored = entry.copy(id = nextEntryId++)
            entries += stored
            syncDayEntries()
            return stored.id
        }

        override suspend fun deleteById(entryId: Long) {
            deletedEntryIds += entryId
            entries.removeAll { it.id == entryId }
            syncDayEntries()
        }

        override suspend fun updateAmountInGrams(entryId: Long, amountInGrams: Double) {
            val index = entries.indexOfFirst { it.id == entryId }
            if (index >= 0) {
                entries[index] = entries[index].copy(amountInGrams = amountInGrams)
                syncDayEntries()
            }
        }

        override suspend fun updateDateAndMealType(entryId: Long, date: java.time.LocalDate, mealType: MealType) {
            val index = entries.indexOfFirst { it.id == entryId }
            if (index >= 0) {
                entries[index] = entries[index].copy(date = date, mealType = mealType)
                syncDayEntries()
            }
        }

        override fun getEntriesWithFoodForDate(date: java.time.LocalDate): Flow<List<DiaryEntryWithFood>> {
            return flowOf(dayEntriesFlow.value.filter { it.date == date })
        }

        override suspend fun getRecentlyAddedFoods(limit: Int): List<FoodItem> {
            return entries
                .asReversed()
                .mapNotNull { foodLookup(it.foodItemId) }
                .distinctBy { it.id }
                .take(limit)
        }

        override suspend fun getFrequentlyAddedFoods(limit: Int): List<FoodItem> {
            return entries
                .groupingBy { it.foodItemId }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .mapNotNull { (foodId, _) -> foodLookup(foodId) }
                .take(limit)
        }

        override suspend fun getAllRaw(): List<DiaryEntry> = entries.toList()

        override suspend fun insertAll(entries: List<DiaryEntry>) {
            this.entries += entries
            nextEntryId = (this.entries.maxOfOrNull { it.id } ?: 0L) + 1L
            syncDayEntries()
        }

        override suspend fun deleteAll() {
            entries.clear()
            syncDayEntries()
        }

        private fun syncDayEntries() {
            dayEntriesFlow.value = entries.mapNotNull { entry ->
                val food = foodLookup(entry.foodItemId) ?: return@mapNotNull null
                DiaryEntryWithFood(
                    entryId = entry.id,
                    date = entry.date,
                    mealType = entry.mealType,
                    foodItemId = entry.foodItemId,
                    amountInGrams = entry.amountInGrams,
                    foodName = food.name,
                    caloriesPer100g = food.caloriesPer100g,
                    proteinPer100g = food.proteinPer100g,
                    fatPer100g = food.fatPer100g,
                    carbsPer100g = food.carbsPer100g
                )
            }
        }
    }
}
