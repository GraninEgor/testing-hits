package org.example.recipebook.api.dto

data class DishIngredientCreateDto(
    val productId: Long,
    val amount: Double
)