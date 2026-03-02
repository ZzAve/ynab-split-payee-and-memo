package com.github.zzave.ynabsplitpayeeandmemo.e2e

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * E2E test validating --dry-run flag behavior.
 *
 * Validates that:
 * - CLI with --dry-run analyzes transactions but makes zero PATCH calls (requirement E2E-08)
 * - CLI with --dry-run exits successfully (code 0)
 * - CLI with --dry-run logs what would be updated
 * - Same transactions trigger updates when --dry-run is disabled (baseline)
 *
 * Prerequisites:
 * - Test JAR built with: ./gradlew shadowJar -PtestBuild=true
 */
class DryRunTest : WireMockTestBase({

    beforeSpec {
        // Build test JAR once before all tests in this spec
        val buildProcess = ProcessBuilder("./gradlew", "shadowJar", "-PtestBuild=true")
            .directory(java.io.File(System.getProperty("user.dir")))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        val exitCode = buildProcess.waitFor()
        if (exitCode != 0) {
            val stderr = buildProcess.errorStream.bufferedReader().readText()
            throw IllegalStateException("Failed to build test JAR for dry-run test. Exit code: $exitCode\nError: $stderr")
        }
    }

    test("CLI with --dry-run analyzes transactions but makes zero PATCH calls") {
        // Create 10 splittable transactions
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
            createSplittableTransaction("txn-010", "MEDIAMARKT - ELECTRONICS")
        )

        // Stub GET /v1/budgets/{id}/transactions endpoint
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(splittableTransactions))
                )
        )

        // Stub PATCH endpoint (should NOT be called in dry-run mode)
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

        // Invoke CLI with --dry-run flag
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID,
            "--dry-run"
        )

        // Verify CLI exit code is 0 (success)
        result.exitCode shouldBe 0

        // Verify zero PATCH requests made (dry-run prevents updates)
        val patchRequests = wireMockServer.findAll(patchRequestedFor(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions")))
        patchRequests shouldHaveSize 0

        // Verify GET request WAS made (proves CLI analyzed transactions)
        wireMockServer.verify(getRequestedFor(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved")))
    }

    test("CLI with --dry-run logs transactions that would be updated") {
        // Create 10 splittable transactions
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
            createSplittableTransaction("txn-010", "MEDIAMARKT - ELECTRONICS")
        )

        // Stub GET endpoint
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(splittableTransactions))
                )
        )

        // Invoke CLI with --dry-run flag
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID,
            "--dry-run"
        )

        // Verify exit code is 0
        result.exitCode shouldBe 0

        // Verify log output contains dry-run message and transaction count
        val combinedOutput = result.stdout + result.stderr
        combinedOutput shouldContain "DRY RUN"
        combinedOutput shouldContain "10"  // 10 transactions would be updated
    }

    test("Baseline - same transactions trigger PATCH when --dry-run is disabled") {
        // Create 10 splittable transactions (same as dry-run tests)
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
            createSplittableTransaction("txn-010", "MEDIAMARKT - ELECTRONICS")
        )

        // Stub GET endpoint
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(splittableTransactions))
                )
        )

        // Stub PATCH endpoint
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

        // Invoke CLI WITHOUT --dry-run flag
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID
        )

        // Verify exit code is 0
        result.exitCode shouldBe 0

        // Verify exactly 1 PATCH request made (10 transactions = single batch)
        val patchRequests = wireMockServer.findAll(patchRequestedFor(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions")))
        patchRequests shouldHaveSize 1

        // Parse PATCH request body to verify correct content
        val batch = parseRequestBody(patchRequests[0].bodyAsString)
        batch.transactions shouldHaveSize 10

        // Verify first transaction was split correctly
        val firstTransaction = batch.transactions[0]
        firstTransaction.id shouldBe "txn-001"
        firstTransaction.payeeName shouldBe "ALBERT HEIJN"
        firstTransaction.memo shouldBe "GROCERY"

        // Verify last transaction was split correctly
        val lastTransaction = batch.transactions[9]
        lastTransaction.id shouldBe "txn-010"
        lastTransaction.payeeName shouldBe "MEDIAMARKT"
        lastTransaction.memo shouldBe "ELECTRONICS"
    }
})
