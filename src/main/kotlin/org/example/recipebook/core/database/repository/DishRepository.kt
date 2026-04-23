package org.example.recipebook.core.database.repository

import org.example.recipebook.core.database.entity.Dish
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.Optional

interface DishRepository : JpaRepository<Dish, Long>, JpaSpecificationExecutor<Dish> {
    fun existsByIngredientsProductId(productId: Long): Boolean
}