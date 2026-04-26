package org.example.recipebook.core.database.entity

import jakarta.persistence.*

import java.time.LocalDateTime
import jakarta.validation.constraints.*

@Entity
@Table(name = "products")
class Product(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @field:NotBlank
    @field:Size(min = 2)
    @Column(nullable = false)
    var name: String,

    @ElementCollection
    @CollectionTable(name = "product_photos", joinColumns = [JoinColumn(name = "product_id")])
    @Column(name = "photo_url")
    @field:Size(max = 5)
    var photos: List<@NotBlank String> = emptyList(),

    @field:NotNull
    @field:DecimalMin("0.0", inclusive = true)
    @Column(nullable = false)
    var calories: Double,

    @field:NotNull
    @field:DecimalMin("0.0", inclusive = true)
    @field:DecimalMax("100.0", inclusive = true)
    @Column(nullable = false)
    var proteins: Double,

    @field:NotNull
    @field:DecimalMin("0.0", inclusive = true)
    @field:DecimalMax("100.0", inclusive = true)
    @Column(nullable = false)
    var fats: Double,

    @field:NotNull
    @field:DecimalMin("0.0", inclusive = true)
    @field:DecimalMax("100.0", inclusive = true)
    @Column(nullable = false)
    var carbohydrates: Double,

    @Column(columnDefinition = "TEXT", length = 500)
    var composition: String? = null,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: Category,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var cookingRequirement: CookingRequirement,

    @ElementCollection(targetClass = FeatureFlag::class)
    @CollectionTable(name = "product_flags", joinColumns = [JoinColumn(name = "product_id")])
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

enum class Category {
    FROZEN,
    MEAT,
    VEGETABLES,
    GREENS,
    SPICES,
    GRAINS,
    CANNED,
    LIQUID,
    SWEETS
}

enum class CookingRequirement {
    READY_TO_EAT,
    SEMI_FINISHED,
    REQUIRES_COOKING
}

enum class FeatureFlag {
    VEGAN,
    GLUTEN_FREE,
    SUGAR_FREE
}