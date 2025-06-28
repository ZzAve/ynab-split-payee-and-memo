import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

private val logger = Logger.withTag("Main")

class YnabSplitPayeeAndMemo : CliktCommand() {
    private val token by option(
        "-t", "--token",
        help = "YNAB Personal Access Token"
    ).required()

    private val budgetId by option(
        "-b", "--budget-id",
        help = "YNAB Budget ID (default: last used budget)"
    )

    private val accountId by option(
        "-a", "--account-id",
        help = "YNAB Account ID (default: all accounts)"
    )

    private val daysBack by option(
        "-d", "--days-back",
        help = "Number of days to look back for transactions (default: 30)"
    ).int().default(30)

    private val dryRun by option(
        "--dry-run",
        help = "Don't actually update transactions, just show what would be updated"
    ).flag()

    override fun run() = runBlocking {
        logger.i { "Starting YNAB Split Payee and Memo" }
        logger.i { "Token: ${token.take(5)}..." }
        logger.i { "Budget ID: ${budgetId ?: "default"}" }
        logger.i { "Account ID: ${accountId ?: "all"}" }
        logger.i { "Days back: $daysBack" }
        logger.i { "Dry run: $dryRun" }

        val ynabClient = YnabClient(token)

        // If no budget ID is provided, use the last used budget
        val effectiveBudgetId = budgetId ?: run {
            logger.i { "No budget ID provided, fetching default budget" }
            ynabClient.getDefaultBudgetId()
        }

        // Calculate the date daysBack days ago
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val sinceDate = LocalDate(
            year = today.year,
            monthNumber = today.monthNumber,
            dayOfMonth = today.dayOfMonth
        ).minus(daysBack, DateTimeUnit.DAY )

        // Fetch transactions
        val transactions = ynabClient.getTransactions(
            budgetId = effectiveBudgetId,
            accountId = accountId,
            sinceDate = sinceDate
        )

        logger.i { "Found ${transactions.size} transactions" }

        // Process transactions
        val transactionsToUpdate = transactions.filter { 
            // Filter transactions that need processing
            it.memo.isNullOrBlank() && !it.payeeName.isNullOrBlank() && !it.importPayeeName.isNullOrBlank()
        }

        logger.i { "Found ${transactionsToUpdate.size} transactions to update" }

        // Process each transaction
        transactionsToUpdate.forEach { transaction ->
            val (newPayee, newMemo) = parseDescription(transaction.importPayeeName!!)

            logger.i { "Transaction: ${transaction.id}" }
            logger.i { "  Original payee: ${transaction.payeeName}" }
            logger.i { "  Import payee: ${transaction.importPayeeName}" }
            logger.i { "  New payee: $newPayee" }
            logger.i { "  New memo: $newMemo" }

            if (!dryRun) {
                ynabClient.updateTransaction(
                    budgetId = effectiveBudgetId,
                    transactionId = transaction.id,
                    payeeName = newPayee,
                    memo = newMemo
                )
                logger.i { "  Updated!" }
            }
        }

        logger.i { "Done!" }
    }

    private fun parseDescription(description: String): Pair<String, String> {
        // This is a simple implementation that can be customized based on the specific format
        // of the bank's transaction descriptions

        // Example: "PURCHASE AMAZON.COM AMZN.COM/BILL WA"
        // Might be split into:
        // Payee: "AMAZON.COM"
        // Memo: "PURCHASE AMZN.COM/BILL WA"

        // Another example: "POS PURCHASE - KROGER #123 - CINCINNATI OH"
        // Might be split into:
        // Payee: "KROGER"
        // Memo: "POS PURCHASE - #123 - CINCINNATI OH"

        // This is a very basic implementation that assumes the second word is the merchant name
        // and everything else goes into the memo
        val parts = description.split(" ", limit = 3)

        return when {
            parts.size >= 3 -> {
                // Assume format is "TYPE MERCHANT DETAILS"
                val type = parts[0]
                val merchant = parts[1]
                val details = parts[2]

                Pair(merchant, "$type $details")
            }
            parts.size == 2 -> {
                // Assume format is "TYPE MERCHANT"
                val type = parts[0]
                val merchant = parts[1]

                Pair(merchant, type)
            }
            else -> {
                // Just use the whole thing as the payee
                Pair(description, "")
            }
        }

        // Note: In a real implementation, you would want to use more sophisticated
        // pattern matching based on your bank's specific format
    }
}

// The main function is defined in the platform-specific source sets
