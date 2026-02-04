package com.github.zzave.ynabsplitpayeeandmemo

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory
import kotlin.collections.isNotEmpty
import kotlin.time.Clock

class YnabSplitPayeeAndMemo : CliktCommand() {
    private val logger = LoggerFactory.getLogger(javaClass)

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

    private val budgetIds by option(
        "--budget-ids",
        help = "Comma seperated list of YNAB Budget ID. Only this one or budget-id should be provided",
        envvar = "YNAB_BUDGET_IDS",
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
            logger.info("Budget IDs: $budgetIds")
            logger.info("Account ID: ${accountId ?: "all"}")
            logger.info("Days back: $daysBack")
            logger.info("Dry run: $dryRun")
            logger.info("Only unapproved: $onlyUnapproved")

            require(budgetId == null || budgetIds == null) {
                "Either budget-id or budget-ids should be provided, but not both"
            }

            val ynabClient = YnabClient(token)

            // If no budget ID is provided, use the last used budget
            val effectiveBudgetIds: List<String> = getEffectiveBudgetIds(ynabClient, budgetId, budgetIds)
            val sinceDate = retrieveDateFromToday()

            effectiveBudgetIds.forEach { budgetId ->
                logger.info("")
                logger.info("")
                logger.info("Processing budget $budgetId")
                processBudget(ynabClient, budgetId, sinceDate)
                logger.info("Processed budget $budgetId")
                logger.info("")
                logger.info("")
            }
        }
    }

    private suspend fun getEffectiveBudgetIds(
        ynabClient: YnabClient,
        budgetId: String?,
        budgetIds: String?,
    ): List<String> =
        when {
            budgetIds != null -> {
                budgetIds.split(",")
            }

            budgetId != null -> {
                listOf(budgetId)
            }

            else -> {
                logger.info("No budget ID provided, fetching default budget")
                val defaultBudget = ynabClient.getDefaultBudget()
                logger.info("Using budget: ${defaultBudget.name} (${defaultBudget.id})")
                listOf(defaultBudget.id)
            }
        }

    private fun retrieveDateFromToday(): LocalDate {
        // Calculate the date daysBack days ago
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val sinceDate =
            today.minus(daysBack, DateTimeUnit.DAY)
        return sinceDate
    }

    private suspend fun processBudget(
        ynabClient: YnabClient,
        budgetId: String,
        sinceDate: LocalDate,
    ) {
        // Fetch transactions
        val transactions =
            ynabClient.getTransactions(
                budgetId = budgetId,
                accountId = accountId,
                sinceDate = sinceDate,
                onlyUnapproved = onlyUnapproved,
            )

        logger.info("Found ${transactions.size} transactions")

        // Process transactions in batches of 25
        val batchSize = 25
        val batches = transactions.chunked(batchSize)

        logger.info("Processing transactions in ${batches.size} batches of up to $batchSize transactions each")

        batches.forEachIndexed { batchIndex, batch ->
            logger.info("Processing batch ${batchIndex + 1} of ${batches.size} (${batch.size} transactions)")
            batch.processTransactionBatch(ynabClient, budgetId)
            logger.info("Completed batch ${batchIndex + 1} of ${batches.size} (${batch.size} transactions)")
        }
    }

    private suspend fun List<Transaction>.processTransactionBatch(
        ynabClient: YnabClient,
        budgetId: String,
    ) {
        val transactionsToUpdate = findTransactionsToUpdate()

        if (!dryRun && transactionsToUpdate.isNotEmpty()) {
            val updatedTransactions =
                ynabClient.updateTransactions(
                    budgetId = budgetId,
                    transactions = transactionsToUpdate,
                )
            logger.info("  Updated ${updatedTransactions.size} transactions in batch!")
        }
    }
}
