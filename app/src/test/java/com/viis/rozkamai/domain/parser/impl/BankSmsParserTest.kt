package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Covers SBI, HDFC, ICICI, Axis parsers against sample SMS data. */
class BankSmsParserTest {

    private val receivedAt = 1000L

    // ── SBI ──────────────────────────────────────────────────────────────────

    private val sbi = SbiBankSmsParser()

    @Test fun `SBI canParse true for SBI sender`() = assertEquals(true, sbi.canParse("SBI", ""))
    @Test fun `SBI canParse true for SBIUPI sender`() = assertEquals(true, sbi.canParse("SBIUPI", ""))

    @Test fun `SBI parse credit pattern 1`() {
        val result = sbi.parse("SBI", "Your A/c XXXX1234 is credited by Rs 500 on 01/01/25. Balance Rs 10500", receivedAt)
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals(PaymentSource.SBI, result.source)
    }

    @Test fun `SBI parse credit pattern 2`() {
        val result = sbi.parse("SBI", "SBI: INR 1000.00 credited to A/C XXXX5678 on 02/01/25 by UPI. Avl Bal: INR 15000.00", receivedAt)
        assertNotNull(result)
        assertEquals(1000.0, result!!.amount, 0.001)
    }

    @Test fun `SBI parse debit`() {
        val result = sbi.parse("SBI", "Your A/c XXXX1234 is debited by Rs 200 on 01/01/25. Balance Rs 10300", receivedAt)
        assertNotNull(result)
        assertEquals(200.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
    }

    @Test fun `SBI priority is 40`() = assertEquals(40, sbi.priority)

    // ── HDFC ─────────────────────────────────────────────────────────────────

    private val hdfc = HdfcBankSmsParser()

    @Test fun `HDFC canParse true for HDFCBK sender`() = assertEquals(true, hdfc.canParse("HDFCBK", ""))

    @Test fun `HDFC parse credit pattern 1`() {
        val result = hdfc.parse("HDFCBK", "Rs 300 credited to HDFC Bank A/c XX1234 by NEFT/UPI on 01-01-25. Avl Bal Rs 8300", receivedAt)
        assertNotNull(result)
        assertEquals(300.0, result!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals(PaymentSource.HDFC, result.source)
    }

    @Test fun `HDFC parse credit pattern 2`() {
        val result = hdfc.parse("HDFCBK", "Money received! Rs 750.00 to HDFC A/c XXXX on 02-01-25 via UPI", receivedAt)
        assertNotNull(result)
        assertEquals(750.0, result!!.amount, 0.001)
    }

    @Test fun `HDFC priority is 50`() = assertEquals(50, hdfc.priority)

    // ── ICICI ────────────────────────────────────────────────────────────────

    private val icici = IciciBankSmsParser()

    @Test fun `ICICI canParse true for ICICIB sender`() = assertEquals(true, icici.canParse("ICICIB", ""))

    @Test fun `ICICI parse credit pattern 1`() {
        val result = icici.parse("ICICIB", "ICICI Bank: Rs 450.00 credited to A/c XX1234 on 01-Jan-25 by UPI. Avl Bal: Rs 12450.00", receivedAt)
        assertNotNull(result)
        assertEquals(450.0, result!!.amount, 0.001)
        assertEquals(PaymentSource.ICICI, result.source)
    }

    @Test fun `ICICI parse credit pattern 2`() {
        val result = icici.parse("ICICIB", "Dear Customer, Rs 200 has been credited to your ICICI Bank A/c XXXX via UPI Ref TEST040", receivedAt)
        assertNotNull(result)
        assertEquals(200.0, result!!.amount, 0.001)
    }

    @Test fun `ICICI priority is 60`() = assertEquals(60, icici.priority)

    // ── Axis ─────────────────────────────────────────────────────────────────

    private val axis = AxisBankSmsParser()

    @Test fun `Axis canParse true for AXISBK sender`() = assertEquals(true, axis.canParse("AXISBK", ""))

    @Test fun `Axis parse credit pattern 1`() {
        val result = axis.parse("AXISBK", "Axis Bank: Rs.600.00 credited to your A/c XXXX on 01Jan25 by UPI. Avl Bal: Rs.9600.00", receivedAt)
        assertNotNull(result)
        assertEquals(600.0, result!!.amount, 0.001)
        assertEquals(PaymentSource.AXIS, result.source)
    }

    @Test fun `Axis parse credit pattern 2`() {
        val result = axis.parse("AXISBK", "INR 350 credited to Axis Bank A/C XXXX via UPI on 01/01/25. Bal: INR 5350", receivedAt)
        assertNotNull(result)
        assertEquals(350.0, result!!.amount, 0.001)
    }

    @Test fun `Axis priority is 70`() = assertEquals(70, axis.priority)

    // ── shared: failed SMS returns null ───────────────────────────────────────

    @Test fun `SBI failed SMS returns null`() = assertNull(sbi.parse("SBI", "Transaction failed. Rs 100 could not be processed.", receivedAt))
    @Test fun `HDFC failed SMS returns null`() = assertNull(hdfc.parse("HDFCBK", "Transaction failed Rs 300", receivedAt))
}
