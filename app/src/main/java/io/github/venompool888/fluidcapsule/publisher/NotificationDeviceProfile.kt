package io.github.venompool888.fluidcapsule.publisher

/** Device-specific presentation only; notification parsing and actions remain shared. */
internal enum class NotificationDeviceProfile(val preferSourceSmallIcon: Boolean) {
    OPPO(false),
    PIXEL(true),
    DEFAULT(false);

    companion object {
        fun detect(manufacturer: String, brand: String, model: String): NotificationDeviceProfile {
            val maker = manufacturer.trim()
            val deviceBrand = brand.trim()
            return when {
                maker.equals("OPPO", ignoreCase = true) ||
                    deviceBrand.equals("OPPO", ignoreCase = true) -> OPPO
                maker.equals("Google", ignoreCase = true) &&
                    model.trim().let {
                        it.equals("Pixel", ignoreCase = true) ||
                            it.startsWith("Pixel ", ignoreCase = true)
                    } -> PIXEL
                else -> DEFAULT
            }
        }
    }
}
