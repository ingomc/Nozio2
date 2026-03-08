package de.ingomc.nozio.ui.dashboard

import de.ingomc.nozio.data.local.SupplementAmountUnit
import de.ingomc.nozio.data.local.SupplementDayPart
import de.ingomc.nozio.data.repository.SupplementPlanItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DashboardViewModelTest {

    @Test
    fun resolveMetric_usesValueForSelectedDate_whenPresent() {
        val selectedDate = LocalDate.parse("2026-03-08")

        val resolved = resolveMetric(
            selectedDate = selectedDate,
            valueForDate = 79.2,
            fallbackEntry = 78.9 to LocalDate.parse("2026-03-06")
        )

        assertEquals(79.2, resolved.value ?: 0.0, 0.001)
        assertEquals(selectedDate, resolved.date)
        assertFalse(resolved.isFallback)
    }

    @Test
    fun resolveMetric_usesFallbackAndMarksSubdued_whenDateDiffers() {
        val selectedDate = LocalDate.parse("2026-03-08")
        val fallbackDate = LocalDate.parse("2026-03-06")

        val resolved = resolveMetric(
            selectedDate = selectedDate,
            valueForDate = null,
            fallbackEntry = 19.3 to fallbackDate
        )

        assertEquals(19.3, resolved.value ?: 0.0, 0.001)
        assertEquals(fallbackDate, resolved.date)
        assertTrue(resolved.isFallback)
    }

    @Test
    fun resolveMetric_returnsEmpty_whenNoCurrentOrFallbackValueExists() {
        val selectedDate = LocalDate.parse("2026-03-08")

        val resolved = resolveMetric(
            selectedDate = selectedDate,
            valueForDate = null,
            fallbackEntry = null
        )

        assertNull(resolved.value)
        assertNull(resolved.date)
        assertFalse(resolved.isFallback)
    }

    @Test
    fun buildSupplementTimelineItems_mapsTakenStateAndSortsChronologically() {
        val items = listOf(
            SupplementPlanItem(
                id = 2,
                name = "Magnesium",
                dayPart = SupplementDayPart.EVENING,
                scheduledMinutesOfDay = 20 * 60,
                amountValue = 1.0,
                amountUnit = SupplementAmountUnit.TABLET
            ),
            SupplementPlanItem(
                id = 1,
                name = "Omega 3",
                dayPart = SupplementDayPart.PRE_BREAKFAST,
                scheduledMinutesOfDay = 7 * 60,
                amountValue = 2.0,
                amountUnit = SupplementAmountUnit.CAPSULE
            )
        )

        val timeline = buildSupplementTimelineItems(
            planItems = items,
            takenIds = setOf(2L)
        )

        assertEquals(2, timeline.size)
        assertEquals(1L, timeline[0].id)
        assertFalse(timeline[0].isTaken)
        assertEquals(2L, timeline[1].id)
        assertTrue(timeline[1].isTaken)
    }

    @Test
    fun resolveSupplementTimelineInitialIndex_usesClosestItemForToday() {
        val selectedDate = LocalDate.now()
        val items = listOf(
            timelineItem(id = 1, minutes = 8 * 60),
            timelineItem(id = 2, minutes = 12 * 60 + 30),
            timelineItem(id = 3, minutes = 20 * 60)
        )

        val index = resolveSupplementTimelineInitialIndex(
            selectedDate = selectedDate,
            items = items,
            now = LocalTime.of(12, 40)
        )

        assertEquals(1, index)
    }

    @Test
    fun resolveSupplementTimelineInitialIndex_returnsZeroForOtherDates() {
        val items = listOf(
            timelineItem(id = 1, minutes = 8 * 60),
            timelineItem(id = 2, minutes = 12 * 60)
        )

        val index = resolveSupplementTimelineInitialIndex(
            selectedDate = LocalDate.parse("2026-03-01"),
            items = items,
            now = LocalTime.of(12, 0)
        )

        assertEquals(0, index)
    }
}

private fun timelineItem(id: Long, minutes: Int) = de.ingomc.nozio.data.repository.SupplementTimelineItem(
    id = id,
    name = "Item $id",
    dayPart = SupplementDayPart.PRE_BREAKFAST,
    scheduledMinutesOfDay = minutes,
    amountValue = 1.0,
    amountUnit = SupplementAmountUnit.CAPSULE,
    isTaken = false
)
