package org.example.recipebook.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

class ProductDetailPage(driver: WebDriver) : BasePage(driver) {

    private val backBtn = By.xpath("//button[contains(text(), '← Назад')]")
    private val detailName = By.cssSelector("#product-detail h2")
    private val detailComposition = By.xpath("//p[b[text()='Состав:']]")

    fun getProductComposition(): String? =
        if (isElementVisible(detailComposition)) {
            getText(detailComposition).replace("Состав:", "").trim()
        } else null

}