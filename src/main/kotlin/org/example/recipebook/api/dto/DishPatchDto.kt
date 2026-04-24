package org.example.recipebook.api.dto

import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.FeatureFlag

data class DishPatchDto(
    val name: String? = null,
    val photos: List<String>? = null,

    val calories: Double? = null,
    val proteins: Double? = null,
    val fats: Double? = null,
    val carbohydrates: Double? = null,

    val ingredients: List<DishIngredientCreateDto>? = null,
    val portionSize: Double? = null,

    val category: DishCategory? = null,
    val flags: Set<FeatureFlag>? = null
)