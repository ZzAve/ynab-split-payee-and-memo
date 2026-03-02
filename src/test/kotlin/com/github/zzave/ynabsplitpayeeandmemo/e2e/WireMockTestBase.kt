package com.github.zzave.ynabsplitpayeeandmemo.e2e

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecRootScope

/**
 * Base test class for E2E tests that provides automatic WireMock server lifecycle management.
 *
 * The WireMock server starts automatically in beforeSpec hook (before any tests in the class run)
 * and stops automatically in afterSpec hook (after all tests complete).
 *
 * Test classes extending this get:
 * - `wireMockServer`: WireMockServer instance for stubbing API responses
 * - `apiBaseUrl`: Base URL for CLI --api-url flag (http://localhost:{port}/v1)
 *
 * Example usage:
 * ```kotlin
 * class MyE2ETest : WireMockTestBase({
 *     test("my test") {
 *         wireMockServer.stubFor(
 *             WireMock.get("/v1/budgets")
 *                 .willReturn(aResponse().withStatus(200).withBody("..."))
 *         )
 *
 *         // Run CLI with --api-url $apiBaseUrl
 *     }
 * })
 * ```
 */
abstract class WireMockTestBase(body: WireMockTestBase.() -> Unit) : FunSpec() {

    /**
     * WireMock server instance. Automatically started before tests and stopped after.
     */
    lateinit var wireMockServer: WireMockServer

    /**
     * Base URL for the mock YNAB API, including /v1 path.
     * Use this value for the CLI --api-url parameter.
     *
     * Format: http://localhost:{dynamicPort}/v1
     */
    val apiBaseUrl: String
        get() = "http://localhost:${wireMockServer.port()}/v1"

    init {
        // Start WireMock server before any tests in this spec run
        beforeSpec {
            wireMockServer = WireMockServer(
                WireMockConfiguration.options()
                    .dynamicPort()  // Use random available port (not fixed 8080)
                    .disableRequestJournal()  // Reduce memory usage
            )
            wireMockServer.start()
        }

        // Stop WireMock server after all tests in this spec complete
        afterSpec {
            wireMockServer.stop()
        }

        // Execute the test body lambda with this class instance as receiver
        body()
    }
}
