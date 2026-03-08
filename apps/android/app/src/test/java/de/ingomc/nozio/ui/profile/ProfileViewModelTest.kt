package de.ingomc.nozio.ui.profile

import de.ingomc.nozio.data.repository.BodyFatEntry
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.WeightEntry
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

    @Test
    fun mergeBodyMetricHistory_mergesByDateAndSortsAscending() {
        val merged = mergeBodyMetricHistory(
            weightHistory = listOf(
                WeightEntry(LocalDate.parse("2026-03-02"), 81.1),
                WeightEntry(LocalDate.parse("2026-03-04"), 80.7)
            ),
            bodyFatHistory = listOf(
                BodyFatEntry(LocalDate.parse("2026-03-01"), 21.4),
                BodyFatEntry(LocalDate.parse("2026-03-04"), 21.0)
            )
        )

        assertEquals(3, merged.size)
        assertEquals(LocalDate.parse("2026-03-01"), merged[0].date)
        assertEquals(null, merged[0].weightKg)
        assertEquals(21.4, merged[0].bodyFatPercent ?: 0.0, 0.001)
        assertEquals(LocalDate.parse("2026-03-04"), merged[2].date)
        assertEquals(80.7, merged[2].weightKg ?: 0.0, 0.001)
        assertEquals(21.0, merged[2].bodyFatPercent ?: 0.0, 0.001)
    }
}
