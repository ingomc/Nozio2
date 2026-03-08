package de.ingomc.nozio.ui.profile

import de.ingomc.nozio.data.repository.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ProfileViewModelTest {

    @Test
    fun computeWeightTrend_returnsNullWhenHistoryTooShort() {
        val summary = computeWeightTrend(
            weightHistory = listOf(
                WeightHistoryPoint(date = LocalDate.parse("2026-03-01"), weightKg = 80.0)
            ),
            goalWeightKg = 78.0,
            latestWeightKg = 80.0
        )

        assertNull(summary.deltaKg)
        assertNull(summary.weeksEstimate)
    }

    @Test
    fun computeWeightTrend_calculatesDeltaAndWeeksEstimate() {
        val summary = computeWeightTrend(
            weightHistory = listOf(
                WeightHistoryPoint(date = LocalDate.parse("2026-01-01"), weightKg = 82.0),
                WeightHistoryPoint(date = LocalDate.parse("2026-03-01"), weightKg = 80.0)
            ),
            goalWeightKg = 78.0,
            latestWeightKg = 80.0
        )

        assertEquals(-2.0, summary.deltaKg ?: 0.0, 0.001)
        assertNotNull(summary.weeksEstimate)
    }

    @Test
    fun buildGoalSummaryItems_containsConfiguredGoalsAndSteps() {
        val preferences = UserPreferences(
            calorieGoal = 2354.0,
            proteinGoal = 180.0,
            fatGoal = 70.0,
            carbsGoal = 210.0,
            currentWeightKg = 79.0
        )

        val items = buildGoalSummaryItems(preferences, todaySteps = 8123)

        assertEquals(4, items.size)
        assertEquals("Kalorien: 2354 kcal", items[0])
        assertEquals("Heute Schritte: 8123", items.last())
    }
}
