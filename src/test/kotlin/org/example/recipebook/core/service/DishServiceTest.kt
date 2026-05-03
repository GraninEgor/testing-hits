package org.example.recipebook.core.service

import org.example.recipebook.api.dto.DishIngredientCreateDto
import org.example.recipebook.core.database.entity.Category
import org.example.recipebook.core.database.entity.CookingRequirement
import org.example.recipebook.core.database.entity.Product
import org.example.recipebook.core.database.repository.ProductRepository
import org.example.recipebook.core.exception.ProductNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DisplayName("Dish Service Tests")
@ExtendWith(MockitoExtension::class)
class DishServiceTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var dishService: DishServiceImpl

    @BeforeEach
    fun setUp() {
        productRepository = mock()
        dishService = DishServiceImpl(
            dishRepository = mock(),
            objectMapper = mock(),
            productRepository = productRepository,
            dishIngredientRepository = mock()
        )
    }

    @Test
    @DisplayName("empty list returns zero")
    fun `should return zero when ingredients list is empty`() {
        // ARRANGE
        val emptyComposition = emptyList<DishIngredientCreateDto>()

        // ACT
        val result = dishService.calculateCalories(emptyComposition)

         // ASSERT
        assertAll(
            "All are zero",
            { assertEquals(0.0, result.calories, "calories") },
            { assertEquals(0.0, result.protein, "protein") },
            { assertEquals(0.0, result.fat, "fat") },
            { assertEquals(0.0, result.carbs, "carbs") }
        )
    }

    @ParameterizedTest(name = "amount = {0} -> calories = {1}, protein = {2}, fat = {3}, carbs = {4}")
    @CsvSource(
        delimiter = '|',
        value = [
            "150.0   | 270.0   | 18.0    | 9.0     | 37.5",
            "42.5    | 76.5    | 5.1     | 2.55    | 10.625",
            "0.0     | 0.0     | 0.0     | 0.0     | 0.0",
            "2.5e-10 | 4.5e-10 | 3.0e-11 | 1.5e-11 | 6.25e-10",
            "5e9     | 9e9     | 6e8     | 3e8     | 1.25e9"
        ]
    )
    fun `should calculate correct for single product with amount {0}`(
        amount: Double,
        expectedCal: Double,
        expectedPro: Double,
        expectedFat: Double,
        expectedCarb: Double
    ) {
        // ARRANGE
        val productId = 1L
        val product = createTestProduct(
            id = productId,
            name = "Test Product Alpha",
            calories = 180.0,
            proteins = 12.0,
            fats = 6.0,
            carbohydrates = 25.0
        )
        whenever(productRepository.findById(productId)).thenReturn(Optional.of(product))

        val composition = listOf(DishIngredientCreateDto(productId, amount))

        // ACT
        val result = dishService.calculateCalories(composition)

         // ASSERT
        val delta = 1e-9
        assertAll(
            "Verify for amount = $amount",
            { assertEquals(expectedCal, result.calories, delta, "calories") },
            { assertEquals(expectedPro, result.protein, delta, "protein") },
            { assertEquals(expectedFat, result.fat, delta, "fat") },
            { assertEquals(expectedCarb, result.carbs, delta, "carbs") }
        )
    }

    @Test
    @DisplayName("correct calculation for multiple products")
    fun `should sum correctly when multiple products are provided`() {
        // ARRANGE
        val product1 = createTestProduct(
            id = 1L,
            name = "Product 1",
            calories = 120.0,
            proteins = 6.0,
            fats = 3.0,
            carbohydrates = 18.0
        )
        val product2 = createTestProduct(
            id = 20L,
            name = "Product 2",
            calories = 250.0,
            proteins = 12.0,
            fats = 8.0,
            carbohydrates = 35.0
        )

        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product1))
        whenever(productRepository.findById(20L)).thenReturn(Optional.of(product2))

        val composition = listOf(
            DishIngredientCreateDto(10L, 100.0),
            DishIngredientCreateDto(20L, 50.0)
        )

        // ACT
        val result = dishService.calculateCalories(composition)

        // calories = 120.0×1.0 + 250.0×0.5 = 120 + 125 = 245.0
        // proteins = 6.0×1.0 + 12.0×0.5 = 6 + 6 = 12.0
        // fats = 3.0×1.0 + 8.0×0.5 = 3 + 4 = 7.0
        // carbs = 18.0×1.0 + 35.0×0.5 = 18 + 17.5 = 35.5
        val expectedCal = 245.0
        val expectedPro = 12.0
        val expectedFat = 7.0
        val expectedCarb = 35.5

         // ASSERT
        assertAll(
            "Verify sum",
            { assertEquals(expectedCal, result.calories, "calories") },
            { assertEquals(expectedPro, result.protein, "protein") },
            { assertEquals(expectedFat, result.fat, "fat") },
            { assertEquals(expectedCarb, result.carbs, "carbs") }
        )
    }

    @ParameterizedTest(name = "Non-existent ID = {0}")
    @CsvSource(value = ["888", "-5", "99999"])
    fun `should throw ProductNotFoundException when product with id {0} is not found`(productId: Long) {
        whenever(productRepository.findById(productId)).thenReturn(Optional.empty())

        val composition = listOf(DishIngredientCreateDto(productId, 100.0))

        val exception = assertFailsWith<ProductNotFoundException> {
            dishService.calculateCalories(composition)
        }
        assertEquals("Product not found: $productId", exception.message)
    }

    @Test
    @DisplayName("throws exception when one product is missing")
    fun `should throw exception when at least one product in list is not found`() {
        // ARRANGE
        val validId = 1L
        val invalidId = 2L

        val validProduct = createTestProduct(
            id = validId,
            name = "Valid Product",
            calories = 120.0,
            proteins = 6.0,
            fats = 3.0,
            carbohydrates = 18.0
        )

        whenever(productRepository.findById(validId)).thenReturn(Optional.of(validProduct))
        whenever(productRepository.findById(invalidId)).thenReturn(Optional.empty())

        val composition = listOf(
            DishIngredientCreateDto(validId, 50.0),
            DishIngredientCreateDto(invalidId, 30.0)
        )

        // ACT & ASSERT
        val exception = assertFailsWith<ProductNotFoundException> {
            dishService.calculateCalories(composition)
        }
        assertEquals("Product not found: $invalidId", exception.message)
    }

    @ParameterizedTest(name = "amount = {0} (boundary value)")
    @MethodSource("realisticBoundaryValues")
    fun `should handle boundary values correctly for amount {0}`(
        amount: Double,
        expectedCal: Double,
        expectedPro: Double,
        expectedFat: Double,
        expectedCarb: Double
    ) {
        // ARRANGE
        val productId = 10L
        val product = createTestProduct(
            id = productId,
            name = "Boundary Product",
            calories = 3.0,
            proteins = 0.2,
            fats = 0.08,
            carbohydrates = 0.4
        )
        whenever(productRepository.findById(productId)).thenReturn(Optional.of(product))

        val composition = listOf(DishIngredientCreateDto(productId, amount))

        // ACT
        val result = dishService.calculateCalories(composition)

         // ASSERT
        val delta = 1e-12
        assertAll(
            "Boundary values",
            { assertEquals(expectedCal, result.calories, delta, "calories") },
            { assertEquals(expectedPro, result.protein, delta, "protein") },
            { assertEquals(expectedFat, result.fat, delta, "fat") },
            { assertEquals(expectedCarb, result.carbs, delta, "carbs") }
        )
    }

    companion object {
        @JvmStatic
        fun realisticBoundaryValues(): Stream<Arguments> = Stream.of(
            Arguments.of(0.0, 0.0, 0.0, 0.0, 0.0),
            Arguments.of(0.001, 3.0E-5, 2.0E-6, 8.0E-7, 4.0E-6),
            Arguments.of(2_000_000.0, 60_000.0, 4_000.0, 1_600.0, 8_000.0)
        )

        private fun createTestProduct(
            id: Long,
            name: String,
            calories: Double,
            proteins: Double,
            fats: Double,
            carbohydrates: Double,
            category: Category = Category.VEGETABLES,
            cookingRequirement: CookingRequirement = CookingRequirement.READY_TO_EAT
        ): Product {
            return Product(
                id = id,
                name = name,
                calories = calories,
                proteins = proteins,
                fats = fats,
                carbohydrates = carbohydrates,
                category = category,
                cookingRequirement = cookingRequirement
            )
        }
    }
}