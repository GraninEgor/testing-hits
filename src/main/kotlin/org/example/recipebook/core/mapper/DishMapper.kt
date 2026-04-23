package org.example.recipebook.core.mapper

import org.example.recipebook.api.dto.DishCreateDto
import org.example.recipebook.api.dto.DishDto
import org.example.recipebook.api.dto.DishIngredientDto
import org.example.recipebook.core.database.entity.Dish
import org.example.recipebook.core.database.entity.DishIngredient
import org.example.recipebook.core.database.entity.FeatureFlag

fun Dish.toDishDto() = DishDto(
    id = this.id,
    name = this.name,
    photos = this.photos,
    calories = this.calories,
    proteins = this.proteins,
    fats = this.fats,
    carbohydrates = this.carbohydrates,
    ingredients = this.ingredients.map {
        DishIngredientDto(
            productId = it.product.id!!,
            productName = it.product.name,
            amount = it.amount
        )
    },
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

fun DishCreateDto.toEntity(): Dish {
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