package com.github.zzave.ynabsplitpayeeandmemo.e2e

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Test that verifies WireMockTestBase provides automatic WireMock server lifecycle management.
 * This test should fail initially (RED) since WireMockTestBase doesn't exist yet.
 */
class WireMockTestBaseTest : WireMockTestBase() {

    test("WireMock server is automatically started before tests") {
        // Access the wireMockServer property - should be started
        wireMockServer shouldNotBe null
        wireMockServer.isRunning shouldBe true
    }

    test("apiBaseUrl property returns correct format") {
        // Should return http://localhost:{port}/v1
        apiBaseUrl shouldNotBe null
        apiBaseUrl shouldStartWith "http://localhost:"
        apiBaseUrl shouldContain "/v1"
    }

    test("WireMock server responds to stubbed requests").config(coroutineTestScope = true) {
        // Stub a simple endpoint
        wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.get("/v1/test")
                .willReturn(
                    com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withBody("test response")
                )
        )

        // Make HTTP request to verify server is running and responds
        val client = HttpClient(CIO)
        val response = client.get("${apiBaseUrl}/test")

        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldBe "test response"

        client.close()
    }

    test("WireMock server port is dynamic") {
        // Should use dynamic port (not hardcoded)
        val port = wireMockServer.port()
        port shouldNotBe 8080 // Not using default fixed port
        port shouldNotBe 0    // Should be assigned
    }
}
