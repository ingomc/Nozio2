package de.ingomc.nozio.ui.search

import de.ingomc.nozio.data.local.MealType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class MealTypeDefaultsTest {

    @Test
    fun resolveInitialMealType_usesPreselectedMealType_whenProvided() {
        val mealType = resolveInitialMealType(
            preselectedMealType = MealType.DINNER,
            localTime = LocalTime.of(8, 30)
        )

        assertEquals(MealType.DINNER, mealType)
    }

    @Test
    fun defaultMealTypeForTime_mapsBreakfastWindow() {
        assertEquals(MealType.BREAKFAST, defaultMealTypeForTime(LocalTime.of(5, 0)))
        assertEquals(MealType.BREAKFAST, defaultMealTypeForTime(LocalTime.of(10, 59)))
    }

    @Test
    fun defaultMealTypeForTime_mapsLunchWindow() {
        assertEquals(MealType.LUNCH, defaultMealTypeForTime(LocalTime.of(11, 0)))
        assertEquals(MealType.LUNCH, defaultMealTypeForTime(LocalTime.of(14, 59)))
    }

    @Test
    fun defaultMealTypeForTime_mapsDinnerWindow() {
        assertEquals(MealType.DINNER, defaultMealTypeForTime(LocalTime.of(18, 0)))
        assertEquals(MealType.DINNER, defaultMealTypeForTime(LocalTime.of(21, 59)))
    }

    @Test
    fun defaultMealTypeForTime_mapsSnackOutsideMainWindows() {
        assertEquals(MealType.SNACK, defaultMealTypeForTime(LocalTime.of(0, 15)))
        assertEquals(MealType.SNACK, defaultMealTypeForTime(LocalTime.of(15, 0)))
        assertEquals(MealType.SNACK, defaultMealTypeForTime(LocalTime.of(22, 30)))
    }
}
