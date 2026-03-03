---
phase: 01-cli-configuration
verified: 2026-03-02T14:35:00Z
status: passed
score: 7/7 must-haves verified
---

# Phase 01: CLI Configuration Verification Report

**Phase Goal:** Make the YNAB API base URL configurable via CLI flag or environment variable so tests can inject a mock server URL

**Verified:** 2026-03-02T14:35:00Z

**Status:** PASSED ✓

**Score:** 7/7 must-haves verified

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | CLI accepts `--api-url` flag to override default YNAB API base URL | ✓ VERIFIED | `YnabSplitPayeeAndMemo.kt` line 68-72: `private val apiUrl by option("--api-url", help = "YNAB API base URL (for testing)", envvar = "YNAB_API_URL")` |
| 2 | CLI respects `YNAB_API_URL` environment variable when `--api-url` is not provided | ✓ VERIFIED | Same option defines `envvar = "YNAB_API_URL"` for environment variable support |
| 3 | Production shadow JAR uses hardcoded YNAB API URL (no override possible) | ✓ VERIFIED | `YnabSplitPayeeAndMemo.kt` lines 97-101: Rejects `--api-url` when `BuildInfo.isTestBuild = false` |
| 4 | CLI can successfully connect to a mock server at the configured URL | ✓ VERIFIED | `YnabClient.kt` line 30: Constructor accepts `baseUrl` parameter; lines 60, 103, 105, 150 use `$baseUrl` in all API calls |

**Score:** 4/4 truths verified

---

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `YnabSplitPayeeAndMemo.kt` | CLI option for `--api-url` | ✓ VERIFIED | Lines 68-72: Option defined with envvar support |
| `YnabClient.kt` | Constructor parameter for `baseUrl` | ✓ VERIFIED | Lines 29-31: Parameter with default `"https://api.ynab.com/v1"` |
| `build.gradle.kts` | generateBuildInfo task and isTestBuild flag | ✓ VERIFIED | Lines 25-47: Task generates BuildInfo.kt with `isTestBuild` based on `-PtestBuild` property |
| `YnabClientTest.kt` | Tests for baseUrl parameter behavior | ✓ VERIFIED | Created with 2 tests: accepts baseUrl, defaults to production URL |
| `YnabSplitPayeeAndMemoTest.kt` | Tests for CLI option parsing | ✓ VERIFIED | Created with 3 tests: reads flag, handles missing flag, rejects in production |
| `BuildInfoTest.kt` | Tests for isTestBuild flag | ✓ VERIFIED | Created with 1 test: validates false in production builds |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| `YnabSplitPayeeAndMemo.kt` | `YnabClient` constructor | Passes `apiUrl` parameter | ✓ WIRED | Line 123: `YnabClient(token, apiUrl ?: "https://api.ynab.com/v1")` |
| `YnabClient.kt` | API calls (`/budgets`, `/transactions`) | Uses `$baseUrl` | ✓ WIRED | Lines 60, 103, 105, 150: All API endpoints constructed with `$baseUrl` |
| `build.gradle.kts` | `BuildInfo.kt` generation | Writes `isTestBuild` const | ✓ WIRED | Lines 28, 31, 42: Property read, configured as input, written to generated file |
| `YnabSplitPayeeAndMemo.kt` | CLI validation | Checks `BuildInfo.isTestBuild` | ✓ WIRED | Lines 97-101: Guards `--api-url` usage with `!BuildInfo.isTestBuild` check |

---

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CLI-01 | 01-01-PLAN.md | Separate Gradle task builds test JAR with configurable API base URL | ✓ SATISFIED | `build.gradle.kts` line 28: `-PtestBuild` property generates `isTestBuild = true` in BuildInfo.kt |
| CLI-02 | 01-01-PLAN.md | Production shadow JAR keeps hardcoded YNAB API URL | ✓ SATISFIED | `YnabSplitPayeeAndMemo.kt` lines 97-101: Validation rejects `--api-url` when `isTestBuild = false` |

