# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.0 — E2E Test Suite

**Shipped:** 2026-03-03
**Phases:** 4 | **Plans:** 7 | **Sessions:** ~7

### What Was Built
- Configurable API URL with build-time test/production flag (`--api-url`, `-PtestBuild`)
- WireMock 3.10.0 mock server infrastructure with automatic lifecycle management
- ProcessBuilder CLI invocation helper for real subprocess E2E testing
- Happy path E2E test validating 26-transaction batch splitting
- Auth error E2E test validating 401 handling and early exit
- Edge case tests: empty budgets, skip conditions (3 types), and --dry-run flag

### What Worked
- Linear dependency chain (Phase 1 → 2 → 3 → 4) made execution straightforward
- TDD approach (RED → GREEN commits) caught issues early in each task
- WireMock request journal for verifying PATCH request contents post-hoc
- TestFixtures helper pattern enabled Phase 4 tests to reuse Phase 3 infrastructure
- Auto-fixes were caught and documented cleanly within the deviation protocol

### What Was Inefficient
- Plan 04-01 scope creep: created DryRunTest.kt that was assigned to plan 04-02, wasting a plan cycle
- SUMMARY frontmatter `requirements-completed` field inconsistently populated (7/7 SUMMARYs had different field names or omitted it)
- ROADMAP.md and REQUIREMENTS.md checkboxes fell out of sync with actual completion status
- BuildInfo compile-time flag issue (unit test flakiness) was identified early but deferred repeatedly

### Patterns Established
- WireMockTestBase as abstract base class for all E2E tests (lifecycle management)
- CliRunner.runCli() for CLI subprocess invocation with environment cleanup
- TestFixtures with createSplittableTransaction/createSkippableTransaction/buildBudgetTransactionsResponse helpers
- parseRequestBody() for WireMock PATCH request verification
- Instance method `wireMockServer.stubFor()` over static imports for dynamic port support

### Key Lessons
1. Build-time feature flags (like `-PtestBuild`) create compile-time coupling that breaks unit tests when both modes run in same build — consider runtime detection or split test execution
2. WireMock `disableRequestJournal()` must not be used when tests need `verify()` or `findAll()` — request journal is essential for E2E verification
3. Host environment variables (YNAB_*) leak into ProcessBuilder subprocesses — always clear domain-specific env vars in test harness
4. 26 transactions is the optimal test data size for batch boundary validation (25+1) — minimal data, maximum coverage

### Cost Observations
- Model mix: Budget profile (mostly haiku for agents, sonnet/opus for orchestration)
- Sessions: ~7 (one per phase-plan + planning sessions)
- Notable: Quick depth (4 phases) completed in 2 days with minimal rework

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0 | ~7 | 4 | Initial milestone — established E2E testing patterns |

### Cumulative Quality

| Milestone | Tests | Coverage | Zero-Dep Additions |
|-----------|-------|----------|-------------------|
| v1.0 | 52+ | 13/13 requirements | WireMock (1 new dependency) |

### Top Lessons (Verified Across Milestones)

1. (First milestone — lessons to be validated in future milestones)
