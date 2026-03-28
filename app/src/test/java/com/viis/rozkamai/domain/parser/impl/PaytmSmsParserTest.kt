package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaytmSmsParserTest {

    private lateinit var parser: PaytmSmsParser
    private val receivedAt = 1000L

    @Before fun setUp() { parser = PaytmSmsParser() }

    @Test fun `parse received-from-Paytm pattern`() {
        val result = parser.parse("PAYTM", "Received Rs. 150 from Paytm user. Txn ID: PAYTM100001", receivedAt)
        assertNotNull(result)
        assertEquals(150.0, result!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("PAYTM100001", result.referenceId)
    }

    @Test fun `parse received-in-wallet pattern`() {
        val result = parser.parse("PAYTM", "You have received Rs. 400 in your Paytm wallet from customer. Txn ID: PAYTM100002", receivedAt)
        assertNotNull(result)
        assertEquals(400.0, result!!.amount, 0.001)
    }

    @Test fun `parse Paytm-prefix pattern`() {
        val result = parser.parse("PAYTM", "Paytm: Rs.600 received from buyer. Txn ID: PAYTM100003", receivedAt)
        assertNotNull(result)
        assertEquals(600.0, result!!.amount, 0.001)
    }

    @Test fun `parse failed SMS returns null`() {
        assertNull(parser.parse("PAYTM", "Your Paytm payment of Rs. 150 has failed.", receivedAt))
    }

    @Test fun `parse declined SMS returns null`() {
        assertNull(parser.parse("PAYTM", "Paytm: Rs.300 payment declined.", receivedAt))
    }

    @Test fun `canParse true for PAYTM sender`() = assertTrue(parser.canParse("PAYTM", "any"))
    @Test fun `canParse true for PYTMUPI sender`() = assertTrue(parser.canParse("PYTMUPI", "any"))
    @Test fun `canParse true for body containing Paytm`() = assertTrue(parser.canParse("UNKNOWN", "Rs 100 received via Paytm"))

    @Test fun `decimal amount is parsed correctly`() {
        val result = parser.parse("PAYTM", "Received Rs. 250.75 from Paytm user.", receivedAt)
        assertNotNull(result)
        assertEquals(250.75, result!!.amount, 0.001)
    }

    @Test fun `rawSenderMasked has correct format`() {
        val result = parser.parse("PAYTM", "Received Rs. 150 from Paytm user.", receivedAt)!!
        assertEquals("PAYT***", result.rawSenderMasked)
    }

    @Test fun `upiHandleHash is null for Paytm (not in SMS body)`() {
        val result = parser.parse("PAYTM", "Received Rs. 150 from Paytm user.", receivedAt)!!
        assertNull(result.upiHandleHash)
    }

    @Test fun `source is PAYTM`() = assertEquals(PaymentSource.PAYTM, parser.source)
    @Test fun `priority is 30`() = assertEquals(30, parser.priority)
}
