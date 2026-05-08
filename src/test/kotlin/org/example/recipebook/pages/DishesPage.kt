package org.example.recipebook.pages

import org.example.recipebook.utils.AlertUtils
import org.example.recipebook.utils.WaitUtils
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.Select
import java.util.*

class DishesPage(driver: WebDriver) : BasePage(driver) {

    private val dishSearch = By.id("dish-search")
    private val dishCategoryFilter = By.id("dish-category")
    private val filterVegan = By.id("f-vegan")

    private val formName = By.id("d-name")
    private val formCategory = By.id("d-category")
    private val formPortion = By.id("d-portion")
    private val formCalories = By.id("d-calories")
    private val formProteins = By.id("d-proteins")
    private val formFats = By.id("d-fats")
    private val formCarbs = By.id("d-carbs")
    private val addIngredientBtn = By.xpath("//button[text()='Добавить ингредиент']")
    private val flagVegan = By.xpath("//input[@class='d-flag' and @value='VEGAN']")
    private val flagGluten = By.xpath("//input[@class='d-flag' and @value='GLUTEN_FREE']")
    private val flagSugar = By.xpath("//input[@class='d-flag' and @value='SUGAR_FREE']")
    private val saveBtn = By.xpath("//button[text()='Сохранить блюдо']")
    private val cancelBtn = By.xpath("//button[text()='Отмена']")

    private val dishCard = By.cssSelector("#dishes-list .card")
    private val dishNameInCard = By.cssSelector("#dishes-list .card b")

    fun searchDishes(query: String) = apply { fillField(dishSearch, query); WaitUtils.waitForDebouncedUpdate() }
    fun filterByCategory(category: String) = apply { selectOptionByText(dishCategoryFilter, category) }
    fun filterByFlagVegan(enabled: Boolean) = apply { setCheckbox(filterVegan, enabled) }

    fun fillBasicDishInfo(name: String, category: String, portionSize: Number) = apply {
        fillField(formName, name)
        selectOptionByValue(formCategory, category)
        fillNumberField(formPortion, portionSize)
    }

    fun fillDishMacros(calories: Number, proteins: Number, fats: Number, carbs: Number) = apply {
        fillNumberField(formCalories, calories)
        fillNumberField(formProteins, proteins)
        fillNumberField(formFats, fats)
        fillNumberField(formCarbs, carbs)
    }

    fun setDishFlags(vegan: Boolean, glutenFree: Boolean, sugarFree: Boolean) = apply {
        setCheckbox(flagVegan, vegan)
        setCheckbox(flagGluten, glutenFree)
        setCheckbox(flagSugar, sugarFree)
    }

    fun addIngredient(productNameSubstring: String, amount: Number) = apply {
        click(addIngredientBtn)
        WaitUtils.waitForDebouncedUpdate()

        val selectLocator = By.cssSelector("#ingredients .ingredient-product")
        var select: Select? = null
        val end = System.currentTimeMillis() + 10000L
        while (System.currentTimeMillis() < end && select == null) {
            try {
                val selects = driver.findElements(selectLocator)
                if (selects.isNotEmpty()) select = Select(selects.last())
            } catch (_: Exception) {}
            if (select == null) Thread.sleep(200)
        }
        if (select == null) throw AssertionError("Не удалось найти select ингредиента")

        var options = select.options
        val end2 = System.currentTimeMillis() + 10000L
        while (System.currentTimeMillis() < end2 && options.size <= 1) {
            Thread.sleep(200)
            options = select.options
        }

        val option = options.find { it.text.contains(productNameSubstring, ignoreCase = true) }
        if (option == null) {
            val sample = options.take(10).joinToString(", ") { it.text }
            throw AssertionError("Продукт '$productNameSubstring' не найден. Примеры: [$sample]")
        }

        select.selectByVisibleText(option.text)
        val inputs = driver.findElements(By.cssSelector("#ingredients .ingredient-amount"))
        inputs.last().clear()
        inputs.last().sendKeys(amount.toString())
        WaitUtils.waitForDebouncedUpdate()
    }

    fun saveDish() = apply {
        click(saveBtn)
        WaitUtils.waitForDebouncedUpdate()
        AlertUtils.acceptAlert(driver, 2)
        WaitUtils.waitForDebouncedUpdate()
    }

    fun cancelEdit() = apply {
        AlertUtils.acceptAlert(driver, 1)
        click(cancelBtn)
        WaitUtils.waitForDebouncedUpdate()
    }

    fun hasDish(dishName: String, timeoutSec: Int = 10): Boolean {
        val end = System.currentTimeMillis() + timeoutSec * 1000L
        while (System.currentTimeMillis() < end) {
            try {
                val cards = driver.findElements(dishCard)
                if (cards.any { card ->
                        try { card.findElement(dishNameInCard).text.contains(dishName) } catch (_: Exception) { false }
                    }) return true
            } catch (_: Exception) {}
            Thread.sleep(200)
        }
        return false
    }

    fun clickEditDish(dishName: String) = apply {
        AlertUtils.acceptAlert(driver, 1)
        val card = findDishCard(dishName) ?: throw AssertionError("Блюдо '$dishName' не найдено")
        val editBtn = card.findElement(By.xpath(".//button[text()='Редактировать']"))
        (driver as JavascriptExecutor).executeScript("arguments[0].click();", editBtn)
        WaitUtils.waitForDebouncedUpdate()
    }

    fun clickDeleteDish(dishName: String) = apply {
        val card = findDishCard(dishName) ?: throw AssertionError("Блюдо '$dishName' не найдено")
        card.findElement(By.xpath(".//button[text()='Удалить']")).click()
        AlertUtils.acceptConfirm(driver, 5)
        AlertUtils.acceptAlert(driver, 3)
        WaitUtils.waitForDebouncedUpdate()
    }

    fun openDish(dishName: String): DishDetailPage {
        val card = findDishCard(dishName) ?: throw AssertionError("Блюдо '$dishName' не найдено")
        card.findElement(By.xpath(".//button[text()='Открыть']")).click()
        return DishDetailPage(driver)
    }

    private fun findDishCard(dishName: String): WebElement? {
        return driver.findElements(dishCard).firstOrNull { card ->
            try { card.findElement(dishNameInCard).text.contains(dishName) } catch (_: Exception) { false }
        }
    }
}