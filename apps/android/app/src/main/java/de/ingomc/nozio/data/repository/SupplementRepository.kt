package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.SupplementAmountUnit
import de.ingomc.nozio.data.local.SupplementDao
import de.ingomc.nozio.data.local.SupplementDayPart
import de.ingomc.nozio.data.local.SupplementIntakeDao
import de.ingomc.nozio.data.local.SupplementIntakeEntity
import de.ingomc.nozio.data.local.SupplementPlanItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class SupplementPlanItem(
    val id: Long = 0,
    val name: String,
    val dayPart: SupplementDayPart,
    val scheduledMinutesOfDay: Int,
    val amountValue: Double,
    val amountUnit: SupplementAmountUnit
)

data class SupplementTimelineItem(
    val id: Long,
    val name: String,
    val dayPart: SupplementDayPart,
    val scheduledMinutesOfDay: Int,
    val amountValue: Double,
    val amountUnit: SupplementAmountUnit,
    val isTaken: Boolean
)

class SupplementRepository(
    private val supplementDao: SupplementDao,
    private val supplementIntakeDao: SupplementIntakeDao
) {
    fun observePlanItems(): Flow<List<SupplementPlanItem>> {
        return supplementDao.observePlanItems().map { items ->
            items.map { it.toDomain() }
        }
    }

    fun observeTakenSupplementIds(date: LocalDate): Flow<Set<Long>> {
        return supplementIntakeDao.observeTakenSupplementIds(date).map { it.toSet() }
    }

    suspend fun upsertPlanItem(
        id: Long = 0,
        name: String,
        dayPart: SupplementDayPart,
        scheduledMinutesOfDay: Int,
        amountValue: Double,
        amountUnit: SupplementAmountUnit
    ): Long {
        val normalizedName = name.trim()
        require(normalizedName.isNotBlank()) { "Supplement name cannot be blank." }
        require(amountValue > 0.0) { "Supplement amount must be greater than zero." }
        require(scheduledMinutesOfDay in 0..1439) { "Supplement time is out of range." }

        return supplementDao.upsert(
            SupplementPlanItemEntity(
                id = id,
                name = normalizedName,
                dayPart = dayPart,
                scheduledMinutesOfDay = scheduledMinutesOfDay,
                amountValue = amountValue,
                amountUnit = amountUnit
            )
        )
    }

    suspend fun deletePlanItem(id: Long) {
        supplementDao.deleteById(id)
    }

    suspend fun setTaken(date: LocalDate, supplementId: Long, taken: Boolean) {
        if (!supplementDao.existsById(supplementId)) return
        if (taken) {
            supplementIntakeDao.upsert(
                SupplementIntakeEntity(
                    date = date,
                    supplementId = supplementId,
                    takenAtEpochMs = System.currentTimeMillis()
                )
            )
        } else {
            supplementIntakeDao.deleteForDate(date, supplementId)
        }
    }
}

private fun SupplementPlanItemEntity.toDomain(): SupplementPlanItem = SupplementPlanItem(
    id = id,
    name = name,
    dayPart = dayPart,
    scheduledMinutesOfDay = scheduledMinutesOfDay,
    amountValue = amountValue,
    amountUnit = amountUnit
)
