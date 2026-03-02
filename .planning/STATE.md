# STATE: YNAB Split Payee & Memo — E2E Test Suite

**Project:** End-to-end test suite for YNAB split-payee-and-memo CLI
**Initialized:** 2026-03-02
**Current Phase:** Planning (roadmap complete, awaiting phase 1 planning)

---

## Project Reference

**Core Value:** Confidence that the CLI behaves correctly when talking to the real YNAB API — validated through realistic end-to-end scenarios without hitting the actual API.

**Vision:** Build a foundation of ~5-10 E2E tests using WireMock mock server and ProcessBuilder CLI invocation. Tests validate happy path, auth failures, and edge cases. Infrastructure is language-agnostic (mock server portability) to survive potential rewrite to Go or Rust.

**Constraints:**
- Must use existing Gradle/Kotest infrastructure for test runner
- Mock server must be standalone HTTP (not in-process mocking) for language portability
- Tests must invoke the actual built JAR, not call Kotlin functions directly
- Scope limited to ~5-10 foundational tests, not comprehensive coverage

---

## Current Position

| Aspect | Status |
|--------|--------|
| **Roadmap** | Complete ✓ |
| **Current Phase** | 1 (CLI Configuration) — Ready for planning |
| **Phase Plan** | Awaiting `/gsd:plan-phase 1` |
| **Progress** | 0/4 phases started |

**Progress Bar:**
```
Roadmap    [████████████████████████████] 100%
Phase 1    [                              ] 0%
Overall    [███                           ] 8%
```

---

## Roadmap Structure

**Total phases:** 4
**Total requirements:** 13 (all v1)

1. **Phase 1: CLI Configuration** (2 requirements) — Add --api-url support
2. **Phase 2: Test Infrastructure Setup** (3 requirements) — WireMock + ProcessBuilder harness
3. **Phase 3: Core E2E Tests** (3 requirements) — Happy path + auth error
4. **Phase 4: Edge Case Coverage** (5 requirements) — Boundaries and special cases

**Dependencies:** 1 → 2 → 3 → 4 (linear critical path)

---

## Key Decisions

| Decision | Rationale | Status |
|----------|-----------|--------|
| 4 phases (Quick depth) | Requirements cluster naturally; linear dependencies suit quick delivery | Approved |
| WireMock for mock server | Standalone HTTP, language-agnostic, well-established | — Pending implementation |
| ProcessBuilder for CLI invocation | Real JAR subprocess, validates actual CLI behavior | — Pending implementation |
| Kotest test runner | Already in project, can orchestrate WireMock + subprocess | — Pending implementation |
| Add --api-url / env var | Required to point CLI at mock server in tests | — Pending implementation |

---

## Accumulated Context

### Session 1 (2026-03-02)

**Completed:**
- Read PROJECT.md (core value: E2E test confidence)
- Read REQUIREMENTS.md (13 v1 requirements across 6 categories)
- Read config.json (quick depth, yolo mode)
- Identified 4 natural phases from requirements
- Derived success criteria for each phase (goal-backward)
- Validated 100% requirement coverage
- Wrote ROADMAP.md, STATE.md
- Updated REQUIREMENTS.md traceability

**Key insight:** Linear dependency chain (1 → 2 → 3 → 4) means phases must be sequential; no parallel work possible.

**Blockers:** None

**Next step:** `/gsd:plan-phase 1` to create detailed plan for CLI configuration

---

## Performance Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Roadmap completion | 100% | 100% ✓ |
| Requirement coverage | 100% | 100% ✓ |
| Orphaned requirements | 0 | 0 ✓ |
| Success criteria per phase | 2-5 | 4-5 ✓ |

---

## Session Continuity

**Working branch:** feature/landing-page (git status at start of work)

**Files created/modified:**
- `.planning/ROADMAP.md` (created)
- `.planning/STATE.md` (created)
- `.planning/REQUIREMENTS.md` (traceability section updated)

**Environment:**
- OS: macOS (darwin)
- Shell: zsh
- Repo: git (main branch: main, current branch: feature/landing-page)

**Context preserved:** All planning artifacts written to disk. Next session can load STATE.md for continuity.

---

*State initialized: 2026-03-02*
