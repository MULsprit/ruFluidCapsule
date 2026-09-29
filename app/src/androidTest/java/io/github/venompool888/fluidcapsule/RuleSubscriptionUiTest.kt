package io.github.venompool888.fluidcapsule

import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.venompool888.fluidcapsule.rules.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.io.File
import java.util.Base64
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class RuleSubscriptionUiTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var fake: FakeGateway

    @Before fun setup() {
        context.getSharedPreferences("rule_subscription", 0).edit().clear().commit()
        fake = FakeGateway(available())
        RuleUpdateGatewayProvider.testFactory = { fake }
    }

    @After fun cleanup() {
        RuleUpdateGatewayProvider.testFactory = null
        context.getSharedPreferences("rule_subscription", 0).edit().clear().commit()
    }

    @Test fun rulesPageShowsDefaultOnAndBundledVersion() {
        fake.offerUpdate = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                openRules(activity)
                val page = field(activity, "rulesPage") as View
                assertTrue(find(page) { it is Switch && it.contentDescription == "订阅规则更新" && it.isChecked } != null)
                assertTrue(find(page) { it is TextView && it.text.contains("已安装规则：1") } != null)
            }
        }
    }

    @Test fun availableUpdateShowsOneDialogAndLaterSuppressesRepeat() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await { fake.checks.get() >= 1 }
            scenario.onActivity { activity ->
                val dialog = field(activity, "ruleUpdateDialog") as AlertDialog?
                assertTrue(dialog?.isShowing == true)
                dialog!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
            }
            scenario.recreate()
            await { fake.checks.get() >= 2 }
            scenario.onActivity { activity ->
                assertNull(field(activity, "ruleUpdateDialog"))
            }
            assertEquals(1, fake.dismissals.get())
        }
    }

    @Test fun updateTapInstallsExactlyOnce() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await { fake.checks.get() >= 1 }
            scenario.onActivity { activity ->
                (field(activity, "ruleUpdateDialog") as AlertDialog)
                    .getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            await { fake.installs.get() == 1 }
            assertEquals(1, fake.installs.get())
        }
    }

    @Test fun signedFixtureInstallsThroughUiAndVerifiedStore() {
        val keys = KeyPairGenerator.getInstance("Ed25519", BouncyCastleProvider()).generateKeyPair()
        val trust = RuleTrust(keys.public)
        val pack = """{"schemaVersion":1,"version":2,"otpKeywords":[{"id":"synthetic","phrase":"Synthetic secure number"}],"otpBindings":[],"verificationRequests":[],"otpExclusions":[],"linkExclusions":[]}""".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(pack).joinToString("") { "%02x".format(it) }
        val manifest = """{"schemaVersion":1,"version":2,"minAppVersionCode":38,"packSha256":"$hash","notes":"Synthetic test"}""".toByteArray()
        val signer = Signature.getInstance("Ed25519", BouncyCastleProvider())
        signer.initSign(keys.private)
        signer.update(manifest)
        val signature = Base64.getEncoder().encode(signer.sign())
        val dir = File(context.filesDir, "test-rule-ui-${UUID.randomUUID()}")
        val store = RuleStore(context, trust, dir, 38)
        val source = object : RuleSource {
            override fun fetchManifest() = manifest to signature
            override fun fetchPack(version: Int) = pack
        }
        val state = object : RuleUpdateStateStore {
            override var subscriptionEnabled = true
            override var lastSuccessfulCheckMillis = 0L
            override var manifest: ByteArray? = null
            override var signature: ByteArray? = null
            override var dismissedVersion = 0
            override var restoredVersion = 0
        }
        RuleUpdateGatewayProvider.testFactory = {
            RuleUpdateCoordinator(source, trust, { store.loadLatest()?.pack?.version ?: 1 },
                { store.install(it) }, { store.restoreBuiltIn() }, state, 38)
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await { state.lastSuccessfulCheckMillis > 0 }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { activity ->
                    (field(activity, "ruleUpdateDialog") as AlertDialog)
                        .getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                }
                await { store.loadLatest()?.pack?.version == 2 }
                assertEquals("Synthetic secure number", store.loadLatest()!!.pack.otpKeywords.single().phrase)
            }
        } finally { dir.deleteRecursively() }
    }

    @Test fun subscriptionOffSkipsAutomaticButManualCheckWorks() {
        RuleSubscriptionPrefs(context).subscriptionEnabled = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(300)
            assertEquals(0, fake.checks.get())
            scenario.onActivity { activity ->
                openRules(activity)
                val page = field(activity, "rulesPage") as View
                (find(page) { it is Button && it.text == "检查规则更新" } as Button).performClick()
            }
            await { fake.checks.get() == 1 }
            assertTrue(fake.lastForce)
        }
    }

    private fun openRules(activity: MainActivity) {
        val tab = field(activity, "rulesTab")!!
        val root = tab.javaClass.getDeclaredField("root").apply { isAccessible = true }.get(tab) as View
        root.performClick()
    }

    private fun field(activity: MainActivity, name: String): Any? =
        MainActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.get(activity)

    private fun find(root: View, predicate: (View) -> Boolean): View? {
        if (predicate(root)) return root
        if (root is ViewGroup) for (i in 0 until root.childCount) {
            find(root.getChildAt(i), predicate)?.let { return it }
        }
        return null
    }

    private fun await(condition: () -> Boolean) {
        repeat(100) { if (condition()) return; Thread.sleep(50) }
        fail("Timed out waiting for UI state")
    }

    private fun available(): VerifiedManifest {
        val keys = KeyPairGenerator.getInstance("Ed25519", BouncyCastleProvider()).generateKeyPair()
        val manifest = """{"schemaVersion":1,"version":2,"minAppVersionCode":38,"packSha256":"${"0".repeat(64)}","notes":"test"}""".toByteArray()
        val signer = Signature.getInstance("Ed25519", BouncyCastleProvider())
        signer.initSign(keys.private)
        signer.update(manifest)
        return RuleTrust(keys.public).verifyManifest(manifest, Base64.getEncoder().encode(signer.sign()))
    }

    private class FakeGateway(private val available: VerifiedManifest) : RuleUpdateGateway {
        val checks = AtomicInteger()
        val installs = AtomicInteger()
        val dismissals = AtomicInteger()
        @Volatile var lastForce = false
        @Volatile var offerUpdate = true
        override fun check(force: Boolean): RuleCheckResult {
            lastForce = force
            checks.incrementAndGet()
            return if (offerUpdate) RuleCheckResult.Available(available, dismissals.get() == 0)
            else RuleCheckResult.UpToDate(1)
        }
        override fun install(available: VerifiedManifest): RuleInstallResult {
            installs.incrementAndGet()
            return RuleInstallResult.Installed(2)
        }
        override fun dismiss(version: Int) { dismissals.incrementAndGet() }
        override fun restoreBuiltIn() = Unit
    }
}
