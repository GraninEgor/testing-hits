import org.example.recipebook.api.dto.DishIngredientCreateDto
import org.example.recipebook.core.database.entity.Category
import org.example.recipebook.core.database.entity.CookingRequirement
import org.example.recipebook.core.database.entity.Product
import org.example.recipebook.core.database.repository.ProductRepository
import org.example.recipebook.core.exception.ProductNotFoundException
import org.example.recipebook.core.service.DishServiceImpl
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
class DishServiceImplTest {

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
    @DisplayName("empty list returns zero macros")
    fun `should return zero macros when ingredients list is empty`() {
        val emptyComposition = emptyList<DishIngredientCreateDto>()
        val result = dishService.calculateCalories(emptyComposition)

        assertAll("All macros are zero",
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
            "150.0   | 300.0   | 15.0    | 7.5     | 45.0",
            "33.33   | 66.66   | 3.333   | 1.6665  | 9.999",
            "0.0     | 0.0     | 0.0     | 0.0     | 0.0",
            "1e-10   | 2e-10   | 1e-11   | 5e-12   | 3e-10",
            "1e10    | 2e10    | 1e9     | 5e8     | 3e9"
        ]
    )
    fun `should calculate correct macros for single product with amount {0}`(
        amount: Double,
        expectedCal: Double,
        expectedPro: Double,
        expectedFat: Double,
        expectedCarb: Double
    ) {
        // Arrange
        val productId = 1L
        val product = createTestProduct(
            id = productId,
            name = "Test Product",
            calories = 200.0,
            proteins = 10.0,
            fats = 5.0,
            carbohydrates = 30.0
        )
        whenever(productRepository.findById(productId)).thenReturn(Optional.of(product))

        val composition = listOf(DishIngredientCreateDto(productId, amount))

        // Act
        val result = dishService.calculateCalories(composition)

        // Assert
        val delta = 1e-9
        assertAll("Verify macros for amount = $amount",
            { assertEquals(expectedCal, result.calories, delta, "calories") },
            { assertEquals(expectedPro, result.protein, delta, "protein") },
            { assertEquals(expectedFat, result.fat, delta, "fat") },
            { assertEquals(expectedCarb, result.carbs, delta, "carbs") }
        )
    }

    @Test
    @DisplayName("correct calculation for multiple products")
    fun `should sum macros correctly when multiple products are provided`() {
        // Arrange
        val product1 = createTestProduct(
            id = 1L,
            name = "Product One",
            calories = 100.0,
            proteins = 5.0,
            fats = 2.0,
            carbohydrates = 20.0
        )
        val product2 = createTestProduct(
            id = 2L,
            name = "Product Two",
            calories = 300.0,
            proteins = 15.0,
            fats = 10.0,
            carbohydrates = 40.0
        )

        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product1))
        whenever(productRepository.findById(2L)).thenReturn(Optional.of(product2))

        val composition = listOf(
            DishIngredientCreateDto(1L, 100.0),
            DishIngredientCreateDto(2L, 50.0)
        )

        // Act
        val result = dishService.calculateCalories(composition)

        // Expected
        val expectedCal = 100.0 + 300.0 * 0.5
        val expectedPro = 5.0 + 15.0 * 0.5
        val expectedFat = 2.0 + 10.0 * 0.5
        val expectedCarb = 20.0 + 40.0 * 0.5

        // Assert
        assertAll("Verify sum of macros",
            { assertEquals(expectedCal, result.calories, "calories") },
            { assertEquals(expectedPro, result.protein, "protein") },
            { assertEquals(expectedFat, result.fat, "fat") },
            { assertEquals(expectedCarb, result.carbs, "carbs") }
        )
    }

    @ParameterizedTest(name = "Non-existent ID = {0}")
    @CsvSource(value = ["999", "-1", "123456789"])
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
        // Arrange
        val validId = 1L
        val invalidId = 999L

        val validProduct = createTestProduct(
            id = validId,
            name = "Valid Product",
            calories = 100.0,
            proteins = 5.0,
            fats = 2.0,
            carbohydrates = 20.0
        )

        whenever(productRepository.findById(validId)).thenReturn(Optional.of(validProduct))
        whenever(productRepository.findById(invalidId)).thenReturn(Optional.empty())

        val composition = listOf(
            DishIngredientCreateDto(validId, 50.0),
            DishIngredientCreateDto(invalidId, 30.0)
        )

        // Act & Assert
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
        // Arrange
        val productId = 1L
        val product = createTestProduct(
            id = productId,
            name = "Boundary Product",
            calories = 2.0,
            proteins = 0.1,
            fats = 0.05,
            carbohydrates = 0.3
        )
        whenever(productRepository.findById(productId)).thenReturn(Optional.of(product))

        val composition = listOf(DishIngredientCreateDto(productId, amount))

        // Act
        val result = dishService.calculateCalories(composition)

        // Assert
        val delta = 1e-12
        assertAll("Boundary values",
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
            Arguments.of(0.001, 2.0E-5, 1.0E-6, 5.0E-7, 3.0E-6),
            Arguments.of(1_000_000.0, 20_000.0, 1_000.0, 500.0, 3_000.0)
        )

        fun createTestProduct(
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