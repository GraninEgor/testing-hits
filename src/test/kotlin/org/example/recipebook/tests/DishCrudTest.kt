package org.example.recipebook.tests

import org.example.recipebook.pages.*
import org.example.recipebook.utils.WaitUtils
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.time.Duration

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("🍽️ CRUD тесты блюд")
class DishCrudTest {

    companion object {
        private lateinit var driver: WebDriver
        private const val BASE_URL = "http://localhost:8080"

        // 🔹 Продукты, которые точно есть
        private const val EXISTING_POTATO = "Картофель"
        private const val EXISTING_WATER = "Вода"
        private const val EXISTING_MEAT = "Мясо"

        // 🔹 НОВЫЙ: имя веган-продукта, который создадим
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

            // 🔹 Создаём веган-продукт для тестов флагов
            createVeganTestProduct()
        }

        // 🔹 Вспомогательный метод создания веган-продукта
        private fun createVeganTestProduct() {
            val productsPage = ProductsPage(driver)
            productsPage.switchToProductsTab()

            // Если продукт ещё не создан — создаём его
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
        // 🔹 Перезагружаем для чистого состояния
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
    @DisplayName("✅ Создание блюда")
    inner class CreateTests {

        @Test
        @Order(1)
        fun createDish_withIngredients_success() {
            val productName = EXISTING_POTATO // 🔹 Точно есть в списке!
            val dishName = "Блюдо-Тест-${System.currentTimeMillis()}"

            dishesPage
                .fillBasicDishInfo(dishName, "SECOND", 300)
                .addIngredient(productName, 150)
                .fillDishMacros(250, 60, 30, 90)
                .saveDish()

            assertTrue(dishesPage.hasDish(dishName, 20), "Блюдо не отображается")
            createdDishes.add(dishName)
        }

        @Test
        @Order(2)
        fun createDish_withFlags_success() {
            val productName = EXISTING_POTATO
            val dishName = "Веган-Блюдо-${System.currentTimeMillis()}"

            dishesPage
                .fillBasicDishInfo(dishName, "SALAD", 200)
                .addIngredient(productName, 100)
                .setDishFlags(vegan = true, glutenFree = true, sugarFree = false)
                .saveDish()

            assertTrue(dishesPage.hasDish(dishName, 20))
            createdDishes.add(dishName)
        }
    }

    @Nested
    @DisplayName("🔍 Поиск и фильтрация блюд")
    inner class ReadTests {

        @Test
        @Order(3)
        fun searchDish_byName_findsCorrectly() {
            val productName = EXISTING_WATER
            val dishName = "Поиск-Блюдо-${System.currentTimeMillis()}"

            dishesPage
                .fillBasicDishInfo(dishName, "SOUP", 250)
                .addIngredient(productName, 100)
                .saveDish()
            createdDishes.add(dishName)

            dishesPage.searchDishes("Поиск-Блюдо")
            assertTrue(dishesPage.hasDish(dishName, 10), "Поиск не нашёл блюдо")
        }

        @Test
        @Order(4)
        fun filterDishes_byCategory_showsOnlyMatching() {
            val productName = EXISTING_POTATO
            val dishName = "Фильтр-Блюдо-${System.currentTimeMillis()}"

            dishesPage
                .fillBasicDishInfo(dishName, "DESSERT", 150)
                .addIngredient(productName, 100)
                .saveDish()
            createdDishes.add(dishName)

            dishesPage.filterByCategory("Десерт")
            assertTrue(dishesPage.hasDish(dishName, 10), "Фильтрация не показала блюдо")
        }
    }

    @Nested
    @DisplayName("✏️ Обновление блюда")
    inner class UpdateTests {

        @Test
        @Order(5)
        fun updateDish_basicFields_success() {
            val productName = EXISTING_MEAT
            val originalName = "Оригинал-${System.currentTimeMillis()}"
            val updatedName = "Обновлённый-${System.currentTimeMillis()}"

            dishesPage
                .fillBasicDishInfo(originalName, "FIRST", 400)
                .addIngredient(productName, 200)
                .fillDishMacros(180, 12, 6, 25)
                .saveDish()
            createdDishes.add(updatedName)

            assertTrue(dishesPage.hasDish(originalName, 10))

            dishesPage.clickEditDish(originalName)
                .fillBasicDishInfo(updatedName, "SECOND", 500)
                .fillDishMacros(220, 18, 8, 30)
                .saveDish()

            assertFalse(dishesPage.hasDish(originalName, 5))
            assertTrue(dishesPage.hasDish(updatedName, 20))
        }

        @Test
        @Order(6)
        fun updateDish_ingredients_success() {
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
                detailText.contains(newProduct) && detailText.contains("75 г"),
                "Ингредиент '$newProduct (75 г)' не найден"
            )
            detail.goBack()
        }
    }

