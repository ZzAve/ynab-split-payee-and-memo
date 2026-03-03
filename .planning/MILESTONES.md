# Milestones

## v1.0 E2E Test Suite (Shipped: 2026-03-03)

**Phases completed:** 4 phases, 7 plans, 8 tasks

**Key accomplishments:**
- Configurable API URL with build-time test/production mode flag (`--api-url`, `-PtestBuild`)
- WireMock 3.10.0 mock server with automatic lifecycle management via Kotest base class
- ProcessBuilder CLI invocation helper for real subprocess E2E testing
- Happy path E2E test validates 26-transaction batch splitting (25+1 batches)
- Auth error E2E test validates 401 handling and early exit
- Edge case coverage: empty budgets, skip conditions, and --dry-run flag

---

