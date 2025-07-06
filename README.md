# YNAB Split Payee and Memo

## What is this for

YNAB Split Payee and Memo is a small utility that uses the YNAB API to split transaction descriptions into separate
Payee and Memo fields. The application is packaged as a lightweight Docker container with an optimized JRE for efficient
deployment.

This application helps you clean up your YNAB transactions by:

1. Pulling transactions via the YNAB API
2. Parsing the original description (splitting by " - ")
3. Updating transactions with separated Payee and Memo fields

## Why is this useful

When using linked accounts and direct import for transactions into YNAB from some banks (e.g. bunq and Knab in the Netherlands), the transaction descriptions often contain both the merchant name
and additional information about the purchase. For example, "AMAZON.COM - BOOKS" contains both the merchant (AMAZON.COM)
and what was purchased (BOOKS).

By default, YNAB places this entire string in the Payee field, which can make your transaction list cluttered and harder
to categorize or search. This application automatically:

- Separates the merchant name into the Payee field
- Moves the purchase details into the Memo field
- Preserves any existing memo content

The application uses a simple parsing algorithm to split transaction descriptions:

1. It looks for transactions where the payee name matches the import payee name (meaning it hasn't been manually changed
   by the user)
2. It then splits the import payee name by " - " (dash with spaces):
    - The first part becomes the new payee name
    - The second part (if it exists) is combined with any existing memo to form the new memo

For example, if the import payee name is "AMAZON.COM - BOOKS", it will be split into:

- Payee: "AMAZON.COM"
- Memo: "BOOKS" (plus any existing memo content)

If there's no dash in the import payee name, the entire name becomes the payee and the memo remains unchanged.

## How to use this

### Prerequisites

- YNAB Personal Access Token (get it from your YNAB account settings)
- Docker (for containerized deployment) or Java 21+ (for running the JAR directly)

> ℹ️ The YNAB api has a ratelimit per access token in order to prevent misuse or poorly configured scripts
>
> More info on their [api documentation site](https://api.ynab.com/#rate-limiting).

### Running the Application

#### Running with Docker (recommended)

```bash
# build your docker container
docker build -t zzave/ynab-split-payee .
docker run --rm zzave/ynab-split-payee --token YOUR_YNAB_TOKEN
```

#### Running the JVM Application

```bash
./gradlew shadowJar
java -jar ynab-split-payee-and-memo-1.0-SNAPSHOT-all.jar --token YOUR_YNAB_TOKEN
```

### Command-line Options

- `-t, --token`: YNAB Personal Access Token (required) - Can also be set with `YNAB_TOKEN` environment variable
- `-b, --budget-id`: YNAB Budget ID (default: last used budget) - Can also be set with `YNAB_BUDGET_ID` environment variable
- `-a, --account-id`: YNAB Account ID (default: all accounts) - Can also be set with `YNAB_ACCOUNT_ID` environment variable
- `-d, --days-back`: Number of days to look back for transactions (default: 30)
- `--dry-run`: Don't actually update transactions, just show what would be updated
- `--only-unapproved`: Only process unapproved transactions (default: true)
- `--all`: Process all transactions, not just unapproved ones

### Examples

Run with default settings (last 30 days, all accounts, last used budget, only unapproved transactions):

```bash
docker run --rm zzave/ynab-split-payee --token YOUR_YNAB_TOKEN
```

Run with specific budget and account:

```bash
docker run --rm zzave/ynab-split-payee --token YOUR_YNAB_TOKEN --budget-id BUDGET_ID --account-id ACCOUNT_ID
```

Run in dry-run mode (no actual updates):

```bash
docker run --rm zzave/ynab-split-payee --token YOUR_YNAB_TOKEN --dry-run
```

Process all transactions, not just unapproved ones:

```bash
docker run --rm zzave/ynab-split-payee --token YOUR_YNAB_TOKEN --all
```

Using environment variables instead of command-line arguments:

```bash
docker run --rm -e YNAB_TOKEN=YOUR_YNAB_TOKEN -e YNAB_BUDGET_ID=BUDGET_ID -e YNAB_ACCOUNT_ID=ACCOUNT_ID zzave/ynab-split-payee
```

### Customizing the Parsing Logic

You can customize the parsing logic by modifying the `extractNewPayeeAndMemo` method in the
`src/main/kotlin/YnabSplitter.kt` file. This allows you to adapt the application to your bank's specific transaction
description format.

After making changes, rebuild the application following the instructions in the "Build from source" section.

## Build from source

### Prerequisites

- JDK 21 or later
- Docker (for containerized builds)

### Building the JVM Application

The application uses Gradle to build a fat JAR that includes all dependencies:

```bash
./gradlew shadowJar
```

This will create a fat JAR file in the `build/libs/` directory with the suffix `-all.jar`.

### Building with Docker

You can also build the application using Docker:

```bash
docker build -t ynab-split-payee .
```

The Docker build creates an optimized container with a custom JRE that includes only the necessary modules, resulting in
a smaller image size.

## Docker Optimization

The application uses a multi-stage Docker build to create a lightweight container:

1. The first stage builds a custom, optimized JRE using jlink, which includes only the necessary Java modules.
2. The second stage creates a minimal Alpine Linux container with the optimized JRE and the application JAR.
3. The application runs as a non-root user for improved security.

Benefits of this approach:

- **Smaller Container Size**: The optimized JRE is much smaller than a full JDK or JRE.
- **Faster Startup**: The container starts quickly due to the optimized JRE.
- **Improved Security**: Running as a non-root user reduces the risk of container breakout.
- **Simplified Deployment**: The container includes everything needed to run the application.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
