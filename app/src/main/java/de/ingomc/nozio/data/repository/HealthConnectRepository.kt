package de.ingomc.nozio.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId

class HealthConnectRepository(context: Context) {

    private val healthConnectClient: HealthConnectClient? = when (
        HealthConnectClient.getSdkStatus(context)
    ) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectClient.getOrCreate(context)
        else -> null
    }

    private suspend fun hasPermissions(permissions: Set<String>): Boolean {
        val client = healthConnectClient ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun getTodaySteps(): Long {
        val client = healthConnectClient ?: return 0L
        val now = Instant.now()
        val startOfDay = now.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class)
        )

        if (!hasPermissions(permissions)) {
            return 0L
        }

        return try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun getTodayActiveCalories(): Double {
        val client = healthConnectClient ?: return 0.0
        val now = Instant.now()
        val startOfDay = now.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val permissions = setOf(
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
        )

        if (!hasPermissions(permissions)) {
            return 0.0
        }

        return try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}
