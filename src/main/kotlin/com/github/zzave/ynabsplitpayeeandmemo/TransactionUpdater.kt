package com.github.zzave.ynabsplitpayeeandmemo

import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.github.zzave.ynabsplitpayeeandmemo.TransactionUpdater")

fun List<Transaction>.findTransactionsToUpdate(): List<SaveTransactionWithId> {
    val transactionsToUpdateBatch = mutableListOf<SaveTransactionWithId>()
    filter {
        // Filter transactions that need processing
        !it.payeeName.isNullOrBlank() && !it.importPayeeName.isNullOrBlank()
    }.forEach { transaction ->
        transaction
            .extractNewPayeeAndMemo()
            ?.let { newPayeeAndMemo ->

                logger.info("com.github.zzave.ynabsplitpayeeandmemo.Transaction: ${transaction.id}")
                logger.info("  Original payee: ${transaction.payeeName}")
                logger.info("  Import payee: ${transaction.importPayeeName}")
                logger.info("  New payee: ${newPayeeAndMemo.payee}")
                logger.info("  Original memo: ${transaction.importMemo} (${transaction.memo})")
                logger.info("  New memo: ${newPayeeAndMemo.memo}")

                transactionsToUpdateBatch.add(
                    SaveTransactionWithId(
                        id = transaction.id,
                        payeeId = null, // set explicitly to null to prevent payeeName from being ignored
                        payeeName = newPayeeAndMemo.payee,
                        memo = newPayeeAndMemo.memo,
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
    }
    return transactionsToUpdateBatch
}

private data class NewPayeeAndMemo(
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
