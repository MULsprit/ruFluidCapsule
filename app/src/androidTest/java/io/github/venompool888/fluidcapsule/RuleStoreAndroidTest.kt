package io.github.venompool888.fluidcapsule

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.venompool888.fluidcapsule.parser.OtpParseResult
import io.github.venompool888.fluidcapsule.parser.OtpParser
import io.github.venompool888.fluidcapsule.rules.RuleStore
import io.github.venompool888.fluidcapsule.rules.RuleTrust
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RuleStoreAndroidTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pair = KeyPairGenerator.getInstance("Ed25519", BouncyCastleProvider()).generateKeyPair()
    private val trust = RuleTrust(pair.public)
    private val dir = File(context.filesDir, "test-rules-${UUID.randomUUID()}")
    private fun store(versionCode: Int = 38) = RuleStore(context, trust, dir, versionCode)

    @Test
    fun installSurvivesRestartAndChangesParserBehavior() {
        try {
            store().install(bundle(2))
            val loaded = store().loadLatest()!!
            assertEquals(2, loaded.pack.version)
            assertEquals("482913", (OtpParser.parse("Secure number: 482913", loaded.pack) as OtpParseResult.Success).code)
        } finally { dir.deleteRecursively() }
    }

    @Test
    fun corruptCurrentFallsBackToPrevious() {
        try {
            store().install(bundle(2))
            store().install(bundle(3))
            File(dir, "current.bin").writeText("corrupt")
            assertEquals(2, store().loadLatest()?.pack?.version)
        } finally { dir.deleteRecursively() }
    }

    @Test
    fun corruptBothUsesBuiltInAndRestoreClearsOnlyRules() {
        try {
            store().install(bundle(2))
            store().install(bundle(3))
            File(dir, "current.bin").writeText("corrupt")
            File(dir, "previous.bin").writeText("corrupt")
            assertNull(store().loadLatest())
            store().restoreBuiltIn()
            assertNull(store().loadLatest())
            assertTrue(context.filesDir.exists())
        } finally { dir.deleteRecursively() }
    }

    @Test
    fun incompatibleStoredPackUsesBuiltIn() {
        try {
            store(99).install(bundle(2, minApp = 99))
            assertNull(store(38).loadLatest())
        } finally { dir.deleteRecursively() }
    }

    private fun bundle(version: Int, minApp: Int = 38): io.github.venompool888.fluidcapsule.rules.VerifiedRuleBundle {
        val pack = """{"schemaVersion":1,"version":$version,"otpKeywords":[{"id":"secure","phrase":"Secure number"}],"otpBindings":[],"verificationRequests":[],"otpExclusions":[],"linkExclusions":[]}""".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(pack).joinToString("") { "%02x".format(it) }
        val manifest = """{"schemaVersion":1,"version":$version,"minAppVersionCode":$minApp,"packSha256":"$hash","notes":"test"}""".toByteArray()
        val signer = Signature.getInstance("Ed25519", BouncyCastleProvider())
        signer.initSign(pair.private)
        signer.update(manifest)
        return trust.verifyPack(trust.verifyManifest(manifest, Base64.getEncoder().encode(signer.sign())), pack)
    }
}
