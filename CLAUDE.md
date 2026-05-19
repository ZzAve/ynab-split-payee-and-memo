# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kotlin CLI application that uses the YNAB API to split transaction payee descriptions into separate Payee and Memo fields. When banks import transactions like "AMAZON.COM - BOOKS", the app splits this into Payee: "AMAZON.COM" and Memo: "BOOKS".

## Build & Test Commands

Use `make` targets as the primary interface for building and testing:

```bash
make build      # Full build with tests
make test       # Run unit tests only
make test TEST=com.github.zzave.ynabsplitpayeeandmemo.TransactionUpdaterTest  # Single test class
make test TEST="*TransactionUpdaterTest"  # Wildcard pattern
make e2e-test   # Build debug JAR + run E2E integration tests
make yolo       # Build without tests
make clean      # Clean build artifacts
make docker     # Build JVM Docker image (jlink + Alpine)
make run        # Build JVM Docker image + run with .env file
make native     # Compile GraalVM native binary (requires GraalVM JDK 25)
make native-test # Compile native binary + smoke-test --help
make docker-native  # Build native Docker image (GraalVM build + debian:12-slim runtime)
make dry-run    # Build native Docker image + run in dry-run mode
```

Two JAR outputs:
- `./gradlew shadowJar` → `build/libs/*-all.jar` (production, rejects `--api-url`)
- `./gradlew debugShadowJar` → `build/libs/*-debug-all.jar` (debug, accepts `--api-url`)

## Architecture

**Entry point:** `Main.kt` → `YnabSplitPayeeAndMemo().main(args)`

Four main classes in `src/main/kotlin/com/github/zzave/ynabsplitpayeeandmemo/`:

- **YnabSplitPayeeAndMemo.kt** — CLI orchestrator (Clikt `CliktCommand`). Parses CLI args/env vars, fetches transactions, processes them in batches of 25, and updates via API.
- **YnabClient.kt** — YNAB API v1 HTTP client (Ktor Java engine / JDK HttpClient). Handles auth, fetching budgets/transactions, and batch updates. JSON is decoded explicitly via `json.decodeFromString<T>()` inline calls (compile-time serializer resolution, required for native-image compatibility).
- **TransactionUpdater.kt** — Pure business logic. `findTransactionsToUpdate()` filters eligible transactions; `extractNewPayeeAndMemo()` splits on " - " (space-dash-space); `removeDuplicatedSuffix()` deduplicates repeated memo content.
- **YnabModels.kt** — Kotlinx Serialization data classes for YNAB API request/response types.

## Key Business Rules

- Only processes transactions where `payee_name == import_payee_name` (not manually changed by user)
- Skips transfers (payee starts with "Transfer : ")
- Splits import payee on first " - " — left part → payee, right part → memo
- Preserves existing memo content (appends split result with " - " separator)
- Deduplicates suffix patterns in memos (e.g., "CoolBlue - CoolBlue" → "CoolBlue")

## Tech Stack

- **Kotlin** with JVM toolchain 25
- **Ktor Client** (Java engine — JDK HttpClient) for HTTP
- **Kotlinx Serialization** for JSON
- **Clikt** for CLI argument parsing
- **SLF4J Simple** for logging (configured via `src/main/resources/simplelogger.properties`)
- **Kotest** (FunSpec style) with JUnit 5 runner for tests
- **Gradle** (Kotlin DSL) with Shadow plugin for fat JAR and GraalVM Native Build Tools plugin for native binary
- Version catalog in `gradle/libs.versions.toml`

## Configuration

CLI options can also be set via environment variables: `YNAB_TOKEN`, `YNAB_BUDGET_ID`, `YNAB_BUDGET_IDS`, `YNAB_ACCOUNT_ID`. A `.env` file is used by `make run` / `make dry-run`.

Logging level is controlled via `YNAB_VERBOSE=true` (env var) or `--verbose` / `-v` (CLI flag), which sets `org.slf4j.simpleLogger.defaultLogLevel=debug` before any logger is initialized. Default is INFO. Log format and destination are configured in `src/main/resources/simplelogger.properties`.

## Testing Infrastructure

**Unit tests** (`src/test/`) run via `make test` / `./gradlew test`. No JAR build required.

**E2E integration tests** (`src/integrationTest/`) run via `make e2e-test` / `./gradlew integrationTest`. The `integrationTest` Gradle task automatically builds the debug JAR first.

**Two JAR variants** are produced from a single compilation:

**Production JAR** (`*-all.jar`):
- Built with `./gradlew shadowJar`
- Embeds `build-info.properties` with `isDebugBuild=false`
- Rejects `--api-url` flag with error message

**Debug JAR** (`*-debug-all.jar`):
- Built with `./gradlew debugShadowJar`
- Embeds `build-info.properties` with `isDebugBuild=true`
- Accepts `--api-url` flag to override API base URL
- Used by E2E tests against WireMock mock server

**Implementation:**
- `BuildInfo.IS_DEBUG_BUILD` and `BuildInfo.VERSION` are loaded from `/build-info.properties` resource at runtime
- Both JARs embed `build-info.properties` with the project version
- Production JAR has `isDebugBuild=false` → rejects `--api-url`
- Debug JAR has `isDebugBuild=true` → accepts `--api-url`

**Native binary** (`make native` / `make docker-native`):
- Built with GraalVM Native Build Tools plugin (`org.graalvm.buildtools.native`)
- Always has `isDebugBuild=false` — no native debug variant exists; E2E tests continue using the debug JAR
- Reflection metadata lives in `src/main/resources/META-INF/native-image/com.github.zzave.ynabsplitpayeeandmemo/` (reflect-config.json + resource-config.json)
- Serialization uses `json.decodeFromString<T>()` inline functions — serializers are resolved at compile time, not via reflection
- Docker runtime image: `debian:12-slim` running as non-root user `ynab`
- `YNAB_LOG` / `YNAB_LOG_FORMAT` have no effect in native builds (slf4j-simple, no logback)

## Documentation Site

The project has a GitHub Pages site in `docs/`:

- **`docs/index.html`** — Landing page (hero, before/after demo, trust section, get started)
- **`docs/usage.html`** — Tutorial-style usage guide (CLI options, Docker examples, automation)
- **`docs/logo.svg`** — Project logo

**After any feature or change, check if the site documentation needs updating.** Specifically:

- CLI option added/changed/removed → update the CLI reference in `docs/usage.html` and any relevant examples
- Business logic changed (splitting rules, filtering, memo handling) → update `docs/index.html` before/after examples and trust section claims
- Docker image name or usage changed → update Docker commands in both pages
- New configuration options → add to the usage guide