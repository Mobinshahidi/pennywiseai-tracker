package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import com.pennywiseai.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.*
import java.math.BigDecimal

class RefahBankParserTest {

    private val parser = RefahBankParser()

    @TestFactory
    fun `refah bank parser handles key paths`(): List<DynamicTest> {
        ParserTestUtils.printTestHeader(
            parserName = "Refah Bank (Iran)",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val cases = listOf(
            ParserTestCase(
                name = "Refah Bank Income Transaction",
                message = """بانک رفاه
حساب207853186
کارت5,000,000+
مانده81,108,644
11/13-00:43""",
                sender = "REFAH",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5000000"),
                    currency = "IRR",
                    type = TransactionType.INCOME,
                    merchant = "Card Transaction",
                    balance = BigDecimal("81108644"),
                    accountLast4 = "53186"
                )
            ),
            ParserTestCase(
                name = "Refah Bank Expense Transaction",
                message = """بانک رفاه
حساب207853186
خرید2,450,000-
مانده76,108,644
11/12-19:14""",
                sender = "Refah Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("2450000"),
                    currency = "IRR",
                    type = TransactionType.EXPENSE,
                    merchant = "Purchase",
                    balance = BigDecimal("76108644"),
                    accountLast4 = "53186"
                )
            ),
            ParserTestCase(
                name = "Refah Bank Withdrawal Transaction",
                message = """بانك رفاه
حساب 207853186
برداشت570,000-
خريد اينترنتي از پرداخت الکترونيک سپش·پ137842 
مانده108,009,244
4/11/11-11:15""",
                sender = "RF-BANK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("570000"),
                    currency = "IRR",
                    type = TransactionType.EXPENSE,
                    merchant = "Withdrawal",
                    balance = BigDecimal("108009244"),
                    accountLast4 = "53186"
                )
            ),
            ParserTestCase(
                name = "Refah Bank Card Deposit Transaction",
                message = """بانک رفاه
حساب207853186
کارت3,000,000+
مانده75,780,644
11/12-12:30""",
                sender = "BANK REFAH",
                expected = ExpectedTransaction(
                    amount = BigDecimal("3000000"),
                    currency = "IRR",
                    type = TransactionType.INCOME,
                    merchant = "Card Transaction",
                    balance = BigDecimal("75780644"),
                    accountLast4 = "53186"
                )
            )
        )

        return ParserTestUtils.runTestSuite(parser, cases)
    }

    @TestFactory
    fun `factory resolves refah bank`(): List<DynamicTest> {
        val cases = listOf(
            SimpleTestCase(
                bankName = "Refah Bank",
                sender = "REFAH",
                currency = "IRR",
                message = """بانک رفاه
حساب207853186
کارت5,000,000+
مانده81,108,644
11/13-00:43""",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5000000"),
                    currency = "IRR",
                    type = TransactionType.INCOME
                ),
                shouldHandle = true
            ),
            SimpleTestCase(
                bankName = "Refah Bank",
                sender = "Refah Bank",
                currency = "IRR",
                message = """بانک رفاه
حساب207853186
خرید2,450,000-
مانده76,108,644
11/12-19:14""",
                expected = ExpectedTransaction(
                    amount = BigDecimal("2450000"),
                    currency = "IRR",
                    type = TransactionType.EXPENSE
                ),
                shouldHandle = true
            )
        )

        return ParserTestUtils.runFactoryTestSuite(cases, "Factory smoke tests")
    }
}