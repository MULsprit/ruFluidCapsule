package io.github.venompool888.fluidcapsule.rules

import android.content.Context
import android.util.AtomicFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/** App-private, complete signed bundles. Every load rechecks the signature and pack hash. */
class RuleStore(
    private val context: Context,
    private val trust: RuleTrust,
    private val directory: File = File(context.filesDir, "rule_subscription"),
    private val appVersionCode: Int = context.packageManager
        .getPackageInfo(context.packageName, 0).longVersionCode.toInt(),
) {
    private val current = AtomicFile(File(directory, "current.bin"))
    private val previous = AtomicFile(File(directory, "previous.bin"))

    @Synchronized
    fun loadLatest(): VerifiedRuleBundle? = read(current) ?: read(previous)

    @Synchronized
    fun install(bundle: VerifiedRuleBundle) {
        val checked = reverify(bundle)
        require(checked.pack.version > RulePack.BUNDLED.version) { "Not newer than bundled rules" }
        val existing = loadLatest()
        require(existing == null || checked.pack.version > existing.pack.version) { "Rule downgrade" }
        directory.mkdirs()
        if (existing != null) write(previous, encode(existing))
        write(current, encode(checked))
    }

    @Synchronized
    fun restoreBuiltIn() {
        current.delete()
        previous.delete()
    }

    private fun reverify(bundle: VerifiedRuleBundle): VerifiedRuleBundle {
        val manifest = trust.verifyManifest(
            bundle.verifiedManifest.manifestBytes,
            bundle.verifiedManifest.signatureBytes,
        )
        require(manifest.manifest.minAppVersionCode <= appVersionCode) { "Incompatible app version" }
        return trust.verifyPack(manifest, bundle.packBytes)
    }

    private fun read(file: AtomicFile): VerifiedRuleBundle? = runCatching {
        if (!file.baseFile.exists()) return null
        val bytes = file.openRead().use { input ->
            input.readNBytes(73_997).also { require(it.size <= 73_996) }
        }
        val data = DataInputStream(ByteArrayInputStream(bytes))
        fun part(max: Int): ByteArray {
            val size = data.readInt()
            require(size in 1..max)
            return ByteArray(size).also { data.readFully(it) }
        }
        val manifest = part(8_192)
        val signature = part(256)
        val pack = part(65_536)
        require(data.available() == 0) { "Trailing bundle bytes" }
        val checked = trust.verifyManifest(manifest, signature)
        require(checked.manifest.minAppVersionCode <= appVersionCode) { "Incompatible app version" }
        trust.verifyPack(checked, pack)
    }.getOrNull()

    private fun encode(bundle: VerifiedRuleBundle): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            listOf(bundle.verifiedManifest.manifestBytes, bundle.verifiedManifest.signatureBytes, bundle.packBytes)
                .forEach { bytes -> data.writeInt(bytes.size); data.write(bytes) }
        }
        return output.toByteArray()
    }

    private fun write(file: AtomicFile, bytes: ByteArray) {
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }
}
