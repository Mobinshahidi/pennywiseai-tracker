package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import com.pennywiseai.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.*
import java.math.BigDecimal

class BluBankParserTest {

    private val parser = BluBankParser()

    @TestFactory
    fun `blu bank parser handles key paths`(): List<DynamicTest> {
        ParserTestUtils.printTestHeader(
            parserName = "Blu Bank (Iran)",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val cases = listOf(
            ParserTestCase(
                name = "Blu Bank Deposit Transaction",
                message = """بلو
واریز پول
عرشیا عزیز، 8,200,000 ریال به حساب شما نشست.
موجودی: 100,029,351 ریال
۲۳:۱۹
۱۴۰۴.۱۰.۲۹""",
                sender = "BLU",
                expected = ExpectedTransaction(
                    amount = BigDecimal("8200000"),
                    currency = "IRR",
                    type = TransactionType.INCOME,
                    merchant = "Deposit",
                    balance = BigDecimal("100029351")
                )
            ),
            ParserTestCase(
                name = "Blu Bank Second Deposit Transaction",
                message = """بلو
واریز پول
عرشیا عزیز، 8,200,000 ریال به حساب شما نشست.
موجودی: 100,029,351 ریال
۲۳:۱۹
۱۴۰۴.۱۰.۲۹""",
                sender = "Blu Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("8200000"),
                    currency = "IRR",
                    type = TransactionType.INCOME,
                    merchant = "Deposit",
                    balance = BigDecimal("100029351")
                )
            )
        )

        return ParserTestUtils.runTestSuite(parser, cases)
    }

    @TestFactory
    fun `factory resolves blu bank`(): List<DynamicTest> {
        val cases = listOf(
            SimpleTestCase(
                bankName = "Blu Bank",
                sender = "BLU",
                currency = "IRR",
                message = """بلو
واریز پول
عرشیا عزیز، 8,200,000 ریال به حساب شما نشست.
موجودی: 100,029,351 ریال
۲۳:۱۹
۱۴۰۴.۱۰.۲۹""",
                expected = ExpectedTransaction(
                    amount = BigDecimal("8200000"),
                    currency = "IRR",
                    type = TransactionType.INCOME
                ),
                shouldHandle = true
            ),
            SimpleTestCase(
                bankName = "Blu Bank",
                sender = "Blu Bank",
                currency = "IRR",
                message = """بلو
واریز پول
عرشیا عزیز، 8,200,000 ریال به حساب شما نشست.
موجودی: 100,029,351 ریال
۲۳:۱۹
۱۴۰۴.۱۰.۲۹""",
                expected = ExpectedTransaction(
                    amount = BigDecimal("8200000"),
                    currency = "IRR",
                    type = TransactionType.INCOME
                ),
                shouldHandle = true
            )
        )

        return ParserTestUtils.runFactoryTestSuite(cases, "Factory smoke tests")
    }
}