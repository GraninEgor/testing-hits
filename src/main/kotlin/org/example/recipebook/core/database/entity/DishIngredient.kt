package org.example.recipebook.core.database.entity

import jakarta.persistence.*
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "dish_ingredients")
class DishIngredient(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    var dish: Dish,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false)
    var amount: Double
)