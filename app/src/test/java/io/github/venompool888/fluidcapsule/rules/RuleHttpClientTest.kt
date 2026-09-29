package io.github.venompool888.fluidcapsule.rules

import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class RuleHttpClientTest {
    @Test fun rejectsRedirectWithoutFollowingIt() {
        val fake = FakeConnection(302, ByteArray(0))
        val client = RuleHttpClient { fake }
        assertThrows(IllegalArgumentException::class.java) { client.fetchManifest() }
        assertEquals(false, fake.instanceFollowRedirects)
    }

    @Test fun rejectsOversizePack() {
        val fake = FakeConnection(200, ByteArray(65_537))
        assertThrows(IllegalArgumentException::class.java) { RuleHttpClient { fake }.fetchPack(2) }
    }

    private class FakeConnection(private val status: Int, private val bytes: ByteArray) : HttpURLConnection(URL("https://example.test")) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getInputStream(): InputStream = ByteArrayInputStream(bytes)
    }
}
