package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.util.HashUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GPaySmsParserTest {

    private lateinit var parser: GPaySmsParser
    private val receivedAt = 1000L

    @Before
    fun setUp() {
        parser = GPaySmsParser()
    }

    // ── canParse ─────────────────────────────────────────────────────────────

    @Test fun `canParse true for GPAY sender`() = assertTrue(parser.canParse("GPAY", "any body"))
    @Test fun `canParse true for body containing via GPay`() = assertTrue(parser.canParse("UNKNOWN", "Rs. 100 credited via GPay"))
    @Test fun `canParse false for unrelated sender and body`() = assertTrue(!parser.canParse("ZOMATO", "Your order is ready"))

    // ── credit parsing ───────────────────────────────────────────────────────

    @Test
    fun `parse credit SMS returns CREDIT transaction`() {
        val body = "Rs. 100 credited to your account by user@okaxis via GPay. Ref: TEST001"
        val result = parser.parse("GPAY", body, receivedAt)
        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result!!.type)
        assertEquals(100.0, result.amount, 0.001)
        assertEquals(PaymentSource.GPAY, result.source)
    }

    @Test
    fun `parse credit SMS hashes UPI handle`() {
        val body = "Rs. 500 credited to your account by customer@oksbi via GPay. Ref: TEST002"
        val result = parser.parse("GPAY", body, receivedAt)
        assertNotNull(result?.upiHandleHash)
        assertEquals(64, result!!.upiHandleHash!!.length) // SHA-256 hex
    }

    @Test
    fun `parse credit SMS extracts reference ID`() {
        val body = "Rs. 100 credited to your account by user@okaxis via GPay. Ref: TEST001"
        val result = parser.parse("GPAY", body, receivedAt)
        assertEquals("TEST001", result?.referenceId)
    }

    @Test
    fun `parse credit SMS with comma-formatted amount`() {
        val body = "Rs. 1,000 credited to your account by user@okaxis via GPay. Ref: TEST003"
        val result = parser.parse("GPAY", body, receivedAt)
        assertEquals(1000.0, result!!.amount, 0.001)
    }

    // ── debit parsing ────────────────────────────────────────────────────────

    @Test
    fun `parse debit SMS returns DEBIT transaction`() {
        val body = "Rs. 100 debited from your account to merchant@oksbi via GPay. Ref: TEST010"
        val result = parser.parse("GPAY", body, receivedAt)
        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(100.0, result.amount, 0.001)
    }

    // ── failure cases ────────────────────────────────────────────────────────

    @Test
    fun `parse failed SMS returns null`() {
        val body = "Your GPay transaction of Rs. 500 to merchant@okaxis has failed. Please try again."
        assertNull(parser.parse("GPAY", body, receivedAt))
    }

    @Test
    fun `parse declined SMS returns null`() {
        val body = "Payment of Rs. 100 declined via GPay."
        assertNull(parser.parse("GPAY", body, receivedAt))
    }

    @Test
    fun `parse unrecognised body returns null`() {
        assertNull(parser.parse("GPAY", "GPay is now available in your region!", receivedAt))
    }

    // ── metadata ─────────────────────────────────────────────────────────────

    @Test fun `priority is 10`() = assertEquals(10, parser.priority)
    @Test fun `source is GPAY`() = assertEquals(PaymentSource.GPAY, parser.source)

    @Test
    fun `rawSenderMasked is first 4 chars plus stars`() {
        val body = "Rs. 100 credited to your account by user@okaxis via GPay. Ref: TEST001"
        val result = parser.parse("GPAY", body, receivedAt)
        assertEquals("GPAY***", result?.rawSenderMasked)
    }

    @Test
    fun `credit UPI hash is SHA-256 of lowercase handle`() {
        val body = "Rs. 500 credited to your account by USER@OkAxis via GPay."
        val result = parser.parse("GPAY", body, receivedAt)!!
        assertEquals(HashUtils.sha256("user@okaxis"), result.upiHandleHash)
    }

    @Test
    fun `debit SMS hashes UPI handle`() {
        val body = "Rs. 100 debited from your account to merchant@oksbi via GPay."
        val result = parser.parse("GPAY", body, receivedAt)!!
        assertEquals(HashUtils.sha256("merchant@oksbi"), result.upiHandleHash)
    }

    @Test
    fun `timestamp is preserved`() {
        val body = "Rs. 500 credited to your account by user@okaxis via GPay."
        val result = parser.parse("GPAY", body, 9876543L)!!
        assertEquals(9876543L, result.timestamp)
    }
}
