package de.ingomc.nozio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class MealTemplateIngredientWithFood(
    val ingredientId: Long,
    val templateId: Long,
    val foodItemId: Long,
    val position: Int,
    val defaultAmountValue: Double,
    val amountUnit: RecipeAmountUnit,
    val foodName: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val servingQuantity: Double?,
    val packageQuantity: Double?
)

data class MealTemplateWithIngredients(
    val id: Long,
    val name: String,
    val defaultMealType: MealType,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val ingredients: List<MealTemplateIngredientWithFood>
)

@Dao
interface MealTemplateDao {
    @Query(
        """
        SELECT 
            i.id AS ingredientId, i.templateId, i.foodItemId, i.position,
            i.defaultAmountValue, i.amountUnit,
            f.name AS foodName, f.caloriesPer100g, f.proteinPer100g,
            f.fatPer100g, f.carbsPer100g, f.servingQuantity, f.packageQuantity
        FROM meal_template_ingredients i
        INNER JOIN food_items f ON i.foodItemId = f.id
        WHERE i.templateId = :templateId
        ORDER BY i.position ASC
        """
    )
    suspend fun getIngredientsWithFood(templateId: Long): List<MealTemplateIngredientWithFood>

    @Query("SELECT * FROM meal_templates ORDER BY updatedAtEpochMs DESC")
    fun observeAllTemplates(): Flow<List<MealTemplateEntity>>

    @Query("SELECT * FROM meal_templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: Long): MealTemplateEntity?

    @Insert
    suspend fun insertTemplate(template: MealTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: MealTemplateEntity)

    @Query("DELETE FROM meal_templates WHERE id = :templateId")
    suspend fun deleteTemplate(templateId: Long)

    @Query("DELETE FROM meal_template_ingredients WHERE templateId = :templateId")
    suspend fun deleteIngredientsForTemplate(templateId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<MealTemplateIngredientEntity>)

    @Query("SELECT * FROM meal_templates ORDER BY id ASC")
    suspend fun getAllRaw(): List<MealTemplateEntity>

    @Query("SELECT * FROM meal_template_ingredients ORDER BY id ASC")
    suspend fun getAllIngredientsRaw(): List<MealTemplateIngredientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTemplates(templates: List<MealTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllIngredients(ingredients: List<MealTemplateIngredientEntity>)

    @Query("DELETE FROM meal_template_ingredients")
    suspend fun deleteAllIngredients()

    @Query("DELETE FROM meal_templates")
    suspend fun deleteAllTemplates()
}
