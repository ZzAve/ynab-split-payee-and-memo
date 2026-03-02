# ROADMAP: YNAB Split Payee & Memo — E2E Test Suite

**Project:** End-to-end test suite for YNAB split-payee-and-memo CLI
**Defined:** 2026-03-02
**Depth:** Quick (4 phases, critical path)
**Coverage:** 13/13 v1 requirements mapped

---

## Phases

- [ ] **Phase 1: CLI Configuration** - Add --api-url flag and env var support so tests can point CLI at mock server
- [ ] **Phase 2: Test Infrastructure Setup** - Establish WireMock mock server, ProcessBuilder CLI invocation, and test harness
- [ ] **Phase 3: Core E2E Tests** - Validate happy path (splitting, batching) and auth failure handling
- [ ] **Phase 4: Edge Case Coverage** - Test empty budgets, already-split transactions, separators, transfers, and dry-run mode

---

## Phase Details

### Phase 1: CLI Configuration
**Goal:** Make the YNAB API base URL configurable via CLI flag or environment variable so tests can inject a mock server URL

**Depends on:** Nothing (foundation)

**Requirements:** CLI-01, CLI-02

**Success Criteria** (what must be TRUE):
  1. CLI accepts `--api-url` flag to override the default YNAB API base URL
  2. CLI respects `YNAB_API_URL` environment variable when `--api-url` is not provided
  3. Production shadow JAR still uses hardcoded YNAB API URL (no override possible in production builds)
  4. CLI can successfully connect to a mock server at the configured URL

**Plans:** TBD

---

### Phase 2: Test Infrastructure Setup
**Goal:** Establish the foundation for E2E tests: WireMock mock server, ProcessBuilder CLI invocation harness, and test helpers

**Depends on:** Phase 1 (requires configurable API URL)

**Requirements:** TEST-01, TEST-02, TEST-03

**Success Criteria** (what must be TRUE):
  1. WireMock mock server starts and stops cleanly per test without manual cleanup
  2. Tests invoke the built test JAR via ProcessBuilder as a real subprocess
  3. Test helper captures and exposes CLI exit code, stdout, and stderr for assertions
  4. Mock server simulates YNAB API v1 endpoints (budgets, transactions, batch updates)

**Plans:** TBD

---

### Phase 3: Core E2E Tests
**Goal:** Validate that the CLI correctly splits transactions and handles authentication errors in realistic end-to-end scenarios

**Depends on:** Phase 2 (requires test infrastructure)

**Requirements:** E2E-01, E2E-02, E2E-03

**Success Criteria** (what must be TRUE):
  1. CLI splits transactions with " - " separator into correct payee and memo fields
  2. CLI sends batch updates to the mock API in correct format and batch size (25)
  3. CLI exits with non-zero code and logs clear error when token is invalid (401 response)
  4. Happy path test passes with realistic transaction data and API responses

**Plans:** TBD

---

### Phase 4: Edge Case Coverage
**Goal:** Validate that the CLI correctly handles boundary conditions and special scenarios

**Depends on:** Phase 3 (requires core test infrastructure and assertions)

**Requirements:** E2E-04, E2E-05, E2E-06, E2E-07, E2E-08

**Success Criteria** (what must be TRUE):
  1. CLI handles empty budget (no transactions) gracefully without errors
  2. CLI skips already-split transactions where payee differs from import_payee
  3. CLI skips transactions that lack the " - " separator without errors
  4. CLI skips transfers (transactions where payee starts with "Transfer : ")
  5. `--dry-run` flag prevents actual API update calls while still validating logic

**Plans:** TBD

---

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. CLI Configuration | 0/1 | Not started | — |
| 2. Test Infrastructure Setup | 0/2 | Not started | — |
| 3. Core E2E Tests | 0/2 | Not started | — |
| 4. Edge Case Coverage | 0/2 | Not started | — |

---

## Coverage Summary

**v1 Requirements:** 13
**Mapped to phases:** 13
**Orphaned:** 0

**Coverage:** 100% ✓

| Requirement | Phase | Category |
|-------------|-------|----------|
| CLI-01 | Phase 1 | CLI Infrastructure |
| CLI-02 | Phase 1 | CLI Infrastructure |
| TEST-01 | Phase 2 | Test Infrastructure |
| TEST-02 | Phase 2 | Test Infrastructure |
| TEST-03 | Phase 2 | Test Infrastructure |
| E2E-01 | Phase 3 | Happy Path |
| E2E-02 | Phase 3 | Happy Path |
| E2E-03 | Phase 3 | Auth & Errors |
| E2E-04 | Phase 4 | Edge Cases |
| E2E-05 | Phase 4 | Edge Cases |
| E2E-06 | Phase 4 | Edge Cases |
| E2E-07 | Phase 4 | Edge Cases |
| E2E-08 | Phase 4 | Edge Cases |

---

*Roadmap created: 2026-03-02*
