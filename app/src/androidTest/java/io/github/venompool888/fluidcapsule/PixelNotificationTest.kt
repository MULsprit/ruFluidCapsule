package io.github.venompool888.fluidcapsule

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.venompool888.fluidcapsule.core.CapsuleAction
import io.github.venompool888.fluidcapsule.core.CapsuleEvent
import io.github.venompool888.fluidcapsule.core.CapsuleKind
import io.github.venompool888.fluidcapsule.core.CapsulePrivacy
import io.github.venompool888.fluidcapsule.notification.NotificationNormalizer
import io.github.venompool888.fluidcapsule.publisher.NotificationFactory
import io.github.venompool888.fluidcapsule.publisher.NotificationDeviceProfile
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelNotificationTest {
    @Test
    fun uppercaseHttpSchemeIsValidForVerificationAction() {
        assertEquals("https", io.github.venompool888.fluidcapsule.action.OpenOriginalActivity
            .safeVerificationUri("HTTPS://example.test/verify")?.scheme)
        assertEquals("http", io.github.venompool888.fluidcapsule.action.OpenOriginalActivity
            .safeVerificationUri("HtTp://example.test/verify")?.scheme)
        assertNull(io.github.venompool888.fluidcapsule.action.OpenOriginalActivity
            .safeVerificationUri("javascript:alert(1)"))
    }

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun senderAvatarWinsOverGenericLargeIcon() {
        val source = messageNotification(latestHasAvatar = true)
        val normalized = NotificationNormalizer.normalize(source)
        assertNotNull(normalized.senderIcon)
        assertNotNull(normalized.largeIcon)
        assertSame(normalized.senderIcon, normalized.preferredLargeIcon)
        assertEquals("latest", normalized.primaryText)
    }

    @Test
    fun missingLatestAvatarDoesNotBorrowAnotherSendersFace() {
        val normalized = NotificationNormalizer.normalize(messageNotification(latestHasAvatar = false))
        assertNull(normalized.senderIcon)
        assertSame(normalized.largeIcon, normalized.preferredLargeIcon)
    }

    @Test
    fun sourceIconHintPreservesPromotionAvatarAndPrivacyVersion() {
        val now = System.currentTimeMillis()
        val avatar = icon()
        val builder = NotificationFactory.baseBuilder(context, CapsuleEvent(
            sourcePackage = "test.only",
            sourceLabel = "TEST ONLY",
            sourceSmallIcon = Icon.createWithResource(context, R.drawable.ic_content_copy),
            sourceLargeIcon = avatar,
            eventId = "test-only:pixel",
            kind = CapsuleKind.NOTIFICATION,
            title = "TEST ONLY",
            shortText = "TEST",
            body = "NO ACTION REQUIRED",
            action = CapsuleAction.None,
            privacy = CapsulePrivacy.HIDE_SENSITIVE,
            createdAtMillis = now,
            expiresAtMillis = now + 60_000,
            dedupeKey = "test-only:pixel",
        ))
        val notification = builder.addExtras(Bundle().apply {
            putBoolean("android.requestPromotedOngoing", true)
        }).build()
        val expected = NotificationDeviceProfile.detect(
            Build.MANUFACTURER, Build.BRAND, Build.MODEL,
        ).preferSourceSmallIcon
        assertEquals(expected, notification.extras.getBoolean(NotificationFactory.EXTRA_PREFER_SMALL_ICON))
        assertEquals(expected, notification.publicVersion.extras.getBoolean(NotificationFactory.EXTRA_PREFER_SMALL_ICON))
        assertEquals(R.drawable.ic_content_copy, notification.smallIcon.resId)
        assertNotNull(notification.getLargeIcon())
        assertNull(notification.publicVersion.getLargeIcon())
        val requestedExtra = notification.extras.getBoolean("android.requestPromotedOngoing")
        assertTrue(requestedExtra)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        // Stock API 36 used the earlier colorized eligibility rule. Pixel API 37
        // uses the explicit promotion request, while this test's icon/privacy
        // assertions apply to both versions.
        if (Build.VERSION.SDK_INT >= 37) assertTrue(notification.hasPromotableCharacteristics())
        assertEquals("解锁后查看", notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT))
    }

    @Test
    fun verificationLinkNotificationHasExplicitOpenAction() {
        val now = System.currentTimeMillis()
        val notification = NotificationFactory.baseBuilder(context, CapsuleEvent(
            sourcePackage = "test.only",
            sourceLabel = "TEST ONLY",
            eventId = "test-only:verify-link",
            kind = CapsuleKind.VERIFICATION,
            title = "验证请求",
            shortText = "待验证",
            body = "点击打开验证链接",
            action = CapsuleAction.OpenVerificationLink("https://example.test/verify?token=fake"),
            privacy = CapsulePrivacy.HIDE_SENSITIVE,
            createdAtMillis = now,
            expiresAtMillis = now + 60_000,
            dedupeKey = "test-only:verify-link",
        )).build()
        assertEquals("打开验证链接", notification.actions.first().title.toString())
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun verificationRequestWithoutVisibleUrlOpensSourceEmail() {
        val now = System.currentTimeMillis()
        val original = PendingIntent.getActivity(
            context,
            9200,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationFactory.baseBuilder(context, CapsuleEvent(
            sourcePackage = "com.google.android.gm",
            sourceLabel = "Gmail",
            eventId = "test-only:verify-email",
            kind = CapsuleKind.VERIFICATION,
            title = "验证请求 · Gmail",
            shortText = "待验证",
            body = "点击查看验证邮件",
            action = CapsuleAction.OpenOriginal(original),
            privacy = CapsulePrivacy.HIDE_SENSITIVE,
            createdAtMillis = now,
            expiresAtMillis = now + 60_000,
            dedupeKey = "test-only:verify-email",
        )).build()
        assertEquals("查看验证邮件", notification.actions.first().title.toString())
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun otpWithVerificationLinkOffersCopyAndOpenActions() {
        val now = System.currentTimeMillis()
        val notification = NotificationFactory.baseBuilder(context, CapsuleEvent(
            sourcePackage = "test.only",
            eventId = "test-only:otp-and-link",
            kind = CapsuleKind.OTP,
            title = "验证码",
            shortText = "482913",
            body = "点击复制",
            action = CapsuleAction.CopySensitiveText("482913"),
            privacy = CapsulePrivacy.HIDE_SENSITIVE,
            createdAtMillis = now,
            expiresAtMillis = now + 60_000,
            dedupeKey = "test-only:otp-and-link",
            verificationUrl = "https://example.test/verify?token=fake",
        )).build()
        assertEquals(
            listOf("复制验证码", "打开验证链接"),
            notification.actions.map { it.title.toString() },
        )
    }

    private fun messageNotification(latestHasAvatar: Boolean): StatusBarNotification {
        val first = Person.Builder().setName("First").setIcon(icon()).build()
        val latest = Person.Builder().setName("Latest")
            .also { if (latestHasAvatar) it.setIcon(icon()) }.build()
        val notification = Notification.Builder(context, NotificationFactory.CAPSULE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_capsule)
            .setLargeIcon(icon())
            .setStyle(Notification.MessagingStyle(Person.Builder().setName("Me").build())
                .addMessage("first", 1, first)
                .addMessage("latest", 2, latest))
            .build()
        return StatusBarNotification(context.packageName, context.packageName, 1, null,
            Process.myUid(), Process.myPid(), 0, notification, Process.myUserHandle(), 2)
    }

    private fun icon() = Icon.createWithBitmap(Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888))
}
