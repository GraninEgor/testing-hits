package org.example.recipebook.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

class DishDetailPage(driver: WebDriver) : BasePage(driver) {

    private val backBtn = By.xpath("//button[contains(text(), '← Назад')]")

    fun goBack(): DishesPage {
        click(backBtn)
        return DishesPage(driver)
    }
}