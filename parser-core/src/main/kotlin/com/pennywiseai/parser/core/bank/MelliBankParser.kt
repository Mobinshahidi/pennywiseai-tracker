package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Bank Melli (Meli) Bank specific parser for Iranian banking SMS messages.
 * Handles Bank Melli's unique message formats including:
 * - Persian language transaction messages
 * - Amounts in Rials and Tomans
 * - Various transaction types (debit/credit/transfer)
 */
class MelliBankParser : BankParser() {

    override fun getBankName() = "Melli Bank"

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()

        // Common Melli Bank sender IDs
        val melliSenders = setOf(
            "+98700717",
            "+98700017",
            "+9870017",
            "MELLI",
            "MELLIBANK",
            "MELLI BANK",
            "BANK MELLI",
            "BANKMELLI",
            "IRAN MELLI",
            "BANK MELLI IRAN"
        )

        // Check for the specific number patterns
        if (upperSender in melliSenders) {
            return true
        }

        // Check if sender contains Iranian country code +98 and contains "melli" or "meli"
        return (sender.startsWith("+98") &&
                (sender.contains("melli", ignoreCase = true) || sender.contains("meli", ignoreCase = true))) ||
               (sender.contains("melli", ignoreCase = true) &&
                (sender.contains("iran", ignoreCase = true) || sender.contains("bank", ignoreCase = true)))
    }

    override fun getCurrency(): String = "IRR" // Iranian Rial

    override fun extractAmount(message: String): BigDecimal? {
        // First, try to extract amounts from specific transaction patterns in your examples
        // Pattern for "خريداينترنتي:318,340-" or "انتقال:3,409,000-" or "برداشت:850,000-" or "انتقالي:20,000,000+"
        val transactionPattern = Regex("""(خريداينترنتي|انتقال|برداشت|انتقالي|واریز|خرید):\s*([+-]?\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""")
        transactionPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[2] // Get the amount part
            val cleanAmount = amountStr.replace(",", "")

            return try {
                val amountValue = cleanAmount.toBigDecimal()
                // Only accept amounts >= 1000 based on the Python script logic
                if (amountValue.abs() >= BigDecimal.valueOf(1000)) {
                    amountValue.abs() // Return absolute value since sign is handled by transaction type
                } else {
                    null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        // Also handle the "خرید اینترنتی" pattern specifically
        val persianInternetPurchasePattern = Regex("""خرید\s+اینترنتی:\s*([+-]?\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)""")
        persianInternetPurchasePattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1] // Get the amount part
            val cleanAmount = amountStr.replace(",", "")

            return try {
                val amountValue = cleanAmount.toBigDecimal()
                // Only accept amounts >= 1000 based on the Python script logic
                if (amountValue.abs() >= BigDecimal.valueOf(1000)) {
                    amountValue.abs() // Return absolute value since sign is handled by transaction type
                } else {
                    null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        // Parse amounts using the original regex pattern from the original Python script
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

        // Check for specific patterns with signs in the message
        // Pattern for "انتقال:3,409,000-" (expense) or "انتقالي:20,000,000+" (income) - sign at the end
        val transactionPattern = Regex("""(انتقال|انتقالي|واریز|خرید):\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)([-+])""")
        transactionPattern.find(message)?.let { match ->
            val sign = match.groupValues[3] // Get the sign at the end
            return if (sign == "+") {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            }
        }

        // Check for Persian keywords from the original Python script
        return when {
            // Income keywords in Persian - check for + sign first
            lowerMessage.contains("واریز") ||
            lowerMessage.contains("credited") ||
            // Check for + followed by number (common in Iranian bank messages)
            containsPlusSignAmount(message) -> TransactionType.INCOME

            // Expense keywords in Persian - including the specific patterns from examples
            lowerMessage.contains("برداشت") ||
            lowerMessage.contains("پرداخت") ||
            lowerMessage.contains("خرید") ||  // General purchase
            lowerMessage.contains("انتقال") ||
            lowerMessage.contains("مصرف") ||
            lowerMessage.contains("خريداينترنتي") ||  // Internet purchase (Farsi)
            lowerMessage.contains("خرید اینترنتی") ||  // Internet purchase (Persian)
            lowerMessage.contains("انتقالي:") -> TransactionType.EXPENSE  // Transfer pattern

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
        // Extract merchant from specific patterns in Iranian bank messages
        val patterns = listOf(
            "خرید اینترنتی",  // Internet purchase (Persian)
            "خريداينترنتي",  // Internet purchase (Farsi)
            "خرید",          // General purchase
            "انتقال",         // Transfer
            "برداشت",        // Withdrawal
            "انتقالي",       // Transfer (another form)
            "واریز"          // Deposit
        )

        for (pattern in patterns) {
            if (message.contains(pattern, ignoreCase = true)) {
                return when (pattern) {
                    "خرید اینترنتی" -> "Internet Purchase"
                    "خريداينترنتي" -> "Internet Purchase"
                    "خرید" -> "Purchase"
                    "انتقال" -> "Transfer"
                    "برداشت" -> "Withdrawal"
                    "انتقالي" -> "Transfer"
                    "واریز" -> "Deposit"
                    else -> pattern
                }
            }
        }

        // Fallback to card number extraction if no specific pattern found
        val cardPattern = Regex("""(\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4})""")
        cardPattern.find(message)?.let { match ->
            return "Card ${match.groupValues[1]}"
        }

        return null
    }

    override fun extractReference(message: String): String? {
        // Melli Bank messages don't typically have reference numbers
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

        // Check for Iranian bank transaction patterns - including variations
        val iranianPatterns = listOf(
            "خريداينترنتي",  // Internet purchase (Farsi)
            "خرید اینترنتی",  // Internet purchase (Persian)
            "خرید",          // Purchase
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