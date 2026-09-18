package io.github.venompool888.fluidcapsule

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.venompool888.fluidcapsule.history.NotificationHistoryStore
import io.github.venompool888.fluidcapsule.notification.NormalizedNotification
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HistoryExportTest {
    @Test
    fun cursorPagesKeepTheirInsertionBoundaryAndFullText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = "test.only.history.${UUID.randomUUID()}"
        val body = "TEST ONLY · 验证码482913\nUnicode、引号\"与换行必须完整导出"
        fun insert(index: Int) = NotificationHistoryStore.record(context, NormalizedNotification(
            packageName = source,
            notificationKey = "$source:$index",
            title = "TEST ONLY $index",
            primaryText = body,
            messageTexts = listOf(body),
            combinedText = body,
            postedAtMillis = System.currentTimeMillis(),
            contentIntent = null,
            smallIcon = null,
            largeIcon = null,
            senderIcon = null,
            actions = emptyList(),
            isGroupSummary = false,
            isOngoing = false,
            channelId = "test-only",
        ))
        try {
            repeat(3, ::insert)
            val originalIds = NotificationHistoryStore.forPackage(context, source)
                .map { it.id }.sorted()
            assertEquals(3, originalIds.size)
            val upperBound = originalIds.last()
            insert(3)
            var cursor = originalIds.first() - 1
            val seen = mutableListOf<Long>()
            do {
                val page = NotificationHistoryStore.exportPage(context, cursor, 1, upperBound)
                assertEquals(upperBound, page.snapshotMaxId)
                assertEquals(1, page.entries.size)
                val entry = page.entries.single()
                assertTrue(entry.id > cursor && entry.id <= upperBound)
                if (entry.sourcePackage == source) {
                    assertEquals(body, entry.primaryText)
                    assertEquals(body, entry.combinedText)
                    seen += entry.id
                }
                cursor = page.nextAfterId ?: upperBound
            } while (page.hasMore)
            assertEquals(originalIds, seen)
            val end = NotificationHistoryStore.exportPage(context, upperBound, 1, upperBound)
            assertTrue(end.entries.isEmpty())
            assertFalse(end.hasMore)
            assertNull(end.nextAfterId)
        } finally {
            // Delete only these synthetic rows, never existing device history.
            NotificationHistoryStore.deletePackage(context, source)
        }
    }
}
