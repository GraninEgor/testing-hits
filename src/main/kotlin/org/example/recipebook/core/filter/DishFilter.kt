package org.example.recipebook.core.filter

import org.example.recipebook.core.database.entity.Dish
import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.DishIngredient
import org.example.recipebook.core.database.entity.FeatureFlag
import org.example.recipebook.core.database.entity.Product
import org.springframework.data.jpa.domain.Specification

data class DishFilter(
    val id: Long = 0,
    val name: String,
    val photos: MutableList<String>,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbohydrates: Double,
    val ingredients: MutableList<DishIngredient> = mutableListOf(),
    val ingredientsId: Long? = null,
    val ingredientsDish: Dish? = null,
    val ingredientsProduct: Product? = null,
    val ingredientsAmount: Double? = null,
    val portionSize: Double,
    val category: DishCategory,
    val flags: MutableSet<FeatureFlag>
) {
    fun toSpecification() = idSpec()
        .and(nameSpec())
        .and(caloriesSpec())
        .and(proteinsSpec())
        .and(fatsSpec())
        .and(carbohydratesSpec())
        .and(ingredientsSpec())
        .and(ingredientsIdSpec())
        .and(ingredientsDishSpec())
        .and(ingredientsProductSpec())
        .and(ingredientsAmountSpec())
        .and(portionSizeSpec())
        .and(categorySpec())
        .and(flagsSpec())

    private fun idSpec() = Specification<Dish> { root, _, cb ->
        cb.equal(root.get<Long>("id"), id)
    }

    private fun nameSpec() = Specification<Dish> { root, _, cb ->
        name.takeIf(String::isNotBlank)?.let {
            cb.equal(root.get<String>("name"), it)
        }
    }

    private fun caloriesSpec() = Specification<Dish> { root, _, cb ->
        cb.equal(root.get<Double>("calories"), calories)
    }

    private fun proteinsSpec() = Specification<Dish> { root, _, cb ->
        cb.equal(root.get<Double>("proteins"), proteins)
    }

    private fun fatsSpec() = Specification<Dish> { root, _, cb ->
        cb.equal(root.get<Double>("fats"), fats)
    }

    private fun carbohydratesSpec() = Specification<Dish> { root, _, cb ->
        cb.equal(root.get<Double>("carbohydrates"), carbohydrates)
    }

    private fun ingredientsSpec() = Specification<Dish> { root, _, cb ->
        cb.equal(root.get<DishIngredient>("ingredients"), ingredients)
    }

    private fun ingredientsIdSpec() = Specification<Dish> { root, _, cb ->
        ingredientsId?.let {
            cb.equal(root.get<Any>("ingredients").get<Long>("id"), it)
        }
    }

    private fun ingredientsDishSpec() = Specification<Dish> { root, _, cb ->
        ingredientsDish?.let {
            cb.equal(root.get<Any>("ingredients").get<Dish>("dish"), it)
        }
    }

    private fun ingredientsProductSpec() = Specification<Dish> { root, _, cb ->
        ingredientsProduct?.let {
            cb.equal(root.get<Any>("ingredients").get<Product>("product"), it)
        }
    }

    private fun ingredientsAmountSpec() = Specification<Dish> { root, _, cb ->
        ingredientsAmount?.let {
            cb.equal(root.get<Any>("ingredients").get<Double>("amount"), it)
        }
    }

    private fun portionSizeSpec() = Specification<Dish> { root, _, cb ->
        cb.equal(root.get<Double>("portionSize"), portionSize)
    }

    private fun categorySpec() = Specification<Dish> { root, _, cb ->
        cb.equal(root.get<DishCategory>("category"), category)
    }

    private fun flagsSpec() = Specification<Dish> { root, _, cb ->
        cb.equal(root.get<FeatureFlag>("flags"), flags)
    }
}