package io.github.venompool888.fluidcapsule.rules

import android.content.Context

/** Single immutable snapshot read by notification parsing, never by a network operation. */
object RuleRuntime {
    @Volatile private var active: RulePack? = null

    fun current(context: Context): RulePack {
        active?.let { return it }
        return synchronized(this) {
            active ?: (store(context).loadLatest()?.pack ?: RulePack.BUNDLED).also { active = it }
        }
    }

    fun activate(context: Context, verified: VerifiedRuleBundle) {
        synchronized(this) {
            store(context).install(verified)
            active = verified.pack
        }
    }

    fun restoreBuiltIn(context: Context) {
        synchronized(this) {
            store(context).restoreBuiltIn()
            active = RulePack.BUNDLED
        }
    }

    private fun store(context: Context) = RuleStore(context, RuleTrust.official(context))
}
