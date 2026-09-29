package io.github.venompool888.fluidcapsule.rules

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

interface RuleSource {
    fun fetchManifest(): Pair<ByteArray, ByteArray>
    fun fetchPack(version: Int): ByteArray
}

/** Only the official, fixed raw GitHub paths can be requested. */
class RuleHttpClient(
    private val connectionFactory: (URL) -> HttpURLConnection = { url -> url.openConnection() as HttpURLConnection },
) : RuleSource {
    private val base = "https://raw.githubusercontent.com/Venompool888/FluidCapsule/main/rules/stable/"

    override fun fetchManifest(): Pair<ByteArray, ByteArray> =
        get("manifest.json", 8_192) to get("manifest.sig", 256)

    override fun fetchPack(version: Int): ByteArray {
        require(version > 0) { "Invalid pack version" }
        return get("packs/$version.json", 65_536)
    }

    private fun get(path: String, maxBytes: Int): ByteArray {
        val url = URL(base + path)
        val connection = connectionFactory(url)
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.requestMethod = "GET"
            connection.useCaches = false
            require(connection.responseCode == HttpURLConnection.HTTP_OK) { "Unexpected rule response" }
            require(connection.contentLengthLong <= maxBytes) { "Rule response too large" }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(4_096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= maxBytes) { "Rule response too large" }
                    output.write(buffer, 0, count)
                }
            }
            return output.toByteArray()
        } finally {
            connection.disconnect()
        }
    }
}
