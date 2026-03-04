package de.ingomc.nozio.notifications

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MealReminderSchedulerTest {

    @Test
    fun `nextTriggerAtMillis schedules next day when time already passed`() {
        val now = LocalDateTime.of(2026, 3, 4, 20, 0)
        val triggerMillis = MealReminderScheduler.nextTriggerAtMillis(19, 30, now)
        val trigger = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(triggerMillis),
            ZoneId.systemDefault()
        )

        assertTrue(trigger.toLocalDate().isAfter(now.toLocalDate()))
    }

    @Test
    fun `nextTriggerAtMillis keeps same day when time is in future`() {
        val now = LocalDateTime.of(2026, 3, 4, 10, 0)
        val triggerMillis = MealReminderScheduler.nextTriggerAtMillis(19, 30, now)
        val trigger = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(triggerMillis),
            ZoneId.systemDefault()
        )

        assertTrue(trigger.toLocalDate().isEqual(now.toLocalDate()))
    }
}
