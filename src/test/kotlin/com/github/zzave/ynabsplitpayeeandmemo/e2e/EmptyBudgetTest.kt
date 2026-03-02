package com.github.zzave.ynabsplitpayeeandmemo.e2e

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * E2E test validating CLI handles empty budgets gracefully without errors.
 *
 * Verifies requirements:
 * - E2E-04: CLI exits successfully when budget has zero transactions
 * - E2E-04: CLI makes no PATCH requests when budget is empty
 * - E2E-04: CLI logs appropriate message about zero transactions found
 *
 * Prerequisites:
 * - Test JAR built with: ./gradlew shadowJar -PtestBuild=true
 */
class EmptyBudgetTest : WireMockTestBase({

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
            throw IllegalStateException("Failed to build test JAR for empty budget test. Exit code: $exitCode\nError: $stderr")
        }
    }

    test("CLI exits with code 0 when budget has zero transactions") {
        // Stub GET endpoint returning empty transactions array
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(emptyList(), serverKnowledge = 0))
                )
        )

        // Invoke CLI
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID
        )

        // Verify exit code is 0 (success - empty budget is not an error)
        result.exitCode shouldBe 0
    }

    test("CLI makes zero PATCH requests when budget is empty") {
        // Stub GET endpoint returning empty transactions array
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(emptyList(), serverKnowledge = 0))
                )
        )

        // Invoke CLI
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID
        )

        // Verify no PATCH requests made
        val patchRequests = wireMockServer.findAll(
            patchRequestedFor(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions"))
        )
        patchRequests shouldHaveSize 0

        // Also verify exit was successful
        result.exitCode shouldBe 0
    }

    test("CLI logs appropriate message about no transactions found") {
        // Stub GET endpoint returning empty transactions array
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/$DEFAULT_BUDGET_ID/transactions\\?since_date=.*&type=unapproved"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(buildBudgetTransactionsResponse(emptyList(), serverKnowledge = 0))
                )
        )

        // Invoke CLI
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "fake-token",
            "--budget-id", DEFAULT_BUDGET_ID
        )

        // Verify output indicates 0 transactions analyzed/updated
        val combinedOutput = result.stdout + result.stderr
        combinedOutput shouldContain "0"

        // Also verify exit was successful
        result.exitCode shouldBe 0
    }
})
