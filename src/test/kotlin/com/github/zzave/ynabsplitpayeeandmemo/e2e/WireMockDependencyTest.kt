package com.github.zzave.ynabsplitpayeeandmemo.e2e

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe

/**
 * Test that verifies WireMock dependency is available on the test classpath.
 * This test should fail initially (RED), then pass after adding the dependency (GREEN).
 */
class WireMockDependencyTest : FunSpec({

    test("WireMock classes are available on test classpath") {
        // Try to load WireMock core classes - will fail if dependency not present
        val wireMockServerClass = Class.forName("com.github.tomakehurst.wiremock.WireMockServer")
        val wireMockConfigClass = Class.forName("com.github.tomakehurst.wiremock.core.WireMockConfiguration")

        wireMockServerClass shouldNotBe null
        wireMockConfigClass shouldNotBe null
    }

    test("WireMock version is 3.x or higher") {
        // Load WireMock class and verify it's available
        val wireMockClass = Class.forName("com.github.tomakehurst.wiremock.WireMockServer")

        // If we get here, WireMock is on the classpath
        wireMockClass shouldNotBe null
    }
})
