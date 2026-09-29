package io.github.venompool888.fluidcapsule.rules

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object RulePackCodec {
    private val idPattern = Regex("[a-z0-9._-]{1,64}")
    private val rootKeys = setOf("schemaVersion", "version", "otpKeywords", "otpBindings", "verificationRequests", "otpExclusions", "linkExclusions")
    private val literalKeys = setOf("id", "phrase")
    private val bindingKeys = literalKeys + setOf("codePosition", "maxDistance")

    fun decode(bytes: ByteArray): RulePack {
        require(bytes.size <= 65_536) { "Rule pack exceeds 64 KiB" }
        val decoded = try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid UTF-8", e)
        }
        try {
            val root = JSONObject(decoded)
            requireKeys(root, rootKeys)
            require(integer(root, "schemaVersion") == 1) { "Unsupported schema" }
            val version = integer(root, "version")
            require(version > 0) { "Invalid version" }
            val ids = mutableSetOf<String>()
            fun literalList(name: String): List<LiteralRule> = array(root, name).map { entry ->
                requireKeys(entry, literalKeys)
                LiteralRule(id(entry, ids), phrase(entry))
            }
            val keywords = literalList("otpKeywords")
            val bindings = array(root, "otpBindings").map { entry ->
                requireKeys(entry, bindingKeys)
                val position = when (string(entry, "codePosition")) {
                    "before" -> CodePosition.BEFORE
                    "after" -> CodePosition.AFTER
                    else -> throw IllegalArgumentException("Invalid codePosition")
                }
                val distance = integer(entry, "maxDistance")
                require(distance in 0..32) { "Invalid maxDistance" }
                OtpBindingRule(id(entry, ids), phrase(entry), position, distance)
            }
            val requests = literalList("verificationRequests")
            val otpExclusions = literalList("otpExclusions")
            val linkExclusions = literalList("linkExclusions")
            require(ids.size <= 200) { "Too many rules" }
            return RulePack(version, keywords, bindings, requests, otpExclusions, linkExclusions)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid rule pack", e)
        }
    }

    private fun requireKeys(obj: JSONObject, expected: Set<String>) {
        val actual = obj.keys().asSequence().toSet()
        require(actual == expected) { "Missing or unknown fields: ${actual - expected}, ${expected - actual}" }
    }

    private fun array(obj: JSONObject, key: String): List<JSONObject> {
        val arr: JSONArray = obj.getJSONArray(key)
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun id(obj: JSONObject, ids: MutableSet<String>): String {
        val value = string(obj, "id")
        require(idPattern.matches(value) && ids.add(value)) { "Invalid or duplicate ID" }
        return value
    }

    private fun phrase(obj: JSONObject): String {
        val value = string(obj, "phrase")
        require(value.isNotBlank() && value.length <= 120 && value == value.trim()) { "Invalid phrase" }
        return value
    }

    private fun string(obj: JSONObject, key: String): String =
        (obj.get(key) as? String) ?: throw IllegalArgumentException("$key must be a string")

    private fun integer(obj: JSONObject, key: String): Int {
        val value = obj.get(key)
        require(value is Int) { "$key must be an integer" }
        return value
    }
}
