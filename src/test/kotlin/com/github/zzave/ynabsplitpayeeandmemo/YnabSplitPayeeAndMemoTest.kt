package com.github.zzave.ynabsplitpayeeandmemo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import com.github.ajalt.clikt.testing.test

class YnabSplitPayeeAndMemoTest : FunSpec({

    test("CLI reads --api-url flag") {
        // Test that the --api-url option can be provided without error
        val cmd = YnabSplitPayeeAndMemo()
        val result = cmd.test("--token fake-token --api-url http://localhost:8080/v1 --dry-run")

        // Verify command doesn't reject the --api-url flag
        // (It will fail on API call with fake token, but shouldn't reject unknown flag)
        result.output shouldNotContain "no such option"
        result.output shouldNotContain "Error: No such option"
    }

    test("CLI accepts missing --api-url (uses default)") {
        // Test that the YNAB_API_URL environment variable support exists
        // (we can't easily test env vars in unit tests, but we can verify the option is optional)
        val cmd = YnabSplitPayeeAndMemo()
        val result = cmd.test("--token fake-token --dry-run")

        // Command should accept missing --api-url without complaining
        result.output shouldNotContain "Missing option \"--api-url\""
        result.output shouldNotContain "no such option"
    }
})
