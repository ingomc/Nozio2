package de.ingomc.nozio.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import de.ingomc.nozio.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("x-api-key", BuildConfig.FOOD_API_KEY)
                .build()
            chain.proceed(request)
        }
        .build()

    val api: FoodApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.FOOD_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FoodApi::class.java)
    }
}
