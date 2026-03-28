package com.viis.rozkamai.domain.parser

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.parser.impl.AxisBankSmsParser
import com.viis.rozkamai.domain.parser.impl.FallbackSmsParser
import com.viis.rozkamai.domain.parser.impl.GPaySmsParser
import com.viis.rozkamai.domain.parser.impl.HdfcBankSmsParser
import com.viis.rozkamai.domain.parser.impl.IciciBankSmsParser
import com.viis.rozkamai.domain.parser.impl.PaytmSmsParser
import com.viis.rozkamai.domain.parser.impl.PhonePeSmsParser
import com.viis.rozkamai.domain.parser.impl.SbiBankSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Parser accuracy validation against the anonymized SMS sample dataset at
 * app/src/test/resources/sms-samples/.
 *
 * Each test loads a sample file, runs every line through the full ParserRegistry,
 * and asserts that:
 *   - The correct parser claimed the sample (right source)
 *   - The transaction direction (CREDIT / DEBIT) is correct
 *   - The extracted amount is non-zero and positive
 *   - Failed/non-financial samples return null
 *
 * This acts as a regression guard: if a regex change breaks real-world sample
 * parsing, these tests will catch it before CI merges the change.
 */
class ParserAccuracyTest {

    private lateinit var registry: ParserRegistry
    private val receivedAt = 1_000_000L

    // Resolve sample dir relative to the Android module root (working dir during unit tests)
    private val sampleDir = File("src/test/resources/sms-samples")

