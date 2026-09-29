package io.github.venompool888.fluidcapsule.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RulePackCodecTest {
    @Test
    fun validPackDecodesExactly() {
        val pack = RulePackCodec.decode(
            json(
                """"otpKeywords":[{"id":"otp.secure","phrase":"secure number"}],
                   "otpBindings":[{"id":"otp.bound","phrase":"secure number","codePosition":"after","maxDistance":12}],
                   "verificationRequests":[{"id":"link.confirm","phrase":"Complete email confirmation"}],
                   "otpExclusions":[{"id":"otp.deny","phrase":"order number"}],
                   "linkExclusions":[{"id":"link.deny","phrase":"read details"}]""",
            ).toByteArray(Charsets.UTF_8),
        )
        assertEquals(2, pack.version)
        assertEquals("secure number", pack.otpKeywords.single().phrase)
        assertEquals(CodePosition.AFTER, pack.otpBindings.single().codePosition)
        assertEquals(12, pack.otpBindings.single().maxDistance)
        assertEquals("Complete email confirmation", pack.verificationRequests.single().phrase)
        assertEquals("order number", pack.otpExclusions.single().phrase)
        assertEquals("read details", pack.linkExclusions.single().phrase)
    }

    @Test
    fun rejectsUnknownFieldDuplicateIdAndOversize() {
        val invalid = listOf(
            json("\"unknown\":true"),
            json("\"otpKeywords\":[{\"id\":\"duplicate\",\"phrase\":\"secure number\"}]," +
                "\"linkExclusions\":[{\"id\":\"duplicate\",\"phrase\":\"read details\"}]"),
            json("\"otpKeywords\":[{\"id\":\"long\",\"phrase\":\"${"x".repeat(121)}\"}]"),
            json("\"otpKeywords\":[${(1..201).joinToString(",") { "{\"id\":\"r$it\",\"phrase\":\"word$it\"}" }}]"),
            json("\"otpBindings\":[{\"id\":\"gap\",\"phrase\":\"secure number\"," +
                "\"codePosition\":\"after\",\"maxDistance\":33}]"),
            json("\"otpKeywords\":[{\"id\":\"BAD ID\",\"phrase\":\"secure number\"}]"),
            """{"schemaVersion":"1","version":2,"otpKeywords":[],"otpBindings":[],"verificationRequests":[],"otpExclusions":[],"linkExclusions":[]}""",
            json("\"otpKeywords\":[{\"id\":\"badtype\",\"phrase\":123}]"),
        )
        invalid.forEach { value ->
            assertThrows(IllegalArgumentException::class.java) {
                RulePackCodec.decode(value.toByteArray(Charsets.UTF_8))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            RulePackCodec.decode(ByteArray(65_537) { ' '.code.toByte() })
        }
        assertThrows(IllegalArgumentException::class.java) {
            RulePackCodec.decode(byteArrayOf(0xC3.toByte(), 0x28))
        }
    }

    private fun json(fields: String): String = """{"schemaVersion":1,"version":2,$fields}"""
}
