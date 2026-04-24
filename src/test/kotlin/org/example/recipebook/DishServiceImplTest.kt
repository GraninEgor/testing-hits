import com.fasterxml.jackson.databind.ObjectMapper
import org.example.recipebook.core.database.entity.*
import org.example.recipebook.core.database.repository.DishRepository
import org.example.recipebook.core.database.repository.ProductRepository
import org.example.recipebook.core.service.DishServiceImpl
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.extension.ExtendWith
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
@DisplayName("DishServiceImpl — расчет калорий")
class DishServiceImplTest {

    @Mock lateinit var dishRepository: DishRepository
    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var objectMapper: ObjectMapper

    @InjectMocks lateinit var service: DishServiceImpl

    private fun product(
        calories: Double,
        proteins: Double,
        fats: Double,
        carbs: Double
    ) = Product(
        name = "product",
        calories = calories,
        proteins = proteins,
        fats = fats,
        carbohydrates = carbs,
        category = Category.MEAT,
        cookingRequirement = CookingRequirement.READY_TO_EAT
    )

    private fun dish() = Dish(
        name = "dish",
        calories = 0.0,
        proteins = 0.0,
        fats = 0.0,
        carbohydrates = 0.0,
        portionSize = 100.0,
        category = DishCategory.SECOND
    )

    private fun ingredient(dish: Dish, product: Product, amount: Double) =
        DishIngredient(
            dish = dish,
            product = product,
            amount = amount
        )


    @ParameterizedTest
    @CsvSource(
        "0.0, 0.0",
        "100.0, 200.0",
        "50.0, 100.0",
        "0.001, 0.002",
        "1000000.0, 2000000.0"
    )
    fun `recalc single ingredient equivalence classes`(
        amount: Double,
        expectedCalories: Double
    ) {
        val dish = dish()
        val product = product(200.0, 0.0, 0.0, 0.0)

        dish.ingredients = mutableListOf(
            ingredient(dish, product, amount)
        )

        invokeRecalc(dish)

        assertEquals(expectedCalories, dish.calories, 1e-6)
    }

    @ParameterizedTest
    @MethodSource("boundaryValues")
    fun `recalc boundary values`(
        amount: Double,
        expectedCalories: Double
    ) {
        val dish = dish()
        val product = product(2.0, 0.0, 0.0, 0.0)

        dish.ingredients = mutableListOf(
            ingredient(dish, product, amount)
        )

        invokeRecalc(dish)

        assertEquals(expectedCalories, dish.calories, 1e-9)
    }

    companion object {
        @JvmStatic
        fun boundaryValues(): Stream<Arguments> = Stream.of(
            Arguments.of(0.0, 0.0),
            Arguments.of(0.001, 0.00002),
            Arguments.of(100.0, 2.0),
            Arguments.of(1_000_000.0, 20000.0)
        )
    }


    @Test
    fun `recalc multiple ingredients sums correctly`() {
        val dish = dish()

        val p1 = product(100.0, 10.0, 5.0, 20.0)
        val p2 = product(200.0, 20.0, 10.0, 40.0)

        dish.ingredients = mutableListOf(
            ingredient(dish, p1, 100.0),
            ingredient(dish, p2, 50.0)
        )

        invokeRecalc(dish)

        assertAll(
            { assertEquals(200.0, dish.calories) },
            { assertEquals(20.0, dish.proteins) },
            { assertEquals(10.0, dish.fats) },
            { assertEquals(40.0, dish.carbohydrates) }
        )
    }

    @Test
    fun `empty ingredients returns zero`() {
        val dish = dish()
        dish.ingredients = mutableListOf()

        invokeRecalc(dish)

        assertEquals(0.0, dish.calories)
    }



    @Test
    fun `validate exactly 100 is valid`() {
        val dish = dish().apply {
            proteins = 30.0
            fats = 30.0
            carbohydrates = 40.0
        }

        assertDoesNotThrow {
            invokeValidate(dish)
        }
    }


    private fun invokeRecalc(dish: Dish) {
        val m = service.javaClass.getDeclaredMethod("recalc", Dish::class.java)
        m.isAccessible = true
        m.invoke(service, dish)
    }

    private fun invokeValidate(dish: Dish) {
        val m = service.javaClass.getDeclaredMethod("validate", Dish::class.java)
        m.isAccessible = true
        m.invoke(service, dish)
    }
}