    @Before
    fun setUp() {
        registry = ParserRegistry(
            setOf(
                GPaySmsParser(),
                PhonePeSmsParser(),
                PaytmSmsParser(),
                SbiBankSmsParser(),
                HdfcBankSmsParser(),
                IciciBankSmsParser(),
                AxisBankSmsParser(),
                FallbackSmsParser(),
            ),
        )
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun samples(source: String, category: String): List<String> =
        File(sampleDir, "$source/$category.txt")
            .readLines()
            .filter { it.isNotBlank() }

    private fun sender(source: String): String = when (source) {
        "gpay" -> "GPAY"
        "phonepe" -> "PhonePe"
        "paytm" -> "PAYTM"
        "sbi" -> "SBI"
        "hdfc" -> "HDFCBK"
        "icici" -> "ICICIB"
        "axis" -> "AXISBK"
        else -> "UNKNOWN"
    }

    // ── GPay ─────────────────────────────────────────────────────────────────

    @Test
    fun `all GPay credit samples parse as GPAY CREDIT`() {
        samples("gpay", "credit").forEach { body ->
            val result = registry.parse(sender("gpay"), body, receivedAt)
            assertNotNull("Expected parse result for: $body", result)
            assertEquals("GPay credit should be CREDIT for: $body", TransactionType.CREDIT, result!!.type)
            assertEquals("GPay credit should have source GPAY for: $body", PaymentSource.GPAY, result.source)
            assertTrue("GPay credit amount should be positive for: $body", result.amount > 0)
        }
    }

    @Test
    fun `all GPay credit samples contain a hashed UPI handle`() {
        samples("gpay", "credit").forEach { body ->
            val result = registry.parse(sender("gpay"), body, receivedAt)
            assertNotNull("GPay credit should have upiHandleHash for: $body", result?.upiHandleHash)
            assertEquals("UPI hash should be 64 hex chars for: $body", 64, result!!.upiHandleHash!!.length)
        }
    }

    @Test
    fun `all GPay debit samples parse as GPAY DEBIT`() {
        samples("gpay", "debit").forEach { body ->
            val result = registry.parse(sender("gpay"), body, receivedAt)
            assertNotNull("Expected parse result for: $body", result)
            assertEquals(TransactionType.DEBIT, result!!.type)
            assertEquals(PaymentSource.GPAY, result.source)
        }
    }

    @Test
    fun `all GPay failed samples return null`() {
        samples("gpay", "failed").forEach { body ->
            val result = registry.parse(sender("gpay"), body, receivedAt)
            assertNull("Expected null for failed SMS: $body", result)
        }
    }

    // ── PhonePe ──────────────────────────────────────────────────────────────

    @Test
    fun `all PhonePe credit samples parse as PHONEPE CREDIT`() {
        samples("phonepe", "credit").forEach { body ->
            val result = registry.parse(sender("phonepe"), body, receivedAt)
            assertNotNull("Expected parse result for: $body", result)
            assertEquals(TransactionType.CREDIT, result!!.type)
            assertEquals(PaymentSource.PHONEPE, result.source)
            assertTrue(result.amount > 0)
        }
    }

    @Test
    fun `all PhonePe debit samples parse as PHONEPE DEBIT`() {
        samples("phonepe", "debit").forEach { body ->
            val result = registry.parse(sender("phonepe"), body, receivedAt)
            assertNotNull("Expected parse result for: $body", result)
            assertEquals(TransactionType.DEBIT, result!!.type)
            assertEquals(PaymentSource.PHONEPE, result.source)
        }
    }

    @Test
    fun `all PhonePe failed samples return null`() {
        samples("phonepe", "failed").forEach { body ->
            val result = registry.parse(sender("phonepe"), body, receivedAt)
            assertNull("Expected null for failed SMS: $body", result)
        }
    }

    // ── Paytm ─────────────────────────────────────────────────────────────────

    @Test
    fun `all Paytm credit samples parse as PAYTM CREDIT`() {
        samples("paytm", "credit").forEach { body ->
            val result = registry.parse(sender("paytm"), body, receivedAt)
            assertNotNull("Expected parse result for: $body", result)
            assertEquals(TransactionType.CREDIT, result!!.type)
            assertEquals(PaymentSource.PAYTM, result.source)
            assertTrue(result.amount > 0)
        }
    }

    // ── SBI ───────────────────────────────────────────────────────────────────

    @Test
    fun `all SBI credit samples parse as SBI CREDIT`() {
        samples("sbi", "credit").forEach { body ->
            val result = registry.parse(sender("sbi"), body, receivedAt)
            assertNotNull("Expected parse result for: $body", result)
            assertEquals(TransactionType.CREDIT, result!!.type)
            assertEquals(PaymentSource.SBI, result.source)
            assertTrue(result.amount > 0)
        }
    }

    @Test
    fun `all SBI debit samples parse as SBI DEBIT`() {
        samples("sbi", "debit").forEach { body ->
            val result = registry.parse(sender("sbi"), body, receivedAt)
            assertNotNull("Expected parse result for: $body", result)
            assertEquals(TransactionType.DEBIT, result!!.type)
            assertEquals(PaymentSource.SBI, result.source)
        }
    }

    // ── HDFC ──────────────────────────────────────────────────────────────────

    @Test
    fun `all HDFC credit samples parse as HDFC CREDIT`() {
        samples("hdfc", "credit").forEach { body ->
            val result = registry.parse(sender("hdfc"), body, receivedAt)
            assertNotNull("Expected parse result for: $body", result)
            assertEquals(TransactionType.CREDIT, result!!.type)
            assertEquals(PaymentSource.HDFC, result.source)
            assertTrue(result.amount > 0)
        }
    }

    // ── ICICI ─────────────────────────────────────────────────────────────────

    @Test
    fun `all ICICI credit samples parse as ICICI CREDIT`() {
        samples("icici", "credit").forEach { body ->
            val result = registry.parse(sender("icici"), body, receivedAt)
            assertNotNull("Expected parse result for: $body", result)
            assertEquals(TransactionType.CREDIT, result!!.type)
            assertEquals(PaymentSource.ICICI, result.source)
            assertTrue(result.amount > 0)
        }
    }

    // ── Axis ──────────────────────────────────────────────────────────────────

    @Test
    fun `all Axis credit samples parse as AXIS CREDIT`() {
        samples("axis", "credit").forEach { body ->
            val result = registry.parse(sender("axis"), body, receivedAt)
            assertNotNull("Expected parse result for: $body", result)
            assertEquals(TransactionType.CREDIT, result!!.type)
            assertEquals(PaymentSource.AXIS, result.source)
            assertTrue(result.amount > 0)
        }
    }

    // ── Unknown / non-financial ───────────────────────────────────────────────

    @Test
    fun `non-financial samples all return null`() {
        samples("unknown", "non-financial").forEach { body ->
            val result = registry.parse("UNKNOWN", body, receivedAt)
            assertNull("Expected null for non-financial SMS: $body", result)
        }
    }

    @Test
    fun `ambiguous samples without amount or direction return null`() {
        samples("unknown", "ambiguous").forEach { body ->
            val result = registry.parse("UNKNOWN", body, receivedAt)
            // Ambiguous samples have no amount or direction — fallback parser should return null
            assertNull("Expected null for ambiguous SMS: $body", result)
        }
    }

    // ── Dataset completeness ──────────────────────────────────────────────────

    @Test
    fun `GPay duplicate sample parses successfully (dedup handled at use-case layer)`() {
        // Both lines in duplicate.txt are identical — registry alone has no dedup,
        // so both should parse. Dedup is the use-case's responsibility.
        val lines = samples("gpay", "duplicate")
        assertEquals("duplicate.txt should have 2 lines", 2, lines.size)
        lines.forEach { body ->
            val result = registry.parse(sender("gpay"), body, receivedAt)
            assertNotNull(result)
            assertEquals(TransactionType.CREDIT, result!!.type)
        }
    }
}
