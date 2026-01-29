package com.pennywiseai.tracker.presentation.common

import java.math.BigDecimal

/**
 * Data class to hold financial totals grouped by currency
 */
data class CurrencyGroupedTotals(
    val totalsByCurrency: Map<String, CurrencyTotals> = emptyMap(),
    val availableCurrencies: List<String> = emptyList(),
    val transactionCount: Int = 0
) {
    fun getTotalsForCurrency(currency: String): CurrencyTotals {
        return totalsByCurrency[currency] ?: CurrencyTotals(currency = currency)
    }

    fun hasAnyCurrency(): Boolean = availableCurrencies.isNotEmpty()

    fun getPrimaryCurrency(): String {
        // Prioritize AED for FAB bank transactions, otherwise INR if available, then first available currency
        return when {
            availableCurrencies.contains("AED") -> "AED"  // FAB bank uses AED
            availableCurrencies.contains("INR") -> "INR"
            availableCurrencies.isNotEmpty() -> availableCurrencies.first()
            else -> "INR" // Default fallback
        }
    }

    fun getAllTotalsCombined(): CurrencyTotals {
        if (totalsByCurrency.isEmpty()) {
            return CurrencyTotals(currency = "ALL")
        }

        // Combine all currency totals
        val totalIncome = totalsByCurrency.values.sumOf { it.income }
        val totalExpenses = totalsByCurrency.values.sumOf { it.expenses }
        val totalCredit = totalsByCurrency.values.sumOf { it.credit }
        val totalTransfer = totalsByCurrency.values.sumOf { it.transfer }
        val totalInvestment = totalsByCurrency.values.sumOf { it.investment }
        val totalTransactionCount = totalsByCurrency.values.sumOf { it.transactionCount }
        val totalNetWorth = totalsByCurrency.values.sumOf { it.netWorth }

        return CurrencyTotals(
            currency = "ALL",
            income = totalIncome,
            expenses = totalExpenses,
            credit = totalCredit,
            transfer = totalTransfer,
            investment = totalInvestment,
            transactionCount = totalTransactionCount,
            netWorth = totalNetWorth
        )
    }

    fun hasMultipleCurrencies(): Boolean {
        return totalsByCurrency.size > 1
    }
}

/**
 * Financial totals for a specific currency
 */
data class CurrencyTotals(
    val currency: String,
    val income: BigDecimal = BigDecimal.ZERO,
    val expenses: BigDecimal = BigDecimal.ZERO,
    val credit: BigDecimal = BigDecimal.ZERO,
    val transfer: BigDecimal = BigDecimal.ZERO,
    val investment: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0,
    val netWorth: BigDecimal = BigDecimal.ZERO  // Represents actual account balances/net worth
) {
    val netBalance: BigDecimal
        get() = income - expenses - credit - transfer - investment

    /**
     * Calculates the net value based on user preference
     * @param netDisplayType The user's preference: "default" for income - expense, "maneh" for current balance
     */
    fun calculateNetValue(netDisplayType: String = "default"): BigDecimal {
        return when (netDisplayType) {
            "maneh" -> netWorth  // Current balance (Maneh)
            else -> netBalance    // Income - Expense (Default)
        }
    }
}