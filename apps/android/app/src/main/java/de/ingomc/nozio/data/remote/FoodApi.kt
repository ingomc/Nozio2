package de.ingomc.nozio.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FoodApi {
    @GET("v1/foods/search")
    suspend fun searchFoods(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): FoodSearchResponseDto

    @GET("v1/foods/barcode/{barcode}")
    suspend fun getFoodByBarcode(
        @Path("barcode") barcode: String
    ): FoodBarcodeResponseDto
}
