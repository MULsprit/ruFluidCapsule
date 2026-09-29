package io.github.venompool888.fluidcapsule.rules

data class LiteralRule(val id: String, val phrase: String)

enum class CodePosition { BEFORE, AFTER }

data class OtpBindingRule(
    val id: String,
    val phrase: String,
    val codePosition: CodePosition,
    val maxDistance: Int,
)

data class RulePack(
    val version: Int,
    val otpKeywords: List<LiteralRule>,
    val otpBindings: List<OtpBindingRule>,
    val verificationRequests: List<LiteralRule>,
    val otpExclusions: List<LiteralRule>,
    val linkExclusions: List<LiteralRule>,
) {
    companion object {
        val EMPTY = RulePack(0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val BUNDLED = EMPTY.copy(version = 1)
    }
}
