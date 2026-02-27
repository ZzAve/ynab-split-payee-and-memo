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

                logger.info("  Transaction: ${transaction.id}")
                logger.info("  Original payee: ${transaction.payeeName}")
                logger.info("  Import payee: ${transaction.importPayeeName}")
                logger.info("  New payee: ${newPayeeAndMemo.payee}")
                logger.info("  Import memo: ${transaction.importMemo} ")
                logger.info("  Current / old memo: ${transaction.memo}")
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
    // TODO: allow to reprocess if payeeName != importName if a 'reprocess' flag or similar is set.
    if (payeeName != importPayeeName) {
        logger.info("Payee name does not match import payee name and was already changed by YNAB or user, skipping transaction: $id")
        return null
    }

    if (payeeName != null && payeeName.startsWith("Transfer : ")) {
        logger.info("Transaction is marked as a transfer, skipping transaction: $id")
        return null
    }
    if (importPayeeName == null) {
        logger.info("Import payee name is null, skipping transaction: $id")
        return null
    }

    val split =
        importPayeeName
            .removeSuffix(" -")
            .split(" - ", limit = 2)

    val newPayee =
        split.firstOrNull()?.trim() ?: return null.also { logger.info("No payee name found in transaction: $id") }
    val memoFromPayee = split.getOrNull(1)
    val newMemo =
        when {
            memoFromPayee.isNullOrBlank() -> {
                memo?.removeDuplicatedSuffix()
            }

            memo == null -> {
                memoFromPayee.trim()
            }

            else -> {
                val removeDuplicatedSuffixd = memo.removeDuplicatedSuffix()
                if (removeDuplicatedSuffixd != memo || removeDuplicatedSuffixd.contains(memoFromPayee.trim())) {
                    removeDuplicatedSuffixd
                } else {
                    "$memo - ${memoFromPayee.trim()}"
                }
            }
        }

    if (payeeName == newPayee && memo == newMemo) {
        return null.also { logger.info("Payee name and memo are unchanged, skipping transaction: $id") }
    }

    return NewPayeeAndMemo(payee = newPayee, memo = newMemo)
}

/**
 * Removes repeated suffix sequences from a " - "-delimited string.
 *
 * Examples:
 *  "A - A"                 → "A"
 *  "A - A - A - A"         → "A"
 *  "X - A - A"             → "X - A"
 *  "X - A - B - A - B"     → "X - A - B"
 */
private fun String.removeDuplicatedSuffix(): String {
    val parts = this.split(" - ").map { it.trim() }
    if (parts.size <= 1) return this

    var result = parts
    var changed = true
    while (changed) {
        changed = false
        for (len in 1..result.size / 2) {
            val suffix = result.subList(result.size - len, result.size)
            val beforeSuffix = result.subList(result.size - 2 * len, result.size - len)
            if (suffix == beforeSuffix) {
                result = result.subList(0, result.size - len)
                changed = true
                break
            }
        }
    }

    return result.joinToString(" - ")
}
