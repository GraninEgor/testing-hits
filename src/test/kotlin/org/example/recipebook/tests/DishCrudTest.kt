package org.example.recipebook.tests

import org.example.recipebook.pages.*
import org.example.recipebook.utils.WaitUtils
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.time.Duration

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DishCrudTest {

    companion object {
        private lateinit var driver: WebDriver
        private const val BASE_URL = "http://localhost:8080"

        private const val EXISTING_POTATO = "Картофель"
        private const val EXISTING_WATER = "Вода"
        private const val EXISTING_MEAT = "Мясо"
        private const val VEGAN_TEST_PRODUCT = "Тест-Веган-Продукт"

        @BeforeAll
        @JvmStatic
        fun setUpClass() {
            WebDriverManager.chromedriver().setup()
            val options = ChromeOptions().apply {
                addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080")
            }
            driver = ChromeDriver(options)
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2))
            driver.get(BASE_URL)
            Thread.sleep(2000)
            createVeganTestProduct()
        }

        private fun createVeganTestProduct() {
            val productsPage = ProductsPage(driver)
            productsPage.switchToProductsTab()
            if (!productsPage.hasProduct(VEGAN_TEST_PRODUCT, 3)) {
                productsPage
                    .fillProductForm(VEGAN_TEST_PRODUCT, 50, 2, 1, 8, "овощи, зелень", "VEGETABLES", "READY_TO_EAT")
                    .setProductFlags(vegan = true, glutenFree = false, sugarFree = false)
                    .saveProduct()
                Thread.sleep(500)
            }
        }

        @AfterAll
        @JvmStatic
        fun tearDownClass() {
            driver.quit()
        }
    }

    private lateinit var productsPage: ProductsPage
    private lateinit var dishesPage: DishesPage
    private val createdDishes = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        driver.navigate().refresh()
        Thread.sleep(1000)
        productsPage = ProductsPage(driver)
        dishesPage = DishesPage(driver)
        dishesPage.switchToDishesTab()
        createdDishes.clear()
    }

    @AfterEach
    fun cleanup() {
        createdDishes.forEach { name ->
            try {
                if (dishesPage.hasDish(name, 3)) {
                    dishesPage.clickDeleteDish(name)
                }
            } catch (_: Exception) {}
        }
        createdDishes.clear()
    }

    @Nested
    @Order(1)
    inner class CreateTests {

        @ParameterizedTest
        @CsvSource(
            "SECOND, 300, 250, 60, 30, 90",
            "FIRST, 400, 180, 12, 6, 25",
            "SALAD, 200, 150, 10, 5, 20",
            "SOUP, 350, 200, 15, 8, 30"
        )
        fun `should create dish with valid macros and category`(
            category: String,
            portion: Int,
            calories: Int,
            proteins: Int,
            fats: Int,
            carbs: Int
        ) {
            val productName = EXISTING_POTATO
            val dishName = "Блюдо-Тест-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(dishName, category, portion)
                .addIngredient(productName, 150)
                .fillDishMacros(calories, proteins, fats, carbs)
                .saveDish()
            assertTrue(dishesPage.hasDish(dishName, 20))
            createdDishes.add(dishName)
        }

        @ParameterizedTest
        @CsvSource(
            "true, true, true",
            "true, false, false",
            "false, true, false",
            "false, false, true"
        )
        fun `should create dish with different flag combinations`(
            vegan: Boolean,
            glutenFree: Boolean,
            sugarFree: Boolean
        ) {
            val productName = EXISTING_POTATO
            val dishName = "Флаги-Блюдо-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(dishName, "SALAD", 200)
                .addIngredient(productName, 100)
                .setDishFlags(vegan = vegan, glutenFree = glutenFree, sugarFree = sugarFree)
                .saveDish()
            assertTrue(dishesPage.hasDish(dishName, 20))
            createdDishes.add(dishName)
        }
    }

    @Nested
    @Order(2)
    inner class ReadTests {

        @ParameterizedTest
        @ValueSource(strings = ["Поиск", "Блюдо", "Тест"])
        fun `should find dish when search query matches part of name`(searchTerm: String) {
            val productName = EXISTING_WATER
            val dishName = "${searchTerm}-Блюдо-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(dishName, "SOUP", 250)
                .addIngredient(productName, 100)
                .saveDish()
            createdDishes.add(dishName)
            dishesPage.searchDishes(searchTerm)
            assertTrue(dishesPage.hasDish(dishName, 10))
        }

        @ParameterizedTest
        @CsvSource(
            "Десерт, DESSERT",
            "Первое, FIRST",
            "Второе, SECOND",
            "Салат, SALAD"
        )
        fun `should filter dishes correctly by category`(filterName: String, categoryValue: String) {
            val productName = EXISTING_POTATO
            val dishName = "Фильтр-${categoryValue}-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(dishName, categoryValue, 150)
                .addIngredient(productName, 100)
                .saveDish()
            createdDishes.add(dishName)
            dishesPage.filterByCategory(filterName)
            assertTrue(dishesPage.hasDish(dishName, 10))
        }
    }

    @Nested
    @Order(3)
    inner class UpdateTests {

        @ParameterizedTest
        @CsvSource(
            "FIRST, SECOND, 400, 500",
            "SALAD, DESSERT, 200, 250",
            "SOUP, FIRST, 300, 350"
        )
        fun `should update dish category and portion successfully`(
            originalCategory: String,
            updatedCategory: String,
            originalPortion: Int,
            updatedPortion: Int
        ) {
            val productName = EXISTING_MEAT
            val originalName = "Оригинал-${System.currentTimeMillis()}"
            val updatedName = "Обновлённый-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(originalName, originalCategory, originalPortion)
                .addIngredient(productName, 200)
                .fillDishMacros(180, 12, 6, 25)
                .saveDish()
            createdDishes.add(updatedName)
            assertTrue(dishesPage.hasDish(originalName, 10))
            dishesPage.clickEditDish(originalName)
                .fillBasicDishInfo(updatedName, updatedCategory, updatedPortion)
                .fillDishMacros(220, 18, 8, 30)
                .saveDish()
            assertFalse(dishesPage.hasDish(originalName, 5))
            assertTrue(dishesPage.hasDish(updatedName, 20))
        }

        @Test
        fun `should update dish ingredients successfully`() {
            val oldProduct = EXISTING_WATER
            val newProduct = EXISTING_POTATO
            val dishName = "Блюдо-Состав-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(dishName, "SNACK", 150)
                .addIngredient(oldProduct, 50)
                .saveDish()
            createdDishes.add(dishName)
            dishesPage.clickEditDish(dishName)
                .addIngredient(newProduct, 75)
                .saveDish()
            val detail = dishesPage.openDish(dishName)
            val detailText = driver.findElement(By.id("dish-detail")).text
            assertTrue(
                detailText.contains(newProduct) && detailText.contains("75 г")
            )
            detail.goBack()
        }
    }

    @Nested
    @Order(4)
    inner class DeleteTests {

        @Test
        fun `should delete dish via UI successfully`() {
            val productName = EXISTING_MEAT
            val dishName = "Удалить-Блюдо-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(dishName, "DRINK", 250)
                .addIngredient(productName, 100)
                .saveDish()
            assertTrue(dishesPage.hasDish(dishName, 10))
            dishesPage.clickDeleteDish(dishName)
            assertFalse(dishesPage.hasDish(dishName, 10))
        }
    }

    @Nested
    @Order(5)
    inner class MacrosValidationTests {

        @ParameterizedTest
        @CsvSource(
            "100, 500, 50, 40, 30",
            "150, 400, 45, 35, 25",
            "200, 300, 40, 30, 35"
        )
        fun `should show validation error when macros per 100g exceed 100`(
            portion: Int,
            calories: Int,
            proteins: Int,
            fats: Int,
            carbs: Int
        ) {
            val productName = EXISTING_MEAT
            val dishName = "БЖУ-Блюдо-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(dishName, "SECOND", portion)
                .addIngredient(productName, 100)
                .fillDishMacros(calories, proteins, fats, carbs)
                .saveDish()
            assertTrue(dishesPage.isElementVisible(By.id("d-name")))
        }

        @ParameterizedTest
        @CsvSource(
            "300, 300, 30, 20, 40",
            "250, 200, 25, 15, 30",
            "400, 350, 35, 25, 45"
        )
        fun `should create dish successfully when macros per 100g are valid`(
            portion: Int,
            calories: Int,
            proteins: Int,
            fats: Int,
            carbs: Int
        ) {
            val productName = EXISTING_POTATO
            val dishName = "Валид-Блюдо-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(dishName, "SECOND", portion)
                .addIngredient(productName, 150)
                .fillDishMacros(calories, proteins, fats, carbs)
                .saveDish()
            assertTrue(dishesPage.hasDish(dishName, 20))
            createdDishes.add(dishName)
        }
    }

    @Nested
    @Order(6)
    inner class FlagsLogicTests {

        @ParameterizedTest
        @ValueSource(strings = ["Мясо", "Вода"])
        fun `should disable vegan checkbox when non vegan ingredient is added`(nonVeganProduct: String) {
            val dishName = "Блюдо-Не-Веган-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(dishName, "FIRST", 400)
                .addIngredient(nonVeganProduct, 200)
            val veganCheckbox = driver.findElement(By.xpath("//input[@class='d-flag' and @value='VEGAN']"))
            assertTrue(veganCheckbox.isEnabled)
            dishesPage.cancelEdit()
        }

        @ParameterizedTest
        @ValueSource(strings = ["Картофель", "Тест-Веган-Продукт"])
        fun `should enable vegan checkbox when vegan ingredient is added`(veganProduct: String) {
            val dishName = "Блюдо-Веган-${System.currentTimeMillis()}"
            dishesPage
                .fillBasicDishInfo(dishName, "SALAD", 200)
                .addIngredient(veganProduct, 100)
            WaitUtils.waitForDebouncedUpdate()
            val veganCheckbox = driver.findElement(By.xpath("//input[@class='d-flag' and @value='VEGAN']"))
            assertFalse(veganCheckbox.isEnabled)
            dishesPage.cancelEdit()
        }
    }
}