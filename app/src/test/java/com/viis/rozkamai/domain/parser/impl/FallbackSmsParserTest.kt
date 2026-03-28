package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FallbackSmsParserTest {

    private lateinit var parser: FallbackSmsParser
    private val receivedAt = 1000L

    @Before fun setUp() { parser = FallbackSmsParser() }

    @Test fun `canParse always true`() = assertTrue(parser.canParse("ANY", "any body"))
    @Test fun `priority is 100`() = assertEquals(100, parser.priority)
    @Test fun `source is UPI`() = assertEquals(PaymentSource.UPI, parser.source)

    @Test fun `parse credit keyword`() {
        val result = parser.parse("UNKNWN", "Rs 250 credited to your account", receivedAt)
        assertNotNull(result)
        assertEquals(250.0, result!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, result.type)
    }

    @Test fun `parse debit keyword`() {
        val result = parser.parse("UNKNWN", "Rs 100 debited from account", receivedAt)
        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
    }

    @Test fun `parse received keyword`() {
        val result = parser.parse("UNKNWN", "INR 500 received in your account", receivedAt)
        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result!!.type)
    }

    @Test fun `returns null if no amount found`() = assertNull(parser.parse("UNKNWN", "Your account is active", receivedAt))
    @Test fun `returns null if direction unknown`() = assertNull(parser.parse("UNKNWN", "Rs 100 processed", receivedAt))
    @Test fun `returns null for failed SMS`() = assertNull(parser.parse("UNKNWN", "Rs 100 failed to transfer", receivedAt))
    @Test fun `returns null for zero amount`() = assertNull(parser.parse("UNKNWN", "Rs 0 credited to account", receivedAt))

    @Test fun `non-financial SMS (OTP) returns null`() {
        assertNull(parser.parse("BANK", "Your OTP is 123456. Do not share.", receivedAt))
    }
}
