package de.ingomc.nozio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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

open class CalorieWidgetProvider : BaseCalorieWidgetProvider(WidgetVariant.COMPACT) {
    companion object {
        suspend fun updateAll(context: Context) {
            BaseCalorieWidgetProvider.updateAll(context)
        }
    }
}

class ExpandedCalorieWidgetProvider : BaseCalorieWidgetProvider(WidgetVariant.EXPANDED)

enum class WidgetVariant(
    val layoutResId: Int,
    val ringSizeDp: Float,
    val ringStrokeWidthDp: Float
) {
    COMPACT(
        layoutResId = R.layout.widget_calorie_summary,
        ringSizeDp = 64f,
        ringStrokeWidthDp = 4f
    ),
    EXPANDED(
        layoutResId = R.layout.widget_calorie_summary_expanded,
        ringSizeDp = 136f,
        ringStrokeWidthDp = 8f
    )
}

abstract class BaseCalorieWidgetProvider(
    private val variant: WidgetVariant
) : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            updateWidgets(context, appWidgetManager, appWidgetIds, variant)
            pendingResult.finish()
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            updateVariant(
                context = context,
                appWidgetManager = appWidgetManager,
                providerClass = CalorieWidgetProvider::class.java,
                variant = WidgetVariant.COMPACT
            )
            updateVariant(
                context = context,
                appWidgetManager = appWidgetManager,
                providerClass = ExpandedCalorieWidgetProvider::class.java,
                variant = WidgetVariant.EXPANDED
            )
        }

        private suspend fun updateVariant(
            context: Context,
            appWidgetManager: AppWidgetManager,
            providerClass: Class<out AppWidgetProvider>,
            variant: WidgetVariant
        ) {
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, providerClass))
            if (appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds, variant)
            }
        }

        private suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            variant: WidgetVariant
        ) {
            val data = loadWidgetData(context)
            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, variant.layoutResId)
                views.setTextViewText(R.id.widget_eaten_value, data.eaten.toString())
                views.setTextViewText(R.id.widget_burned_value, data.burned.toString())
                views.setTextViewText(R.id.widget_remaining_value, data.remaining.toString())
                views.setProgressBar(R.id.widget_carbs_progress, 100, data.carbsProgress, false)
                views.setProgressBar(R.id.widget_protein_progress, 100, data.proteinProgress, false)
                views.setProgressBar(R.id.widget_fat_progress, 100, data.fatProgress, false)
                if (variant == WidgetVariant.EXPANDED) {
                    views.setTextViewText(R.id.widget_carbs_value, data.carbsText)
                    views.setTextViewText(R.id.widget_protein_value, data.proteinText)
                    views.setTextViewText(R.id.widget_fat_value, data.fatText)
                }
                views.setImageViewBitmap(
                    R.id.widget_remaining_ring,
                    createRemainingRingBitmap(
                        context = context,
                        progressPercent = data.remainingProgress,
                        isPositive = data.remaining >= 0,
                        sizeDp = variant.ringSizeDp,
                        strokeWidthDp = variant.ringStrokeWidthDp
                    )
                )
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

                val addIntent = MainActivity.createIntent(context, WidgetLaunchAction.SEARCH_FOCUS)
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
            val remaining = goal - eaten + burnedRounded
            val remainingProgress = if (goal > 0) {
                ((remaining.coerceAtLeast(0).toDouble() / goal.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            } else {
                0
            }
            return WidgetCalorieData(
                eaten = eaten,
                burned = burnedRounded,
                remaining = remaining,
                remainingProgress = remainingProgress,
                carbsProgress = progressPercent(summary.totalCarbs, preferences.carbsGoal),
                proteinProgress = progressPercent(summary.totalProtein, preferences.proteinGoal),
                fatProgress = progressPercent(summary.totalFat, preferences.fatGoal),
                carbsText = macroValueText(summary.totalCarbs, preferences.carbsGoal),
                proteinText = macroValueText(summary.totalProtein, preferences.proteinGoal),
                fatText = macroValueText(summary.totalFat, preferences.fatGoal)
            )
        }

        private fun estimateActiveCalories(steps: Long, weightKg: Double, bodyFatPercent: Double): Double {
            val safeWeightKg = weightKg.coerceIn(35.0, 250.0)
            val safeBodyFat = bodyFatPercent.coerceIn(3.0, 60.0)
            val leanMassKg = safeWeightKg * (1.0 - safeBodyFat / 100.0)
            val kcalPerStep = 0.015 + (0.00057 * leanMassKg)
            return (steps * kcalPerStep).coerceAtLeast(0.0)
        }

        private fun progressPercent(value: Double, goal: Double): Int {
            if (goal <= 0.0) return 0
            return ((value / goal) * 100.0).roundToInt().coerceIn(0, 100)
        }

        private fun macroValueText(value: Double, goal: Double): String {
            return "${value.roundToInt()} / ${goal.roundToInt()} g"
        }

        private fun createRemainingRingBitmap(
            context: Context,
            progressPercent: Int,
            isPositive: Boolean,
            sizeDp: Float,
            strokeWidthDp: Float
        ): Bitmap {
            val sizePx = context.dpToPx(sizeDp)
            val strokeWidthPx = context.dpToPx(strokeWidthDp).toFloat()
            val inset = strokeWidthPx / 2f + context.dpToPx(6f)
            val startAngle = 140f
            val sweepAngle = 260f
            val progressSweep = sweepAngle * (progressPercent.coerceIn(0, 100) / 100f)

            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val oval = RectF(inset, inset, sizePx - inset, sizePx - inset)

            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = context.getColor(R.color.widget_calorie_border)
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                strokeCap = Paint.Cap.ROUND
            }
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = context.getColor(
                    if (isPositive) R.color.widget_calorie_eaten else R.color.widget_calorie_negative
                )
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                strokeCap = Paint.Cap.ROUND
            }

            canvas.drawArc(oval, startAngle, sweepAngle, false, trackPaint)
            if (progressSweep > 0f) {
                canvas.drawArc(oval, startAngle, progressSweep, false, progressPaint)
            }
            return bitmap
        }

        private fun Context.dpToPx(dp: Float): Int {
            return (dp * resources.displayMetrics.density).roundToInt()
        }
    }
}

private data class WidgetCalorieData(
    val eaten: Int,
    val burned: Int,
    val remaining: Int,
    val remainingProgress: Int,
    val carbsProgress: Int,
    val proteinProgress: Int,
    val fatProgress: Int,
    val carbsText: String,
    val proteinText: String,
    val fatText: String
)
