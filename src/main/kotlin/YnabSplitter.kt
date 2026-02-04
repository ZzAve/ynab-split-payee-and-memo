import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory
import kotlin.time.Clock

private val logger = LoggerFactory.getLogger("Main")

class YnabSplitPayeeAndMemo : CliktCommand() {
    private val token by option(
        "-t",
        "--token",
        help = "YNAB Personal Access Token",
        envvar = "YNAB_TOKEN",
    ).required()

    private val budgetId by option(
        "-b",
        "--budget-id",
        help = "YNAB Budget ID (default: last used budget)",
        envvar = "YNAB_BUDGET_ID",
    )

    private val accountId by option(
        "-a",
        "--account-id",
        help = "YNAB Account ID (default: all accounts)",
        envvar = "YNAB_ACCOUNT_ID",
    )

    private val dryRun by option(
        "--dry-run",
        help = "Don't actually update transactions, just show what would be updated",
    ).flag()

    private val daysBack by option(
        "-d",
        "--days-back",
        help = "Number of days to look back for transactions (default: 30)",
    ).int().default(30)

    private val onlyUnapproved by option(
        "--only-unapproved",
        help = "Only process unapproved transactions",
    ).flag("--all", default = true)

    override fun run() {
        try {
            doRun()
        } catch (e: Exception) {
            logger.error("Failed to run", e)
            throw Abort()
        }
    }

    private fun doRun() {
        runBlocking {
            logger.info("")
            logger.info("=====================================")
            logger.info("=====================================")
            logger.info("Starting YNAB Split Payee and Memo")
            logger.info("=====================================")
            logger.info("=====================================")
            logger.info("Token: ${token.take(5)}...")
            logger.info("Budget ID: ${budgetId ?: "default"}")
            logger.info("Account ID: ${accountId ?: "all"}")
            logger.info("Days back: $daysBack")
            logger.info("Dry run: $dryRun")
            logger.info("Only unapproved: $onlyUnapproved")

            val ynabClient = YnabClient(token)

            // If no budget ID is provided, use the last used budget
            val effectiveBudgetId =
                budgetId ?: run {
                    logger.info("No budget ID provided, fetching default budget")
                    val defaultBudget = ynabClient.getDefaultBudget()
                    logger.info("Using budget: ${defaultBudget.name} (${defaultBudget.id})")
                    defaultBudget.id
                }

            // Calculate the date daysBack days ago
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val sinceDate = today.minus(daysBack, DateTimeUnit.DAY)

            // Fetch transactions
            val transactions =
                ynabClient.getTransactions(
                    budgetId = effectiveBudgetId,
                    accountId = accountId,
                    sinceDate = sinceDate,
                    onlyUnapproved = onlyUnapproved,
                )

            logger.info("Found ${transactions.size} transactions")

            // Process transactions
            val transactionsToUpdate =
                transactions.filter {
                    // Filter transactions that need processing
                    !it.payeeName.isNullOrBlank() && !it.importPayeeName.isNullOrBlank()
                }

            logger.info("Found ${transactionsToUpdate.size} transactions to update")

            // Process transactions in batches of 25
            val batchSize = 25
            val batches = transactionsToUpdate.chunked(batchSize)

            logger.info("Processing transactions in ${batches.size} batches of up to $batchSize transactions each")

            batches.forEachIndexed { batchIndex, batch ->
                logger.info("Processing batch ${batchIndex + 1} of ${batches.size} (${batch.size} transactions)")
                batch.processTransactionBatch(ynabClient, effectiveBudgetId)
                logger.info("Completed batch ${batchIndex + 1} of ${batches.size} (${batch.size} transactions)")
            }

            logger.info("Done!")
        }
    }

    private suspend fun List<Transaction>.processTransactionBatch(
        ynabClient: YnabClient,
        budgetId: String,
    ) {
        val transactionsToUpdateBatch = mutableListOf<SaveTransactionWithId>()

        forEach { transaction ->
            val updated = transaction.extractNewPayeeAndMemo() ?: return@forEach

            logger.info("Transaction: ${transaction.id}")
            logger.info("  Original payee: ${transaction.payeeName}")
            logger.info("  Import payee: ${transaction.importPayeeName}")
            logger.info("  New payee: ${updated.payee}")
            logger.info("  Original memo: ${transaction.importMemo} (${transaction.memo})")
            logger.info("  New memo: ${updated.memo}")

            transactionsToUpdateBatch.add(
                SaveTransactionWithId(
                    id = transaction.id,
                    payeeId = null, // set explicitly to null to prevent payeeName from being ignored
                    payeeName = updated.payee,
                    memo = updated.memo,
                    accountId = transaction.accountId,
                    date = transaction.date,
                    amount = transaction.amount,
                    categoryId = transaction.categoryId,
                    cleared = transaction.cleared,
                    approved = transaction.approved,
                    flagColor = transaction.flagColor,
                ),
            )
        }

        if (!dryRun && transactionsToUpdateBatch.isNotEmpty()) {
            val updatedTransactions =
                ynabClient.updateTransactions(
                    budgetId = budgetId,
                    transactions = transactionsToUpdateBatch,
                )
            logger.info("  Updated ${updatedTransactions.size} transactions in batch!")
        }
    }

    data class NewPayeeAndMemo(
        val payee: String,
        val memo: String?,
    )

    private fun Transaction.extractNewPayeeAndMemo(): NewPayeeAndMemo? {
        if (payeeName != importPayeeName) {
            logger.info("Payee name does not match import payee name and was already changed by YNAB or user, skipping transaction: $id")
            return null
        }
        if (importPayeeName == null) {
            logger.info("Import payee name is null, skipping transaction: $id")
            return null
        }

        val split = importPayeeName.split("-", limit = 2)

        val newPayee =
            split.firstOrNull()?.trim() ?: return null.also { logger.info("No payee name found in transaction: $id") }
        val memoFromPayee = split.getOrNull(1)
        val newMemo =
            when {
                memoFromPayee.isNullOrBlank() -> memo
                memo == null -> null
                else -> "$memo - ${memoFromPayee.trim()}"
            }

        if (payeeName == newPayee && memo == newMemo) {
            return null.also { logger.info("Payee name and memo are unchanged, skipping transaction: $id") }
        }
        return NewPayeeAndMemo(payee = newPayee, memo = newMemo)
    }
}