---

## Anti-Patterns Found

**None.** Scanned all modified files for TODO/FIXME/HACK comments, stub implementations, and incomplete handlers. No blockers or warnings found.

---

## Test Results

All test suites execute successfully:

| Test Class | Tests | Failures | Status |
|-----------|-------|----------|--------|
| `YnabClientTest` | 2 | 0 | ✓ PASSED |
| `YnabSplitPayeeAndMemoTest` | 3 | 0 | ✓ PASSED |
| `BuildInfoTest` | 1 | 0 | ✓ PASSED |
| `TransactionUpdaterTest` (existing) | 12 | 0 | ✓ PASSED |
| **TOTAL** | **18** | **0** | **✓ PASSED** |

**Command:** `./gradlew test` → BUILD SUCCESSFUL

---

## Build Verification

### Production Build (Default)

```
./gradlew clean shadowJar
→ BUILD SUCCESSFUL
→ BuildInfo.isTestBuild = false
→ Rejects --api-url flag at runtime
```

### Test Build

```
./gradlew clean shadowJar -PtestBuild=true
→ BUILD SUCCESSFUL
→ BuildInfo.isTestBuild = true
→ Accepts --api-url flag for mock server injection
```

---

## Documentation Updates

**CLAUDE.md** updated with new "Testing Infrastructure" section:
- Explains production vs test build modes
- Documents `-PtestBuild=true` property usage
- Provides example commands for test builds
- Notes that feature is test-only infrastructure

Verified sections:
- Build & Test Commands: Added `./gradlew shadowJar -PtestBuild=true` example
- New "Testing Infrastructure" section with complete documentation

---

## Success Criteria Verification

From ROADMAP.md Phase 1 Success Criteria:

1. **CLI accepts `--api-url` flag to override the default YNAB API base URL**
   - ✓ VERIFIED: `YnabSplitPayeeAndMemo.kt` line 68-72 defines option
   - ✓ VERIFIED: Tests confirm flag is accepted when `isTestBuild = true`

2. **CLI respects `YNAB_API_URL` environment variable when `--api-url` is not provided**
   - ✓ VERIFIED: Option defines `envvar = "YNAB_API_URL"`
   - ✓ VERIFIED: Clikt handles environment variable resolution automatically

3. **Production shadow JAR still uses hardcoded YNAB API URL (no override possible in production builds)**
   - ✓ VERIFIED: YnabClient default parameter: `baseUrl: String = "https://api.ynab.com/v1"`
   - ✓ VERIFIED: CLI validation at lines 97-101 rejects flag when `!BuildInfo.isTestBuild`

4. **CLI can successfully connect to a mock server at the configured URL**
   - ✓ VERIFIED: All API endpoints use `$baseUrl` variable (lines 60, 103, 105, 150)
   - ✓ VERIFIED: URL construction is parameterized throughout YnabClient

---

## Summary

Phase 01 goal achieved completely. The YNAB API base URL is now:

- **Configurable via CLI flag** (`--api-url`)
- **Configurable via environment variable** (`YNAB_API_URL`)
- **Restricted to test builds** (production builds reject the flag automatically)
- **Wired through the entire API client** (all endpoints respect the configured URL)

All must-haves verified:
- ✓ Observable truths: 4/4
- ✓ Required artifacts: 6/6 exist and are substantive
- ✓ Key links: 4/4 wired correctly
- ✓ Requirements: CLI-01 and CLI-02 satisfied
- ✓ Tests: 18 tests pass, 0 failures
- ✓ Builds: Both production and test builds succeed
- ✓ Documentation: CLAUDE.md updated with testing infrastructure section

Foundation is ready for Phase 2 (E2E test infrastructure with WireMock).

---

_Verified: 2026-03-02T14:35:00Z_
_Verifier: Claude Code (gsd-verifier)_
