package com.github.zzave.ynabsplitpayeeandmemo.e2e

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking

/**
 * Test that verifies WireMockTestBase provides automatic WireMock server lifecycle management.
 * This test should fail initially (RED) since WireMockTestBase doesn't exist yet.
 */
class WireMockTestBaseTest :
    WireMockTestBase({

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

        test("WireMock server responds to stubbed requests") {
            // Stub a simple endpoint
            wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock
                    .get("/v1/test")
                    .willReturn(
                        com.github.tomakehurst.wiremock.client.WireMock
                            .aResponse()
                            .withStatus(200)
                            .withBody("test response"),
                    ),
            )

            // Make HTTP request to verify server is running and responds
            runBlocking {
                val client =
                    HttpClient(Java) {
                        engine {
                            protocolVersion = java.net.http.HttpClient.Version.HTTP_1_1
                        }
                    }
                val response = client.get("$apiBaseUrl/test")

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "test response"

                client.close()
            }
        }

        test("WireMock server port is dynamic") {
            // Should use dynamic port (not hardcoded)
            val port = wireMockServer.port()
            port shouldNotBe 8080 // Not using default fixed port
            port shouldNotBe 0 // Should be assigned
        }
    })
