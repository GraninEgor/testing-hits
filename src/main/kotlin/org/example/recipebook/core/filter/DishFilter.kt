package org.example.recipebook.core.filter

import org.example.recipebook.core.database.entity.Dish
import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.DishIngredient
import org.example.recipebook.core.database.entity.FeatureFlag
import org.example.recipebook.core.database.entity.Product
import org.springframework.data.jpa.domain.Specification

data class DishFilter(
    val name: String? = null,
    val category: DishCategory? = null,
    val calories: Double? = null,
    val proteins: Double? = null,
    val fats: Double? = null,
    val carbohydrates: Double? = null,
    val flags: Set<FeatureFlag>? = null
) {
    fun toSpecification(): Specification<Dish> =
        Specification.where(nameSpec())
            .and(categorySpec())
            .and(caloriesSpec())
            .and(proteinsSpec())
            .and(fatsSpec())
            .and(carbsSpec())
            .and(flagsSpec())

    private fun nameSpec() = Specification<Dish> { root, _, cb ->
        name?.let {
            cb.like(cb.lower(root.get("name")), "%${it.lowercase()}%")
        }
    }

    private fun categorySpec() = Specification<Dish> { root, _, cb ->
        category?.let {
            cb.equal(root.get<DishCategory>("category"), it)
        }
    }

    private fun caloriesSpec() = Specification<Dish> { root, _, cb ->
        calories?.let {
            cb.equal(root.get<Double>("calories"), it)
        }
    }

    private fun proteinsSpec() = Specification<Dish> { root, _, cb ->
        proteins?.let {
            cb.equal(root.get<Double>("proteins"), it)
        }
    }

    private fun fatsSpec() = Specification<Dish> { root, _, cb ->
        fats?.let {
            cb.equal(root.get<Double>("fats"), it)
        }
    }

    private fun carbsSpec() = Specification<Dish> { root, _, cb ->
        carbohydrates?.let {
            cb.equal(root.get<Double>("carbohydrates"), it)
        }
    }

    private fun flagsSpec() = Specification<Dish> { root, _, cb ->
        flags?.let {
            root.join<Dish, FeatureFlag>("flags").`in`(it)
        }
    }
}