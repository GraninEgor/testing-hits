package org.example.recipebook.api.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.example.recipebook.core.database.entity.Category
import org.example.recipebook.core.database.entity.CookingRequirement
import org.example.recipebook.core.database.entity.FeatureFlag

data class ProductCreateDto(
    @field:NotBlank @field:Size(min = 3)
    val name: String,

    @field:Size(max = 5)
    val photos: List<String> = emptyList(),

    @field:NotNull @field:DecimalMin("0")
    val calories: Double,

    @field:NotNull @field:DecimalMin("0") @field:DecimalMax("100")
    val proteins: Double,

    @field:NotNull @field:DecimalMin("0") @field:DecimalMax("100")
    val fats: Double,

    @field:NotNull @field:DecimalMin("0") @field:DecimalMax("100")
    val carbohydrates: Double,

    @field:Size(max = 500, message = "Состав не должен превышать 500 символов")
    val composition: String? = null,

    @field:NotNull
    val category: Category,

    @field:NotNull
    val cookingRequirement: CookingRequirement,

    @field:NotNull
    val flags: Set<FeatureFlag> = emptySet()
)