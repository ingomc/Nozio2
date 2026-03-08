package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.SupplementAmountUnit
import de.ingomc.nozio.data.local.SupplementDao
import de.ingomc.nozio.data.local.SupplementDayPart
import de.ingomc.nozio.data.local.SupplementIntakeDao
import de.ingomc.nozio.data.local.SupplementIntakeEntity
import de.ingomc.nozio.data.local.SupplementPlanItemEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplementRepositoryTest {

    @Test(expected = IllegalArgumentException::class)
    fun upsertPlanItem_rejectsBlankName() = runTest {
        val repository = SupplementRepository(FakeSupplementDao(), FakeSupplementIntakeDao())

        repository.upsertPlanItem(
            name = "   ",
            dayPart = SupplementDayPart.PRE_BREAKFAST,
            scheduledMinutesOfDay = 420,
            amountValue = 1.0,
            amountUnit = SupplementAmountUnit.CAPSULE
        )
    }

    @Test
    fun setTaken_writesAndDeletesIntakeForDate() = runTest {
        val supplementDao = FakeSupplementDao()
        val intakeDao = FakeSupplementIntakeDao()
        val repository = SupplementRepository(supplementDao, intakeDao)
        val supplementId = repository.upsertPlanItem(
            name = "Omega 3",
            dayPart = SupplementDayPart.MIDDAY,
            scheduledMinutesOfDay = 12 * 60,
            amountValue = 2.0,
            amountUnit = SupplementAmountUnit.CAPSULE
        )
        val date = LocalDate.parse("2026-03-08")

        repository.setTaken(date, supplementId, true)
        repository.setTaken(date, supplementId, true)

        val takenIdsAfterSet = repository.observeTakenSupplementIds(date).first()
        assertEquals(setOf(supplementId), takenIdsAfterSet)

        repository.setTaken(date, supplementId, false)

        val takenIdsAfterUnset = repository.observeTakenSupplementIds(date).first()
        assertTrue(takenIdsAfterUnset.isEmpty())
    }

    @Test
    fun setTaken_ignoresUnknownSupplementId() = runTest {
        val repository = SupplementRepository(FakeSupplementDao(), FakeSupplementIntakeDao())
        val date = LocalDate.parse("2026-03-08")

        repository.setTaken(date, supplementId = 999, taken = true)

        val ids = repository.observeTakenSupplementIds(date).first()
        assertTrue(ids.isEmpty())
    }
}

private class FakeSupplementDao : SupplementDao {
    private var nextId = 1L
    private val itemsById = linkedMapOf<Long, SupplementPlanItemEntity>()
    private val state = MutableStateFlow(emptyList<SupplementPlanItemEntity>())

    override fun observePlanItems(): Flow<List<SupplementPlanItemEntity>> = state

    override suspend fun getAllRaw(): List<SupplementPlanItemEntity> = state.value

    override suspend fun upsert(item: SupplementPlanItemEntity): Long {
        val id = if (item.id == 0L) nextId++ else item.id
        itemsById[id] = item.copy(id = id)
        emitCurrent()
        return id
    }

    override suspend fun insertAll(items: List<SupplementPlanItemEntity>) {
        items.forEach { item ->
            val id = if (item.id == 0L) nextId++ else item.id
            itemsById[id] = item.copy(id = id)
        }
        emitCurrent()
    }

    override suspend fun existsById(id: Long): Boolean = itemsById.containsKey(id)

    override suspend fun deleteById(id: Long) {
        itemsById.remove(id)
        emitCurrent()
    }

    override suspend fun deleteAll() {
        itemsById.clear()
        emitCurrent()
    }

    private fun emitCurrent() {
        state.value = itemsById.values.sortedWith(
            compareBy<SupplementPlanItemEntity> { it.scheduledMinutesOfDay }
                .thenBy { it.dayPart.sortOrder }
                .thenBy { it.id }
        )
    }
}

private class FakeSupplementIntakeDao : SupplementIntakeDao {
    private val state = MutableStateFlow(emptyList<SupplementIntakeEntity>())

    override fun observeTakenSupplementIds(date: LocalDate): Flow<List<Long>> {
        return state.map { items ->
            items.filter { it.date == date }.map { it.supplementId }
        }
    }

    override suspend fun upsert(intake: SupplementIntakeEntity) {
        val filtered = state.value.filterNot {
            it.date == intake.date && it.supplementId == intake.supplementId
        }
        state.value = (filtered + intake).sortedWith(
            compareBy<SupplementIntakeEntity> { it.date }.thenBy { it.supplementId }
        )
    }

    override suspend fun insertAll(items: List<SupplementIntakeEntity>) {
        state.value = items.sortedWith(
            compareBy<SupplementIntakeEntity> { it.date }.thenBy { it.supplementId }
        )
    }

    override suspend fun deleteForDate(date: LocalDate, supplementId: Long) {
        state.value = state.value.filterNot { it.date == date && it.supplementId == supplementId }
    }

    override suspend fun getAllRaw(): List<SupplementIntakeEntity> = state.value

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}
