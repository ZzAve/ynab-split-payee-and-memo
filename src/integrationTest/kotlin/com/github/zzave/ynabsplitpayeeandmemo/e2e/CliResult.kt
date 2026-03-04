package com.github.zzave.ynabsplitpayeeandmemo.e2e

/**
 * Result from executing the CLI as a subprocess.
 *
 * @property exitCode The process exit code (0 = success)
 * @property stdout Standard output captured from the process
 * @property stderr Standard error output captured from the process
 */
data class CliResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)
