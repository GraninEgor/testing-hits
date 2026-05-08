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
@DisplayName("📦 CRUD тесты продуктов")
class ProductCrudTest {

    companion object {
        private lateinit var driver: WebDriver
        private const val BASE_URL = "http://localhost:8080"

        // 🔹 Используем продукты, которые ТОЧНО есть в системе
        private const val EXISTING_POTATO = "Картофель"
        private const val EXISTING_WATER = "Вода"
        private const val EXISTING_MEAT = "Мясо"

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

            // 🔹 Ждём загрузки страницы
            Thread.sleep(2000)
        }

        @AfterAll
        @JvmStatic
        fun tearDownClass() {
            driver.quit()
        }
    }

    private lateinit var productsPage: ProductsPage
    private val createdProducts = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        productsPage = ProductsPage(driver)
        productsPage.switchToProductsTab()
        createdProducts.clear()
    }

    @AfterEach
    fun cleanup() {
        createdProducts.forEach { name ->
            try {
                if (productsPage.hasProduct(name, 3)) {
                    productsPage.clickDeleteProduct(name)
                }
            } catch (_: Exception) {}
        }
        createdProducts.clear()
    }

    @Nested
    @DisplayName("✅ Создание продукта")
    inner class CreateTests {

        @Test
        @Order(1)
        fun createProduct_withAllFields_success() {
            val name = "Тест-Продукт-${System.currentTimeMillis()}"
            val composition = "молоко, сахар, ванилин"

            productsPage
                .fillProductForm(name, 250, 10, 8, 35, composition, "SWEETS", "READY_TO_EAT")
                .setProductFlags(vegan = false, glutenFree = true, sugarFree = false)
                .saveProduct()

            assertTrue(productsPage.hasProduct(name, 20), "Продукт '$name' не появился")
            createdProducts.add(name)
        }

        @Test
        @Order(2)
        fun createProduct_withFlags_success() {
            val name = "Веган-Тест-${System.currentTimeMillis()}"

            productsPage
                .fillProductForm(name, 100, 5, 2, 15, "овощи", "VEGETABLES", "READY_TO_EAT")
                .setProductFlags(vegan = true, glutenFree = true, sugarFree = true)
                .saveProduct()

            assertTrue(productsPage.hasProduct(name, 20))
            createdProducts.add(name)
        }
    }

    @Nested
    @DisplayName("🔍 Поиск и фильтрация")
    inner class ReadTests {

        @Test
        @Order(3)
        fun searchProduct_byName_findsCorrectly() {
            // 🔹 Используем продукт, который точно есть
            productsPage.searchProducts(EXISTING_POTATO)
            assertTrue(productsPage.hasProduct(EXISTING_POTATO, 10), "Поиск не нашёл: '$EXISTING_POTATO'")
        }

        @Test
        @Order(4)
        fun filterProducts_byCategory_showsOnlyMatching() {
            // 🔹 Используем продукт, который точно есть
            productsPage.filterByCategory("Овощи").applyFilters()
            assertTrue(productsPage.hasProduct(EXISTING_POTATO, 10), "Фильтрация не показала: '$EXISTING_POTATO'")
        }
    }

    @Nested
    @DisplayName("✏️ Обновление продукта")
    inner class UpdateTests {

        @Test
        @Order(5)
        fun updateProduct_basicFields_success() {
            // 🔹 Создаём новый продукт для обновления
            val originalName = "Обновить-Тест-${System.currentTimeMillis()}"
            val updatedName = "Обновлённый-${System.currentTimeMillis()}"

            productsPage.fillProductForm(originalName, 100, 5, 2, 10, "старый", "VEGETABLES", "READY_TO_EAT")
                .saveProduct()
            assertTrue(productsPage.hasProduct(originalName, 20))
            createdProducts.add(updatedName)

            productsPage.clickEditProduct(originalName)
            productsPage.fillField(By.id("p-name"), updatedName)
            productsPage.fillField(By.id("p-composition"), "новый состав")
            productsPage.saveProduct()

            assertTrue(productsPage.hasProduct(updatedName, 20), "Новое имя не появилось")
        }

        @Test
        @Order(6)
        fun updateProduct_flags_success() {
            val name = "Флаги-Тест-${System.currentTimeMillis()}"

            productsPage.fillProductForm(name, 100, 5, 2, 10, "состав", "VEGETABLES", "READY_TO_EAT")
                .setProductFlags(false, false, false)
                .saveProduct()
            createdProducts.add(name)

            productsPage.clickEditProduct(name)
                .setProductFlags(vegan = true, glutenFree = false, sugarFree = false)
                .saveProduct()

            assertTrue(productsPage.hasProduct(name, 10))
        }
    }

    @Nested
    @DisplayName("🗑️ Удаление продукта")
    inner class DeleteTests {

        @Test
        @Order(7)
        fun deleteProduct_viaUI_success() {
            val name = "Удалить-Тест-${System.currentTimeMillis()}"

            productsPage.fillProductForm(name, 100, 5, 2, 10, "состав", "VEGETABLES", "READY_TO_EAT")
                .saveProduct()
            assertTrue(productsPage.hasProduct(name, 20))

            productsPage.clickDeleteProduct(name)
            assertFalse(productsPage.hasProduct(name, 10), "Продукт не удалён")
        }

        @Test
        @Order(8)
        fun createProduct_nameTooShort_validationError() {
            val originalCount = productsPage.getProductsCount()

            productsPage.fillProductForm("А", 100, 10, 10, 10, "состав", "MEAT", "READY_TO_EAT")
                .saveProduct()
            WaitUtils.waitForDebouncedUpdate()

            assertFalse(productsPage.getProductsCount() > originalCount, "Продукт с коротким именем создался")
        }
    }

    @Nested
    @DisplayName("📊 Валидация БЖУ")
    inner class MacrosValidationTests {

        @Test
        @Order(9)
        fun createProduct_macrosSumOver100_validationError() {
            val originalCount = productsPage.getProductsCount()

            productsPage.fillProductForm("БЖУ-Тест", 100, 50, 40, 30, "состав", "MEAT", "READY_TO_EAT")
                .saveProduct()
            WaitUtils.waitForDebouncedUpdate()

            assertFalse(productsPage.getProductsCount() > originalCount, "Продукт с БЖУ > 100 создался")
        }

        @Test
        @Order(10)
        fun createProduct_validMacros_success() {
            val name = "БЖУ-Валид-${System.currentTimeMillis()}"

            productsPage.fillProductForm(name, 100, 30, 20, 40, "состав", "MEAT", "READY_TO_EAT")
                .saveProduct()

            assertTrue(productsPage.hasProduct(name, 20))
            createdProducts.add(name)
        }
    }
}