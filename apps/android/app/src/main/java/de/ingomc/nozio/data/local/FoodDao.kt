package de.ingomc.nozio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodItem: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foodItems: List<FoodItem>): List<Long>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchByName(query: String): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getById(id: Long): FoodItem?

    @Query("SELECT * FROM food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): FoodItem?

    @Query("SELECT * FROM food_items ORDER BY id ASC")
    suspend fun getAll(): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE ASC LIMIT :limit")
    suspend fun getFavorites(limit: Int): List<FoodItem>

    @Query("UPDATE food_items SET isFavorite = :isFavorite WHERE id = :foodId")
    suspend fun setFavorite(foodId: Long, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWithIds(foodItems: List<FoodItem>)

    @Query("DELETE FROM food_items")
    suspend fun deleteAll()
}
