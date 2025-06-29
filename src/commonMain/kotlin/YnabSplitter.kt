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

        // Process transactions in batches of 25
        val batchSize = 25
        val batches = transactionsToUpdate.chunked(batchSize)

        logger.i { "Processing transactions in ${batches.size} batches of up to $batchSize transactions each" }

        batches.forEachIndexed { batchIndex, batch ->
            logger.i { "Processing batch ${batchIndex + 1} of ${batches.size} (${batch.size} transactions)" }

            val transactionsToUpdateBatch = mutableListOf<SaveTransactionWithId>()

            batch
                .forEach { transaction ->
                val updated = parseDescription(transaction) ?: return@forEach

                logger.i { "Transaction: ${transaction.id}" }
                logger.i { "  Original payee: ${transaction.payeeName}" }
                logger.i { "  Import payee: ${transaction.importPayeeName}" }
                logger.i { "  New payee: ${updated.payee}" }
                logger.i { "  Original memo: ${transaction.importMemo} (${transaction.memo})" }
                logger.i { "  New memo: ${updated.memo}" }

                transactionsToUpdateBatch.add(
                    SaveTransactionWithId(
                        id = transaction.id,
                        payeeId = null ,//transaction.payeeId, // null?,
                        payeeName = updated.payee,
                        memo = updated.memo,
                        accountId = transaction.accountId,
                        date = transaction.date,
                        amount = transaction.amount,
                        categoryId = transaction.categoryId,
                        cleared = transaction.cleared,
                        approved = transaction.approved,
                        flagColor = transaction.flag_color,
                    )
                )
            }

            if (!dryRun && transactionsToUpdateBatch.isNotEmpty()) {
                val updatedTransactions = ynabClient.updateTransactions(
                    budgetId = effectiveBudgetId,
                    transactions = transactionsToUpdateBatch
                )
                logger.i { "  Updated ${updatedTransactions.size} transactions in batch ${batchIndex + 1}!" }
            }
        }

        logger.i { "Done!" }
    }

    data class NewPayeeAndMemo(
        val payee: String,
        val memo: String?,
    )

    private fun parseDescription(transaction: Transaction): NewPayeeAndMemo? {
        if (transaction.payeeName != transaction.importPayeeName) {
            logger.i { "Payee name does not match import payee name and was already changed by YNAB or user, skipping transaction: ${transaction.id}" }
            return null
        }
        if (transaction.importPayeeName == null) {
            logger.i { "Import payee name is null, skipping transaction: ${transaction.id}" }
            return null
        }


        val split = transaction.importPayeeName.split(" - ", limit = 2)

        val newPayee = split.firstOrNull() ?: return null.also{logger.i{ "No payee name found in transaction: ${transaction.id}"} }
        val newMemo = if (transaction.memo == null && split.getOrNull(1) == null) {
            null
        } else {
            val oldMemoPart = transaction.memo?.let { "$it - " } ?: ""
            val newMemoPart = split.getOrElse(1) { "" }

            oldMemoPart + newMemoPart
        }

        if (transaction.payeeName == newPayee && transaction.memo == newMemo) {
            return null.also{logger.i{ "Payee name and memo are unchanged, skipping transaction: ${transaction.id}"} }
        }
        return NewPayeeAndMemo(payee = newPayee, memo = newMemo)

    }
}

// The main function is defined in the platform-specific source sets
