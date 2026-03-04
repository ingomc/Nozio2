package de.ingomc.nozio.ui.search

import de.ingomc.nozio.data.local.MealType
import java.time.LocalTime

internal fun resolveInitialMealType(
    preselectedMealType: MealType?,
    localTime: LocalTime = LocalTime.now()
): MealType {
    return preselectedMealType ?: defaultMealTypeForTime(localTime)
}

internal fun defaultMealTypeForTime(localTime: LocalTime): MealType {
    val hour = localTime.hour
    return when (hour) {
        in 5..10 -> MealType.BREAKFAST
        in 11..14 -> MealType.LUNCH
        in 18..21 -> MealType.DINNER
        else -> MealType.SNACK
    }
}
