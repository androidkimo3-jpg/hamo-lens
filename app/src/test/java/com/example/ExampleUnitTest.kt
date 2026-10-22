package com.example

import com.example.data.media.MediaStoreHelper
import com.example.localization.AppStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testAppStringsTranslations() {
        assertEquals("Lens", AppStrings.t("app", "en"))
        assertEquals("عدسة", AppStrings.t("app", "ar"))
        assertEquals("3 selected", AppStrings.t("selN", "en", 3))
        assertEquals("تم تحديد 3", AppStrings.t("selN", "ar", 3))
    }

    @Test
    fun testFileNameGeneration() {
        val timestamp = 1700000000000L
        val fileName = MediaStoreHelper.generateFileName(timestamp)
        assertTrue(fileName.startsWith("Lens_"))
        assertTrue(fileName.endsWith(".jpg"))
    }
}
