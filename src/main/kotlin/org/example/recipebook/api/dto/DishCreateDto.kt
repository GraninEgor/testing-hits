package org.example.recipebook.api.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.FeatureFlag

data class DishCreateDto(
    @field:NotBlank @field:Size(min = 2)
    val name: String,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    @field:Size(max = 5)
    val photos: List<String> = emptyList(),

    @field:NotNull @field:DecimalMin("0")
    val calories: Double,

    @field:NotNull @field:DecimalMin("0")
    val proteins: Double,

    @field:NotNull @field:DecimalMin("0")
    val fats: Double,

    @field:NotNull @field:DecimalMin("0")
    val carbohydrates: Double,

    @field:NotEmpty @field:Valid
    val ingredients: List<DishIngredientCreateDto>,

    @field:NotNull @field:Positive
    val portionSize: Double,

    @field:NotNull
    val category: DishCategory,

    @field:NotNull
    val flags: Set<FeatureFlag> = emptySet()
)