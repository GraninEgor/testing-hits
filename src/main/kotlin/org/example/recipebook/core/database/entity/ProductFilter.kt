package org.example.recipebook.core.database.entity

import org.springframework.data.jpa.domain.Specification

data class ProductFilter(val name: String, val category: Category, val calories: Double) {
    fun toSpecification() = nameSpec()
        .and(categorySpec())
        .and(caloriesSpec())

    private fun nameSpec() = Specification<Product> { root, _, cb ->
        name.takeIf(String::isNotBlank)?.let {
            cb.equal(root.get<String>("name"), it)
        }
    }

    private fun categorySpec() = Specification<Product> { root, _, cb ->
        cb.equal(root.get<Category>("category"), category)
    }

    private fun caloriesSpec() = Specification<Product> { root, _, cb ->
        cb.equal(root.get<Double>("calories"), calories)
    }
}