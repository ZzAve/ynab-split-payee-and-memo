# YNAB Split Payee and Memo

A Kotlin Native application that uses the YNAB API to split transaction descriptions into separate Payee and Memo fields. The application can run natively on macOS and Linux without requiring a JVM.

## Overview

This application helps you clean up your YNAB transactions by:

1. Pulling transactions via the YNAB API
2. Parsing the original description
3. Updating transactions with separated Payee and Memo fields

## Prerequisites

- Kotlin Native (installed automatically by Gradle)
- YNAB Personal Access Token (get it from your YNAB account settings)

## Building the Application

The application uses Gradle with the Kotlin Multiplatform plugin to build native executables for macOS and Linux.

### Building for macOS

```bash
./gradlew macosX64Binaries
```

This will create a native executable in the `build/bin/macos/releaseExecutable/` directory.

### Building for Linux

```bash
./gradlew linuxX64Binaries
```

This will create a native executable in the `build/bin/linux/releaseExecutable/` directory.

## Running the Application

### On macOS

```bash
./build/bin/macos/releaseExecutable/ynab-split-payee-and-memo.kexe --token YOUR_YNAB_TOKEN
```

### On Linux

```bash
./build/bin/linux/releaseExecutable/ynab-split-payee-and-memo.kexe --token YOUR_YNAB_TOKEN
```

### Command-line Options

- `-t, --token`: YNAB Personal Access Token (required)
- `-b, --budget-id`: YNAB Budget ID (default: last used budget)
- `-a, --account-id`: YNAB Account ID (default: all accounts)
- `-d, --days-back`: Number of days to look back for transactions (default: 30)
- `--dry-run`: Don't actually update transactions, just show what would be updated

### Examples

Run with default settings (last 30 days, all accounts, last used budget):

#### macOS
```bash
./build/bin/macos/releaseExecutable/ynab-split-payee-and-memo.kexe --token YOUR_YNAB_TOKEN
```

#### Linux
```bash
./build/bin/linux/releaseExecutable/ynab-split-payee-and-memo.kexe --token YOUR_YNAB_TOKEN
```

Run with specific budget and account:

#### macOS
```bash
./build/bin/macos/releaseExecutable/ynab-split-payee-and-memo.kexe --token YOUR_YNAB_TOKEN --budget-id BUDGET_ID --account-id ACCOUNT_ID
```

#### Linux
```bash
./build/bin/linux/releaseExecutable/ynab-split-payee-and-memo.kexe --token YOUR_YNAB_TOKEN --budget-id BUDGET_ID --account-id ACCOUNT_ID
```

Run in dry-run mode (no actual updates):

#### macOS
```bash
./build/bin/macos/releaseExecutable/ynab-split-payee-and-memo.kexe --token YOUR_YNAB_TOKEN --dry-run
```

#### Linux
```bash
./build/bin/linux/releaseExecutable/ynab-split-payee-and-memo.kexe --token YOUR_YNAB_TOKEN --dry-run
```

## How It Works

The application uses a simple parsing algorithm to split transaction descriptions into payee and memo fields:

1. For descriptions with 3 or more words (e.g., "PURCHASE AMAZON.COM AMZN.COM/BILL WA"):
   - Payee: The second word ("AMAZON.COM")
   - Memo: The first word + the rest ("PURCHASE AMZN.COM/BILL WA")

2. For descriptions with 2 words (e.g., "PURCHASE AMAZON.COM"):
   - Payee: The second word ("AMAZON.COM")
   - Memo: The first word ("PURCHASE")

3. For descriptions with 1 word:
   - Payee: The entire description
   - Memo: Empty

## Customizing the Parsing Logic

You can customize the parsing logic by modifying the `parseDescription` method in the `src/commonMain/kotlin/Main.kt` file. This allows you to adapt the application to your bank's specific transaction description format.

After making changes, rebuild the application for your platform:

```bash
# For macOS
./gradlew macosX64Binaries

# For Linux
./gradlew linuxX64Binaries
```

## Kotlin/Native Benefits and Limitations

### Benefits

- **No JVM Required**: The application runs natively on the target platform without requiring a Java Virtual Machine.
- **Faster Startup**: Native executables start up faster than JVM applications.
- **Smaller Distribution**: The executable contains only what's needed, without the overhead of the JVM.
- **Cross-Platform**: The same codebase can target multiple platforms (macOS, Linux).

### Limitations

- **Platform-Specific Builds**: You need to build separate executables for each target platform.
- **Library Compatibility**: Not all JVM libraries have Kotlin/Native compatible alternatives.
- **Memory Management**: Kotlin/Native has different memory management than the JVM, which can affect performance characteristics.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
