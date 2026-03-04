package com.github.zzave.ynabsplitpayeeandmemo.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

/**
 * Tests for CliRunner - ProcessBuilder-based CLI invocation helper.
 *
 * These are unit tests for the CliRunner helper logic:
 * - CliResult data class structure
 * - JAR file discovery logic
 * - Error handling for missing JAR
 *
 * Full integration testing (actually running the CLI) happens in SmokeTest.
 */
class CliRunnerTest : FunSpec({

    test("CliResult captures exit code, stdout, stderr") {
        // This test verifies the CliResult data class structure
        val result = CliResult(
            exitCode = 0,
            stdout = "test output",
            stderr = "test error"
        )

        result.exitCode shouldBe 0
        result.stdout shouldBe "test output"
        result.stderr shouldBe "test error"
    }

    test("CliResult is a data class with correct properties") {
        val result1 = CliResult(0, "out", "err")
        val result2 = CliResult(0, "out", "err")
        val result3 = CliResult(1, "out", "err")

        // Data class equality
        result1 shouldBe result2
        result1.toString() shouldContain "CliResult"
        result1.toString() shouldContain "exitCode=0"

        // Different exit codes are not equal
        (result1 == result3) shouldBe false
    }

    test("runCli() throws clear error when JAR not found") {
        // Temporarily hide the libs directory
        val libsDir = File(System.getProperty("user.dir"), "build/libs")
        val tempDir = File(System.getProperty("user.dir"), "build/libs-temp")

        if (libsDir.exists()) {
            libsDir.renameTo(tempDir)
        }

        try {
            val exception = shouldThrow<IllegalStateException> {
                runCli("--help")
            }

            exception.message shouldContain "build/libs directory not found"
            exception.message shouldContain "./gradlew debugShadowJar"
        } finally {
            // Restore libs directory
            if (tempDir.exists()) {
                tempDir.renameTo(libsDir)
            }
        }
    }

    test("runCli() throws clear error when no JAR files found") {
        val libsDir = File(System.getProperty("user.dir"), "build/libs")

        // Temporarily rename any JAR files
        val jarFiles = libsDir.listFiles { f -> f.name.endsWith("-debug-all.jar") }?.toList() ?: emptyList()
        val tempNames = jarFiles.map { it to File(libsDir, "${it.name}.hidden") }

        tempNames.forEach { (jar, temp) ->
            if (jar.exists()) jar.renameTo(temp)
        }

        try {
            val exception = shouldThrow<IllegalStateException> {
                runCli("--help")
            }

            exception.message shouldContain "Debug JAR not found"
            exception.message shouldContain "./gradlew debugShadowJar"
        } finally {
            // Restore JAR files
            tempNames.forEach { (jar, temp) ->
                if (temp.exists()) temp.renameTo(jar)
            }
        }
    }
})
