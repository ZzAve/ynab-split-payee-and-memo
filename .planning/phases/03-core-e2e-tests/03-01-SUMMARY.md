---
phase: 03-core-e2e-tests
plan: 01
subsystem: e2e-tests
tags: [testing, e2e, wiremock, happy-path, batch-processing]

dependency-graph:
  requires: [02-01, 02-02]  # WireMock infrastructure, CLI runner helper
  provides: [test-fixtures, happy-path-validation]
  affects: [core-business-logic]

tech-stack:
  added: []
  patterns: [test-fixtures, realistic-test-data, batch-verification]

key-files:
  created:
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/TestFixtures.kt
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/TestFixturesTest.kt
    - src/test/kotlin/com/github/zzave/ynabsplitpayeeandmemo/e2e/HappyPathTest.kt
  modified:
    - src/main/kotlin/com/github/zzave/ynabsplitpayeeandmemo/YnabModels.kt

decisions:
  - Remove --dry-run flag from E2E tests to validate actual PATCH requests
  - Use wireMockServer.stubFor() instead of static stubFor() for dynamic port support
  - Create 26 transactions in happy path test to validate batch size handling (25 per batch)
  - Use realistic European transaction names for authenticity

metrics:
  duration_seconds: 687
  duration_minutes: 11
  completed_date: "2026-03-02T16:40:34Z"
  tasks_completed: 2
  files_created: 3
  files_modified: 1
  commits: 3
  tests_added: 12
  auto_fixes_applied: 1
---

# Phase 03 Plan 01: Core E2E Tests - Happy Path Summary

**One-liner:** Happy path E2E test validates CLI correctly splits 26 transactions with " - " separator into separate payee/memo fields and sends 2 batch PATCH calls (25+1).

## What Was Built

### Test Fixtures (Task 1)
Created reusable test helper functions for E2E test development:

**Transaction builders:**
- `createTransaction()`: Base builder with sensible defaults (accountId, date, amount, cleared, approved)
- `createSplittableTransaction()`: Shorthand for transactions with " - " separator (eligible for splitting)
- `createSkippableTransaction()`: Shorthand for non-splittable transactions (no separator, transfers, already-changed)

**API response builders:**
- `buildBudgetTransactionsResponse()`: Takes List<Transaction>, wraps in YNAB API format with server_knowledge
- Uses kotlinx.serialization.json.Json to serialize transactions into proper JSON

**Request parsing:**
- `parseRequestBody()`: Takes WireMock LoggedRequest body string, deserializes into PatchTransactionsWrapper for assertions

**Constants:**
- `DEFAULT_BUDGET_ID = "test-budget-id"`
- `DEFAULT_ACCOUNT_ID = "test-account-id"`

**Test coverage:** 11 tests validating transaction builders, JSON formatting, and parsing logic.

### Happy Path E2E Test (Task 2)
Created comprehensive E2E test demonstrating full happy path with 26 transactions:

**Test scenario:**
- 26 splittable transactions: "ALBERT HEIJN - GROCERY", "SHELL - FUEL", "COOLBLUE - ELECTRONICS", etc.
- 5 skippable transactions:
  * 2 without separator: "DIRECT DEBIT", "CASH WITHDRAWAL"
  * 2 transfers: "Transfer : Savings Account"
  * 1 already-changed: "STORE - STUFF" with custom payee

**Verification:**
- CLI exit code is 0 (success)
- Exactly 2 PATCH requests made (26 transactions / batch size 25 = 2 batches)
- First batch has 25 transactions, second batch has 1 transaction
- Each transaction has correct split: `payee_name` is left side of " - ", `memo` is right side
- `payee_id` is null (forces YNAB to create/match payee by name)
- All required fields present: account_id, date, amount, cleared, approved
- GET request made with correct query params: since_date, type=unapproved

**Realistic test data:** Uses European transaction names (ALBERT HEIJN, JUMBO, COOLBLUE, HEMA, etc.) for authenticity.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Add default values to SaveTransactionWithId optional fields**
- **Found during:** Task 2 (HappyPathTest development)
- **Issue:** SaveTransactionWithId data class fields (payee_id, category_id, flag_color, etc.) were declared as nullable (`String?`) but lacked default values, making them required by kotlinx.serialization. When CLI sent PATCH requests omitting these null fields, test parsing failed with MissingFieldException.
- **Fix:** Added `= null` defaults to all optional fields: payee_id, payee_name, category_id, memo, cleared, approved, flag_color
- **Files modified:** YnabModels.kt
- **Commit:** 911a2e8

**Process deviation:** Removed `--dry-run` flag from E2E test invocation to validate actual PATCH requests. Dry-run mode doesn't send updates, preventing verification of batch API calls.

**Import correction:** Removed static `stubFor` import, used `wireMockServer.stubFor()` instead for dynamic port support.

## Verification Results

✓ All existing tests pass (31 tests from Phase 2)
✓ New tests pass (HappyPathTest + TestFixturesTest: 12 new tests)
✓ Total test count: 43 tests
✓ Test execution time: ~11 seconds (well under 30 second target)
✓ CLI correctly splits 26 transactions into 2 batches
✓ Full payload verification confirms correct payee/memo splitting

## Success Criteria Met

✓ TestFixtures.kt provides reusable helpers for transaction creation and API response building
✓ HappyPathTest validates CLI correctly splits 26+ transactions into separate payee and memo fields
✓ HappyPathTest verifies exactly 2 batch PATCH calls made (batch size 25)
✓ HappyPathTest verifies full payload content: payee_name split, memo split, payee_id=null
✓ All tests pass, CLI exits with code 0 for happy path
✓ Requirements E2E-01 and E2E-02 complete

## Technical Insights

1. **WireMock dynamic ports require instance methods:** Static `stubFor()` doesn't work with dynamic ports. Must use `wireMockServer.stubFor()` to register stubs on the specific server instance.

2. **Dry-run mode prevents E2E verification:** The `--dry-run` flag prevents CLI from making PATCH requests, making it impossible to verify batch update behavior. E2E tests must run without dry-run.

3. **kotlinx.serialization default values:** Fields declared as nullable (`Type?`) without default values are REQUIRED by serialization. Adding `= null` makes them optional, allowing omission during serialization/deserialization.

4. **Batch size validation:** Testing with 26 transactions (one more than batch size 25) effectively validates batch splitting logic with minimal test data.

5. **Realistic test data improves debugging:** Using real store names (ALBERT HEIJN, SHELL, etc.) makes test output more readable and debugging easier than generic names.

## Related Requirements

- **E2E-01:** Happy path E2E test (batch size 25) — COMPLETE
- **E2E-02:** Happy path E2E test (26+ transactions) — COMPLETE

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 89a14c5 | feat | Add test fixtures for E2E tests |
| 911a2e8 | fix | Add default values to SaveTransactionWithId optional fields |
| b33f6c7 | feat | Add happy path E2E test with 26 transactions |

## Self-Check: PASSED

**Created files:**
- TestFixtures.kt: FOUND
- TestFixturesTest.kt: FOUND
- HappyPathTest.kt: FOUND

**Commits:**
- 89a14c5: FOUND (feat: add test fixtures for E2E tests)
- 911a2e8: FOUND (fix: add default values to SaveTransactionWithId optional fields)
- b33f6c7: FOUND (feat: add happy path E2E test with 26 transactions)

All files created and commits exist as documented.
