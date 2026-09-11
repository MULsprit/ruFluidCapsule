package io.github.venompool888.fluidcapsule.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpParserTest {
    @Test
    fun parsesChineseOtp() {
        val result = OtpParser.parse("您的验证码为 482913，5 分钟内有效")
        assertTrue(result is OtpParseResult.Success)
        result as OtpParseResult.Success
        assertEquals("482913", result.code)
        assertEquals(5, result.validForMinutes)
    }

    @Test
    fun parsesEnglishOtp() {
        val result = OtpParser.parse("Your verification code is 3812")
        assertEquals("3812", (result as OtpParseResult.Success).code)
    }

    @Test
    fun rejectsBankTailWithoutOtpKeyword() {
        val result = OtpParser.parse("您尾号 1234 的账户支出 88.50 元")
        assertEquals(OtpParseResult.None, result)
    }

    @Test
    fun selectsCodeNearestKeyword() {
        val result = OtpParser.parse("尾号 1234 的账户验证码为 905771，请勿告诉他人")
        assertEquals("905771", (result as OtpParseResult.Success).code)
    }

    @Test
    fun rejectsOrderNumber() {
        val result = OtpParser.parse("订单 20260809123456 已发货")
        assertEquals(OtpParseResult.None, result)
    }

    @Test
    fun rejectsCarrierSubscriptionReminder() {
        val result = OtpParser.parse(
            "【订购提醒】您已成功开通任我享套餐，编号 25JS205838，2026-08-09 生效。如需咨询请登录 https://example.invalid/path",
        )
        assertEquals(OtpParseResult.None, result)
    }

    @Test
    fun rejectsServiceNumberNearOtpSafetyReminder() {
        val result = OtpParser.parse("请勿向任何人提供验证码，如有疑问请拨打客服热线 10086")
        assertEquals(OtpParseResult.None, result)
    }

    @Test
    fun parsesCodeBeforeChineseKeyword() {
        val result = OtpParser.parse("482913 是您的登录验证码，请勿向他人泄露")
        assertEquals("482913", (result as OtpParseResult.Success).code)
    }

    @Test
    fun parsesEnglishLoginCode() {
        val result = OtpParser.parse("Your login code is 735194. Do not share it with anyone.")
        assertEquals("735194", (result as OtpParseResult.Success).code)
    }

    @Test
    fun parsesEnglishConfirmationCode() {
        val result = OtpParser.parse("Confirmation code: 281604. It expires in 10 minutes.")
        assertEquals("281604", (result as OtpParseResult.Success).code)
    }

    @Test
    fun parsesAlphanumericOtp() {
        val result = OtpParser.parse("Your verification code is A7K29Q. Do not share this code.")
        assertEquals("A7K29Q", (result as OtpParseResult.Success).code)
    }

    @Test
    fun parsesSecretCode() {
        val result = OtpParser.parse(
            "Enter the following code to confirm the login attempt. Secret code: 90779837",
        )
        assertEquals("90779837", (result as OtpParseResult.Success).code)
    }

    @Test
    fun parsesSixDigitCodeSeparatedFromKeywordByAppName() {
        val result = OtpParser.parse(
            "Verify your email. Use this 6-digit code in the felix mobile app: 418428. " +
                "This code is valid for 60 mins.",
        )
        assertEquals("418428", (result as OtpParseResult.Success).code)
    }

    @Test
    fun parsesDeliveryPin() {
        val result = OtpParser.parse(
            "Time to meet BEHNAM. Tell them your PIN is 1662 to confirm the delivery.",
        )
        assertEquals("1662", (result as OtpParseResult.Success).code)
    }

    @Test
    fun parsesBarePin() {
        val result = OtpParser.parse("PIN: 4831")
        assertEquals("4831", (result as OtpParseResult.Success).code)
    }

    @Test
    fun parsesAccessCode() {
        val result = OtpParser.parse("Your access code is 745920. It expires in 10 minutes.")
        assertEquals("745920", (result as OtpParseResult.Success).code)
    }

    @Test
    fun parsesVerificationNumber() {
        val result = OtpParser.parse("Verification number: 583104")
        assertEquals("583104", (result as OtpParseResult.Success).code)
    }

    @Test
    fun parsesTemporaryPassword() {
        val result = OtpParser.parse(
            "Your login information is: Temporary Password: 0205. " +
                "Next time you login, you will be prompted to change your password.",
        )
        assertEquals("0205", (result as OtpParseResult.Success).code)
    }

    @Test
    fun rejectsPromoCode() {
        val result = OtpParser.parse("Use promo code SAVE20 to receive 20% off your next order.")
        assertEquals(OtpParseResult.None, result)
    }

    @Test
    fun rejectsSubmissionConfirmationNumber() {
        val result = OtpParser.parse(
            "Assignment submitted successfully. Confirmation number: " +
                "92f28db62fb54153ad411a53cc9c8a6c. Submitted in 2026.",
        )
        assertEquals(OtpParseResult.None, result)
    }

    @Test
    fun rejectsTemporaryPasswordPolicyYear() {
        val result = OtpParser.parse("The temporary password policy was updated in 2026.")
        assertEquals(OtpParseResult.None, result)
    }

    @Test
    fun rejectsInformationalOtpWarningWithoutCode() {
        val result = OtpParser.parse("警方提醒：不要向陌生人泄露验证码或开启屏幕共享")
        assertEquals(OtpParseResult.None, result)
    }
}
