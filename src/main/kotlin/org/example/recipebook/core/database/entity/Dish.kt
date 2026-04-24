package org.example.recipebook.core.database.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.LocalDateTime

@Entity
@Table(name = "dishes")
class Dish(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @field:NotBlank
    @field:Size(min = 2)
    @Column(nullable = false)
    var name: String,

    @ElementCollection
    @CollectionTable(name = "dish_photos", joinColumns = [JoinColumn(name = "dish_id")])
    @Column(name = "photo_url")
    @field:Size(max = 5)
    var photos: List<@NotBlank String> = emptyList(),

    @field:NotNull
    @field:DecimalMin("0.0")
    @Column(nullable = false)
    var calories: Double,

    @field:NotNull
    @field:DecimalMin("0.0")
    @Column(nullable = false)
    var proteins: Double,

    @field:NotNull
    @field:DecimalMin("0.0")
    @Column(nullable = false)
    var fats: Double,

    @field:NotNull
    @field:DecimalMin("0.0")
    @Column(nullable = false)
    var carbohydrates: Double,

    @OneToMany(mappedBy = "dish", cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:Size(min = 1)
    var ingredients: MutableList<DishIngredient> = mutableListOf(),

    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false)
    var portionSize: Double,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: DishCategory,

    @ElementCollection(targetClass = FeatureFlag::class)
    @CollectionTable(name = "dish_flags", joinColumns = [JoinColumn(name = "dish_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "flag")
    var flags: Set<FeatureFlag> = emptySet(),

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime? = null
) {

    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

enum class DishCategory {
    DESSERT,
    FIRST,
    SECOND,
    DRINK,
    SALAD,
    SOUP,
    SNACK
}