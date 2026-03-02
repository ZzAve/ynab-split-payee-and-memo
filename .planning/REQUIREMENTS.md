# Requirements: YNAB Split Payee & Memo — E2E Tests

**Defined:** 2026-03-02
**Core Value:** Confidence that the CLI behaves correctly when talking to the real YNAB API — validated through realistic end-to-end scenarios without hitting the actual API.

## v1 Requirements

### CLI Infrastructure

- [x] **CLI-01**: Separate Gradle task builds a test JAR with configurable API base URL
- [x] **CLI-02**: Production shadow JAR keeps hardcoded YNAB API URL (no override possible)

### Test Infrastructure

- [x] **TEST-01**: WireMock mock server starts/stops per test, simulating YNAB API v1
- [x] **TEST-02**: Tests invoke the test JAR via ProcessBuilder (full CLI invocation)
- [x] **TEST-03**: Test helper captures CLI exit code, stdout, and stderr

### Happy Path

- [x] **E2E-01**: CLI splits transactions with " - " separator into payee and memo
- [x] **E2E-02**: CLI sends batch updates to the API correctly

### Auth & Errors

- [x] **E2E-03**: CLI exits with non-zero code and clear error on invalid token (401)

### Edge Cases

- [ ] **E2E-04**: CLI handles empty budget (no transactions) gracefully
- [ ] **E2E-05**: CLI skips already-split transactions (payee != import_payee)
- [ ] **E2E-06**: CLI skips transactions without " - " separator
- [ ] **E2E-07**: CLI skips transfers (payee starts with "Transfer : ")

### Dry Run

- [ ] **E2E-08**: --dry-run flag prevents actual API update calls

## v2 Requirements

### Extended Error Handling

- **ERR-01**: CLI handles rate limits (429) with retry
- **ERR-02**: CLI handles server errors (500) gracefully
- **ERR-03**: CLI handles malformed JSON responses

### Extended Scenarios

- **EXT-01**: CLI handles multiple budgets correctly
- **EXT-02**: CLI handles transactions across batch boundaries (>25)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Rewriting existing unit tests | Current Kotest unit tests for TransactionUpdater work fine |
| Performance/load testing | Personal CLI tool, not needed |
| CI integration | Can layer on later, focus on tests first |
| Language-agnostic test runner | Kotest is fine for now; mock server portability is what matters |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CLI-01 | Phase 1 | Complete |
| CLI-02 | Phase 1 | Complete |
| TEST-01 | Phase 2 | Complete |
| TEST-02 | Phase 2 | Complete |
| TEST-03 | Phase 2 | Complete |
| E2E-01 | Phase 3 | Complete |
| E2E-02 | Phase 3 | Complete |
| E2E-03 | Phase 3 | Complete |
| E2E-04 | Phase 4 | Pending |
| E2E-05 | Phase 4 | Pending |
| E2E-06 | Phase 4 | Pending |
| E2E-07 | Phase 4 | Pending |
| E2E-08 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 13 total
- Mapped to phases: 13
- Unmapped: 0

---

*Requirements defined: 2026-03-02*
*Roadmap traceability updated: 2026-03-02*
