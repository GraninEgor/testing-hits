package org.example.recipebook.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

class DishDetailPage(driver: WebDriver) : BasePage(driver) {

    private val backBtn = By.xpath("//button[contains(text(), '← Назад')]")
    private val detailName = By.cssSelector("#dish-detail h2")
    private val detailIngredients = By.xpath("//h3[text()='Состав']/following-sibling::*")

    fun getDishName(): String = getText(detailName)

    fun hasIngredient(productName: String, expectedAmount: Number): Boolean {
        val text = getText(detailIngredients)
        return text.contains(productName) && text.contains("${expectedAmount} г")
    }

    fun goBack(): DishesPage {
        click(backBtn)
        return DishesPage(driver)
    }
}