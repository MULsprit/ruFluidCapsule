package io.github.venompool888.fluidcapsule.rules

import android.content.Context
import java.io.IOException

sealed interface RuleCheckResult {
    data class Available(val manifest: VerifiedManifest, val shouldPrompt: Boolean) : RuleCheckResult
    data class UpToDate(val version: Int) : RuleCheckResult
    data class Incompatible(val version: Int, val minAppVersionCode: Int) : RuleCheckResult
    data object Disabled : RuleCheckResult
    data object Offline : RuleCheckResult
    data object Failed : RuleCheckResult
}

sealed interface RuleInstallResult {
    data class Installed(val version: Int) : RuleInstallResult
    data object Failed : RuleInstallResult
    data object Incompatible : RuleInstallResult
}

interface RuleUpdateGateway {
    fun check(force: Boolean): RuleCheckResult
    fun install(available: VerifiedManifest): RuleInstallResult
    fun dismiss(version: Int)
    fun restoreBuiltIn()
}

class RuleUpdateCoordinator(
    private val client: RuleSource,
    private val trust: RuleTrust,
    private val installedVersion: () -> Int,
    private val activate: (VerifiedRuleBundle) -> Unit,
    private val restore: () -> Unit,
    private val state: RuleUpdateStateStore,
    private val appVersionCode: Int,
    private val clock: () -> Long = System::currentTimeMillis,
) : RuleUpdateGateway {
    @Synchronized
    override fun check(force: Boolean): RuleCheckResult {
        if (!state.subscriptionEnabled && !force) return RuleCheckResult.Disabled
        val now = clock()
        val cacheFresh = !force && state.lastSuccessfulCheckMillis > 0 &&
            now >= state.lastSuccessfulCheckMillis && now - state.lastSuccessfulCheckMillis < SIX_HOURS
        val cached = if (cacheFresh) {
            val bytes = state.manifest
            val signature = state.signature
            if (bytes != null && signature != null) runCatching { trust.verifyManifest(bytes, signature) }.getOrNull()
            else null
        } else null
        val verified = cached ?: try {
            val (bytes, signature) = client.fetchManifest()
            trust.verifyManifest(bytes, signature).also {
                state.manifest = bytes
                state.signature = signature
                state.lastSuccessfulCheckMillis = now
            }
        } catch (_: IOException) {
            return RuleCheckResult.Offline
        } catch (_: Exception) {
            return RuleCheckResult.Failed
        }
        val candidate = verified.manifest
        if (candidate.version <= installedVersion()) return RuleCheckResult.UpToDate(installedVersion())
        if (candidate.minAppVersionCode > appVersionCode) {
            return RuleCheckResult.Incompatible(candidate.version, candidate.minAppVersionCode)
        }
        return RuleCheckResult.Available(
            verified,
            candidate.version != state.dismissedVersion && candidate.version != state.restoredVersion,
        )
    }

    @Synchronized
    override fun install(available: VerifiedManifest): RuleInstallResult {
        return try {
            val verified = trust.verifyManifest(available.manifestBytes, available.signatureBytes)
            if (verified.manifest.minAppVersionCode > appVersionCode ||
                verified.manifest.version <= installedVersion()
            ) return RuleInstallResult.Incompatible
            val pack = client.fetchPack(verified.manifest.version)
            val bundle = trust.verifyPack(verified, pack)
            activate(bundle)
            RuleInstallResult.Installed(bundle.pack.version)
        } catch (_: Exception) {
            RuleInstallResult.Failed
        }
    }

    override fun dismiss(version: Int) {
        if (version > 0) state.dismissedVersion = version
    }

    override fun restoreBuiltIn() {
        state.restoredVersion = installedVersion()
        restore()
    }

    companion object { const val SIX_HOURS = 6 * 60 * 60 * 1_000L }
}

object RuleUpdateGatewayProvider {
    @Volatile var testFactory: ((Context) -> RuleUpdateGateway)? = null

    fun create(context: Context): RuleUpdateGateway {
        testFactory?.let { return it(context) }
        val app = context.applicationContext
        return RuleUpdateCoordinator(
            RuleHttpClient(), RuleTrust.official(app),
            { RuleRuntime.current(app).version },
            { RuleRuntime.activate(app, it) },
            { RuleRuntime.restoreBuiltIn(app) },
            RuleSubscriptionPrefs(app),
            app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode.toInt(),
        )
    }
}
