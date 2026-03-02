package com.github.zzave.ynabsplitpayeeandmemo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BuildInfoTest : FunSpec({

    test("BuildInfo.isTestBuild reflects compile-time flag") {
        // BuildInfo.isTestBuild is generated at compile time based on -PtestBuild property
        // This test just verifies the property exists and is a boolean
        val isTestBuild = BuildInfo.isTestBuild

        // Should be a boolean (either true or false)
        (isTestBuild is Boolean) shouldBe true

        // Value depends on how the code was compiled:
        // - Default (./gradlew build): isTestBuild = false
        // - Test build (./gradlew build -PtestBuild=true): isTestBuild = true
    }

    // Note: We can't test both states in a single test run because BuildInfo is generated at compile time.
    // The actual behavior (accepting/rejecting --api-url flag) is tested by E2E tests.
})
