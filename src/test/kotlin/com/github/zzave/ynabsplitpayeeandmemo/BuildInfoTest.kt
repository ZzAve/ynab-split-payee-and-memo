package com.github.zzave.ynabsplitpayeeandmemo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BuildInfoTest : FunSpec({

    test("BuildInfo.isTestBuild is false in production builds") {
        // In production builds (default), isTestBuild should be false
        // This test will fail initially because isTestBuild doesn't exist yet
        BuildInfo.isTestBuild shouldBe false
    }

    // Note: We can't easily test "BuildInfo.isTestBuild is true when built with testBuild=true"
    // in the same test run, because BuildInfo is generated at compile time.
    // That will be tested by building the test JAR and running it.
})
