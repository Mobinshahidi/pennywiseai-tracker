package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Resalat Bank specific parser for Iranian banking SMS messages.
 * Handles Resalat Bank's unique message formats including:
 * - Persian language transaction messages
 * - Amounts with + and - signs indicating income/expense
 * - Format like: "10.10055857.1 \n+120,000,000 \n11/11_19:43 \nمانده: 120,025,817"
 */
class ResalatBankParser : BankParser() {

    override fun getBankName() = "Resalat Bank"

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()

        // Common Resalat Bank sender IDs
        val resalatSenders = setOf(
            "RESALAT",
            "RESALATBANK",
            "RESALAT BANK",
            "BANK RESALAT",
            "BANKRESALAT",
            "IRAN RESALAT"
        )

        // Check for the specific number patterns
        if (upperSender in resalatSenders) {
            return true
        }

        // Check if sender contains "resalat" with "iran" or "bank"
        return sender.contains("resalat", ignoreCase = true) &&
               (sender.contains("iran", ignoreCase = true) || sender.contains("bank", ignoreCase = true))
    }

    override fun getCurrency(): String = "IRR" // Iranian Rial

    override fun extractAmount(message: String): BigDecimal? {
        // Pattern for Resalat Bank format: +120,000,000 or -120,000,000
        val amountPattern = Regex("""[+-](\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""")
        
        amountPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1] // Get the amount part without the sign
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

        // Check for + or - signs in the message to determine transaction type
        // Pattern for Resalat Bank format: +120,000,000 (income) or -120,000,000 (expense)
        val amountWithSignPattern = Regex("""([+-])(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""")
        amountWithSignPattern.find(message)?.let { match ->
            val sign = match.groupValues[1] // Get the sign
            return if (sign == "+") {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            }
        }

        return null
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Resalat Bank messages don't typically have merchant information
        // Return a generic transaction type based on the message
        val lowerMessage = message.lowercase()
        
        if (containsPlusSignAmount(message)) {
            return "Income Transaction"
        } else if (containsMinusSignAmount(message)) {
            return "Expense Transaction"
        }
        
        return "Bank Transaction"
    }

    override fun extractReference(message: String): String? {
        // Extract the reference number from the first line like "10.10055857.1"
        val referencePattern = Regex("""^(\d+\.\d+\.\d+)""")
        referencePattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return null
    }

    override fun extractAccountLast4(message: String): String? {
        // Resalat Bank messages don't typically include account numbers in the format provided
        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        // Extract balance using the pattern "مانده: 120,025,817"
        val balancePattern = Regex("""مانده\s*:\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""")
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

        // Check for Resalat Bank specific transaction patterns
        // Look for the format with + or - amounts and Persian balance indicator
        val hasAmountWithSign = containsPlusSignAmount(message) || containsMinusSignAmount(message)
        val hasBalanceIndicator = message.contains("مانده")
        
        return hasAmountWithSign && hasBalanceIndicator
    }

    /**
     * Checks if the message contains a plus sign followed by an amount
     * This indicates income in Iranian bank messages
     */
    private fun containsPlusSignAmount(message: String): Boolean {
        // Look for + followed by digits or comma-separated digits
        val plusAmountPattern = Regex("""\+(\d{1,3}(?:,\d{3})*|\d+)""")
        return plusAmountPattern.containsMatchIn(message)
    }

    /**
     * Checks if the message contains a minus sign followed by an amount
     * This indicates expense in Iranian bank messages
     */
    private fun containsMinusSignAmount(message: String): Boolean {
        // Look for - followed by digits or comma-separated digits
        val minusAmountPattern = Regex("""-(\d{1,3}(?:,\d{3})*|\d+)""")
        return minusAmountPattern.containsMatchIn(message)
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