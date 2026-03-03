---
phase: 04-edge-case-coverage
plan: 01
subsystem: e2e-testing
tags: [edge-cases, boundary-conditions, skip-logic, empty-budget]
dependency_graph:
  requires: [e2e-test-infrastructure, business-logic-skip-conditions]
  provides: [empty-budget-test, skip-conditions-test]
  affects: [e2e-test-suite]
tech_stack:
  added: []
  patterns: [kotest-funspec, wiremock-stubbing, cli-subprocess-testing]
key_files:
  created:
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/EmptyBudgetTest.kt
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/SkipConditionsTest.kt
  modified: []
decisions:
  - name: "Test empty budget as success case, not error"
    rationale: "Empty budget is a valid state - CLI should exit 0 and log appropriately"
    alternatives: ["Exit with error code", "Skip budget silently"]
    chosen: "Exit 0 with informative log"
  - name: "Test skip conditions in isolation and combination"
    rationale: "Ensures each skip condition works independently and together without interference"
    alternatives: ["Only test combined scenario", "Only test isolated scenarios"]
    chosen: "Test both isolated (3 tests) and combined (1 test) for full coverage"
metrics:
  duration_seconds: 244
  duration_minutes: 4
  tasks_completed: 2
  files_created: 2
  commits: 2
  test_cases_added: 7
  completed_date: "2026-03-02"
---

# Phase 04 Plan 01: Edge Case Coverage Summary

**One-liner:** E2E tests validating CLI gracefully handles empty budgets and correctly skips ineligible transactions (already-split, no separator, transfers)

## What Was Built

Created two comprehensive E2E test suites covering four edge case requirements:

1. **EmptyBudgetTest.kt** — Validates CLI handles budgets with zero transactions without errors or crashes
   - Test 1: CLI exits with code 0 when budget has zero transactions
   - Test 2: CLI makes zero PATCH requests when budget is empty
   - Test 3: CLI logs appropriate message about no transactions found

2. **SkipConditionsTest.kt** — Validates CLI correctly identifies and skips ineligible transactions
   - Test 1: Skips transactions where payeeName != importPayeeName (user manually changed payee)
   - Test 2: Skips transactions without " - " separator in import_payee_name
   - Test 3: Skips transfers where payee starts with "Transfer : "
   - Test 4: Processes mixed scenario with 5 eligible + 6 skippable transactions correctly

Both test suites extend WireMockTestBase and follow established E2E testing patterns (mock server, ProcessBuilder CLI invocation, request verification).

## Requirements Completed

- **E2E-04:** CLI handles empty budget without errors or crashes ✓
- **E2E-05:** CLI skips already-split transactions where payee differs from import_payee ✓
- **E2E-06:** CLI skips transactions lacking ' - ' separator without errors ✓
- **E2E-07:** CLI skips transfers where payee starts with 'Transfer : ' ✓

All four requirements validated through automated E2E tests.

## Deviations from Plan

None - plan executed exactly as written.

**Pre-existing issue (out of scope):**
- YnabSplitPayeeAndMemoTest has a failing test "CLI rejects --api-url in production builds"
- This test depends on compile-time BuildInfo.isTestBuild flag and was already failing before this plan started
- According to SCOPE BOUNDARY rules, pre-existing failures in unrelated files are not fixed during task execution
- Test failure is unrelated to edge case coverage work (E2E-04 through E2E-07)
- Documented here for awareness; resolution deferred to future work

## Technical Decisions

### Empty Budget Handling
**Decision:** Treat empty budget as success case (exit 0), not error condition.

**Rationale:** An empty budget is a valid state. The CLI should not fail when there's simply no work to do. This aligns with Unix philosophy of "do one thing well" and "silence is golden" for tools that complete successfully.

**Implementation:** Existing CLI code already handles this correctly:
- `transactions.chunked(batchSize)` creates empty batches list when transactions is empty
- `batches.forEachIndexed` loop doesn't execute with empty list
- `processTransactionBatch` returns early if transactionsToUpdate is empty
- No PATCH requests sent, exit code 0

### Skip Conditions Testing Strategy
**Decision:** Test skip conditions in both isolation (3 tests) and combination (1 test).

