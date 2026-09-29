package io.github.venompool888.fluidcapsule.action

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import io.github.venompool888.fluidcapsule.publisher.CapsuleCoordinator
import java.util.Locale

class OpenOriginalActivity : Activity() {
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (handled) return
        handled = true

        CapsuleCoordinator.consume(this, intent.getStringExtra(EXTRA_EVENT_ID))

        val original = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT)
        }
        val verificationUrl = intent.getStringExtra(EXTRA_VERIFICATION_URL)
        try {
            if (intent.action == ACTION_OPEN_VERIFICATION_LINK && verificationUrl != null) {
                val uri = safeVerificationUri(verificationUrl)
                if (uri != null) {
                    startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
                } else {
                    openSourceApplication(this, intent)
                }
            } else if (original == null) {
                openSourceApplication(this, intent)
            } else if (Build.VERSION.SDK_INT >= 34) {
                val options = ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode =
                        if (Build.VERSION.SDK_INT >= 36) {
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
                        } else {
                            @Suppress("DEPRECATION")
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        }
                }
                original.send(
                    this,
                    0,
                    null,
                    null,
                    null,
                    null,
                    options.toBundle(),
                )
            } else {
                original.send()
            }
        } catch (_: PendingIntent.CanceledException) {
            openSourceApplication(this, intent)
        } catch (_: ActivityNotFoundException) {
            openSourceApplication(this, intent)
        } finally {
            finish()
            if (Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }
    }

    private fun openSourceApplication(context: Context, sourceIntent: Intent) {
        val sourcePackage = sourceIntent.getStringExtra(EXTRA_SOURCE_PACKAGE) ?: return
        context.packageManager.getLaunchIntentForPackage(sourcePackage)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let(context::startActivity)
    }

    companion object {
        fun safeVerificationUri(url: String): Uri? {
            val parsed = Uri.parse(url)
            val scheme = parsed.scheme?.lowercase(Locale.ROOT)
            if (scheme !in setOf("https", "http") || parsed.host.isNullOrBlank() ||
                parsed.encodedUserInfo != null
            ) return null
            return parsed.buildUpon().scheme(scheme).build()
        }

        const val ACTION_OPEN_ORIGINAL = "io.github.venompool888.fluidcapsule.action.OPEN_ORIGINAL"
        const val ACTION_OPEN_VERIFICATION_LINK = "io.github.venompool888.fluidcapsule.action.OPEN_VERIFICATION_LINK"
        const val EXTRA_ORIGINAL_INTENT = "original_intent"
        const val EXTRA_VERIFICATION_URL = "verification_url"
        const val EXTRA_SOURCE_PACKAGE = "source_package"
        const val EXTRA_EVENT_ID = "event_id"
    }
}
