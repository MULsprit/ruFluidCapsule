package io.github.venompool888.fluidcapsule.publisher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeviceProfileTest {
    @Test
    fun oppoKeepsItsExistingPresentation() {
        listOf(
            Triple("OPPO", "OPPO", "CPH2797"),
            Triple(" oppo ", "OPPO", "Find X8"),
            Triple("unknown", "oppo", "CPH2797"),
        ).forEach { (maker, brand, model) ->
            val profile = NotificationDeviceProfile.detect(maker, brand, model)
            assertEquals(NotificationDeviceProfile.OPPO, profile)
            assertFalse(profile.preferSourceSmallIcon)
        }
    }

    @Test
    fun pixelModelsEnableSourceIconPresentation() {
        listOf("Pixel", "Pixel 9", "Pixel 11 Pro XL", "Pixel Fold", " pixel 10 ")
            .forEach { model ->
                val profile = NotificationDeviceProfile.detect("Google", "google", model)
                assertEquals(NotificationDeviceProfile.PIXEL, profile)
                assertTrue(profile.preferSourceSmallIcon)
            }
    }

    @Test
    fun otherDevicesDoNotInheritPixelOverrides() {
        listOf(
            Triple("Google", "google", "Nexus 6P"),
            Triple("Samsung", "samsung", "Pixel 11 Pro XL"),
            Triple("OnePlus", "OnePlus", "CPH2609"),
            Triple("", "", ""),
        ).forEach { (maker, brand, model) ->
            val profile = NotificationDeviceProfile.detect(maker, brand, model)
            assertEquals(NotificationDeviceProfile.DEFAULT, profile)
            assertFalse(profile.preferSourceSmallIcon)
        }
    }
}
