package de.ingomc.nozio.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") searchTerms: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 30,
        @Query("countries_tags_en") countriesTags: String = "germany",
        @Query("fields") fields: String = "product_name,product_name_de,brands,code,nutriments"
    ): OpenFoodFactsResponse
}

