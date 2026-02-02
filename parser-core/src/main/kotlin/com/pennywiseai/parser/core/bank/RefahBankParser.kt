package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Refah Bank specific parser for Iranian banking SMS messages.
 * Handles Refah Bank's unique message formats including:
 * - Persian language transaction messages
 * - Amounts with + and - signs indicating income/expense
 * - Format like: "بانک رفاه\nحساب207853186\nکارت5,000,000+\nمانده81,108,644\n11/13-00:43"
 */
class RefahBankParser : BankParser() {

    override fun getBankName() = "Refah Bank"

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()

        // Common Refah Bank sender IDs
        val refahSenders = setOf(
            "REFAH",
            "REFAHBANK",
            "REFAH BANK",
            "BANK REFAH",
            "BANKREFAH",
            "IRAN REFAH",
            "RF-BANK"
        )

        // Check for the specific number patterns
        if (upperSender in refahSenders) {
            return true
        }

        // Check if sender contains "refah" with "iran" or "bank"
        return sender.contains("refah", ignoreCase = true) &&
               (sender.contains("iran", ignoreCase = true) || sender.contains("bank", ignoreCase = true))
    }

    override fun getCurrency(): String = "IRR" // Iranian Rial

    override fun extractAmount(message: String): BigDecimal? {
        // Pattern for Refah Bank format: کارت5,000,000+ or خرید2,450,000-
        val transactionPattern = Regex("""(کارت|خرید|برداشت|واریز)\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)[+-]""")
        
        transactionPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[2] // Get the amount part
            val cleanAmount = amountStr.replace(",", "")

            return try {
                val amountValue = cleanAmount.toBigDecimal()
                // Only accept amounts >= 1000 based on common Iranian banking standards
                if (amountValue.abs() >= BigDecimal.valueOf(1000)) {
                    amountValue.abs() // Return absolute value since sign is handled by transaction type
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

        // Check for + or - signs after amounts in the message to determine transaction type
        // Pattern for Refah Bank format: کارت5,000,000+ (income) or خرید2,450,000- (expense)
        val transactionPattern = Regex("""(کارت|خرید|برداشت|واریز)\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)([+-])""")
        transactionPattern.find(message)?.let { match ->
            val sign = match.groupValues[3] // Get the sign after the amount
            return if (sign == "+") {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            }
        }

        return null
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Extract merchant based on transaction type in Persian
        val lowerMessage = message.lowercase()
        
        when {
            message.contains("کارت", ignoreCase = true) -> return "Card Transaction"
            message.contains("خرید", ignoreCase = true) -> return "Purchase"
            message.contains("برداشت", ignoreCase = true) -> return "Withdrawal"
            message.contains("واریز", ignoreCase = true) -> return "Deposit"
            else -> return "Bank Transaction"
        }
    }

    override fun extractReference(message: String): String? {
        // Refah Bank messages don't typically have reference numbers in the format provided
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        // Extract account number from format like "حساب207853186"
        val accountPattern = Regex("""حساب\s*(\d+)""")
        accountPattern.find(message)?.let { match ->
            val accountNumber = match.groupValues[1]
            // Return last 4 digits if available, otherwise return the whole number
            return if (accountNumber.length >= 4) {
                accountNumber.takeLast(4)
            } else {
                accountNumber
            }
        }

        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        // Extract balance using the pattern "مانده81,108,644" or "مانده 81,108,644"
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

        // Check for Refah Bank specific transaction patterns
        // Look for the format with transaction type, amount and sign, and balance indicator
        val hasTransactionPattern = Regex("""(کارت|خرید|برداشت|واریز)\s*\d+[+-]""").containsMatchIn(message)
        val hasBalanceIndicator = message.contains("مانده")
        val hasBankName = message.contains("رفاه", ignoreCase = true) || message.contains("refah", ignoreCase = true)
        
        return (hasTransactionPattern && hasBalanceIndicator) || hasBankName
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