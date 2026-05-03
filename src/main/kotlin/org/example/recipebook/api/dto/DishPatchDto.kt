package org.example.recipebook.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.FeatureFlag

data class DishPatchDto(
    @field:Size(min = 2)
    val name: String? = null,

    @field:Size(max = 5)
    val photos: List<String>? = null,

    @field:DecimalMin("0")
    val calories: Double? = null,

    @field:DecimalMin("0")
    val proteins: Double? = null,

    @field:DecimalMin("0")
    val fats: Double? = null,

    @field:DecimalMin("0")
    val carbohydrates: Double? = null,

    @field:Valid
    val ingredients: List<DishIngredientCreateDto>? = null,

    @field:Positive
    val portionSize: Double? = null,

    val category: DishCategory? = null,

    val flags: Set<FeatureFlag>? = null
)