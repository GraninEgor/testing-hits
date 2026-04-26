package org.example.recipebook.core.database.repository

import org.example.recipebook.core.database.entity.DishIngredient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DishIngredientRepository : JpaRepository<DishIngredient, Long> {
    // Можно добавить кастомные методы при необходимости
    fun deleteByDishId(dishId: Long): Long
}