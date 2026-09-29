package io.github.venompool888.fluidcapsule.rules

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class RuleManifest(
    val schemaVersion: Int,
    val version: Int,
    val minAppVersionCode: Int,
    val packSha256: String,
    val notes: String,
)

object RuleManifestCodec {
    private val expected = setOf("schemaVersion", "version", "minAppVersionCode", "packSha256", "notes")
    private val hashPattern = Regex("[0-9a-f]{64}")

    fun decode(bytes: ByteArray): RuleManifest {
        require(bytes.size <= 8_192) { "Manifest exceeds 8 KiB" }
        val content = try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid UTF-8", e)
        }
        try {
            val obj = JSONObject(content)
            require(obj.keys().asSequence().toSet() == expected) { "Unknown or missing manifest field" }
            fun integer(key: String): Int {
                val value = obj.get(key)
                require(value is Int) { "$key must be an integer" }
                return value
            }
            fun string(key: String): String =
                (obj.get(key) as? String) ?: throw IllegalArgumentException("$key must be a string")
            val manifest = RuleManifest(
                integer("schemaVersion"), integer("version"), integer("minAppVersionCode"),
                string("packSha256"), string("notes"),
            )
            require(manifest.schemaVersion == 1 && manifest.version > 0 && manifest.minAppVersionCode > 0)
            require(hashPattern.matches(manifest.packSha256))
            require(manifest.notes.length <= 1_000)
            return manifest
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid manifest", e)
        }
    }
}