    @Nested
    @DisplayName("🗑️ Удаление блюда")
    inner class DeleteTests {

        @Test
        @Order(7)
        fun deleteDish_viaUI_success() {
            val productName = EXISTING_MEAT
            val dishName = "Удалить-Блюдо-${System.currentTimeMillis()}"

            dishesPage
                .fillBasicDishInfo(dishName, "DRINK", 250)
                .addIngredient(productName, 100)
                .saveDish()

            assertTrue(dishesPage.hasDish(dishName, 10))
            dishesPage.clickDeleteDish(dishName)
            assertFalse(dishesPage.hasDish(dishName, 10), "Блюдо не удалено")
        }
    }

    @Nested
    @DisplayName("📊 Валидация БЖУ на порцию")
    inner class MacrosValidationTests {

        @Test
        @Order(8)
        fun createDish_macrosPer100gOver100_validationError() {
            val productName = EXISTING_MEAT
            val dishName = "БЖУ-Блюдо-${System.currentTimeMillis()}"

            dishesPage
                .fillBasicDishInfo(dishName, "SECOND", 100)
                .addIngredient(productName, 100)
                .fillDishMacros(500, 50, 40, 30)
                .saveDish()

            assertTrue(dishesPage.isElementVisible(By.id("d-name")),
                "Форма должна остаться при ошибке валидации")
        }

        @Test
        @Order(9)
        fun createDish_validMacrosPer100g_success() {
            val productName = EXISTING_POTATO
            val dishName = "Валид-Блюдо-${System.currentTimeMillis()}"

            dishesPage
                .fillBasicDishInfo(dishName, "SECOND", 300)
                .addIngredient(productName, 150)
                .fillDishMacros(300, 30, 20, 40)
                .saveDish()

            assertTrue(dishesPage.hasDish(dishName, 20))
            createdDishes.add(dishName)
        }
    }

    @Nested
    @DisplayName("Логика флагов блюда")
    inner class FlagsLogicTests {

        @Test
        @Order(10)
        fun dishFlags_veganDisabled_whenNonVeganIngredient() {
            val nonVeganProduct = EXISTING_MEAT
            val dishName = "Блюдо-Не-Веган-${System.currentTimeMillis()}"

            dishesPage
                .fillBasicDishInfo(dishName, "FIRST", 400)
                .addIngredient(nonVeganProduct, 200)

            val veganCheckbox = driver.findElement(By.xpath("//input[@class='d-flag' and @value='VEGAN']"))
            assertFalse(veganCheckbox.isEnabled, "Чекбокс 'Веган' должен быть отключён")
            dishesPage.cancelEdit()
        }

        @Test
        @Order(11)
        fun dishFlags_veganEnabled_whenVeganIngredient() {
            val veganProduct = EXISTING_POTATO
            val dishName = "Блюдо-Веган-${System.currentTimeMillis()}"

            dishesPage
                .fillBasicDishInfo(dishName, "SALAD", 200)
                .addIngredient(veganProduct, 100)

            WaitUtils.waitForDebouncedUpdate()

            val veganCheckbox = driver.findElement(By.xpath("//input[@class='d-flag' and @value='VEGAN']"))
            assertTrue(veganCheckbox.isEnabled, "Чекбокс 'Веган' должен быть доступен")
            dishesPage.cancelEdit()
        }
    }
}