package com.github.zzave.ynabsplitpayeeandmemo.e2e

import java.io.File

/**
 * Runs the CLI application as a subprocess using ProcessBuilder.
 *
 * This function:
 * - Finds the debug JAR in build/libs/ directory (glob pattern: *-debug-all.jar)
 * - Executes: java -jar {jarPath} {args}
 * - Captures stdout and stderr
 * - Waits for process completion
 * - Returns CliResult with exit code and captured output
 *
 * @param args Command-line arguments to pass to the CLI
 * @return CliResult containing exit code, stdout, and stderr
 * @throws IllegalStateException if debug JAR not found
 */
fun runCli(vararg args: String): CliResult {
    // Find debug JAR in build/libs/ directory
    val jarPath = findDebugJar()

    // Prefer the same JVM that runs the tests to avoid class-version mismatches.
    // Allow override via environment variable for CI/debugging.
    val javaExecutable = resolveJavaExecutable()

    // Build command: java -jar {jarPath} {args}
    val command = mutableListOf(javaExecutable.absolutePath, "-jar", jarPath.absolutePath)
    command.addAll(args)

    // Execute process with clean environment (no YNAB_* env vars)
    // This ensures tests have full control over CLI configuration via explicit arguments
    val process =
        ProcessBuilder(command)
            .directory(File(System.getProperty("user.dir")))
            .apply {
                // Remove YNAB environment variables to prevent interference with test arguments
                val env = environment()
                env.keys.removeIf { it.startsWith("YNAB_") }
            }.start()

    // Capture stdout and stderr
    val stdout = process.inputStream.bufferedReader().readText()
    val stderr = process.errorStream.bufferedReader().readText()

    // Wait for process completion
    val exitCode = process.waitFor()

    return CliResult(
        exitCode = exitCode,
        stdout = stdout,
        stderr = stderr,
    )
}

private fun resolveJavaExecutable(): File {
    // Optional override: set CLI_JAVA_HOME to point to a JDK home directory
    val overrideJavaHome = System.getenv("CLI_JAVA_HOME")?.takeIf { it.isNotBlank() }?.let(::File)
    val javaHome = overrideJavaHome ?: File(System.getProperty("java.home"))

    val binName = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "java.exe" else "java"
    val candidate = File(javaHome, "bin/$binName")

    require(candidate.exists()) {
        "Java executable not found at: ${candidate.absolutePath}. " +
            "java.home=${System.getProperty("java.home")} CLI_JAVA_HOME=${System.getenv("CLI_JAVA_HOME")}"
    }
    return candidate
}

/**
 * Finds the debug JAR file in build/libs/ directory.
 *
 * Searches for files matching pattern: *-debug-all.jar
 * If multiple JARs found, uses the newest by lastModified timestamp.
 *
 * @return File pointing to the debug JAR
 * @throws IllegalStateException if no JAR found
 */
private fun findDebugJar(): File {
    val libsDir = File(System.getProperty("user.dir"), "build/libs")

    if (!libsDir.exists() || !libsDir.isDirectory) {
        throw IllegalStateException(
            "build/libs directory not found. Run: ./gradlew debugShadowJar",
        )
    }

    val jarFiles =
        libsDir
            .listFiles { file ->
                file.name.endsWith("-debug-all.jar")
            }?.toList() ?: emptyList()

    if (jarFiles.isEmpty()) {
        throw IllegalStateException(
            "Debug JAR not found in build/libs/. Run: ./gradlew debugShadowJar",
        )
    }

    // If multiple JARs exist, use the newest one
    return jarFiles.maxByOrNull { it.lastModified() }
        ?: throw IllegalStateException("No JAR files found")
}
