package com.github.zzave.ynabsplitpayeeandmemo.e2e

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Happy path E2E test validating core business logic:
 * - CLI correctly splits transactions with " - " separator into separate payee and memo fields
 * - CLI sends batch updates in correct format with proper batch size handling (25 per batch)
 * - CLI handles 26+ transactions by making multiple batch API calls
 *
 * Prerequisites:
 * - Debug JAR built with: ./gradlew debugShadowJar
 */
class HappyPathTest : WireMockTestBase({

    test("CLI splits 26 transactions with correct payee/memo and sends 2 batch PATCH calls") {
        // Create 26 splittable transactions (will trigger 2 batches: 25 + 1)
        val splittableTransactions = listOf(
            createSplittableTransaction("txn-001", "ALBERT HEIJN - GROCERY"),
            createSplittableTransaction("txn-002", "SHELL - FUEL"),
            createSplittableTransaction("txn-003", "COOLBLUE - ELECTRONICS"),
            createSplittableTransaction("txn-004", "JUMBO - SUPERMARKET"),
            createSplittableTransaction("txn-005", "ETOS - PHARMACY"),
            createSplittableTransaction("txn-006", "HEMA - HOME GOODS"),
            createSplittableTransaction("txn-007", "KRUIDVAT - DRUGSTORE"),
            createSplittableTransaction("txn-008", "ACTION - DISCOUNT STORE"),
            createSplittableTransaction("txn-009", "IKEA - FURNITURE"),
            createSplittableTransaction("txn-010", "MEDIAMARKT - ELECTRONICS"),
            createSplittableTransaction("txn-011", "BOL.COM - ONLINE SHOPPING"),
            createSplittableTransaction("txn-012", "DECATHLON - SPORTS"),
            createSplittableTransaction("txn-013", "GAMMA - DIY"),
            createSplittableTransaction("txn-014", "PRAXIS - HARDWARE"),
            createSplittableTransaction("txn-015", "BLOKKER - HOME"),
            createSplittableTransaction("txn-016", "C&A - CLOTHING"),
            createSplittableTransaction("txn-017", "H&M - FASHION"),
            createSplittableTransaction("txn-018", "ZARA - APPAREL"),
            createSplittableTransaction("txn-019", "UNIQLO - CLOTHING"),
            createSplittableTransaction("txn-020", "PRIMARK - FASHION"),
            createSplittableTransaction("txn-021", "LIDL - GROCERY"),
            createSplittableTransaction("txn-022", "ALDI - SUPERMARKET"),
            createSplittableTransaction("txn-023", "PLUS - GROCERY"),
            createSplittableTransaction("txn-024", "DIRK - SUPERMARKET"),
            createSplittableTransaction("txn-025", "VOMAR - GROCERY"),
            createSplittableTransaction("txn-026", "NETTORAMA - DISCOUNT")
        )

        // Create skippable transactions (should NOT appear in PATCH requests)
        val skippableTransactions = listOf(
            createSkippableTransaction("txn-skip-1", "DIRECT DEBIT"),  // No separator
            createSkippableTransaction("txn-skip-2", "CASH WITHDRAWAL"),  // No separator
            createSkippableTransaction("txn-skip-3", "Transfer : Savings Account"),  // Transfer
            createSkippableTransaction("txn-skip-4", "Transfer : Checking"),  // Transfer
            createSkippableTransaction("txn-skip-5", "STORE - STUFF", "My Custom Payee")  // Already changed
        )

        val allTransactions = splittableTransactions + skippableTransactions

        // Stub GET /v1/budgets/{id}/transactions endpoint
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(allTransactions))
                )
        )

        // Stub PATCH /v1/budgets/{id}/transactions endpoint (batch update)
        wireMockServer.stubFor(
            patch(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions"))
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

        // Invoke CLI with mock server URL (without --dry-run to actually send PATCH requests)
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID
        )

        // Debug output
        if (result.exitCode != 0) {
            java.io.File("/tmp/happy_path_debug.txt").writeText("""
                === CLI Failed ===
                Exit code: ${result.exitCode}
                Stdout: ${result.stdout}
                Stderr: ${result.stderr}
                API Base URL: $apiBaseUrl
            """.trimIndent())
        }

        // Verify CLI exit code
        result.exitCode shouldBe 0

        // Verify exactly 2 PATCH requests made (26 splittable transactions / batch size 25 = 2 batches)
        val patchRequests = wireMockServer.findAll(patchRequestedFor(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions")))
        patchRequests shouldHaveSize 2

        // Parse first batch (should have 25 transactions)
        val firstBatch = parseRequestBody(patchRequests[0].bodyAsString)
        firstBatch.transactions shouldHaveSize 25

        // Parse second batch (should have 1 transaction)
        val secondBatch = parseRequestBody(patchRequests[1].bodyAsString)
        secondBatch.transactions shouldHaveSize 1

        // Verify first transaction in first batch
        val firstTransaction = firstBatch.transactions[0]
        firstTransaction.id shouldBe "txn-001"
        firstTransaction.payeeName shouldBe "ALBERT HEIJN"
        firstTransaction.memo shouldBe "GROCERY"
        firstTransaction.payeeId shouldBe null
        firstTransaction.accountId shouldBe DEFAULT_ACCOUNT_ID
        firstTransaction.cleared shouldNotBe null
        firstTransaction.approved shouldNotBe null

        // Verify second transaction in first batch
        val secondTransaction = firstBatch.transactions[1]
        secondTransaction.id shouldBe "txn-002"
        secondTransaction.payeeName shouldBe "SHELL"
        secondTransaction.memo shouldBe "FUEL"
        secondTransaction.payeeId shouldBe null

        // Verify 25th transaction in first batch
        val twentyFifthTransaction = firstBatch.transactions[24]
        twentyFifthTransaction.id shouldBe "txn-025"
        twentyFifthTransaction.payeeName shouldBe "VOMAR"
        twentyFifthTransaction.memo shouldBe "GROCERY"
        twentyFifthTransaction.payeeId shouldBe null

        // Verify 26th transaction in second batch
        val twentySixthTransaction = secondBatch.transactions[0]
        twentySixthTransaction.id shouldBe "txn-026"
        twentySixthTransaction.payeeName shouldBe "NETTORAMA"
        twentySixthTransaction.memo shouldBe "DISCOUNT"
        twentySixthTransaction.payeeId shouldBe null

        // Verify GET request made with correct query params
        wireMockServer.verify(getRequestedFor(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved")))
    }
})
