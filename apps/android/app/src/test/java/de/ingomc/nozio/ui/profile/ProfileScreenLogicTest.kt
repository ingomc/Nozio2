package de.ingomc.nozio.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProfileScreenLogicTest {

    @Test
    fun filterBodyMetricHistory_keepsInclusiveWindow() {
        val points = listOf(
            BodyMetricHistoryPoint(LocalDate.parse("2026-01-01"), 82.0, 22.0),
            BodyMetricHistoryPoint(LocalDate.parse("2026-01-15"), 81.3, 21.7),
            BodyMetricHistoryPoint(LocalDate.parse("2026-02-01"), 80.9, 21.2),
            BodyMetricHistoryPoint(LocalDate.parse("2026-02-14"), 80.1, 20.9)
        )

        val filtered = filterBodyMetricHistory(points, WeightRange.DAYS_14)

        assertEquals(2, filtered.size)
        assertEquals(LocalDate.parse("2026-02-01"), filtered.first().date)
        assertEquals(LocalDate.parse("2026-02-14"), filtered.last().date)
    }

    @Test
    fun filterBodyMetricHistory_withMultiplierExtendsWindow() {
        val points = listOf(
            BodyMetricHistoryPoint(LocalDate.parse("2026-01-01"), 82.0, 22.0),
            BodyMetricHistoryPoint(LocalDate.parse("2026-01-20"), 81.3, 21.7),
            BodyMetricHistoryPoint(LocalDate.parse("2026-02-10"), 80.9, 21.2),
            BodyMetricHistoryPoint(LocalDate.parse("2026-02-14"), 80.1, 20.9)
        )

        val filtered = filterBodyMetricHistory(points, WeightRange.DAYS_14, spanMultiplier = 3)

        assertEquals(3, filtered.size)
        assertEquals(LocalDate.parse("2026-01-20"), filtered.first().date)
        assertEquals(LocalDate.parse("2026-02-14"), filtered.last().date)
    }

    @Test
    fun buildXAxisTicks_containsStartAndEndWithoutDuplicates() {
        val ticks = buildXAxisTicks(
            start = LocalDate.parse("2026-01-01"),
            end = LocalDate.parse("2026-01-30"),
            range = WeightRange.DAYS_60
        )

        assertEquals(LocalDate.parse("2026-01-01"), ticks.first())
        assertEquals(LocalDate.parse("2026-01-30"), ticks.last())
        assertEquals(ticks.size, ticks.toSet().size)
    }

    @Test
    fun buildXAxisTicks_yearRange_usesMonthlyTicks() {
        val ticks = buildXAxisTicks(
            start = LocalDate.parse("2025-07-20"),
            end = LocalDate.parse("2026-03-01"),
            range = WeightRange.YEAR_1
        )

        assertTrue(ticks.contains(LocalDate.parse("2025-08-01")))
        assertTrue(ticks.contains(LocalDate.parse("2026-02-01")))
        assertEquals(LocalDate.parse("2025-07-20"), ticks.first())
        assertEquals(LocalDate.parse("2026-03-01"), ticks.last())
    }

    @Test
    fun aggregateChartPointsForRange_keepsDailyFor14Days() {
        val points = listOf(
            BodyMetricHistoryPoint(LocalDate.parse("2026-03-01"), 81.0, 21.0),
            BodyMetricHistoryPoint(LocalDate.parse("2026-03-02"), 80.8, 20.9)
        )

        val aggregated = aggregateChartPointsForRange(
            points = points,
            range = WeightRange.DAYS_14,
            windowStartDate = LocalDate.parse("2026-02-17"),
            latestDate = LocalDate.parse("2026-03-02")
        )

        assertEquals(2, aggregated.size)
        assertEquals(LocalDate.parse("2026-03-01"), aggregated[0].date)
        assertEquals(81.0, aggregated[0].weightKg ?: 0.0, 0.001)
    }

    @Test
    fun aggregateChartPointsForRange_usesWeeklyAverageFrom60Days() {
        val points = listOf(
            BodyMetricHistoryPoint(LocalDate.parse("2026-01-10"), 82.0, null),
            BodyMetricHistoryPoint(LocalDate.parse("2026-01-11"), 80.0, 22.0),
            BodyMetricHistoryPoint(LocalDate.parse("2026-01-12"), null, 20.0)
        )

        val aggregated = aggregateChartPointsForRange(
            points = points,
            range = WeightRange.DAYS_60,
            windowStartDate = LocalDate.parse("2026-01-07"),
            latestDate = LocalDate.parse("2026-01-14")
        )

        assertEquals(1, aggregated.size)
        assertEquals(81.0, aggregated[0].weightKg ?: 0.0, 0.001)
        assertEquals(21.0, aggregated[0].bodyFatPercent ?: 0.0, 0.001)
    }

    @Test
    fun aggregateChartPointsForRange_year1_usesMonthlyAverage() {
        val points = listOf(
            BodyMetricHistoryPoint(LocalDate.parse("2026-01-05"), 82.0, 22.0),
            BodyMetricHistoryPoint(LocalDate.parse("2026-01-20"), 80.0, 20.0),
            BodyMetricHistoryPoint(LocalDate.parse("2026-02-10"), 79.0, 19.0)
        )

        val aggregated = aggregateChartPointsForRange(
            points = points,
            range = WeightRange.YEAR_1,
            windowStartDate = LocalDate.parse("2025-03-01"),
            latestDate = LocalDate.parse("2026-02-28")
        )

        assertEquals(2, aggregated.size)
        assertEquals(LocalDate.parse("2026-01-20"), aggregated[0].date)
        assertEquals(81.0, aggregated[0].weightKg ?: 0.0, 0.001)
        assertEquals(21.0, aggregated[0].bodyFatPercent ?: 0.0, 0.001)
    }
}
