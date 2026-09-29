package io.github.venompool888.fluidcapsule.rules

import android.content.Context
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class VerifiedManifest internal constructor(
    val manifest: RuleManifest,
    manifestBytes: ByteArray,
    signatureBytes: ByteArray,
) {
    private val originalManifest = manifestBytes.clone()
    private val originalSignature = signatureBytes.clone()
    val manifestBytes: ByteArray get() = originalManifest.clone()
    val signatureBytes: ByteArray get() = originalSignature.clone()
}

class VerifiedRuleBundle internal constructor(
    val verifiedManifest: VerifiedManifest,
    packBytes: ByteArray,
    val pack: RulePack,
) {
    private val originalPack = packBytes.clone()
    val packBytes: ByteArray get() = originalPack.clone()
}

class RuleTrust(private val publicKey: PublicKey) {
    fun verifyManifest(bytes: ByteArray, base64Signature: ByteArray): VerifiedManifest {
        require(base64Signature.size <= 256) { "Signature too large" }
        val signatureBytes = try {
            Base64.getDecoder().decode(base64Signature.toString(Charsets.US_ASCII).trim())
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid signature encoding", e)
        }
        val verifier = Signature.getInstance("Ed25519", BouncyCastleProvider())
        verifier.initVerify(publicKey)
        verifier.update(bytes)
        require(verifier.verify(signatureBytes)) { "Invalid manifest signature" }
        val manifest = RuleManifestCodec.decode(bytes)
        return VerifiedManifest(manifest, bytes, base64Signature)
    }

    fun verifyPack(manifest: VerifiedManifest, packBytes: ByteArray): VerifiedRuleBundle {
        require(packBytes.size <= 65_536) { "Pack too large" }
        val actualHash = MessageDigest.getInstance("SHA-256").digest(packBytes)
            .joinToString("") { "%02x".format(it) }
        require(actualHash == manifest.manifest.packSha256) { "Pack hash mismatch" }
        val pack = RulePackCodec.decode(packBytes)
        require(pack.version == manifest.manifest.version) { "Pack version mismatch" }
        return VerifiedRuleBundle(manifest, packBytes, pack)
    }

    companion object {
        fun official(context: Context): RuleTrust {
            val bytes = context.assets.open("rule_signing_public.der").use { it.readBytes() }
            val key = KeyFactory.getInstance("Ed25519", BouncyCastleProvider())
                .generatePublic(X509EncodedKeySpec(bytes))
            return RuleTrust(key)
        }
    }
}
