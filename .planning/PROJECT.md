# YNAB Split Payee & Memo — E2E Test Suite

## What This Is

An end-to-end test suite for the YNAB split-payee-and-memo CLI tool. Tests invoke the actual CLI JAR as a subprocess and validate behavior against a standalone WireMock HTTP mock server simulating the YNAB API. The mock server approach is language-agnostic, surviving a potential rewrite to Go or Rust.

## Core Value

Confidence that the CLI behaves correctly when talking to the real YNAB API — validated through realistic end-to-end scenarios without hitting the actual API.

## Requirements

### Validated

- ✓ Configurable API base URL (CLI flag `--api-url` or env var `YNAB_API_URL`) — v1.0
- ✓ Production builds reject `--api-url` via build-time `isTestBuild` flag — v1.0
- ✓ WireMock 3.10.0 mock server with automatic lifecycle management — v1.0
- ✓ ProcessBuilder CLI invocation with exit code, stdout, stderr capture — v1.0
- ✓ Happy path: CLI correctly splits transactions and sends batch updates (25 per batch) — v1.0
- ✓ Auth failure: CLI handles invalid tokens (401) with non-zero exit and clear error — v1.0
- ✓ Edge case: Empty budget handled gracefully (exit 0, no PATCH calls) — v1.0
- ✓ Edge case: Already-split transactions skipped (payee != import_payee) — v1.0
- ✓ Edge case: Transactions without " - " separator skipped — v1.0
- ✓ Edge case: Transfers skipped (payee starts with "Transfer : ") — v1.0
- ✓ Dry-run mode prevents API updates while still analyzing — v1.0

### Active

(None — v1.0 complete, define next milestone requirements with `/gsd:new-milestone`)

### Out of Scope

- Rewriting existing unit tests — current Kotest tests for TransactionUpdater work fine
- Performance/load testing — not needed for a personal CLI tool
- CI integration — can be added later, focus on the tests themselves first
- Testing the documentation site — unrelated concern
- Offline mode — real-time API is core use case

## Context

Shipped v1.0 with 2,723 LOC Kotlin across 48 files changed.
Tech stack: Kotlin, Ktor CIO, Clikt, Kotest, WireMock 3.10.0, Gradle (Shadow plugin).
Test suite: 52+ tests (14 classes) covering unit tests and E2E scenarios.
Known tech debt: pre-existing unit test flakiness with BuildInfo compile-time flag, Gradle test parallelization race condition.

## Constraints

- **Tech stack**: Must use existing Gradle/Kotest infrastructure for test runner
- **Mock server**: Must be standalone HTTP (not in-process mocking) for language portability
- **CLI invocation**: Tests must invoke the actual built JAR, not call Kotlin functions directly
- **Scope**: Foundation of ~10 tests established, expand as needed

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| WireMock for mock server | Standalone HTTP, language-agnostic, well-established | ✓ Good — works well with dynamic ports |
| ProcessBuilder for CLI invocation | Tests actual JAR behavior including arg parsing | ✓ Good — catches real integration issues |
| Kotest as test runner | Already in project, orchestrates WireMock + subprocess | ✓ Good — FunSpec pattern works well |
| Build-time `-PtestBuild` flag | Compile-time check prevents production misuse of `--api-url` | ⚠️ Revisit — causes unit test flakiness |
| Dynamic port allocation | Enables parallel test execution, avoids port conflicts | ✓ Good — no port conflicts |
| Enable WireMock request journal | Required for E2E verification of API calls | ✓ Good — essential for PATCH verification |
| Clear YNAB_* env vars in CliRunner | Prevents host environment interference with tests | ✓ Good — improved test isolation |
| 26 transactions for batch test | Validates batch splitting (25+1) with minimal data | ✓ Good — efficient boundary testing |

---
*Last updated: 2026-03-03 after v1.0 milestone*
