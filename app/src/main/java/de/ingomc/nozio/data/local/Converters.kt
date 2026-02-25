package de.ingomc.nozio.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(dateString: String): LocalDate = LocalDate.parse(dateString)

    @TypeConverter
    fun fromMealType(mealType: MealType): String = mealType.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun fromFoodSource(source: FoodSource): String = source.name

    @TypeConverter
    fun toFoodSource(value: String): FoodSource = FoodSource.valueOf(value)
}

