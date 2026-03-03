---
phase: 01-cli-configuration
plan: 01
subsystem: cli-testing-infrastructure
tags: [cli, testing, e2e, configuration]
requirements: [CLI-01, CLI-02]
dependency_graph:
  requires: []
  provides:
    - configurable-api-url
    - test-build-mode
  affects:
    - e2e-test-framework
tech_stack:
  added:
    - clikt-testing: "5.1.0"
  patterns:
    - build-time-feature-flags
    - gradle-property-based-builds
key_files:
  created:
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/YnabClientTest.kt
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/YnabSplitPayeeAndMemoTest.kt
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/BuildInfoTest.kt
  modified:
    - src/main/kotlin/com/github/zzave/ynabsplitpayeeandmemo/YnabClient.kt
    - src/main/kotlin/com/github/zzave/ynabsplitpayeeandmemo/YnabSplitPayeeAndMemo.kt
    - build.gradle.kts
    - gradle/libs.versions.toml
    - CLAUDE.md
decisions:
  - decision: "Use Gradle property -PtestBuild=true for build-time feature flag"
    rationale: "Compile-time check prevents accidental production use of --api-url flag"
    alternatives: ["Runtime env var check (less secure)", "Separate source sets (more complex)"]
  - decision: "elvis operator for apiUrl default in YnabClient constructor"
    rationale: "Ensures default URL is always used when apiUrl is null"
    alternatives: ["Let baseUrl parameter handle default (current approach is more explicit)"]
  - decision: "Use echo() for error messages in CLI validation"
    rationale: "Clikt's echo() is testable, logger output is not captured by Clikt testing framework"
    alternatives: ["Logger only (not testable in unit tests)"]
metrics:
  duration: 418s
  tasks_completed: 3
  files_created: 3
  files_modified: 5
  tests_added: 6
  commits: 3
  completed_date: 2026-03-02
---

# Phase 01 Plan 01: CLI Configuration Summary

**One-liner:** Added configurable YNAB API base URL support with build-time test/production mode flag, enabling E2E tests to point CLI at WireMock mock server while keeping production builds secure with hardcoded URL.

## Overview

Successfully implemented configurable API URL support for the CLI with a two-tier build system. Production builds (default) reject the `--api-url` flag to prevent accidental use of test infrastructure. Test builds (built with `-PtestBuild=true`) accept the flag and `YNAB_API_URL` environment variable, enabling E2E tests to point the CLI at a local WireMock mock server instead of the real YNAB API.

## Tasks Completed

### Task 1: Add --api-url CLI option and pass to YnabClient (TDD)
**Status:** Complete ✓
**Commit:** b2747b2
**Files:**
- Modified: `YnabClient.kt`, `YnabSplitPayeeAndMemo.kt`, `build.gradle.kts`, `gradle/libs.versions.toml`
- Created: `YnabClientTest.kt`, `YnabSplitPayeeAndMemoTest.kt`

**Implementation:**
- Added `baseUrl` parameter to YnabClient constructor with default value `"https://api.ynab.com/v1"`
- Added `--api-url` CLI option with `YNAB_API_URL` environment variable support
- Added Clikt testing dependency for CLI unit tests
- Created test suite verifying baseUrl parameter behavior and CLI option parsing

**TDD Flow:**
- RED: Created failing tests for YnabClient baseUrl parameter and CLI option parsing
- GREEN: Implemented baseUrl parameter and --api-url option, all tests pass
- Tests: YnabClientTest (2 tests), YnabSplitPayeeAndMemoTest (2 tests)

### Task 2: Create test JAR build with isTestBuild flag (TDD)
**Status:** Complete ✓
**Commit:** 09e0ba9
**Files:**
- Modified: `build.gradle.kts`, `YnabSplitPayeeAndMemo.kt`, `YnabSplitPayeeAndMemoTest.kt`
- Created: `BuildInfoTest.kt`

**Implementation:**
- Modified `generateBuildInfo` task to read `-PtestBuild` Gradle property and generate `BuildInfo.isTestBuild` flag
- Added validation in `YnabSplitPayeeAndMemo.doRun()` to reject `--api-url` when `isTestBuild = false`
- Used Clikt's `echo()` function for testable error messages
- Production build: `./gradlew shadowJar` → `isTestBuild = false`
- Test build: `./gradlew shadowJar -PtestBuild=true` → `isTestBuild = true`

**TDD Flow:**
- RED: Created failing test for BuildInfo.isTestBuild property
- GREEN: Modified generateBuildInfo task, added validation logic, all tests pass
- Tests: BuildInfoTest (1 test), YnabSplitPayeeAndMemoTest (added 1 test for rejection)

### Task 3: Document new options and test build workflow
**Status:** Complete ✓
**Commit:** 634ed57
**Files:**
- Modified: `CLAUDE.md`

**Documentation added:**
- "Testing Infrastructure" section explaining production vs test build modes
- Build command reference for test builds
- Example usage with mock server
- Explanation of isTestBuild flag implementation
- Note that this is test-only infrastructure (not user-facing)

**Verification:** Automated verification passed (grep checks for expected sections)

## Deviations from Plan

None - plan executed exactly as written. All three tasks completed successfully with TDD approach for Tasks 1-2.

## Technical Decisions

1. **Build-time feature flag via Gradle property**
   - Compile-time check prevents production misuse
   - Alternative runtime checks would be less secure
   - Separate source sets considered but deemed too complex

2. **Elvis operator for default URL**
   - Explicit null handling in CLI: `YnabClient(token, apiUrl ?: "https://api.ynab.com/v1")`
   - Makes default behavior clear at call site

3. **Clikt echo() for validation errors**
   - Required for testable error messages
   - Logger output not captured by Clikt testing framework

## Testing

**Unit Tests Added:**
- `YnabClientTest.kt`: 2 tests (baseUrl parameter acceptance, default URL)
- `YnabSplitPayeeAndMemoTest.kt`: 3 tests (--api-url flag parsing, missing flag handling, production rejection)
- `BuildInfoTest.kt`: 1 test (isTestBuild defaults to false)

**Test Execution:**
- All existing tests pass: `make test` → BUILD SUCCESSFUL
- New tests verified with both full class names and wildcard patterns
- Production build rejection verified via Gradle run task

**Build Verification:**
- Production JAR builds: `./gradlew shadowJar` → SUCCESS
- Test JAR builds: `./gradlew shadowJar -PtestBuild=true` → SUCCESS
- BuildInfo.isTestBuild correctly set in both modes

## Impact

**Immediate:**
- Production builds remain secure (hardcoded URL, rejects --api-url)
- Test builds can point CLI at mock servers for E2E testing
- Foundation laid for Phase 2 (WireMock + ProcessBuilder E2E tests)

**Dependencies satisfied:**
- CLI-01: ✓ CLI accepts --api-url flag
- CLI-02: ✓ CLI respects YNAB_API_URL environment variable

**Next phase enabled:**
- Phase 2 can now build test JAR and run E2E tests against WireMock mock server

## Self-Check

**Created files exist:**
```bash
FOUND: src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/YnabClientTest.kt
FOUND: src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/YnabSplitPayeeAndMemoTest.kt
FOUND: src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/BuildInfoTest.kt
```

**Commits exist:**
```bash
FOUND: b2747b2
FOUND: 09e0ba9
FOUND: 634ed57
```

## Self-Check: PASSED

All files created, all commits exist, all tests pass.
