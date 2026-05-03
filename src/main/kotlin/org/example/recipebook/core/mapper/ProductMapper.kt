package org.example.recipebook.core.mapper

import org.example.recipebook.api.dto.ProductCreateDto
import org.example.recipebook.core.database.entity.FeatureFlag
import org.example.recipebook.core.database.entity.Product
import org.example.recipebook.api.dto.ProductDto

fun Product.toProductDto() = ProductDto(
    id = this.id,
    name = this.name,
    photos = this.photos,
    calories = this.calories,
    proteins = this.proteins,
    fats = this.fats,
    carbohydrates = this.carbohydrates,
    composition = this.composition,
    category = this.category,
    cookingRequirement = this.cookingRequirement,
    flags = this.flags,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt ?: createdAt
)

fun ProductDto.toEntity() = Product(
    name = this.name,
    photos = this.photos,
    calories = this.calories,
    proteins = this.proteins,
    fats = this.fats,
    carbohydrates = this.carbohydrates,
    composition = this.composition,
    category = this.category,
    cookingRequirement = this.cookingRequirement,
    flags = this.flags
)

fun ProductCreateDto.toEntity() = Product(
    name = this.name,
    photos = this.photos,
    calories = this.calories,
    proteins = this.proteins,
    fats = this.fats,
    carbohydrates = this.carbohydrates,
    composition = this.composition,
    category = this.category,
    cookingRequirement = this.cookingRequirement,
    flags = this.flags
)

fun Product.updateWithNull(productDto: ProductDto) = apply {
    name = productDto.name
    photos = productDto.photos
    calories = productDto.calories
    proteins = productDto.proteins
    fats = productDto.fats
    carbohydrates = productDto.carbohydrates
    composition = productDto.composition
    category = productDto.category
    cookingRequirement = productDto.cookingRequirement
    flags = productDto.flags
}