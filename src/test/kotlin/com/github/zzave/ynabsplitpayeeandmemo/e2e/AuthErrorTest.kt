package com.github.zzave.ynabsplitpayeeandmemo.e2e

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * E2E test validating CLI authentication error handling.
 *
 * Verifies that when the YNAB API returns 401 (Unauthorized):
 * - CLI exits with non-zero exit code
 * - CLI outputs clear error message
 * - CLI makes zero PATCH calls (stops before making any changes)
 *
 * **Prerequisite:** Test JAR must be built before running:
 * ```
 * ./gradlew shadowJar -PtestBuild=true
 * ```
 */
class AuthErrorTest : WireMockTestBase({

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
            throw IllegalStateException("Failed to build test JAR for auth error test. Exit code: $exitCode\nError: $stderr")
        }
    }

    test("CLI exits with non-zero code and clear error on 401 auth failure") {
        // Stub GET /v1/budgets/{id}/transactions with 401 Unauthorized response
        // This simulates YNAB API rejecting an invalid token
        wireMockServer.stubFor(
            get(urlMatching("/v1/budgets/.*/transactions\\?.*"))
                .willReturn(
                    aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error":{"id":"401","name":"unauthorized","detail":"Unauthorized"}}""")
                )
        )

        // Stub PATCH endpoint with 200 response (should never be called)
        // This is a safety stub - the test will verify zero PATCH calls were made
        wireMockServer.stubFor(
            patch(urlMatching("/v1/budgets/.*/transactions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"data":{"transaction_ids":[],"transactions":[]}}""")
                )
        )

        // Invoke CLI with invalid token
        // Note: Omit --dry-run flag so CLI would attempt real updates if auth succeeded
        val result = runCli(
            "--api-url", apiBaseUrl,
            "--token", "invalid-token",
            "--budget-id", "test-budget-id"
        )


        // Verification 1: CLI exit code must be non-zero
        // When auth fails, CLI should indicate failure via exit code
        result.exitCode shouldNotBe 0

        // Verification 2: CLI should output error message mentioning auth failure
        // Check stderr first (standard practice for error messages)
        // If CLI logs to stdout instead, check both streams
        // Be flexible on exact wording - may say "401", "Unauthorized", "unauthorized", etc.
        val combinedOutput = result.stderr + result.stdout
        val hasAuthError = combinedOutput.contains("401", ignoreCase = true) ||
                          combinedOutput.contains("Unauthorized", ignoreCase = true)

        // If no auth error found, fail with descriptive message showing actual output
        if (!hasAuthError) {
            throw AssertionError(
                "Expected CLI output to contain '401' or 'Unauthorized', but got:\n" +
                "Exit code: ${result.exitCode}\n" +
                "Stdout (first 500 chars): ${result.stdout.take(500)}\n" +
                "Stderr (first 500 chars): ${result.stderr.take(500)}"
            )
        }

        // Verification 3: Zero PATCH calls should be made
        // This proves CLI exited early after receiving 401, without attempting any updates
        wireMockServer.verify(0, patchRequestedFor(urlMatching("/v1/budgets/.*/transactions")))

        // Verification 4: GET was called exactly once
        // This proves 401 happened at the transaction fetch stage (expected entry point)
        wireMockServer.verify(1, getRequestedFor(urlMatching("/v1/budgets/.*/transactions\\?.*")))
    }
})
