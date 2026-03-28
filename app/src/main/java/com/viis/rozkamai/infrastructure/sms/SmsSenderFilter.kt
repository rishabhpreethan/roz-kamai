package com.viis.rozkamai.infrastructure.sms

/**
 * Filters SMS senders to known financial institution sender IDs.
 * Case-insensitive contains check — covers partial matches (e.g. "VK-GPAY" contains "GPAY").
 *
 * Covers P1-003: SMS sender ID filter list.
 */
object SmsSenderFilter {

    private val FINANCIAL_SENDER_IDS = listOf(
        "GPAY",
        "PhonePe",
        "PAYTM",
        "PYTMUPI",
        "SBI",
        "SBIUPI",
        "HDFCBK",
        "ICICIB",
        "AXISBK",
        "KOTAKB",
        "YESBNK",
        "INDBNK",
        "PNBSMS",
        "BOIIND",
        "CANBNK",
    )

    /**
     * Returns true if [sender] contains any known financial sender ID (case-insensitive).
     * Returns false for blank or empty senders.
     */
    fun isFinancialSender(sender: String): Boolean {
        if (sender.isBlank()) return false
        val upperSender = sender.uppercase()
        return FINANCIAL_SENDER_IDS.any { id -> upperSender.contains(id.uppercase()) }
    }
}
