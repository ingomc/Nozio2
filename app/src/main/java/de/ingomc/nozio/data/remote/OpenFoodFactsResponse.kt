package de.ingomc.nozio.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsResponse(
    val count: Int = 0,
    val products: List<OpenFoodFactsProduct> = emptyList()
)

@Serializable
data class OpenFoodFactsProduct(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("brands") val brands: String? = null,
    @SerialName("code") val barcode: String? = null,
    @SerialName("nutriments") val nutriments: Nutriments? = null,
    @SerialName("product_name_de") val productNameDe: String? = null
)

@Serializable
data class Nutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null
)

