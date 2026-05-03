package org.example.recipebook.api.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class DishIngredientCreateDto(
    @field:NotNull
    val productId: Long,

    @field:NotNull @field:Positive
    val amount: Double
)