package org.example.recipebook.pages

import org.example.recipebook.utils.AlertUtils
import org.example.recipebook.utils.WaitUtils
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.util.*

class ProductsPage(driver: WebDriver) : BasePage(driver) {

    private val productSearch = By.id("product-search")
    private val filterCategory = By.id("filter-category")
    private val applyFiltersBtn = By.xpath("//button[text()='Применить']")

    private val formName = By.id("p-name")
    private val formCalories = By.id("p-calories")
    private val formProteins = By.id("p-proteins")
    private val formFats = By.id("p-fats")
    private val formCarbs = By.id("p-carbs")
    private val formComposition = By.id("p-composition")
    private val formCategory = By.id("p-category")
    private val formCooking = By.id("p-cooking")
    private val flagVegan = By.xpath("//input[@class='p-flag' and @value='VEGAN']")
    private val flagGluten = By.xpath("//input[@class='p-flag' and @value='GLUTEN_FREE']")
    private val flagSugar = By.xpath("//input[@class='p-flag' and @value='SUGAR_FREE']")
    private val saveBtn = By.id("save-product-btn")
    private val cancelBtn = By.xpath("//button[text()='Отмена']")

    private val productCard = By.cssSelector("#products-list .card")
    private val productNameInCard = By.cssSelector("#products-list .card b")

    fun searchProducts(query: String) = apply {
        fillField(productSearch, query)
        WaitUtils.waitForDebouncedUpdate()
    }

    fun filterByCategory(category: String) = apply { selectOptionByText(filterCategory, category) }

    fun applyFilters() = apply {
        click(applyFiltersBtn)
        WaitUtils.waitForDebouncedUpdate()
    }

    fun fillProductForm(
        name: String, calories: Number, proteins: Number, fats: Number, carbs: Number,
        composition: String, category: String, cooking: String
    ) = apply {
        fillField(formName, name)
        fillNumberField(formCalories, calories)
        fillNumberField(formProteins, proteins)
        fillNumberField(formFats, fats)
        fillNumberField(formCarbs, carbs)
        fillField(formComposition, composition)
        selectOptionByValue(formCategory, category)
        selectOptionByValue(formCooking, cooking)
    }

    fun setProductFlags(vegan: Boolean, glutenFree: Boolean, sugarFree: Boolean) = apply {
        setCheckbox(flagVegan, vegan)
        setCheckbox(flagGluten, glutenFree)
        setCheckbox(flagSugar, sugarFree)
    }

    fun saveProduct() = apply {
        click(saveBtn)
        WaitUtils.waitForDebouncedUpdate()
        AlertUtils.acceptAlert(driver, 2)
        WaitUtils.waitForDebouncedUpdate()
        Thread.sleep(800)
    }

    fun cancelEdit() = apply {
        AlertUtils.acceptAlert(driver, 1)
        click(cancelBtn)
        WaitUtils.waitForDebouncedUpdate()
    }

    fun hasProduct(productName: String, timeoutSec: Int = 30): Boolean {
        val js = driver as org.openqa.selenium.JavascriptExecutor
        val end = System.currentTimeMillis() + timeoutSec * 1000L

        while (System.currentTimeMillis() < end) {
            try {

                val cards = driver.findElements(By.cssSelector("#products-list .card"))
                for (card in cards) {
                    try {
                        val nameEl = card.findElement(By.cssSelector("b"))
                        if (nameEl.text.contains(productName, ignoreCase = true)) {
                            return true
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

            Thread.sleep(300)
        }
        return false
    }

    fun getProductsCount(): Int = driver.findElements(productCard).size

    fun clickEditProduct(productName: String, timeoutSec: Int = 30) = apply {
        AlertUtils.acceptAlert(driver, 1)

        val end = System.currentTimeMillis() + timeoutSec * 1000L
        var card: WebElement? = null

        while (System.currentTimeMillis() < end && card == null) {
            try {
                val cards = driver.findElements(By.cssSelector("#products-list .card"))
                for (c in cards) {
                    try {
                        val nameEl = c.findElement(By.cssSelector("b"))
                        if (nameEl.text.contains(productName, ignoreCase = true)) {
                            card = c
                            break
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
            if (card == null) Thread.sleep(300)
        }

        if (card == null) {
            val available = driver.findElements(By.cssSelector("#products-list .card b"))
                .mapNotNull { it.text }.take(10).joinToString(", ")
            throw AssertionError(
                "Продукт '$productName' не найден за $timeoutSec сек. Доступные: [$available]"
            )
        }

        val editBtn = card.findElement(By.xpath(".//button[text()='Редактировать']"))
        (driver as JavascriptExecutor).executeScript("arguments[0].click();", editBtn)
        WaitUtils.waitForDebouncedUpdate()
    }

    fun clickDeleteProduct(productName: String) = apply {
        val card = findProductCard(productName) ?: throw AssertionError("Продукт '$productName' не найден")
        card.findElement(By.xpath(".//button[text()='Удалить']")).click()
        AlertUtils.acceptConfirm(driver, 5)
        AlertUtils.acceptAlert(driver, 3)
        WaitUtils.waitForDebouncedUpdate()
    }

    private fun findProductCard(productName: String): WebElement? {
        return driver.findElements(productCard).firstOrNull { card ->
            try { card.findElement(productNameInCard).text.contains(productName) } catch (_: Exception) { false }
        }
    }
}