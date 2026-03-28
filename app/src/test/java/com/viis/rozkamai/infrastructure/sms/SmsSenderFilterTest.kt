package com.viis.rozkamai.infrastructure.sms

import com.viis.rozkamai.util.BaseUnitTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsSenderFilterTest : BaseUnitTest() {

    // --- Known financial senders ---

    @Test
    fun `GPAY sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("GPAY"))
    }

    @Test
    fun `VK-GPAY sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("VK-GPAY"))
    }

    @Test
    fun `PhonePe sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("PhonePe"))
    }

    @Test
    fun `PAYTM sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("PAYTM"))
    }

    @Test
    fun `PYTMUPI sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("PYTMUPI"))
    }

    @Test
    fun `SBI sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("SBI"))
    }

    @Test
    fun `SBIUPI sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("SBIUPI"))
    }

    @Test
    fun `HDFCBK sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("HDFCBK"))
    }

    @Test
    fun `ICICIB sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("ICICIB"))
    }

    @Test
    fun `AXISBK sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("AXISBK"))
    }

    @Test
    fun `KOTAKB sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("KOTAKB"))
    }

    @Test
    fun `YESBNK sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("YESBNK"))
    }

    @Test
    fun `INDBNK sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("INDBNK"))
    }

    @Test
    fun `PNBSMS sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("PNBSMS"))
    }

    @Test
    fun `BOIIND sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("BOIIND"))
    }

    @Test
    fun `CANBNK sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("CANBNK"))
    }

    // --- Partial/prefixed sender IDs (real-world format) ---

    @Test
    fun `VK-HDFCBK sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("VK-HDFCBK"))
    }

    @Test
    fun `TX-ICICIB sender returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("TX-ICICIB"))
    }

    // --- Case-insensitive matching ---

    @Test
    fun `lowercase gpay returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("gpay"))
    }

    @Test
    fun `mixed case Paytm returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("Paytm"))
    }

    @Test
    fun `lowercase hdfcbk returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("hdfcbk"))
    }

    @Test
    fun `mixed case vk-Sbiupi returns true`() {
        assertTrue(SmsSenderFilter.isFinancialSender("vk-Sbiupi"))
    }

    // --- Unknown / non-financial senders ---

    @Test
    fun `unknown sender returns false`() {
        assertFalse(SmsSenderFilter.isFinancialSender("AMAZON"))
    }

    @Test
    fun `OTP sender returns false`() {
        assertFalse(SmsSenderFilter.isFinancialSender("VM-OTPSVC"))
    }

    @Test
    fun `spam sender returns false`() {
        assertFalse(SmsSenderFilter.isFinancialSender("AD-OFFER99"))
    }

    @Test
    fun `phone number sender returns false`() {
        assertFalse(SmsSenderFilter.isFinancialSender("+919876543210"))
    }

    // --- Edge cases ---

    @Test
    fun `empty string returns false`() {
        assertFalse(SmsSenderFilter.isFinancialSender(""))
    }

    @Test
    fun `blank string returns false`() {
        assertFalse(SmsSenderFilter.isFinancialSender("   "))
    }
}
