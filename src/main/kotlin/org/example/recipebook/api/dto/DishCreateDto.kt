package org.example.recipebook.api.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.FeatureFlag

data class DishCreateDto(
    @field:Size(min = 2) @field:NotBlank val name: String,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val photos: List<String> = emptyList(),

    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbohydrates: Double,
    val ingredients: List<DishIngredientCreateDto>,
    val portionSize: Double,
    @field:NotNull val category: DishCategory,
    val flags: Set<FeatureFlag>
)