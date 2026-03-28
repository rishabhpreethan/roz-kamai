package com.viis.rozkamai.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FailedTransactionDetectorTest {

    @Test fun `transaction failed keyword detected`() =
        assertTrue(FailedTransactionDetector.isFailedTransaction("Your GPay transaction of Rs 500 has failed."))

    @Test fun `payment failed keyword detected`() =
        assertTrue(FailedTransactionDetector.isFailedTransaction("Payment of Rs 100 failed. Try again."))

    @Test fun `transfer failed keyword detected`() =
        assertTrue(FailedTransactionDetector.isFailedTransaction("Transfer of Rs 200 failed due to network error."))

    @Test fun `insufficient funds detected`() =
        assertTrue(FailedTransactionDetector.isFailedTransaction("Transaction declined due to insufficient funds."))

    @Test fun `insufficient balance detected`() =
        assertTrue(FailedTransactionDetector.isFailedTransaction("Insufficient balance. Transaction could not proceed."))

    @Test fun `transaction declined detected`() =
        assertTrue(FailedTransactionDetector.isFailedTransaction("Your transaction of Rs 300 was declined by the bank."))

    @Test fun `transaction unsuccessful detected`() =
        assertTrue(FailedTransactionDetector.isFailedTransaction("Transaction unsuccessful. Rs 150 could not be debited."))

    @Test fun `could not be processed detected`() =
        assertTrue(FailedTransactionDetector.isFailedTransaction("Rs 500 could not be processed at this time."))

    @Test fun `not processed detected`() =
        assertTrue(FailedTransactionDetector.isFailedTransaction("Amount not processed. Please retry."))

    @Test fun `normal credit SMS not flagged`() =
        assertFalse(FailedTransactionDetector.isFailedTransaction("Rs. 100 credited to your account by user@okaxis via GPay."))

    @Test fun `OTP SMS not flagged`() =
        assertFalse(FailedTransactionDetector.isFailedTransaction("Your OTP is 123456. Do not share."))

    @Test fun `extractAmount returns amount from failed SMS`() {
        val amount = FailedTransactionDetector.extractAmount("GPay transaction of Rs 500 has failed.")
        assertEquals(500.0, amount!!, 0.001)
    }

    @Test fun `extractAmount returns null when no amount`() {
        assertNull(FailedTransactionDetector.extractAmount("Transaction failed. Please try again."))
    }

    @Test fun `extractAmount handles decimal amounts`() {
        val amount = FailedTransactionDetector.extractAmount("Rs. 1,250.50 payment failed.")
        assertEquals(1250.50, amount!!, 0.001)
    }
}
