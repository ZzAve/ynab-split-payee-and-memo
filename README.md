# YNAB Split Payee and Memo

A JVM application built with GraalVM native image support that uses the YNAB API to split transaction descriptions into separate Payee and Memo fields. The application can run natively on any platform supported by GraalVM without requiring a JVM at runtime.

## Overview

This application helps you clean up your YNAB transactions by:

1. Pulling transactions via the YNAB API
2. Parsing the original description
3. Updating transactions with separated Payee and Memo fields

## Prerequisites

- JDK 21 or later (for building)
- GraalVM with native-image support (for building native executables)
- Docker (for containerized builds and deployment)
- YNAB Personal Access Token (get it from your YNAB account settings)

## Building the Application

The application uses Gradle with the GraalVM native image plugin to build native executables.

### Building the JVM Application

```bash
./gradlew build
```

This will create a JAR file in the `build/libs/` directory.

### Building the Native Executable

```bash
./gradlew nativeCompile
```

This will create a native executable in the `build/native/nativeCompile/` directory.

### Building with Docker

You can also build the application using Docker, which doesn't require GraalVM to be installed locally:

```bash
docker build -t ynab-split-payee .
```

## Running the Application

### Running the JVM Application

```bash
java -jar build/libs/ynab-split-payee-and-memo-1.0-SNAPSHOT.jar --token YOUR_YNAB_TOKEN
```

### Running the Native Executable

```bash
./build/native/nativeCompile/ynab-split-payee --token YOUR_YNAB_TOKEN
```

### Running with Docker

```bash
docker run --rm ynab-split-payee --token YOUR_YNAB_TOKEN
```

### Command-line Options

- `-t, --token`: YNAB Personal Access Token (required)
- `-b, --budget-id`: YNAB Budget ID (default: last used budget)
- `-a, --account-id`: YNAB Account ID (default: all accounts)
- `-d, --days-back`: Number of days to look back for transactions (default: 30)
- `--dry-run`: Don't actually update transactions, just show what would be updated

### Examples

Run with default settings (last 30 days, all accounts, last used budget):

```bash
# Using the native executable
./build/native/nativeCompile/ynab-split-payee --token YOUR_YNAB_TOKEN

# Using the JVM application
java -jar build/libs/ynab-split-payee-and-memo-1.0-SNAPSHOT.jar --token YOUR_YNAB_TOKEN

# Using Docker
docker run --rm ynab-split-payee --token YOUR_YNAB_TOKEN
```

Run with specific budget and account:

```bash
# Using the native executable
./build/native/nativeCompile/ynab-split-payee --token YOUR_YNAB_TOKEN --budget-id BUDGET_ID --account-id ACCOUNT_ID

# Using the JVM application
java -jar build/libs/ynab-split-payee-and-memo-1.0-SNAPSHOT.jar --token YOUR_YNAB_TOKEN --budget-id BUDGET_ID --account-id ACCOUNT_ID

# Using Docker
docker run --rm ynab-split-payee --token YOUR_YNAB_TOKEN --budget-id BUDGET_ID --account-id ACCOUNT_ID
```

Run in dry-run mode (no actual updates):

```bash
# Using the native executable
./build/native/nativeCompile/ynab-split-payee --token YOUR_YNAB_TOKEN --dry-run

# Using the JVM application
java -jar build/libs/ynab-split-payee-and-memo-1.0-SNAPSHOT.jar --token YOUR_YNAB_TOKEN --dry-run

# Using Docker
docker run --rm ynab-split-payee --token YOUR_YNAB_TOKEN --dry-run
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

You can customize the parsing logic by modifying the `extractNewPayeeAndMemo` method in the `src/main/kotlin/YnabSplitter.kt` file. This allows you to adapt the application to your bank's specific transaction description format.

After making changes, rebuild the application:

```bash
# For JVM application
./gradlew build

# For native executable
./gradlew nativeCompile

# For Docker image
docker build -t ynab-split-payee .
```

## GraalVM Native Image Benefits and Limitations

### Benefits

- **No JVM Required at Runtime**: The native executable runs without requiring a Java Virtual Machine.
- **Faster Startup**: Native executables start up faster than JVM applications.
- **Smaller Distribution**: The executable contains only what's needed, without the overhead of the JVM.
- **Cross-Platform**: The same codebase can target multiple platforms (macOS, Linux, Windows).
- **Full JVM Library Ecosystem**: You can use any JVM library, unlike Kotlin/Native which has limited library support.
- **Simplified Development**: Develop using standard JVM tools and libraries, then compile to native code.

### Limitations

- **Build Time**: Building native images takes longer than compiling JVM applications.
- **Reflection Configuration**: Applications using reflection (like JSON serialization) require additional configuration.
- **Dynamic Features**: Some dynamic JVM features (like dynamic class loading) may not work or require special configuration.
- **Debugging**: Debugging native executables can be more challenging than debugging JVM applications.

## Cloud Deployment

This application is configured for deployment to Google Cloud Run as a containerized job. The included GitHub Actions workflow automates the build and deployment process.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
