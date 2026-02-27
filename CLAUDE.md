# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kotlin CLI application that uses the YNAB API to split transaction payee descriptions into separate Payee and Memo fields. When banks import transactions like "AMAZON.COM - BOOKS", the app splits this into Payee: "AMAZON.COM" and Memo: "BOOKS".

## Build & Test Commands

Use `make` targets as the primary interface for building and testing:

```bash
make build     # Full build with tests
make test      # Run all tests
make test TEST=com.github.zzave.ynabsplitpayeeandmemo.TransactionUpdaterTest  # Single test class
make test TEST="*TransactionUpdaterTest"  # Wildcard pattern
make yolo      # Build without tests
make clean     # Clean build artifacts
make docker    # Build Docker image
make run       # Build Docker image + run with .env file
make dry-run   # Build Docker image + run in dry-run mode
```

## Architecture

**Entry point:** `Main.kt` → `YnabSplitPayeeAndMemo().main(args)`

Four main classes in `src/main/kotlin/com/github/zzave/ynabsplitpayeeandmemo/`:

- **YnabSplitPayeeAndMemo.kt** — CLI orchestrator (Clikt `CliktCommand`). Parses CLI args/env vars, fetches transactions, processes them in batches of 25, and updates via API.
- **YnabClient.kt** — YNAB API v1 HTTP client (Ktor CIO). Handles auth, fetching budgets/transactions, and batch updates.
- **TransactionUpdater.kt** — Pure business logic. `findTransactionsToUpdate()` filters eligible transactions; `extractNewPayeeAndMemo()` splits on " - " (space-dash-space); `removeDuplicatedSuffix()` deduplicates repeated memo content.
- **YnabModels.kt** — Kotlinx Serialization data classes for YNAB API request/response types.

## Key Business Rules

- Only processes transactions where `payee_name == import_payee_name` (not manually changed by user)
- Skips transfers (payee starts with "Transfer : ")
- Splits import payee on first " - " — left part → payee, right part → memo
- Preserves existing memo content (appends split result with " - " separator)
- Deduplicates suffix patterns in memos (e.g., "CoolBlue - CoolBlue" → "CoolBlue")

## Tech Stack

- **Kotlin** with JVM toolchain 21
- **Ktor Client** (CIO engine) for HTTP
- **Kotlinx Serialization** for JSON
- **Clikt** for CLI argument parsing
- **Kotest** (FunSpec style) with JUnit 5 runner for tests
- **Gradle** (Kotlin DSL) with Shadow plugin for fat JAR
- Version catalog in `gradle/libs.versions.toml`

## Configuration

CLI options can also be set via environment variables: `YNAB_TOKEN`, `YNAB_BUDGET_ID`, `YNAB_BUDGET_IDS`, `YNAB_ACCOUNT_ID`. A `.env` file is used by `make run` / `make dry-run`.

Logging is controlled by `YNAB_LOG` env var (`FILE` for file output, otherwise console). Config in `src/main/resources/logback.xml`.