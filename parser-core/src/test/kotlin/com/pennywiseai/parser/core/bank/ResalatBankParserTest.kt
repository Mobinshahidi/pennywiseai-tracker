package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import com.pennywiseai.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.*
import java.math.BigDecimal

class ResalatBankParserTest {

    private val parser = ResalatBankParser()

    @TestFactory
    fun `resalat bank parser handles key paths`(): List<DynamicTest> {
        ParserTestUtils.printTestHeader(
            parserName = "Resalat Bank (Iran)",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val cases = listOf(
            ParserTestCase(
                name = "Resalat Bank Income Transaction",
                message = """10.10055857.1 
+120,000,000  
11/11_19:43 
مانده: 120,025,817""",
                sender = "RESALAT",
                expected = ExpectedTransaction(
                    amount = BigDecimal("120000000"),
                    currency = "IRR",
                    type = TransactionType.INCOME,
                    balance = BigDecimal("120025817"),
                    reference = "10.10055857.1"
                )
            ),
            ParserTestCase(
                name = "Resalat Bank Expense Transaction",
                message = """10.10055858.2 
-50,000,000  
12/15_14:30 
مانده: 70,025,817""",
                sender = "Resalat Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("50000000"),
                    currency = "IRR",
                    type = TransactionType.EXPENSE,
                    balance = BigDecimal("70025817"),
                    reference = "10.10055858.2"
                )
            )
        )

        return ParserTestUtils.runTestSuite(parser, cases)
    }

    @TestFactory
    fun `factory resolves resalat bank`(): List<DynamicTest> {
        val cases = listOf(
            SimpleTestCase(
                bankName = "Resalat Bank",
                sender = "RESALAT",
                currency = "IRR",
                message = """10.10055857.1
+120,000,000
11/11_19:43
مانده: 120,025,817""",
                expected = ExpectedTransaction(
                    amount = BigDecimal("120000000"),
                    currency = "IRR",
                    type = TransactionType.INCOME
                ),
                shouldHandle = true
            ),
            SimpleTestCase(
                bankName = "Resalat Bank",
                sender = "Resalat Bank",
                currency = "IRR",
                message = """10.10055858.2
-50,000,000
12/15_14:30
مانده: 70,025,817""",
                expected = ExpectedTransaction(
                    amount = BigDecimal("50000000"),
                    currency = "IRR",
                    type = TransactionType.EXPENSE
                ),
                shouldHandle = true
            )
        )

        return ParserTestUtils.runFactoryTestSuite(cases, "Factory smoke tests")
    }
}