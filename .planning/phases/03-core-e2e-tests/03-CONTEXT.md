# Phase 3: Core E2E Tests - Context

**Gathered:** 2026-03-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Validate that the CLI correctly splits transactions and handles authentication errors in realistic end-to-end scenarios. Tests use WireMock mock server and ProcessBuilder CLI invocation from Phase 2. Scope is happy path (splitting + batching) and auth failure (401). Edge cases (empty budgets, transfers, dry-run) belong to Phase 4.

</domain>

<decisions>
## Implementation Decisions

### Test data design
- Use 26+ transactions to force multiple batch API calls (batch size is 25)
- Mix of splittable and skippable transactions: ~20 splittable (with " - " separator) plus some skippable (no separator, transfers, already-changed payee)
- Use realistic-looking transaction data: real payee names like "ALBERT HEIJN - GROCERY", "SHELL - FUEL", realistic amounts and dates
- Include transactions with existing memo content to exercise memo-append logic

### Verification depth
- Full payload verification: parse JSON request bodies sent to WireMock, check each transaction has correct split payee_name and memo, correct field mapping (payee_id=null, etc.)
- Hardcoded expected JSON strings for assertions: explicit expected values, not dynamically built from input
- Verify GET /transactions request parameters (since_date, type=unapproved) — full request chain validation
- Verify CLI stdout output: assert summary info about number of transactions processed
- Assert exact number of batch PATCH API calls (e.g., 2 calls for 26+ splittable transactions with batch size 25)

### Auth error expectations
- Test 401 at transaction fetch stage only (GET /transactions) — this is the main entry point
- Verify zero PATCH calls to batch endpoint after auth failure — proves CLI stops before making changes
- Exit code and error message assertions: Claude's discretion on specific exit code and error text based on current CLI behavior

### Claude's Discretion
- Specific exit code for auth failure (check current behavior and assert accordingly)
- Whether to assert specific error keywords (e.g., "401", "unauthorized") or just non-zero exit — based on what the CLI currently outputs
- Test class organization and naming
- Helper functions for common WireMock stubs
- Exact transaction field values for non-key fields (amounts, dates, account IDs)

</decisions>

<specifics>
## Specific Ideas

- Transaction data should look like real Dutch/European bank imports (ALBERT HEIJN, SHELL, COOLBLUE, etc.)
- Batch verification should be strict: assert exact call count AND full payload content
- The happy path test should be a complete realistic scenario, not a minimal smoke test

</specifics>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WireMockTestBase`: Base class with automatic WireMock lifecycle (beforeSpec/afterSpec), provides `wireMockServer` and `apiBaseUrl`
- `CliRunner.runCli()`: Invokes test JAR via ProcessBuilder, returns `CliResult(exitCode, stdout, stderr)`
- `CliResult`: Data class capturing process output
- `SmokeTest`: Working example of WireMock stubbing + CLI invocation pattern

### Established Patterns
- Tests extend `WireMockTestBase` with lambda body: `class MyTest : WireMockTestBase({ ... })`
- WireMock stubs use `stubFor(get(urlMatching(...)).willReturn(aResponse()...))` pattern
- Test JAR built via `./gradlew shadowJar -PtestBuild=true` in beforeSpec hook
- CLI invoked with `runCli("--api-url", apiBaseUrl, "--token", "fake-token", "--budget-id", "...", "--dry-run")`
- WireMock request journal enabled for verification via `wireMockServer.verify(getRequestedFor(...))`

### Integration Points
- YNAB API endpoints to stub: GET `/v1/budgets/{id}/transactions`, PATCH `/v1/budgets/{id}/transactions`
- Transaction JSON format defined in `YnabModels.kt`: `Transaction`, `PatchTransactionsWrapper`, `SaveTransactionWithId`
- Batch size of 25 hardcoded in `YnabSplitPayeeAndMemo.kt` main orchestrator
- Split logic in `TransactionUpdater.kt`: splits on first " - ", skips transfers ("Transfer : " prefix), skips when payeeName != importPayeeName

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 03-core-e2e-tests*
*Context gathered: 2026-03-02*
