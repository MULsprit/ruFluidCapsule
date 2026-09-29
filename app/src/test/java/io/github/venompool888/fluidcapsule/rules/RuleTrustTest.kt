package io.github.venompool888.fluidcapsule.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class RuleTrustTest {
    private val pack = """{"schemaVersion":1,"version":2,"otpKeywords":[],"otpBindings":[],"verificationRequests":[],"otpExclusions":[],"linkExclusions":[]}"""
        .toByteArray()
    private val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val trust = RuleTrust(keyPair.public)

    @Test
    fun acceptsSignedManifestAndMatchingPack() {
        val manifest = manifest(pack)
        val verified = trust.verifyManifest(manifest, sign(manifest))
        assertEquals(2, verified.manifest.version)
        assertEquals(2, trust.verifyPack(verified, pack).pack.version)
    }

    @Test
    fun rejectsEveryTrustBoundaryMutation() {
        val manifest = manifest(pack)
        val signature = sign(manifest)
        val verified = trust.verifyManifest(manifest, signature)
        val changedManifest = manifest.clone().also { it[it.lastIndex] = '!'.code.toByte() }
        assertThrows(IllegalArgumentException::class.java) { trust.verifyManifest(changedManifest, signature) }
        assertThrows(IllegalArgumentException::class.java) { trust.verifyManifest(manifest, "%%%".toByteArray()) }
        val wrongKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair().public
        assertThrows(IllegalArgumentException::class.java) { RuleTrust(wrongKey).verifyManifest(manifest, signature) }
        assertThrows(IllegalArgumentException::class.java) { trust.verifyPack(verified, pack + byteArrayOf(1)) }
        val otherVersion = pack.toString(Charsets.UTF_8).replace("\"version\":2", "\"version\":3").toByteArray()
        val otherManifest = manifest(otherVersion)
        assertThrows(IllegalArgumentException::class.java) {
            trust.verifyPack(trust.verifyManifest(otherManifest, sign(otherManifest)), otherVersion)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RuleManifestCodec.decode(manifest.toString(Charsets.UTF_8).dropLast(1).plus(",\"extra\":1}").toByteArray())
        }
        assertThrows(IllegalArgumentException::class.java) { RuleManifestCodec.decode(ByteArray(8_193)) }
    }

    @Test
    fun checkedInOfficialBundleIsValid() {
        val root = Path.of("..", "rules", "stable")
        val publicBytes = Files.readAllBytes(Path.of("src", "main", "assets", "rule_signing_public.der"))
        val publicKey = KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(publicBytes))
        val official = RuleTrust(publicKey)
        val manifest = official.verifyManifest(Files.readAllBytes(root.resolve("manifest.json")), Files.readAllBytes(root.resolve("manifest.sig")))
        assertEquals(1, official.verifyPack(manifest, Files.readAllBytes(root.resolve("packs/1.json"))).pack.version)
    }

    private fun manifest(bytes: ByteArray): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return """{"schemaVersion":1,"version":2,"minAppVersionCode":38,"packSha256":"$hash","notes":"Synthetic"}""".toByteArray()
    }

    private fun sign(bytes: ByteArray): ByteArray {
        val signer = Signature.getInstance("Ed25519")
        signer.initSign(keyPair.private)
        signer.update(bytes)
        return Base64.getEncoder().encode(signer.sign())
    }
}
