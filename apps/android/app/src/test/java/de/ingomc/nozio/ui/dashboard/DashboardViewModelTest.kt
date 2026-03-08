package de.ingomc.nozio.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
}
