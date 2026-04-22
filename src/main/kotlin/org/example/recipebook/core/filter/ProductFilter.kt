 package org.example.recipebook.core.filter

 import org.example.recipebook.core.database.entity.Category
 import org.example.recipebook.core.database.entity.CookingRequirement
 import org.example.recipebook.core.database.entity.FeatureFlag
 import org.example.recipebook.core.database.entity.Product
 import org.springframework.data.jpa.domain.Specification

 data class ProductFilter(
     val category: Category,
     val name: String,
     val calories: Double,
     val cookingRequirement: CookingRequirement,
     val proteins: Double,
     val fats: Double,
     val carbohydrates: Double,
     val composition: String? = null,
     val flags: Set<FeatureFlag> = emptySet()
 ) {
     fun toSpecification() = categorySpec()
     .and(nameSpec())
     .and(caloriesSpec())
     .and(cookingRequirementSpec())
     .and(proteinsSpec())
     .and(fatsSpec())
     .and(carbohydratesSpec())
     .and(compositionSpec())
     .and(flagsSpec())

     private fun categorySpec() = Specification<Product> { root, _, cb ->
         cb.equal(root.get<Category>("category"), category)
     }

     private fun nameSpec() = Specification<Product> { root, _, cb ->
                     name.takeIf(String::isNotBlank)?.let {
     cb.equal(root.get<String>("name"), it)
     }
                 }

     private fun caloriesSpec() = Specification<Product> { root, _, cb ->
         cb.equal(root.get<Double>("calories"), calories)
     }

     private fun cookingRequirementSpec() = Specification<Product> { root, _, cb ->
         cb.equal(root.get<CookingRequirement>("cookingRequirement"), cookingRequirement)
     }

     private fun proteinsSpec() = Specification<Product> { root, _, cb ->
         cb.equal(root.get<Double>("proteins"), proteins)
     }

     private fun fatsSpec() = Specification<Product> { root, _, cb ->
         cb.equal(root.get<Double>("fats"), fats)
     }

     private fun carbohydratesSpec() = Specification<Product> { root, _, cb ->
         cb.equal(root.get<Double>("carbohydrates"), carbohydrates)
     }

     private fun compositionSpec() = Specification<Product> { root, _, cb ->
                     composition?.takeIf(String::isNotBlank)?.let {
     cb.equal(root.get<String>("composition"), it)
     }
                 }

     private fun flagsSpec() = Specification<Product> { root, _, cb ->
         cb.equal(root.get<FeatureFlag>("flags"), flags)
     }
 }