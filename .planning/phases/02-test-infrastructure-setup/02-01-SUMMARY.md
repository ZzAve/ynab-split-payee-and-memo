---
phase: 02-test-infrastructure-setup
plan: 01
subsystem: test-infrastructure
tags: [testing, wiremock, e2e, infrastructure]

dependency_graph:
  requires: [CLI-01, CLI-02]
  provides: [TEST-01]
  affects: []

tech_stack:
  added:
    - WireMock 3.10.0 standalone (HTTP mock server)
  patterns:
    - Kotest FunSpec base class for lifecycle management
    - beforeSpec/afterSpec hooks for setup/teardown
    - Dynamic port allocation for parallel test execution

key_files:
  created:
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/WireMockTestBase.kt
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/WireMockTestBaseTest.kt
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/WireMockDependencyTest.kt
  modified:
    - gradle/libs.versions.toml
    - build.gradle.kts

decisions:
  - decision: Use WireMock standalone over wiremock-jre8
    rationale: JVM 21 compatibility, includes all dependencies
  - decision: Use dynamic port allocation
    rationale: Enables parallel test execution, avoids port conflicts
  - decision: Disable request journal
    rationale: Reduces memory usage for E2E tests
  - decision: Make wireMockServer/apiBaseUrl public (not protected)
    rationale: Kotest lambda receiver scope requires direct access

metrics:
  duration_seconds: 385
  duration_readable: "~6 minutes"
  completed_date: "2026-03-02"
  tasks_completed: 2
  commits: 4
  files_modified: 5
  test_coverage: 100%
---

# Phase 02 Plan 01: WireMock Mock Server Infrastructure Summary

**One-liner:** WireMock 3.10.0 standalone with automatic lifecycle management via Kotest base class for E2E test infrastructure

## Objective Achieved

Established WireMock mock server infrastructure for E2E tests with automatic start/stop per test class. Tests can now run against a local HTTP mock server simulating the YNAB API v1 without hitting the real API.

**Output:** WireMock dependency configured, WireMockTestBase class provides automatic server lifecycle management.

## Tasks Completed

### Task 1: Add WireMock dependency to Gradle

**Commits:**
- `18fa47f` - test(02-01): add failing test for WireMock dependency (RED)
- `0f244d9` - feat(02-01): add WireMock 3.10.0 standalone dependency (GREEN)

**Implementation:**
- Added `wiremock = "3.10.0"` to versions catalog
- Added `wiremock = { group = "org.wiremock", name = "wiremock-standalone", version.ref = "wiremock" }` to libraries catalog
- Added `testImplementation(libs.wiremock)` to build.gradle.kts dependencies

**Verification:**
```bash
./gradlew dependencies --configuration testRuntimeClasspath | grep wiremock
# Output: \--- org.wiremock:wiremock-standalone:3.10.0
```

**Status:** ✅ Complete - WireMock 3.10.0 appears in test dependencies, Gradle sync successful

### Task 2: Create WireMockTestBase with automatic lifecycle management

**Commits:**
- `c790c98` - test(02-01): add failing test for WireMockTestBase lifecycle management (RED)
- `5e38644` - feat(02-01): implement WireMockTestBase with automatic lifecycle management (GREEN)

**Implementation:**
- Created `WireMockTestBase` abstract class extending Kotest FunSpec
- Used `beforeSpec` hook to start WireMock server with dynamic port
- Used `afterSpec` hook to stop WireMock server
- Exposed `wireMockServer` property (lateinit var) for stubbing API responses
- Exposed `apiBaseUrl` property (computed) returning `http://localhost:{port}/v1`
- Configured WireMock with `dynamicPort()` and `disableRequestJournal()`

**Key design decision:** Base class accepts lambda with `WireMockTestBase.() -> Unit` receiver to allow test code to access instance properties (`wireMockServer`, `apiBaseUrl`) directly.

**Verification:**
```bash
make test
# All tests pass including:
# - WireMock server is automatically started before tests
# - apiBaseUrl property returns correct format
# - WireMock server responds to stubbed requests
# - WireMock server port is dynamic
```

**Status:** ✅ Complete - WireMockTestBase compiles, all verification tests pass, demonstrates automatic lifecycle

## Deviations from Plan

None - plan executed exactly as written. Both tasks followed TDD approach (RED → GREEN commits) and met all success criteria.

## Verification Results

All success criteria met:

1. ✅ WireMock 3.10.0 standalone dependency added and resolves successfully
2. ✅ WireMockTestBase class provides automatic server start/stop via Kotest hooks
3. ✅ Base class exposes `apiBaseUrl` property for CLI --api-url injection
4. ✅ All tests pass including new WireMockTestBase verification tests (4 tests)

**Test results:**
- Total tests: 19 (existing 15 + new 4)
- Passed: 19
- Failed: 0
- Test execution time: ~5.5 seconds

**Key files verified:**
```bash
src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/WireMockTestBase.kt (2231 bytes)
src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/WireMockTestBaseTest.kt
src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/WireMockDependencyTest.kt
```

## Technical Implementation Notes

**WireMockTestBase design:**
- Abstract class pattern allows test classes to inherit lifecycle management
- Kotest's `beforeSpec`/`afterSpec` hooks run once per test class (not per test)
- Server started before any tests run, stopped after all tests complete
- No manual cleanup needed in test classes

**Dynamic port allocation:**
- `WireMockConfiguration.options().dynamicPort()` uses random available port
- Prevents port conflicts when running tests in parallel
- `wireMockServer.port()` provides actual assigned port
- `apiBaseUrl` computed property constructs full URL with correct port

**Memory optimization:**
- `disableRequestJournal()` reduces memory footprint
- Suitable for E2E tests that don't need request history verification

## Next Steps

1. **Ready for next plan:** 02-02-PLAN.md - ProcessBuilder test harness for CLI subprocess invocation
2. **Dependencies satisfied:** TEST-01 requirement complete (WireMock infrastructure)
3. **Blocker status:** None - infrastructure ready for E2E test implementation

## Commits Summary

| Hash    | Type | Description                                            |
|---------|------|--------------------------------------------------------|
| 18fa47f | test | Add failing test for WireMock dependency (RED)         |
| 0f244d9 | feat | Add WireMock 3.10.0 standalone dependency (GREEN)      |
| c790c98 | test | Add failing test for WireMockTestBase lifecycle (RED)  |
| 5e38644 | feat | Implement WireMockTestBase with lifecycle mgmt (GREEN) |

**Total commits:** 4 (2 RED, 2 GREEN - proper TDD discipline)

---

*Summary created: 2026-03-02*
*Execution time: ~6 minutes*
*All success criteria met ✅*


## Self-Check: PASSED ✅

**File verification:**
- ✓ src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/WireMockTestBase.kt (2231 bytes)
- ✓ src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/WireMockTestBaseTest.kt (2162 bytes)
- ✓ src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/WireMockDependencyTest.kt (1103 bytes)

**Commit verification:**
- ✓ 18fa47f - test(02-01): add failing test for WireMock dependency
- ✓ 0f244d9 - feat(02-01): add WireMock 3.10.0 standalone dependency
- ✓ c790c98 - test(02-01): add failing test for WireMockTestBase lifecycle management
- ✓ 5e38644 - feat(02-01): implement WireMockTestBase with automatic lifecycle management

All claimed files exist. All claimed commits exist in git history.
