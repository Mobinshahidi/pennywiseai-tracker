package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import com.pennywiseai.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.*
import java.math.BigDecimal

class KeshavarziBankParserTest {

    private val parser = KeshavarziBankParser()

    @TestFactory
    fun `keshavarzi bank parser handles key paths`(): List<DynamicTest> {
        ParserTestUtils.printTestHeader(
            parserName = "Keshavarzi Bank (Iran)",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val cases = listOf(
            ParserTestCase(
                name = "Keshavarzi Bank Purchase Transaction",
                message = """خرید500,000
مانده2,542,509
041001-12:29
کارت8783*
bki. ir""",
                sender = "KESHAVARZI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500000"),
                    currency = "IRR",
                    type = TransactionType.EXPENSE,
                    merchant = "Purchase",
                    balance = BigDecimal("2542509"),
                    accountLast4 = "8783"
                )
            ),
            ParserTestCase(
                name = "Keshavarzi Bank Deposit Transaction",
                message = """واريز4,000,000
مانده8,915,929
040825-11:32
کارت8783*
bki. ir""",
                sender = "BKI",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4000000"),
                    currency = "IRR",
                    type = TransactionType.INCOME,
                    merchant = "Deposit",
                    balance = BigDecimal("8915929"),
                    accountLast4 = "8783"
                )
            ),
            ParserTestCase(
                name = "Keshavarzi Bank Withdrawal Transaction",
                message = """برداشت24,000
مانده6,917,559
040903-13:50
کارت8783*
bki. ir""",
                sender = "KESH",
                expected = ExpectedTransaction(
                    amount = BigDecimal("24000"),
                    currency = "IRR",
                    type = TransactionType.EXPENSE,
                    merchant = "Withdrawal",
                    balance = BigDecimal("6917559"),
                    accountLast4 = "8783"
                )
            )
        )

        return ParserTestUtils.runTestSuite(parser, cases)
    }

    @TestFactory
    fun `factory resolves keshavarzi bank`(): List<DynamicTest> {
        val cases = listOf(
            SimpleTestCase(
                bankName = "Keshavarzi Bank",
                sender = "KESHAVARZI",
                currency = "IRR",
                message = """خرید500,000
مانده2,542,509
041001-12:29
کارت8783*
bki. ir""",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500000"),
                    currency = "IRR",
                    type = TransactionType.EXPENSE
                ),
                shouldHandle = true
            ),
            SimpleTestCase(
                bankName = "Keshavarzi Bank",
                sender = "BKI",
                currency = "IRR",
                message = """واريز4,000,000
مانده8,915,929
040825-11:32
کارت8783*
bki. ir""",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4000000"),
                    currency = "IRR",
                    type = TransactionType.INCOME
                ),
                shouldHandle = true
            )
        )

        return ParserTestUtils.runFactoryTestSuite(cases, "Factory smoke tests")
    }
}