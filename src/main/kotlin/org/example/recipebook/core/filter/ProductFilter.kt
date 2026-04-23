package org.example.recipebook.core.filter

import org.example.recipebook.core.database.entity.*
import org.springframework.data.jpa.domain.Specification

data class ProductFilter(
    val category: Category? = null,
    val name: String? = null,
    val calories: Double? = null,
    val cookingRequirement: CookingRequirement? = null,
    val proteins: Double? = null,
    val fats: Double? = null,
    val carbohydrates: Double? = null,
    val composition: String? = null,
    val flags: Set<FeatureFlag>? = null
) {

    fun toSpecification(): Specification<Product> {
        val specs = listOfNotNull(
            categorySpec(),
            nameSpec(),
            caloriesSpec(),
            cookingRequirementSpec(),
            proteinsSpec(),
            fatsSpec(),
            carbohydratesSpec(),
            compositionSpec(),
            flagsSpec()
        )

        return specs.reduceOrNull { acc, spec -> acc.and(spec) }
            ?: Specification { _, _, _ -> null }
    }

    private fun categorySpec() = Specification<Product> { root, _, cb ->
        category?.let {
            cb.equal(root.get<Category>("category"), it)
        }
    }

    private fun nameSpec() = Specification<Product> { root, _, cb ->
        name?.takeIf { it.isNotBlank() }?.let {
            cb.like(root.get("name"), "%$it%")
        }
    }

    private fun caloriesSpec() = Specification<Product> { root, _, cb ->
        calories?.let {
            cb.equal(root.get<Double>("calories"), it)
        }
    }

    private fun cookingRequirementSpec() = Specification<Product> { root, _, cb ->
        cookingRequirement?.let {
            cb.equal(root.get<CookingRequirement>("cookingRequirement"), it)
        }
    }

    private fun proteinsSpec() = Specification<Product> { root, _, cb ->
        proteins?.let {
            cb.equal(root.get<Double>("proteins"), it)
        }
    }

    private fun fatsSpec() = Specification<Product> { root, _, cb ->
        fats?.let {
            cb.equal(root.get<Double>("fats"), it)
        }
    }

    private fun carbohydratesSpec() = Specification<Product> { root, _, cb ->
        carbohydrates?.let {
            cb.equal(root.get<Double>("carbohydrates"), it)
        }
    }

    private fun compositionSpec() = Specification<Product> { root, _, cb ->
        composition?.takeIf { it.isNotBlank() }?.let {
            cb.like(root.get("composition"), "%$it%")
        }
    }

    private fun flagsSpec() = Specification<Product> { root, _, cb ->
        flags?.takeIf { it.isNotEmpty() }?.let {
            root.get<FeatureFlag>("flags").`in`(it)
        }
    }
}