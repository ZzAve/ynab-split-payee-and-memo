package com.github.zzave.ynabsplitpayeeandmemo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BuildInfoTest :
    FunSpec({

        test("BuildInfo.IS_DEBUG_BUILD defaults to false without properties file") {
            // Unit tests run without build-info.properties on the classpath,
            // so IS_DEBUG_BUILD defaults to false (same as production JAR behavior).
            BuildInfo.IS_DEBUG_BUILD shouldBe false
        }
    })
