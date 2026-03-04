package com.github.zzave.ynabsplitpayeeandmemo.e2e

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * E2E test validating CLI correctly skips ineligible transactions.
 *
 * Verifies requirements:
 * - E2E-05: CLI skips transactions where payeeName != importPayeeName (manually changed by user)
 * - E2E-06: CLI skips transactions without " - " separator
 * - E2E-07: CLI skips transfers where payee starts with "Transfer : "
 *
 * Prerequisites:
 * - Debug JAR built with: ./gradlew debugShadowJar
 */
class SkipConditionsTest : WireMockTestBase({

    test("CLI skips transactions where payeeName != importPayeeName (already manually changed)") {
        // Create 3 transactions with payeeName != importPayeeName (user manually changed payee)
        val alreadyChangedTransactions = listOf(
            createSkippableTransaction("txn-001", "STORE - STUFF", payeeName = "Custom Store Name"),
            createSkippableTransaction("txn-002", "SHOP - ITEMS", payeeName = "My Favorite Shop"),
            createSkippableTransaction("txn-003", "MARKET - GOODS", payeeName = "Local Market")
        )

        // Stub GET endpoint
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(alreadyChangedTransactions))
                )
        )

        // Invoke CLI
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID
        )

        // Verify exit code 0 (success - skipping transactions is not an error)
        result.exitCode shouldBe 0

        // Verify zero PATCH requests (all transactions skipped)
        val patchRequests = wireMockServer.findAll(
            patchRequestedFor(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions"))
        )
        patchRequests shouldHaveSize 0

        // Verify log indicates 0 transactions need updating
        val combinedOutput = result.stdout + result.stderr
        combinedOutput shouldContain "0"
    }

    test("CLI skips transactions without ' - ' separator in import_payee_name") {
        // Create 3 transactions without " - " separator
        val noSeparatorTransactions = listOf(
            createSkippableTransaction("txn-001", "DIRECT DEBIT"),
            createSkippableTransaction("txn-002", "CASH WITHDRAWAL"),
            createSkippableTransaction("txn-003", "ATM WITHDRAWAL")
        )

        // Stub GET endpoint
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(noSeparatorTransactions))
                )
        )

        // Invoke CLI
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID
        )

        // Verify exit code 0
        result.exitCode shouldBe 0

        // Verify zero PATCH requests
        val patchRequests = wireMockServer.findAll(
            patchRequestedFor(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions"))
        )
        patchRequests shouldHaveSize 0

        // Verify log indicates 0 transactions need updating
        val combinedOutput = result.stdout + result.stderr
        combinedOutput shouldContain "0"
    }

    test("CLI skips transfers where payee starts with 'Transfer : '") {
        // Create 3 transfer transactions
        val transferTransactions = listOf(
            createSkippableTransaction("txn-001", "Transfer : Savings Account"),
            createSkippableTransaction("txn-002", "Transfer : Credit Card"),
            createSkippableTransaction("txn-003", "Transfer : Checking")
        )

        // Stub GET endpoint
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(transferTransactions))
                )
        )

        // Invoke CLI
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID
        )

        // Verify exit code 0
        result.exitCode shouldBe 0

        // Verify zero PATCH requests
        val patchRequests = wireMockServer.findAll(
            patchRequestedFor(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions"))
        )
        patchRequests shouldHaveSize 0

        // Verify log indicates 0 transactions need updating
        val combinedOutput = result.stdout + result.stderr
        combinedOutput shouldContain "0"
    }

    test("CLI combines all skip conditions in single run (mixed eligible/skippable transactions)") {
        // Create 5 splittable transactions
        val splittableTransactions = listOf(
            createSplittableTransaction("txn-split-1", "ALBERT HEIJN - GROCERY"),
            createSplittableTransaction("txn-split-2", "SHELL - FUEL"),
            createSplittableTransaction("txn-split-3", "COOLBLUE - ELECTRONICS"),
            createSplittableTransaction("txn-split-4", "JUMBO - SUPERMARKET"),
            createSplittableTransaction("txn-split-5", "ETOS - PHARMACY")
        )

        // Create 2 already-changed transactions (payeeName != importPayeeName)
        val alreadyChangedTransactions = listOf(
            createSkippableTransaction("txn-skip-1", "STORE - STUFF", payeeName = "Custom Store"),
            createSkippableTransaction("txn-skip-2", "SHOP - ITEMS", payeeName = "My Shop")
        )

        // Create 2 no-separator transactions
        val noSeparatorTransactions = listOf(
            createSkippableTransaction("txn-skip-3", "DIRECT DEBIT"),
            createSkippableTransaction("txn-skip-4", "CASH WITHDRAWAL")
        )

        // Create 2 transfer transactions
        val transferTransactions = listOf(
            createSkippableTransaction("txn-skip-5", "Transfer : Savings Account"),
            createSkippableTransaction("txn-skip-6", "Transfer : Credit Card")
        )

        // Combine all transactions (5 eligible + 6 skipped = 11 total)
        val allTransactions = splittableTransactions + alreadyChangedTransactions + noSeparatorTransactions + transferTransactions

        // Stub GET endpoint
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(allTransactions))
                )
        )

        // Stub PATCH endpoint
        wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.patch(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": {
                                "transactions": [],
                                "duplicate_import_ids": [],
                                "server_knowledge": 12346
                              }
                            }
                        """.trimIndent())
                )
        )

        // Invoke CLI
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID
        )

        // Verify exit code 0
        result.exitCode shouldBe 0

        // Verify exactly 1 PATCH request (5 eligible transactions in single batch)
        val patchRequests = wireMockServer.findAll(
            patchRequestedFor(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions"))
        )
        patchRequests shouldHaveSize 1

        // Parse PATCH request body
        val patchBody = parseRequestBody(patchRequests[0].bodyAsString)

        // Verify only 5 transactions in PATCH body (6 skipped)
        patchBody.transactions shouldHaveSize 5

        // Verify each transaction has correct payee/memo split
        val firstTransaction = patchBody.transactions[0]
        firstTransaction.id shouldBe "txn-split-1"
        firstTransaction.payeeName shouldBe "ALBERT HEIJN"
        firstTransaction.memo shouldBe "GROCERY"

        val secondTransaction = patchBody.transactions[1]
        secondTransaction.id shouldBe "txn-split-2"
        secondTransaction.payeeName shouldBe "SHELL"
        secondTransaction.memo shouldBe "FUEL"

        val thirdTransaction = patchBody.transactions[2]
        thirdTransaction.id shouldBe "txn-split-3"
        thirdTransaction.payeeName shouldBe "COOLBLUE"
        thirdTransaction.memo shouldBe "ELECTRONICS"

        val fourthTransaction = patchBody.transactions[3]
        fourthTransaction.id shouldBe "txn-split-4"
        fourthTransaction.payeeName shouldBe "JUMBO"
        fourthTransaction.memo shouldBe "SUPERMARKET"

        val fifthTransaction = patchBody.transactions[4]
        fifthTransaction.id shouldBe "txn-split-5"
        fifthTransaction.payeeName shouldBe "ETOS"
        fifthTransaction.memo shouldBe "PHARMACY"
    }
})
