package org.example.recipebook.api.dto

data class DishIngredientDto(
    val productId: Long,
    val productName: String,
    val amount: Double
)