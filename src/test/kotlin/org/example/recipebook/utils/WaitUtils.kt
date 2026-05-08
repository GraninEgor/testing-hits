package org.example.recipebook.utils

import org.openqa.selenium.WebDriver

object WaitUtils {

    fun waitForDebouncedUpdate() {
        try { Thread.sleep(400) } catch (_: InterruptedException) {}
    }

    fun waitForTabVisible(driver: WebDriver, sectionId: String, timeoutMs: Long = 3000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            try {
                val section = driver.findElement(org.openqa.selenium.By.id(sectionId))
                val className = section.getAttribute("class")
                if (className != null && !className.contains("hidden")) return
            } catch (_: Exception) {}
            Thread.sleep(100)
        }
    }
}