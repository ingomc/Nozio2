package de.ingomc.nozio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.ingomc.nozio.MainActivity
import de.ingomc.nozio.NozioApplication
import de.ingomc.nozio.R
import de.ingomc.nozio.WidgetLaunchAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

class CalorieWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            updateWidgets(context, appWidgetManager, appWidgetIds)
            pendingResult.finish()
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CalorieWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        }

        private suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val data = loadWidgetData(context)
            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_calorie_summary)
                views.setTextViewText(R.id.widget_goal_value, data.goal.toString())
                views.setTextViewText(R.id.widget_eaten_value, data.eaten.toString())
                views.setTextViewText(R.id.widget_burned_value, data.burned.toString())
                views.setTextViewText(R.id.widget_remaining_value, data.remaining.toString())
                views.setInt(
                    R.id.widget_remaining_value,
                    "setTextColor",
                    context.getColor(
                        if (data.remaining >= 0) R.color.widget_calorie_positive else R.color.widget_calorie_negative
                    )
                )

                val openAppIntent = MainActivity.createIntent(context, WidgetLaunchAction.NONE)
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    100,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

                val scanIntent = MainActivity.createIntent(context, WidgetLaunchAction.BARCODE_SCANNER)
                val scanPendingIntent = PendingIntent.getActivity(
                    context,
                    101,
                    scanIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_scan_button, scanPendingIntent)

                val addIntent = MainActivity.createIntent(context, WidgetLaunchAction.QUICK_ADD)
                val addPendingIntent = PendingIntent.getActivity(
                    context,
                    102,
                    addIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private suspend fun loadWidgetData(context: Context): WidgetCalorieData {
            val app = context.applicationContext as NozioApplication
            val today = LocalDate.now()
            val summary = app.diaryRepository.getDaySummary(today).first()
            val preferences = app.userPreferencesRepository.userPreferences.first()
            val totalSteps = app.dailyActivityRepository.getStepsForDate(today).first()
            val burned = estimateActiveCalories(totalSteps, preferences.currentWeightKg, preferences.bodyFatPercent)
            val goal = preferences.calorieGoal.roundToInt()
            val eaten = summary.totalCalories.roundToInt()
            val burnedRounded = burned.roundToInt()
            return WidgetCalorieData(
                goal = goal,
                eaten = eaten,
                burned = burnedRounded,
                remaining = goal - eaten + burnedRounded
            )
        }

        private fun estimateActiveCalories(steps: Long, weightKg: Double, bodyFatPercent: Double): Double {
            val safeWeightKg = weightKg.coerceIn(35.0, 250.0)
            val safeBodyFat = bodyFatPercent.coerceIn(3.0, 60.0)
            val leanMassKg = safeWeightKg * (1.0 - safeBodyFat / 100.0)
            val kcalPerStep = 0.015 + (0.00057 * leanMassKg)
            return (steps * kcalPerStep).coerceAtLeast(0.0)
        }
    }
}

private data class WidgetCalorieData(
    val goal: Int,
    val eaten: Int,
    val burned: Int,
    val remaining: Int
)
