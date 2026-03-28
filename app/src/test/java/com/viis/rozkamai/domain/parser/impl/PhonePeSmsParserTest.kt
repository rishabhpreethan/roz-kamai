package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhonePeSmsParserTest {

    private lateinit var parser: PhonePeSmsParser
    private val receivedAt = 1000L

    @Before fun setUp() { parser = PhonePeSmsParser() }

    @Test fun `canParse true for PhonePe sender`() = assertTrue(parser.canParse("PhonePe", "any"))
    @Test fun `canParse true for body with PhonePe`() = assertTrue(parser.canParse("XX", "Rs 200 credited on PhonePe"))

    @Test
    fun `parse credited-with pattern`() {
        val body = "Your PhonePe A/c XXXX is credited with Rs 200 by user@ybl. UPI Ref: TEST020"
        val result = parser.parse("PhonePe", body, receivedAt)
        assertNotNull(result)
        assertEquals(200.0, result!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals(PaymentSource.PHONEPE, result.source)
        assertNotNull(result.upiHandleHash)
    }

    @Test
    fun `parse received-from pattern`() {
        val body = "Money received! Rs.300.00 received from buyer@axl on PhonePe. Ref: TEST022"
        val result = parser.parse("PhonePe", body, receivedAt)
        assertNotNull(result)
        assertEquals(300.0, result!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, result.type)
    }

    @Test
    fun `parse debit pattern`() {
        val body = "Rs 150 debited from your PhonePe A/c XXXX to merchant@ybl. Ref: TEST030"
        val result = parser.parse("PhonePe", body, receivedAt)
        assertNotNull(result)
        assertEquals(150.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
    }

    @Test fun `parse failed SMS returns null`() {
        assertNull(parser.parse("PhonePe", "PhonePe transaction of Rs 200 to merchant@ybl failed.", receivedAt))
    }

    @Test fun `source is PHONEPE`() = assertEquals(PaymentSource.PHONEPE, parser.source)
    @Test fun `priority is 20`() = assertEquals(20, parser.priority)
}
