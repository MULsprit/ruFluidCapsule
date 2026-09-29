package io.github.venompool888.fluidcapsule.rules

import android.content.Context
import java.util.Base64

interface RuleUpdateStateStore {
    var subscriptionEnabled: Boolean
    var lastSuccessfulCheckMillis: Long
    var manifest: ByteArray?
    var signature: ByteArray?
    var dismissedVersion: Int
    var restoredVersion: Int
}

class RuleSubscriptionPrefs(context: Context) : RuleUpdateStateStore {
    private val prefs = context.getSharedPreferences("rule_subscription", Context.MODE_PRIVATE)

    override var subscriptionEnabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }

    override var lastSuccessfulCheckMillis: Long
        get() = prefs.getLong("last_success", 0L)
        set(value) { prefs.edit().putLong("last_success", value).apply() }

    override var manifest: ByteArray?
        get() = bytes("manifest")
        set(value) { putBytes("manifest", value) }

    override var signature: ByteArray?
        get() = bytes("signature")
        set(value) { putBytes("signature", value) }

    override var dismissedVersion: Int
        get() = prefs.getInt("dismissed_version", 0)
        set(value) { prefs.edit().putInt("dismissed_version", value).apply() }

    override var restoredVersion: Int
        get() = prefs.getInt("restored_version", 0)
        set(value) { prefs.edit().putInt("restored_version", value).apply() }

    private fun bytes(key: String): ByteArray? = prefs.getString(key, null)?.let { encoded ->
        runCatching { Base64.getDecoder().decode(encoded) }.getOrNull()
    }

    private fun putBytes(key: String, value: ByteArray?) {
        val editor = prefs.edit()
        if (value == null) editor.remove(key)
        else editor.putString(key, Base64.getEncoder().encodeToString(value))
        editor.apply()
    }
}
