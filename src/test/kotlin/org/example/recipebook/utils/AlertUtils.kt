package org.example.recipebook.utils

import org.openqa.selenium.WebDriver
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

object AlertUtils {

    fun acceptConfirm(driver: WebDriver, timeoutSeconds: Int = 5) {
        try {
            WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds.toLong()))
                .until(ExpectedConditions.alertIsPresent())
                .accept()
        } catch (_: TimeoutException) {}
    }

    fun acceptAlert(driver: WebDriver, timeoutSeconds: Int = 2) {
        try {
            WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds.toLong()))
                .until(ExpectedConditions.alertIsPresent())
                .accept()
        } catch (_: TimeoutException) {}
    }

    fun isAlertPresent(driver: WebDriver, timeoutSeconds: Int = 1): Boolean {
        return try {
            WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds.toLong()))
                .until(ExpectedConditions.alertIsPresent())
            true
        } catch (_: TimeoutException) { false }
    }
}