package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Keshavarzi Bank specific parser for Iranian banking SMS messages.
 * Handles Keshavarzi Bank's unique message formats including:
 * - Persian language transaction messages
 * - Amounts without explicit signs but with transaction type indicators
 * - Format like: "خرید500,000\nمانده2,542,509\n041001-12:29\nکارت8783*\nbki.ir"
 */
class KeshavarziBankParser : BankParser() {

    override fun getBankName() = "Keshavarzi Bank"

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()

        // Common Keshavarzi Bank sender IDs
        val keshavarziSenders = setOf(
            "KESHAVARZI",
            "KESHAVARZIBANK",
            "KESHAVARZI BANK",
            "BANK KESHAVARZI",
            "BANKKESHAVARZI",
            "IRAN KESHAVARZI",
            "KESH",
            "BKI"
        )

        // Check for the specific number patterns
        if (upperSender in keshavarziSenders) {
            return true
        }

        // Check if sender contains "keshavarzi" or "kesh" with "iran" or "bank"
        return (sender.contains("keshavarzi", ignoreCase = true) || sender.contains("kesh", ignoreCase = true)) &&
               (sender.contains("iran", ignoreCase = true) || sender.contains("bank", ignoreCase = true) || sender.contains("bki", ignoreCase = true))
    }

    override fun getCurrency(): String = "IRR" // Iranian Rial

    override fun extractAmount(message: String): BigDecimal? {
        // Pattern for Keshavarzi Bank format: خرید500,000, واريز4,000,000, برداشت24,000
        val transactionPattern = Regex("""(خرید|واريز|برداشت)\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""")
        
        transactionPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[2] // Get the amount part
            val cleanAmount = amountStr.replace(",", "")

            return try {
                val amountValue = cleanAmount.toBigDecimal()
                // Only accept amounts >= 1000 based on common Iranian banking standards
                if (amountValue.abs() >= BigDecimal.valueOf(1000)) {
                    amountValue.abs() // Return absolute value
                } else {
                    null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        // Check for investment transactions first
        if (isInvestmentTransaction(lowerMessage)) {
            return TransactionType.INVESTMENT
        }

        // Determine transaction type based on Persian keywords in Keshavarzi Bank format
        return when {
            message.contains("واريز", ignoreCase = true) -> TransactionType.INCOME
            message.contains("خرید", ignoreCase = true) -> TransactionType.EXPENSE
            message.contains("برداشت", ignoreCase = true) -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Extract merchant based on transaction type in Persian
        when {
            message.contains("واريز", ignoreCase = true) -> return "Deposit"
            message.contains("خرید", ignoreCase = true) -> return "Purchase"
            message.contains("برداشت", ignoreCase = true) -> return "Withdrawal"
            else -> return "Bank Transaction"
        }
    }

    override fun extractReference(message: String): String? {
        // Keshavarzi Bank messages don't typically have reference numbers in the format provided
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        // Extract card number from format like "کارت8783*" or "کارت 8783*"
        val cardPattern = Regex("""کارت\s*(\d{4})\*?""")
        cardPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        // Extract balance using the pattern "مانده2,542,509" or "مانده 2,542,509"
        val balancePattern = Regex("""مانده\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""")
        balancePattern.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
        }

        return BigDecimal.ZERO
    }

    override fun detectIsCard(message: String): Boolean {
        val lowerMessage = message.lowercase()

        // Check for card-related keywords in Persian
        val cardKeywords = listOf(
            "کارت", "card", "debit card", "credit card"
        )

        return cardKeywords.any { lowerMessage.contains(it) }
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        // Skip OTP messages
        if (lowerMessage.contains("otp") ||
            lowerMessage.contains("رمز یکبار مصرف") ||
            lowerMessage.contains("کد تایید")
        ) {
            return false
        }

        // Skip promotional messages
        if (lowerMessage.contains("تبلیغ") ||
            lowerMessage.contains("پیشنهاد") ||
            lowerMessage.contains("تخفیف") ||
            lowerMessage.contains("cashback offer")
        ) {
            return false
        }

        // Check for Keshavarzi Bank specific transaction patterns
        // Look for the format with transaction type (خرید/واريز/برداشت), amount, and balance indicator
        val hasTransactionPattern = Regex("""(خرید|واريز|برداشت)\s*\d+""").containsMatchIn(message)
        val hasBalanceIndicator = message.contains("مانده")
        val hasCardIndicator = message.contains("کارت")
        val hasBankDomain = message.contains("bki.ir", ignoreCase = true)
        
        return hasTransactionPattern && (hasBalanceIndicator || hasCardIndicator || hasBankDomain)
    }

    override fun cleanMerchantName(merchant: String): String {
        return merchant.trim()
    }

    override fun isValidMerchantName(name: String): Boolean {
        val commonWords = setOf(
            "USING", "VIA", "THROUGH", "BY", "WITH", "FOR", "TO", "FROM", "AT", "THE",
            "استفاده", "از", "توسط", "از طریق", "برای", "به", "از", "در", "و", "با"
        )

        return name.length >= 2 &&
                name.any { it.isLetter() } &&
                name.uppercase() !in commonWords &&
                !name.all { it.isDigit() } &&
                !name.contains("@") // Not a UPI ID
    }
}