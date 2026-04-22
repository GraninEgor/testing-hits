package org.example.recipebook.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.FeatureFlag
import java.time.LocalDateTime

/**
 * DTO for [org.example.recipebook.core.database.entity.Dish]
 */
data class DishDto(
    @field:Size(min = 2) @field:NotBlank val name: String,
    @field:Size(max = 5) val photos: List<String>,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbohydrates: Double,
    val ingredientIds: MutableList<Long>,
    val portionSize: Double,
    @field:NotNull val category: DishCategory,
    val flags: Set<FeatureFlag>,
    val createdAt: LocalDateTime
)