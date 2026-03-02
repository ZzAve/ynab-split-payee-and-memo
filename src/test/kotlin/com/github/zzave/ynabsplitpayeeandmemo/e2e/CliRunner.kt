package com.github.zzave.ynabsplitpayeeandmemo.e2e

import java.io.File

/**
 * Runs the CLI application as a subprocess using ProcessBuilder.
 *
 * This function:
 * - Finds the test JAR in build/libs/ directory (glob pattern: *-all.jar)
 * - Executes: java -jar {jarPath} {args}
 * - Captures stdout and stderr
 * - Waits for process completion
 * - Returns CliResult with exit code and captured output
 *
 * @param args Command-line arguments to pass to the CLI
 * @return CliResult containing exit code, stdout, and stderr
 * @throws IllegalStateException if test JAR not found
 */
fun runCli(vararg args: String): CliResult {
    // Find test JAR in build/libs/ directory
    val jarPath = findTestJar()

    // Build command: java -jar {jarPath} {args}
    val command = mutableListOf("java", "-jar", jarPath.absolutePath)
    command.addAll(args)

    // Execute process
    val process = ProcessBuilder(command)
        .directory(File(System.getProperty("user.dir")))
        .start()

    // Capture stdout and stderr
    val stdout = process.inputStream.bufferedReader().readText()
    val stderr = process.errorStream.bufferedReader().readText()

    // Wait for process completion
    val exitCode = process.waitFor()

    return CliResult(
        exitCode = exitCode,
        stdout = stdout,
        stderr = stderr
    )
}

/**
 * Finds the test JAR file in build/libs/ directory.
 *
 * Searches for files matching pattern: *-all.jar
 * If multiple JARs found, uses the newest by lastModified timestamp.
 *
 * @return File pointing to the test JAR
 * @throws IllegalStateException if no JAR found
 */
private fun findTestJar(): File {
    val libsDir = File(System.getProperty("user.dir"), "build/libs")

    if (!libsDir.exists() || !libsDir.isDirectory) {
        throw IllegalStateException(
            "build/libs directory not found. Run: ./gradlew shadowJar -PtestBuild=true"
        )
    }

    val jarFiles = libsDir.listFiles { file ->
        file.name.endsWith("-all.jar")
    }?.toList() ?: emptyList()

    if (jarFiles.isEmpty()) {
        throw IllegalStateException(
            "Test JAR not found in build/libs/. Run: ./gradlew shadowJar -PtestBuild=true"
        )
    }

    // If multiple JARs exist, use the newest one
    return jarFiles.maxByOrNull { it.lastModified() }
        ?: throw IllegalStateException("No JAR files found")
}
