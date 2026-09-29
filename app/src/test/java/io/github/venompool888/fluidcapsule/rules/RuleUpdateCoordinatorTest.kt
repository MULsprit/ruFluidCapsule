package io.github.venompool888.fluidcapsule.rules

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64

class RuleUpdateCoordinatorTest {
    private val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val trust = RuleTrust(keys.public)
    private val pack = """{"schemaVersion":1,"version":2,"otpKeywords":[],"otpBindings":[],"verificationRequests":[],"otpExclusions":[],"linkExclusions":[]}""".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(pack).joinToString("") { "%02x".format(it) }
    private val manifest = """{"schemaVersion":1,"version":2,"minAppVersionCode":38,"packSha256":"$hash","notes":"test"}""".toByteArray()
    private val signature = Signature.getInstance("Ed25519").run {
        initSign(keys.private); update(manifest); Base64.getEncoder().encode(sign())
    }
    private val state = FakeState()
    private val client = FakeClient(manifest to signature, pack)
    private var installed = 1
    private var activations = 0
    private var now = 1_000_000L

    @Test fun sixHourCacheSkipsSecondNetworkCheck() {
        val coordinator = coordinator()
        assertTrue(coordinator.check(false) is RuleCheckResult.Available)
        assertTrue(coordinator.check(false) is RuleCheckResult.Available)
        assertEquals(1, client.manifestCalls)
        assertEquals(0, client.packCalls)
    }

    @Test fun cachedManifestSurvivesCoordinatorRestart() {
        coordinator().check(false)
        val second = coordinator().check(false)
        assertTrue(second is RuleCheckResult.Available)
        assertEquals(1, client.manifestCalls)
        state.signature = "corrupt".toByteArray()
        coordinator().check(false)
        assertEquals(2, client.manifestCalls)
    }

    @Test fun manualCheckBypassesCache() {
        coordinator().check(false)
        coordinator().check(true)
        assertEquals(2, client.manifestCalls)
    }

    @Test fun incompatibleManifestNeverOffersInstall() {
        assertTrue(coordinator(appVersion = 37).check(false) is RuleCheckResult.Incompatible)
        assertEquals(0, client.packCalls)
    }

    @Test fun offlineKeepsCurrentVersion() {
        client.fail = true
        assertTrue(coordinator().check(false) is RuleCheckResult.Offline)
        assertEquals(1, installed)
        assertEquals(0, activations)
    }

    @Test fun tamperedPackDoesNotInstall() {
        val available = coordinator().check(false) as RuleCheckResult.Available
        client.pack = pack + byteArrayOf(1)
        assertTrue(coordinator().install(available.manifest) is RuleInstallResult.Failed)
        assertEquals(1, installed)
        assertEquals(0, activations)
    }

    @Test fun installRequiresUserCall() {
        val coordinator = coordinator()
        val available = coordinator.check(false) as RuleCheckResult.Available
        assertEquals(0, client.packCalls)
        assertEquals(1, installed)
        assertTrue(coordinator.install(available.manifest) is RuleInstallResult.Installed)
        assertEquals(2, installed)
        assertEquals(1, activations)
    }

    @Test fun dismissedVersionPromptsOnlyOnceAndSubscriptionCanBeDisabled() {
        val coordinator = coordinator()
        assertTrue((coordinator.check(false) as RuleCheckResult.Available).shouldPrompt)
        coordinator.dismiss(2)
        assertFalse((coordinator.check(false) as RuleCheckResult.Available).shouldPrompt)
        state.subscriptionEnabled = false
        assertTrue(coordinator.check(false) is RuleCheckResult.Disabled)
        assertTrue(coordinator.check(true) is RuleCheckResult.Available)
    }

    private fun coordinator(appVersion: Int = 38) = RuleUpdateCoordinator(
        client, trust, { installed }, { installed = it.pack.version; activations++ },
        { installed = 1 }, state, appVersion, { now },
    )

    private class FakeClient(var manifest: Pair<ByteArray, ByteArray>, var pack: ByteArray) : RuleSource {
        var manifestCalls = 0
        var packCalls = 0
        var fail = false
        override fun fetchManifest(): Pair<ByteArray, ByteArray> {
            manifestCalls++
            if (fail) throw IOException("offline")
            return manifest
        }
        override fun fetchPack(version: Int): ByteArray { packCalls++; return pack }
    }

    private class FakeState : RuleUpdateStateStore {
        override var subscriptionEnabled = true
        override var lastSuccessfulCheckMillis = 0L
        override var manifest: ByteArray? = null
        override var signature: ByteArray? = null
        override var dismissedVersion = 0
        override var restoredVersion = 0
    }
}
