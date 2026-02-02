package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Blu Bank specific parser for Iranian banking SMS messages.
 * Handles Blu Bank's unique message formats including:
 * - Persian language transaction messages
 * - Deposit notifications with "واریز پول" 
 * - Format like: "بلو\nواریز پول\nعرشیا عزیز، 8,200,000 ریال به حساب شما نشست.\nموجودی: 100,029,351 ریال\n۲۳:۱۹\n۱۴۰۴.۱۰.۲۹"
 */
class BluBankParser : BankParser() {

    override fun getBankName() = "Blu Bank"

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()

        // Common Blu Bank sender IDs
        val bluSenders = setOf(
            "BLU",
            "BLUBANK",
            "BLU BANK",
            "BANK BLU",
            "BANKBLU",
            "IRAN BLU"
        )

        // Check for the specific number patterns
        if (upperSender in bluSenders) {
            return true
        }

        // Check if sender contains "blu" with "iran" or "bank"
        return sender.contains("blu", ignoreCase = true) &&
               (sender.contains("iran", ignoreCase = true) || sender.contains("bank", ignoreCase = true))
    }

    override fun getCurrency(): String = "IRR" // Iranian Rial

    override fun extractAmount(message: String): BigDecimal? {
        // Pattern for Blu Bank format: "8,200,000 ریال" or similar amounts
        val amountPattern = Regex("""(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)\s*ریال""")
        
        amountPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1] // Get the amount part
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

        // Determine transaction type based on Persian keywords in Blu Bank format
        // "واریز پول" indicates deposit/income
        return when {
            message.contains("واریز", ignoreCase = true) -> TransactionType.INCOME
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Extract merchant based on transaction type in Persian
        when {
            message.contains("واریز", ignoreCase = true) -> return "Deposit"
            message.contains("پول", ignoreCase = true) -> return "Money Transfer"
            else -> return "Bank Transaction"
        }
    }

    override fun extractReference(message: String): String? {
        // Blu Bank messages don't typically have reference numbers in the format provided
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        // Blu Bank messages don't typically include account numbers in the format provided
        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        // Extract balance using the pattern "موجودی: 100,029,351 ریال"
        val balancePattern = Regex("""موجودی\s*:\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)\s*ریال""")
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

        // Check for Blu Bank specific transaction patterns
        // Look for the format with bank name "بلو", transaction type "واریز پول", and amount in Rials
        val hasBankName = message.contains("بلو", ignoreCase = true)
        val hasDepositPattern = message.contains("واریز", ignoreCase = true) && message.contains("پول", ignoreCase = true)
        val hasAmountInRials = message.contains("ریال", ignoreCase = true)
        val hasBalancePattern = message.contains("موجودی", ignoreCase = true)
        
        return hasBankName && (hasDepositPattern || hasAmountInRials || hasBalancePattern)
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