package org.example.recipebook.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.example.recipebook.core.database.entity.Category
import org.example.recipebook.core.database.entity.CookingRequirement
import org.example.recipebook.core.database.entity.FeatureFlag
import java.time.LocalDateTime

data class ProductDto(
    val id: Long,
    val name: String,
    val photos: List<String>,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbohydrates: Double,
    val composition: String?,
    val category: Category,
    val cookingRequirement: CookingRequirement,
    val flags: Set<FeatureFlag>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)