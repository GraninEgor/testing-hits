package org.example.recipebook.tests

import org.example.recipebook.pages.*
import org.example.recipebook.utils.WaitUtils
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.time.Duration

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ProductUITest {

    companion object {
        private lateinit var driver: WebDriver
        private const val BASE_URL = "http://localhost:8080"

        private const val EXISTING_POTATO = "Картофель"

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
        (driver as org.openqa.selenium.JavascriptExecutor)
            .executeScript("window.__productsLoaded = false;")

        productsPage = ProductsPage(driver)
        productsPage.switchToProductsTab()

        val end = System.currentTimeMillis() + 10000L
        while (System.currentTimeMillis() < end) {
            try {
                val loaded = (driver as org.openqa.selenium.JavascriptExecutor)
                    .executeScript("return window.__productsLoaded === true;") as? Boolean
                if (loaded == true) break
            } catch (_: Exception) {}
            Thread.sleep(100)
        }

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
    @Order(1)
    inner class CreateTests {

        @Test
        @Order(1)
        fun `should create product successfully`() {
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
    @Order(2)
    inner class ReadTests {

        @Test
        @Order(1)
        fun `should find product by name when searching`() {
            productsPage.searchProducts(EXISTING_POTATO)
            assertTrue(productsPage.hasProduct(EXISTING_POTATO, 10), "Поиск не нашёл: '$EXISTING_POTATO'")
        }

        @Test
        @Order(2)
        fun `should show only matching products when filtering by category`() {
            productsPage.filterByCategory("Овощи").applyFilters()
            assertTrue(productsPage.hasProduct(EXISTING_POTATO, 10), "Фильтрация не показала: '$EXISTING_POTATO'")
        }
    }

    @Nested
    @Order(4)
    inner class DeleteTests {
        @Test
        @Order(2)
        fun `should not create product when name is too short`() {
            val originalCount = productsPage.getProductsCount()

            productsPage.fillProductForm("А", 100, 10, 10, 10, "состав", "MEAT", "READY_TO_EAT")
                .saveProduct()
            WaitUtils.waitForDebouncedUpdate()

            assertFalse(productsPage.getProductsCount() > originalCount, "Продукт с коротким именем создался")
        }
    }

    @Nested
    @Order(5)
    inner class MacrosValidationTests {

        @Test
        @Order(1)
        fun `should not create product when macros sum exceeds 100`() {
            val originalCount = productsPage.getProductsCount()

            productsPage.fillProductForm("БЖУ-Тест", 100, 50, 40, 30, "состав", "MEAT", "READY_TO_EAT")
                .saveProduct()
            WaitUtils.waitForDebouncedUpdate()

            assertFalse(productsPage.getProductsCount() > originalCount, "Продукт с БЖУ > 100 создался")
        }

        @Test
        @Order(2)
        fun `should create product successfully when macros are valid`() {
            val name = "БЖУ-Валид-${System.currentTimeMillis()}"

            productsPage.fillProductForm(name, 100, 30, 20, 40, "состав", "MEAT", "READY_TO_EAT")
                .saveProduct()

            assertTrue(productsPage.hasProduct(name, 20))
            createdProducts.add(name)
        }
    }
}