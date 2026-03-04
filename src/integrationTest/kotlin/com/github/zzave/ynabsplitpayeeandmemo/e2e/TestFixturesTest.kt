package com.github.zzave.ynabsplitpayeeandmemo.e2e

import com.github.zzave.ynabsplitpayeeandmemo.PatchTransactionsWrapper
import com.github.zzave.ynabsplitpayeeandmemo.Transaction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json

/**
 * Tests for TestFixtures helper functions.
 *
 * Validates that test data builders produce valid Transaction objects
 * and API response formatting functions generate correct JSON structure.
 */
class TestFixturesTest : FunSpec({

    test("createTransaction creates valid Transaction with defaults") {
        val transaction = createTransaction(
            id = "txn-001",
            importPayeeName = "TEST PAYEE"
        )

        transaction.id shouldBe "txn-001"
        transaction.importPayeeName shouldBe "TEST PAYEE"
        transaction.accountId shouldBe DEFAULT_ACCOUNT_ID
        transaction.cleared shouldBe "uncleared"
        transaction.approved shouldBe false
        transaction.deleted shouldBe false
    }

    test("createSplittableTransaction creates transaction with matching payee and import_payee") {
        val transaction = createSplittableTransaction(
            id = "txn-002",
            importPayeeName = "ALBERT HEIJN - GROCERY"
        )

        transaction.id shouldBe "txn-002"
        transaction.importPayeeName shouldBe "ALBERT HEIJN - GROCERY"
        transaction.payeeName shouldBe "ALBERT HEIJN - GROCERY"
        transaction.importPayeeName shouldContain " - "
    }

    test("createSkippableTransaction with no separator creates non-splittable transaction") {
        val transaction = createSkippableTransaction(
            id = "txn-003",
            importPayeeName = "DIRECT DEBIT"
        )

        transaction.id shouldBe "txn-003"
        transaction.importPayeeName shouldBe "DIRECT DEBIT"
        transaction.payeeName shouldBe "DIRECT DEBIT"
        transaction.importPayeeName?.contains(" - ") shouldBe false
    }

    test("createSkippableTransaction with Transfer prefix creates transfer transaction") {
        val transaction = createSkippableTransaction(
            id = "txn-004",
            importPayeeName = "Transfer : Savings Account"
        )

        transaction.id shouldBe "txn-004"
        transaction.importPayeeName shouldBe "Transfer : Savings Account"
        transaction.payeeName shouldBe "Transfer : Savings Account"
        transaction.importPayeeName?.startsWith("Transfer : ") shouldBe true
    }

    test("createSkippableTransaction with already-changed payee creates non-matching payee") {
        val transaction = createSkippableTransaction(
            id = "txn-005",
            importPayeeName = "STORE - STUFF",
            payeeName = "My Custom Payee"
        )

        transaction.id shouldBe "txn-005"
        transaction.importPayeeName shouldBe "STORE - STUFF"
        transaction.payeeName shouldBe "My Custom Payee"
        transaction.payeeName shouldNotBe transaction.importPayeeName
    }

    test("buildBudgetTransactionsResponse creates valid YNAB API JSON response") {
        val transactions = listOf(
            createTransaction(id = "txn-1", importPayeeName = "TEST 1"),
            createTransaction(id = "txn-2", importPayeeName = "TEST 2")
        )

        val jsonResponse = buildBudgetTransactionsResponse(transactions)

        // Verify JSON structure
        jsonResponse shouldContain """"data""""
        jsonResponse shouldContain """"transactions""""
        jsonResponse shouldContain """"server_knowledge""""
        jsonResponse shouldContain """"txn-1""""
        jsonResponse shouldContain """"txn-2""""
        jsonResponse shouldContain """"TEST 1""""
        jsonResponse shouldContain """"TEST 2""""
    }

    test("buildBudgetTransactionsResponse with empty list creates valid empty response") {
        val jsonResponse = buildBudgetTransactionsResponse(emptyList())

        jsonResponse shouldContain """"data""""
        jsonResponse shouldContain """"transactions": []"""
        jsonResponse shouldContain """"server_knowledge""""
    }

    test("parseRequestBody deserializes WireMock request body into PatchTransactionsWrapper") {
        val requestBody = """
            {
              "transactions": [
                {
                  "id": "txn-001",
                  "account_id": "acc-001",
                  "date": "2026-03-01",
                  "amount": -10000,
                  "payee_id": null,
                  "payee_name": "ALBERT HEIJN",
                  "category_id": null,
                  "memo": "GROCERY",
                  "cleared": "uncleared",
                  "approved": false,
                  "flag_color": null
                }
              ]
            }
        """.trimIndent()

        val parsed = parseRequestBody(requestBody)

        parsed.transactions shouldHaveSize 1
        parsed.transactions[0].id shouldBe "txn-001"
        parsed.transactions[0].payeeName shouldBe "ALBERT HEIJN"
        parsed.transactions[0].memo shouldBe "GROCERY"
        parsed.transactions[0].payeeId shouldBe null
    }

    test("parseRequestBody handles multiple transactions") {
        val requestBody = """
            {
              "transactions": [
                {
                  "id": "txn-001",
                  "account_id": "acc-001",
                  "date": "2026-03-01",
                  "amount": -10000,
                  "payee_id": null,
                  "payee_name": "PAYEE 1",
                  "category_id": null,
                  "memo": "MEMO 1",
                  "cleared": "uncleared",
                  "approved": false,
                  "flag_color": null
                },
                {
                  "id": "txn-002",
                  "account_id": "acc-001",
                  "date": "2026-03-02",
                  "amount": -20000,
                  "payee_id": null,
                  "payee_name": "PAYEE 2",
                  "category_id": null,
                  "memo": "MEMO 2",
                  "cleared": "cleared",
                  "approved": true,
                  "flag_color": "red"
                }
              ]
            }
        """.trimIndent()

        val parsed = parseRequestBody(requestBody)

        parsed.transactions shouldHaveSize 2
        parsed.transactions[0].payeeName shouldBe "PAYEE 1"
        parsed.transactions[1].payeeName shouldBe "PAYEE 2"
    }

    test("DEFAULT_BUDGET_ID is defined") {
        DEFAULT_BUDGET_ID shouldNotBe null
        DEFAULT_BUDGET_ID shouldNotBe ""
    }

    test("DEFAULT_ACCOUNT_ID is defined") {
        DEFAULT_ACCOUNT_ID shouldNotBe null
        DEFAULT_ACCOUNT_ID shouldNotBe ""
    }
})
