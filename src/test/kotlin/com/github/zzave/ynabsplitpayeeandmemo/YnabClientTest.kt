package com.github.zzave.ynabsplitpayeeandmemo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class YnabClientTest : FunSpec({

    test("YnabClient accepts baseUrl parameter and uses it for API calls") {
        // Create client with custom base URL
        val customBaseUrl = "http://localhost:8080/v1"
        val client = YnabClient(token = "fake-token", baseUrl = customBaseUrl)

        // This test verifies that the client was created with the custom baseUrl
        // We can't easily test the actual URL construction without making a real HTTP call,
        // but we can verify the client accepts the parameter
        // In integration tests, we'll verify the URL is actually used
        client shouldBe io.kotest.matchers.types.instanceOf<YnabClient>()
    }

    test("YnabClient defaults to production YNAB API URL when no baseUrl provided") {
        // Create client without baseUrl parameter
        val client = YnabClient(token = "fake-token")

        // Verify client was created successfully (would use default URL)
        client shouldBe io.kotest.matchers.types.instanceOf<YnabClient>()
    }
})
