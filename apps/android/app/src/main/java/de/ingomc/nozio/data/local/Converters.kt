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

    @TypeConverter
    fun fromSupplementDayPart(dayPart: SupplementDayPart): String = dayPart.name

    @TypeConverter
    fun toSupplementDayPart(value: String): SupplementDayPart = SupplementDayPart.valueOf(value)

    @TypeConverter
    fun fromSupplementAmountUnit(unit: SupplementAmountUnit): String = unit.name

    @TypeConverter
    fun toSupplementAmountUnit(value: String): SupplementAmountUnit = SupplementAmountUnit.valueOf(value)

    @TypeConverter
    fun fromRecipeAmountUnit(unit: RecipeAmountUnit): String = unit.name

    @TypeConverter
    fun toRecipeAmountUnit(value: String): RecipeAmountUnit = RecipeAmountUnit.valueOf(value)
}
