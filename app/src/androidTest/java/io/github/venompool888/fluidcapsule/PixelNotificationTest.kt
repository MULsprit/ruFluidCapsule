package io.github.venompool888.fluidcapsule

import android.app.Notification
import android.app.Person
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
        assertTrue(notification.hasPromotableCharacteristics())
        assertEquals("解锁后查看", notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT))
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
