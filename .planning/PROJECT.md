# YNAB Split Payee & Memo — E2E Test Suite

## What This Is

An end-to-end test suite for the YNAB split-payee-and-memo CLI tool. Tests invoke the actual CLI JAR as a subprocess and validate behavior against a standalone HTTP mock server (WireMock) that simulates the YNAB API. The mock server approach is language-agnostic, so the test infrastructure survives a potential rewrite to Go or Rust.

## Core Value

Confidence that the CLI behaves correctly when talking to the real YNAB API — validated through realistic end-to-end scenarios without hitting the actual API.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Configurable API base URL (CLI flag or env var) so tests can point the CLI at a mock server
- [ ] Standalone HTTP mock server (WireMock) that simulates YNAB API v1 endpoints
- [ ] Happy path: CLI correctly splits transactions and sends batch updates
- [ ] Auth failure: CLI handles invalid/expired tokens gracefully (401 responses)
- [ ] Edge case: Empty budget (no transactions to process)
- [ ] Edge case: All transactions already split (nothing to update)
- [ ] Edge case: Transactions with no " - " separator (should be skipped)
- [ ] Edge case: Transfers are skipped (payee starts with "Transfer : ")
- [ ] Tests invoke the built JAR via ProcessBuilder (full CLI invocation)
- [ ] Test infrastructure is portable (mock server is not JVM-specific concept)

### Out of Scope

- Rewriting existing unit tests — the current Kotest tests for TransactionUpdater are fine
- Performance/load testing — not needed for a personal CLI tool
- CI integration — can be added later, focus on the tests themselves first
- Testing the documentation site — unrelated concern

## Context

- Existing Kotlin CLI app using Ktor CIO client, Clikt for CLI parsing, Kotest for tests
- The YNAB API base URL is currently hardcoded in YnabClient.kt
- The app processes transactions in batches of 25
- Existing unit tests cover TransactionUpdater business logic well; the gap is integration/e2e coverage
- User is considering rewriting to Go or Rust — mock server portability matters
- Shadow plugin builds a fat JAR that can be invoked directly

## Constraints

- **Tech stack**: Must use existing Gradle/Kotest infrastructure for test runner
- **Mock server**: Must be a standalone HTTP server approach (not in-process mocking) for language portability
- **CLI invocation**: Tests must invoke the actual built JAR, not call Kotlin functions directly
- **Scope**: Foundation of ~5-10 tests, not comprehensive coverage

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| WireMock for mock server | Standalone HTTP server, well-established, language-agnostic concept | — Pending |
| ProcessBuilder for CLI invocation | Invokes the real JAR as a subprocess, tests actual CLI behavior | — Pending |
| Kotest as test runner | Already in the project, familiar, can orchestrate subprocess + WireMock | — Pending |
| Add --api-url / env var to CLI | Required to point CLI at mock server during tests | — Pending |

---
*Last updated: 2026-03-02 after initialization*