**Rationale:**
- **Isolation tests** verify each skip condition works independently without interference from other conditions
- **Combined test** validates realistic scenario where eligible and skippable transactions coexist
- This ensures the CLI's filtering logic (`findTransactionsToUpdate()`) correctly applies all skip rules without conflicts

**Trade-offs:**
- More test cases (4 instead of 1) increases test suite execution time
- However, isolating failures is much easier when tests are separated by condition
- Combined test catches integration issues where skip conditions might interact unexpectedly

## Test Results

**New tests added:** 7 test cases
- EmptyBudgetTest: 3 test cases
- SkipConditionsTest: 4 test cases

**All new tests pass:**
```
✓ CLI exits with code 0 when budget has zero transactions
✓ CLI makes zero PATCH requests when budget is empty
✓ CLI logs appropriate message about no transactions found
✓ CLI skips transactions where payeeName != importPayeeName
✓ CLI skips transactions without ' - ' separator
✓ CLI skips transfers where payee starts with 'Transfer : '
✓ CLI combines all skip conditions in single run
```

**Test suite growth:**
- Previous: 47 E2E tests (Phase 3 complete)
- After this plan: 54 E2E tests
- New: 7 edge case tests

## Key Insights

### Business Logic Correctness
The existing business logic in `TransactionUpdater.kt` already implements all skip conditions correctly:
- Line 110: `if (payeeName != importPayeeName) return null` — skips manually changed transactions
- Line 113: `if (payeeName?.startsWith("Transfer : ") == true) return null` — skips transfers
- Line 116-117: `val split = importPayeeName.split(" - ", limit = 2); if (split.size < 2) return null` — skips transactions without separator

No implementation changes needed - tests validate existing behavior.

### Empty Budget Handling
CLI degrades gracefully with empty budgets:
- Batch processing loop naturally handles zero-length transaction lists
- No special-case code needed
- Logs indicate zero batches processed
- Exit code 0 (success)

This demonstrates good design - the batching logic is generic enough that edge cases (empty input) work without special handling.

### WireMock Request Verification
The mixed scenario test (Task 2, Test 4) demonstrates the power of WireMock's request journal for E2E verification:
- Stub GET endpoint returning 11 transactions (5 eligible + 6 skippable)
- Verify exactly 1 PATCH request sent
- Parse PATCH body to confirm only 5 eligible transactions included
- Verify correct payee/memo splitting for each transaction

This validates end-to-end behavior (CLI parsing → business logic → API call) without hitting real YNAB API.

## Commits

| Hash    | Message                                                                                                       |
| ------- | ------------------------------------------------------------------------------------------------------------- |
| 10b3489 | test(04-01): add E2E test for empty budget handling                                                          |
| 56cbc3c | test(04-01): add E2E test for skip conditions (validates E2E-05, E2E-06, E2E-07)                             |

## Files Created

1. **src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/EmptyBudgetTest.kt** (117 lines)
   - Extends WireMockTestBase
   - 3 test cases validating empty budget handling
   - Uses buildBudgetTransactionsResponse(emptyList()) for API mock
   - Verifies exit code 0, zero PATCH requests, appropriate logging

2. **src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/SkipConditionsTest.kt** (266 lines)
   - Extends WireMockTestBase
   - 4 test cases validating skip conditions
   - Uses createSkippableTransaction() and createSplittableTransaction() helpers
   - Verifies CLI filtering logic in isolation and combination
   - Validates PATCH request contents using parseRequestBody()

## Next Steps

**Phase 4 Plan 2:** Continue with remaining Phase 4 edge case requirements (E2E-08 through E2E-13 if applicable based on ROADMAP.md).

**Deferred issue:** YnabSplitPayeeAndMemoTest failing test should be addressed in a separate fix:
- Test "CLI rejects --api-url in production builds" depends on BuildInfo.isTestBuild compile-time flag
- When tests run with testBuild=true (for E2E tests), this unit test fails
- Need to either: (1) make test conditional on BuildInfo.isTestBuild value, or (2) split test execution into production and test build phases
- Not blocking for edge case coverage work

---

*Plan 04-01 completed in ~4 minutes on 2026-03-02*

## Self-Check: PASSED

All claimed files and commits verified:
- ✓ EmptyBudgetTest.kt exists
- ✓ SkipConditionsTest.kt exists
- ✓ Commit 10b3489 exists
- ✓ Commit 56cbc3c exists
