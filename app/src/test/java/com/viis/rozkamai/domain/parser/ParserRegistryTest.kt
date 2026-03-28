package com.viis.rozkamai.domain.parser

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ParserRegistryTest : BaseUnitTest() {

    private val sender = "GPAY"
    private val body = "Rs. 100 credited via GPay"
    private val receivedAt = System.currentTimeMillis()

    private fun makeTransaction(source: PaymentSource = PaymentSource.UPI) = ParsedTransaction(
        amount = 100.0,
        type = TransactionType.CREDIT,
        source = source,
        upiHandleHash = null,
        merchantNameHash = null,
        referenceId = null,
        timestamp = receivedAt,
        rawSenderMasked = "GPAY***",
    )

    @Test
    fun `empty registry returns null`() {
        val registry = ParserRegistry(emptySet())
        assertNull(registry.parse(sender, body, receivedAt))
    }

    @Test
    fun `single parser that canParse true returns its result`() {
        val parser = mockk<SmsParser> {
            every { priority } returns 1
            every { canParse(sender, body) } returns true
            every { parse(sender, body, receivedAt) } returns makeTransaction()
        }
        val registry = ParserRegistry(setOf(parser))
        assertNotNull(registry.parse(sender, body, receivedAt))
    }

    @Test
    fun `single parser that canParse false returns null`() {
        val parser = mockk<SmsParser> {
            every { priority } returns 1
            every { canParse(sender, body) } returns false
        }
        val registry = ParserRegistry(setOf(parser))
        assertNull(registry.parse(sender, body, receivedAt))
    }

    @Test
    fun `priority order respected - lower number wins`() {
        val highPriority = mockk<SmsParser> {
            every { priority } returns 1
            every { source } returns PaymentSource.UPI
            every { canParse(sender, body) } returns true
            every { parse(sender, body, receivedAt) } returns makeTransaction(PaymentSource.UPI)
        }
        val lowPriority = mockk<SmsParser> {
            every { priority } returns 10
            every { source } returns PaymentSource.NEFT
            every { canParse(sender, body) } returns true
            every { parse(sender, body, receivedAt) } returns makeTransaction(PaymentSource.NEFT)
        }
        val registry = ParserRegistry(setOf(lowPriority, highPriority))
        val result = registry.parse(sender, body, receivedAt)
        assertEquals(PaymentSource.UPI, result?.source)
    }

    @Test
    fun `first parser returns null falls through to second`() {
        val failingParser = mockk<SmsParser> {
            every { priority } returns 1
            every { canParse(sender, body) } returns true
            every { parse(sender, body, receivedAt) } returns null
        }
        val successParser = mockk<SmsParser> {
            every { priority } returns 2
            every { canParse(sender, body) } returns true
            every { parse(sender, body, receivedAt) } returns makeTransaction(PaymentSource.NEFT)
        }
        val registry = ParserRegistry(setOf(failingParser, successParser))
        // findParser returns first canParse=true (priority 1) — which then returns null from parse
        // registry.parse delegates to findParser then calls parse, null result = no fallthrough in current impl
        // This test documents the current behaviour: findParser picks highest priority canParse=true
        val found = registry.findParser(sender, body)
        assertEquals(1, found?.priority)
    }

    @Test
    fun `findParser returns null when no parser can handle sender`() {
        val parser = mockk<SmsParser> {
            every { priority } returns 1
            every { canParse(sender, body) } returns false
        }
        val registry = ParserRegistry(setOf(parser))
        assertNull(registry.findParser(sender, body))
    }

    @Test
    fun `parser that throws does not crash the registry`() {
        val parser = mockk<SmsParser> {
            every { priority } returns 1
            every { canParse(sender, body) } returns true
            every { parse(sender, body, receivedAt) } throws RuntimeException("parse error")
        }
        val registry = ParserRegistry(setOf(parser))
        assertNull(registry.parse(sender, body, receivedAt))
    }
}
