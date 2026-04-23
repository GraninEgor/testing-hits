package org.example.recipebook.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.example.recipebook.core.database.entity.Category
import org.example.recipebook.core.database.entity.CookingRequirement
import org.example.recipebook.core.database.entity.FeatureFlag

data class ProductCreateDto(
    @field:Size(min = 2) @field:NotBlank val name: String,
    val photos: List<String> = emptyList(),
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbohydrates: Double,
    val composition: String?,
    @field:NotNull val category: Category,
    @field:NotNull val cookingRequirement: CookingRequirement,
    val flags: Set<FeatureFlag> = emptySet()
)