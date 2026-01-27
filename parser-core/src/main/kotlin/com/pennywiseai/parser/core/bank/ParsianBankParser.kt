package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parsian Bank specific parser for Iranian banking SMS messages.
 * Handles Parsian Bank's unique message formats including:
 * - Persian language transaction messages
 * - Amounts in Rials and Tomans
 * - Various transaction types (debit/credit/transfer)
 */
class ParsianBankParser : BankParser() {

    override fun getBankName() = "Parsian Bank"

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()

        // Common Parsian Bank sender IDs
        val parsianSenders = setOf(
            "PARSIANBANK",
            "PARSIAN",
            "PARSIAN BANK",
            "PERSIANBANK",
            "PERSIAN"
        )

        return upperSender in parsianSenders
    }

    override fun getCurrency(): String = "IRR" // Iranian Rial

    override fun extractAmount(message: String): BigDecimal? {
        // Parse amounts using the regex pattern from the original Python script
        val amountPattern = Regex("""(?:مبلغ\s*)?(\d{1,3}(?:,\d{3})*|\d+)(?:\s*(?:ریال|تومان))?\s*(?:برداشت|واریز|انتقال|خرید|[-+])""")

        amountPattern.find(message)?.let { match ->
            val rawAmount = Regex("""[^\d,]""").replace(match.groupValues[1], "")
            val cleanAmount = rawAmount.replace(",", "")

            return try {
                val amountValue = cleanAmount.toBigDecimal()
                // Only accept amounts >= 1000 based on the Python script logic
                if (amountValue >= BigDecimal.valueOf(1000)) {
                    amountValue
                } else {
                    null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        // Also check for amounts with + or - signs (like in your examples)
        val plusMinusAmountPattern = Regex("""[+-](\d{1,3}(?:,\d{3})*|\d+)""")
        plusMinusAmountPattern.find(message)?.let { match ->
            val rawAmount = Regex("""[^\d,]""").replace(match.groupValues[1], "")
            val cleanAmount = rawAmount.replace(",", "")

            return try {
                val amountValue = cleanAmount.toBigDecimal()
                // Only accept amounts >= 1000 based on the Python script logic
                if (amountValue >= BigDecimal.valueOf(1000)) {
                    amountValue
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

        // Check for Persian keywords from the original Python script
        return when {
            // Income keywords in Persian - check for + sign first
            lowerMessage.contains("واریز") ||
            lowerMessage.contains("+") ||
            lowerMessage.contains("credited") ||
            // Check for + followed by number (common in Iranian bank messages)
            containsPlusSignAmount(message) -> TransactionType.INCOME

            // Expense keywords in Persian
            lowerMessage.contains("برداشت") ||
            lowerMessage.contains("پرداخت") ||
            lowerMessage.contains("خرید") ||
            lowerMessage.contains("انتقال") ||
            lowerMessage.contains("مصرف") -> TransactionType.EXPENSE

            else -> null
        }
    }

    /**
     * Checks if the message contains a plus sign followed by an amount
     * This is common in Iranian bank messages to indicate income
     */
    private fun containsPlusSignAmount(message: String): Boolean {
        // Look for + followed by digits or comma-separated digits
        val plusAmountPattern = Regex("""\+(\d{1,3}(?:,\d{3})*|\d+)""")
        return plusAmountPattern.containsMatchIn(message)
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Parsian Bank doesn't typically include merchant names in SMS
        // But we can extract card numbers or other relevant info
        val cardPattern = Regex("""(\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4})""")
        cardPattern.find(message)?.let { match ->
            return "Card ${match.groupValues[1]}"
        }

        return null
    }

    override fun extractReference(message: String): String? {
        // Parsian Bank messages don't typically have reference numbers
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        // Extract last 4 digits of card/account number if present
        val cardPattern = Regex("""\d{4}[-\s]?(\d{4})""")
        cardPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        // Extract balance using the pattern from the original Python script
        val balancePattern = Regex("""مانده\s*:?\s*(\d{1,3}(?:,\d{3})*)""")
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
        
        // Check for card-related keywords
        val cardKeywords = listOf(
            "کارت", "card", "debit card", "credit card", "کارت بدهی", "کارت اعتباری"
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

        // Skip payment request messages
        if (lowerMessage.contains("درخواست") && lowerMessage.contains("پرداخت")) {
            return false
        }

        // Check for Iranian bank transaction patterns
        val iranianPatterns = listOf(
            "خريداينترنتي",  // Internet purchase
            "انتقال",         // Transfer
            "برداشت",        // Withdrawal
            "انتقالي",       // Transfer (another form)
            "واریز"          // Deposit
        )

        // Check for transaction keywords from the original Python script
        val transactionKeywords = listOf(
            "مبلغ", "ریال", "تومان", "IRR", "TOMAN",
            "برداشت", "واریز", "پرداخت", "خرید", "انتقال",
            "debit", "credit", "spent", "received", "transferred", "paid"
        )

        // Check if message contains any of the Iranian patterns or transaction keywords
        return (iranianPatterns.any { lowerMessage.contains(it) } ||
                transactionKeywords.any { lowerMessage.contains(it) } ||
                // Also check for + or - signs which indicate transactions
                message.contains('+') || message.contains('-'))
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