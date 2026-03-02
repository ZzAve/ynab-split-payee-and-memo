---
phase: 03-core-e2e-tests
plan: 02
subsystem: testing
tags: [e2e, wiremock, kotest, auth, error-handling]

# Dependency graph
requires:
  - phase: 02-test-infrastructure-setup
    provides: WireMock server infrastructure and CLI invocation harness
provides:
  - Auth error E2E test validating 401 handling
  - Clean environment test pattern (removes YNAB_* env vars)
affects: [03-01, 04-edge-case-coverage]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Clean environment pattern for E2E tests (clear YNAB_* env vars)"
    - "WireMock instance method stubbing (wireMockServer.stubFor())"
    - "Flexible error message assertion (401 OR Unauthorized)"

key-files:
  created:
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/AuthErrorTest.kt
  modified:
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/CliRunner.kt

key-decisions:
  - "Clear YNAB_* environment variables in runCli() for clean test isolation"
  - "Use wireMockServer.stubFor() instance method instead of static stubFor()"
  - "Check for '401' OR 'Unauthorized' in output for flexibility"

patterns-established:
  - "Clean environment E2E test pattern: remove YNAB_* env vars in CliRunner"
  - "WireMock verification pattern: verify zero PATCH calls after auth failure"
  - "Flexible error assertion: check multiple error indicators (401/Unauthorized)"

requirements-completed: [E2E-03]

# Metrics
duration: 584s
completed: 2026-03-02
---

# Phase 03 Plan 02: Auth Error E2E Test Summary

**E2E test validates CLI exits non-zero on 401, outputs clear error, and makes zero update calls**

## Performance

- **Duration:** 9min 44s
- **Started:** 2026-03-02T16:29:15Z
- **Completed:** 2026-03-02T16:38:59Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments
- AuthErrorTest validates CLI properly handles YNAB API 401 responses
- CLI exits with code 1 when auth fails
- CLI outputs clear error message containing "401 Unauthorized"
- CLI makes zero PATCH calls after 401 (proves early exit before updates)
- CliRunner now clears YNAB environment variables for clean test isolation

## Task Commits

Each task was committed atomically:

1. **Task 1: Create auth error E2E test** - `c4a9177` (feat)
   - TDD flow: test created and passed immediately (CLI already handles 401 correctly)

## Files Created/Modified
- `src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/AuthErrorTest.kt` - E2E test validating 401 auth error handling
- `src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/CliRunner.kt` - Added environment cleanup to prevent test interference

## Decisions Made
1. **Clear YNAB environment variables in CliRunner** - Discovered environment variables (YNAB_BUDGET_IDS, YNAB_TOKEN, etc.) were interfering with test arguments. Added environment cleanup to ProcessBuilder to ensure tests have full control.

2. **Use wireMockServer.stubFor() instance method** - Static stubFor() was creating stubs on wrong WireMock instance (global vs test instance). Switched to instance method for correct behavior.

3. **Flexible error assertion (401 OR Unauthorized)** - Check for both "401" and "Unauthorized" in output to handle different HttpStatusCode string representations.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added environment variable cleanup to CliRunner**
- **Found during:** Task 1 (Auth error test execution)
- **Issue:** YNAB_BUDGET_IDS and other env vars from host environment were interfering with test arguments, causing CLI to fail with "Either budget-id or budget-ids should be provided, but not both"
- **Fix:** Added environment cleanup in ProcessBuilder: `environment().keys.removeIf { it.startsWith("YNAB_") }`
- **Files modified:** src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/CliRunner.kt
- **Verification:** Test runs with clean environment, only uses explicit CLI arguments
- **Committed in:** c4a9177 (part of task commit)

**2. [Rule 3 - Blocking] Changed static stubFor() to instance method**
- **Found during:** Task 1 (WireMock stub setup)
- **Issue:** Static stubFor() was not associating stubs with test's WireMock instance, resulting in 404 responses
- **Fix:** Changed `stubFor(...)` to `wireMockServer.stubFor(...)`
- **Files modified:** src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/AuthErrorTest.kt
- **Verification:** WireMock returns 401 as expected, test passes
- **Committed in:** c4a9177 (part of task commit)

---

**Total deviations:** 2 auto-fixed (1 missing critical, 1 blocking)
**Impact on plan:** Both fixes necessary for correct test operation. Environment cleanup improves test isolation for all future E2E tests. No scope creep.

## Issues Encountered
- **Kotest descriptor issue with --tests filter:** Running `make test TEST="*AuthErrorTest*"` triggers IllegalStateException related to BuildInfoTest descriptors. Workaround: run full test suite instead. Does not affect test functionality, only CLI filtering.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Auth error E2E test complete and passing
- Clean environment pattern established for future E2E tests
- Requirement E2E-03 complete
- Ready for remaining phase 03 tests (plan 03-01 still pending)

---
*Phase: 03-core-e2e-tests*
*Completed: 2026-03-02*

## Self-Check: PASSED

- ✓ File exists: src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/AuthErrorTest.kt
- ✓ Commit exists: c4a9177
- ✓ Test passes when run in full test suite
