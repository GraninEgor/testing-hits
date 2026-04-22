package org.example.recipebook.core.mapper

import org.example.recipebook.api.dto.DishDto
import org.example.recipebook.core.database.entity.Dish
import org.example.recipebook.core.database.entity.DishIngredient
import org.example.recipebook.core.database.entity.FeatureFlag

fun Dish.toDishDto() = DishDto(
    name = this.name,
    photos = this.photos,
    calories = this.calories,
    proteins = this.proteins,
    fats = this.fats,
    carbohydrates = this.carbohydrates,
    ingredientIds = ingredients.mapNotNull { it.id }.toMutableList(),
    portionSize = this.portionSize,
    category = this.category,
    flags = this.flags,
    createdAt = this.createdAt
)

fun DishDto.toEntity(): Dish {
    val dish = Dish(
        name = name,
        photos = photos,
        calories = calories,
        proteins = proteins,
        fats = fats,
        carbohydrates = carbohydrates,
        portionSize = portionSize,
        category = category,
        flags = flags
    )
    return dish
}

fun Dish.updateWithNull(dto: DishDto) = apply {
    name = dto.name
    photos = dto.photos
    calories = dto.calories
    proteins = dto.proteins
    fats = dto.fats
    carbohydrates = dto.carbohydrates
    portionSize = dto.portionSize
    category = dto.category
    flags = dto.flags
}