package org.example.recipebook.pages

import org.example.recipebook.utils.AlertUtils
import org.example.recipebook.utils.WaitUtils
import org.openqa.selenium.*
import org.openqa.selenium.support.ui.Select

abstract class BasePage(protected val driver: WebDriver) {

    fun waitForElement(locator: By, timeoutSec: Int = 10): WebElement? {
        val end = System.currentTimeMillis() + timeoutSec * 1000L
        while (System.currentTimeMillis() < end) {
            try {
                val el = driver.findElement(locator)
                if (el.isDisplayed) return el
            } catch (_: Exception) {}
            Thread.sleep(100)
        }
        return null
    }

    fun click(locator: By) {
        val element = waitForElement(locator) ?: return
        (driver as JavascriptExecutor).executeScript("arguments[0].scrollIntoView({block: 'center'});", element)
        AlertUtils.acceptAlert(driver, 1)
        try {
            element.click()
        } catch (e: ElementClickInterceptedException) {
            (driver as JavascriptExecutor).executeScript("arguments[0].click();", element)
        }
        AlertUtils.acceptAlert(driver, 2)
    }

    fun fillField(locator: By, text: String) {
        waitForElement(locator)?.let {
            it.clear()
            it.sendKeys(text)
        }
    }

    fun fillNumberField(locator: By, value: Number) {
        fillField(locator, value.toString())
    }

    fun getText(locator: By): String = waitForElement(locator)?.text ?: ""

    fun isElementVisible(locator: By): Boolean = waitForElement(locator) != null

    fun selectOptionByText(locator: By, text: String) {
        waitForElement(locator)?.let { Select(it).selectByVisibleText(text) }
    }

    fun selectOptionByValue(locator: By, value: String) {
        waitForElement(locator)?.let { Select(it).selectByValue(value) }
    }

    fun setCheckbox(locator: By, checked: Boolean) {
        waitForElement(locator)?.let { checkbox ->
            if (checkbox.isSelected != checked) checkbox.click()
        }
    }

    fun findElements(locator: By): List<WebElement> = try { driver.findElements(locator) } catch (_: Exception) { emptyList() }

    fun switchToProductsTab() {
        click(By.xpath("//button[text()='Продукты']"))
        WaitUtils.waitForTabVisible(driver, "products-section")
    }

    fun switchToDishesTab() {
        click(By.xpath("//button[text()='Блюда']"))
        WaitUtils.waitForTabVisible(driver, "dishes-section")
    }
}