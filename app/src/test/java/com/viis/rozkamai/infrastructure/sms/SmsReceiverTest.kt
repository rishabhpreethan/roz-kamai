package com.viis.rozkamai.infrastructure.sms

import com.viis.rozkamai.util.BaseUnitTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for SmsReceiver logic.
 *
 * SmsReceiver depends on Android platform types (Context, Intent, WorkManager),
 * so we test its filter delegation logic through SmsSenderFilter directly —
 * this is the only branching logic the receiver performs before enqueuing.
 *
 * The enqueue-or-skip decision is: isFinancialSender(sender) → enqueue / skip.
 * This contract is verified via SmsSenderFilter unit tests and the integration contract below.
 */
class SmsReceiverTest : BaseUnitTest() {

    // --- Filter delegation: financial sender → should enqueue ---

    @Test
    fun `financial sender GPAY passes filter — worker should be enqueued`() {
        val sender = "GPAY"
        assertTrue(
            "Receiver should enqueue worker for financial sender: $sender",
            SmsSenderFilter.isFinancialSender(sender),
        )
    }

    @Test
    fun `financial sender VK-HDFCBK passes filter — worker should be enqueued`() {
        val sender = "VK-HDFCBK"
        assertTrue(
            "Receiver should enqueue worker for financial sender: $sender",
            SmsSenderFilter.isFinancialSender(sender),
        )
    }

    @Test
    fun `financial sender PAYTM passes filter — worker should be enqueued`() {
        val sender = "PAYTM"
        assertTrue(
            "Receiver should enqueue worker for financial sender: $sender",
            SmsSenderFilter.isFinancialSender(sender),
        )
    }

    @Test
    fun `financial sender SBI passes filter — worker should be enqueued`() {
        val sender = "SBI"
        assertTrue(
            "Receiver should enqueue worker for financial sender: $sender",
            SmsSenderFilter.isFinancialSender(sender),
        )
    }

    @Test
    fun `financial sender AXISBK passes filter — worker should be enqueued`() {
        val sender = "TX-AXISBK"
        assertTrue(
            "Receiver should enqueue worker for financial sender: $sender",
            SmsSenderFilter.isFinancialSender(sender),
        )
    }

    // --- Filter delegation: non-financial sender → should NOT enqueue ---

    @Test
    fun `non-financial sender AMAZON fails filter — worker should NOT be enqueued`() {
        val sender = "AMAZON"
        assertFalse(
            "Receiver should NOT enqueue worker for non-financial sender: $sender",
            SmsSenderFilter.isFinancialSender(sender),
        )
    }

    @Test
    fun `non-financial sender VM-OTPSVC fails filter — worker should NOT be enqueued`() {
        val sender = "VM-OTPSVC"
        assertFalse(
            "Receiver should NOT enqueue worker for non-financial sender: $sender",
            SmsSenderFilter.isFinancialSender(sender),
        )
    }

    @Test
    fun `phone number sender fails filter — worker should NOT be enqueued`() {
        val sender = "+919876543210"
        assertFalse(
            "Receiver should NOT enqueue worker for phone number sender: $sender",
            SmsSenderFilter.isFinancialSender(sender),
        )
    }

    @Test
    fun `empty sender fails filter — worker should NOT be enqueued`() {
        val sender = ""
        assertFalse(
            "Receiver should NOT enqueue worker for empty sender",
            SmsSenderFilter.isFinancialSender(sender),
        )
    }

    // --- Worker input data key constants are defined ---

    @Test
    fun `SmsProcessingWorker KEY_SENDER is defined`() {
        val key = SmsProcessingWorker.KEY_SENDER
        assertTrue("KEY_SENDER must not be blank", key.isNotBlank())
    }

    @Test
    fun `SmsProcessingWorker KEY_BODY is defined`() {
        val key = SmsProcessingWorker.KEY_BODY
        assertTrue("KEY_BODY must not be blank", key.isNotBlank())
    }

    @Test
    fun `SmsProcessingWorker KEY_RECEIVED_AT is defined`() {
        val key = SmsProcessingWorker.KEY_RECEIVED_AT
        assertTrue("KEY_RECEIVED_AT must not be blank", key.isNotBlank())
    }
}
