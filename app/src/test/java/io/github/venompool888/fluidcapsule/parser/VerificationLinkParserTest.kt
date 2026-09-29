package io.github.venompool888.fluidcapsule.parser

import io.github.venompool888.fluidcapsule.rules.LiteralRule
import io.github.venompool888.fluidcapsule.rules.RulePack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerificationLinkParserTest {
    @Test
    fun remoteVerificationRequestBindsOnlyOneAdjacentUrl() {
        val rules = RulePack.EMPTY.copy(
            verificationRequests = listOf(LiteralRule("confirm", "Complete email confirmation")),
        )
        val url = "https://account.example.test/confirm?token=example"
        assertEquals(url, VerificationLinkParser.parse("Complete email confirmation: $url", rules)?.url)
        assertEquals(null, VerificationLinkParser.parse(
            "Complete email confirmation in the app. Privacy policy: https://example.test/privacy", rules,
        )?.url)
        assertEquals(null, VerificationLinkParser.parse(
            "Complete email confirmation: $url https://account.example.test/other", rules,
        )?.url)
    }

    @Test
    fun remoteLinkExclusionSuppressesAction() {
        val rules = RulePack.EMPTY.copy(
            verificationRequests = listOf(LiteralRule("confirm", "Complete email confirmation")),
            linkExclusions = listOf(LiteralRule("deny", "read details")),
        )
        assertNull(VerificationLinkParser.parse(
            "Complete email confirmation: read details https://example.test/info", rules,
        ))
    }

    @Test
    fun extractsUrlBoundToEmailVerificationRequest() {
        assertEquals(
            "https://accounts.example.test/verify?token=fake-token",
            VerificationLinkParser.parse(
                "Please click the following link to verify your email address: " +
                    "https://accounts.example.test/verify?token=fake-token",
            )?.url,
        )
    }

    @Test
    fun extractsChineseVerificationLinkWithoutTrailingPunctuation() {
        assertEquals(
            "https://example.test/confirm?token=fake-token",
            VerificationLinkParser.parse(
                "请点击以下链接验证邮箱：https://example.test/confirm?token=fake-token。",
            )?.url,
        )
    }

    @Test
    fun recognizesVerificationCtaWithoutVisibleUrl() {
        assertEquals(null, VerificationLinkParser.parse("Verify your work rights")?.url)
    }

    @Test
    fun rejectsCompletedVerificationAndOrdinaryLinks() {
        listOf(
            "Your email address has been verified.",
            "Your package is ready. Track it at https://example.test/track/1234",
            "Read the verification guide at https://example.test/help",
            "https://example.test/verify?token=fake-token",
        ).forEach { assertNull(it, VerificationLinkParser.parse(it)) }
    }

    @Test
    fun keepsVerificationUrlWhenFooterHasUnrelatedLink() {
        assertEquals(
            "https://accounts.example.test/verify?token=fake-token",
            VerificationLinkParser.parse(
                "Verify your account by clicking https://accounts.example.test/verify?token=fake-token\n" +
                    "Privacy policy: https://example.test/privacy",
            )?.url,
        )
    }

    @Test
    fun doesNotUseUnrelatedFooterUrlAsVerificationTarget() {
        assertEquals(
            null,
            VerificationLinkParser.parse(
                "Verify your account in the app.\nPrivacy policy: https://example.test/privacy",
            )?.url,
        )
    }

    @Test
    fun doesNotBindDetailsLinkInAnotherSentence() {
        assertEquals(
            null,
            VerificationLinkParser.parse(
                "Verify your account in the app. Read details at https://example.test/info",
            )?.url,
        )
    }

    @Test
    fun doesNotBindOrdinaryLinkDespiteNearbyVerificationWording() {
        assertEquals(
            null,
            VerificationLinkParser.parse(
                "Verify your email by opening the app and read details at https://example.test/info",
            )?.url,
        )
        assertEquals(
            null,
            VerificationLinkParser.parse(
                "Verify your account. Track a package at https://example.test/track/1234",
            )?.url,
        )
    }

    @Test
    fun doesNotChooseBetweenTwoVerificationUrls() {
        assertEquals(
            null,
            VerificationLinkParser.parse(
                "Please verify your account using one of these links:\n" +
                    "https://a.example.test/verify/one\nhttps://b.example.test/verify/two",
            )?.url,
        )
    }
}
