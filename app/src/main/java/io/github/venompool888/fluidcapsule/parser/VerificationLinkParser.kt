package io.github.venompool888.fluidcapsule.parser

import io.github.venompool888.fluidcapsule.rules.RulePack
import java.net.URI

data class VerificationLinkRequest(val url: String?)

/** Recognizes an unfinished verification action, then binds at most one nearby web URL. */
object VerificationLinkParser {
    private val requestPatterns = listOf(
        Regex(
            "\\b(?:verify|confirm|activate)\\s+(?:your\\s+|the\\s+)?" +
                "(?:email(?:\\s+address)?|account|identity|phone(?:\\s+number)?|" +
                "mobile(?:\\s+number)?|work\\s+rights|right\\s+to\\s+work|registration)\\b",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            "\\b(?:click|tap|follow|open|visit)\\b[^.!?\\n]{0,80}?\\b(?:link|button)\\b" +
                "[^.!?\\n]{0,40}?\\b(?:verify|confirm|activate)\\b",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            "(?:请|請)?(?:点击|點擊|打开|打開|访问|訪問)[^。！？\\n]{0,30}?" +
                "(?:链接|連結|网址|網址)[^。！？\\n]{0,30}?(?:验证|驗證|确认|確認|激活|啟用)",
        ),
        Regex(
            "(?:验证|驗證|确认|確認|激活|啟用)(?:您的|你的)?" +
                "(?:邮箱|郵箱|電子郵件|账户|帳戶|账号|賬號|身份|手机号|手機號)",
        ),
    )
    private val urlPattern = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
    private val unrelatedBridge = Regex(
        "\\b(?:privacy|policy|terms|unsubscribe|support|contact|track|guide|help|details|learn|read)\\b|" +
            "隐私|隱私|退订|退訂|帮助|幫助|详情|詳情|指南|追踪|追蹤",
        RegexOption.IGNORE_CASE,
    )
    private val sentenceBreak = Regex("[.!?。！？]")

    fun parse(text: String, rules: RulePack = RulePack.EMPTY): VerificationLinkRequest? {
        if (text.isBlank()) return null
        if (rules.linkExclusions.any { text.contains(it.phrase, ignoreCase = true) }) return null
        val requests = requestPatterns.flatMap { it.findAll(text).map { match -> match.range }.toList() } +
            rules.verificationRequests.flatMap { rule -> literalRanges(text, rule.phrase) }
        if (requests.isEmpty()) return null

        val matchingUrls = urlPattern.findAll(text)
            .mapNotNull { match ->
                val url = match.value.trimEnd('.', ',', ';', ':', '!', '?', ')', '。', '，', '；', '：', '！', '？')
                val uri = runCatching { URI(url) }.getOrNull() ?: return@mapNotNull null
                if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank() ||
                    uri.userInfo != null
                ) return@mapNotNull null
                if (requests.none { request -> isBoundToRequest(text, request, match.range) }) {
                    return@mapNotNull null
                }
                url
            }
            .distinct()
            .toList()

        return VerificationLinkRequest(matchingUrls.singleOrNull())
    }

    private fun isBoundToRequest(text: String, request: IntRange, url: IntRange): Boolean {
        val gap = when {
            url.first > request.last -> text.substring(request.last + 1, url.first)
            request.first > url.last -> text.substring(url.last + 1, request.first)
            else -> ""
        }
        val proseBetween = urlPattern.replace(gap, "")
        return gap.length <= 180 &&
            !sentenceBreak.containsMatchIn(proseBetween) &&
            !unrelatedBridge.containsMatchIn(proseBetween)
    }

    private fun literalRanges(text: String, phrase: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var from = 0
        while (from < text.length) {
            val index = text.indexOf(phrase, from, ignoreCase = true)
            if (index < 0) break
            ranges += index until index + phrase.length
            from = index + phrase.length
        }
        return ranges
    }
}
