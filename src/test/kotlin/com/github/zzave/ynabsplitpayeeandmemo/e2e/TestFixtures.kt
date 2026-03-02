package com.github.zzave.ynabsplitpayeeandmemo.e2e

import com.github.zzave.ynabsplitpayeeandmemo.PatchTransactionsWrapper
import com.github.zzave.ynabsplitpayeeandmemo.Transaction
import kotlinx.serialization.json.Json

/**
 * Default budget ID used across E2E tests.
 */
const val DEFAULT_BUDGET_ID = "test-budget-id"

/**
 * Default account ID used across E2E tests.
 */
const val DEFAULT_ACCOUNT_ID = "test-account-id"

/**
 * Creates a Transaction object with realistic defaults for E2E testing.
 *
 * @param id Transaction ID
 * @param importPayeeName Import payee name (from bank)
 * @param payeeName Payee name (defaults to importPayeeName for unmodified transactions)
 * @param memo Existing memo content
 * @param accountId Account ID (defaults to DEFAULT_ACCOUNT_ID)
 * @param date Transaction date (defaults to 2026-03-01)
 * @param amount Transaction amount in milliunits (defaults to -10000 = -10.00)
 * @param cleared Transaction cleared status (defaults to "uncleared")
 * @param approved Transaction approval status (defaults to false)
 * @return Transaction object with realistic defaults
 */
fun createTransaction(
    id: String,
    importPayeeName: String,
    payeeName: String? = importPayeeName,
    memo: String? = null,
    accountId: String = DEFAULT_ACCOUNT_ID,
    date: String = "2026-03-01",
    amount: Long = -10000,
    cleared: String = "uncleared",
    approved: Boolean = false
): Transaction {
    return Transaction(
        id = id,
        date = date,
        amount = amount,
        cleared = cleared,
        approved = approved,
        deleted = false,
        accountId = accountId,
        accountName = "Test Account",
        payeeId = null,
        payeeName = payeeName,
        categoryId = null,
        categoryName = null,
        memo = memo,
        flagColor = null,
        importId = "YNAB:${amount}:${date}:1",
        importPayeeName = importPayeeName,
        importMemo = null
    )
}

/**
 * Creates a Transaction that should be split (has " - " separator and payee_name == import_payee_name).
 *
 * These transactions are eligible for splitting because:
 * - import_payee_name contains " - " separator
 * - payee_name == import_payee_name (not manually changed by user)
 *
 * @param id Transaction ID
 * @param importPayeeName Import payee name with " - " separator (e.g., "ALBERT HEIJN - GROCERY")
 * @param memo Existing memo content
 * @return Transaction object eligible for splitting
 */
fun createSplittableTransaction(
    id: String,
    importPayeeName: String,
    memo: String? = null
): Transaction {
    require(importPayeeName.contains(" - ")) {
        "Splittable transaction must contain ' - ' separator in import_payee_name"
    }
    return createTransaction(
        id = id,
        importPayeeName = importPayeeName,
        payeeName = importPayeeName,  // Same as import_payee_name (not manually changed)
        memo = memo
    )
}

/**
 * Creates a Transaction that should NOT be split (no separator, transfer, or already-changed payee).
 *
 * These transactions are skipped because:
 * - No " - " separator in import_payee_name, OR
 * - Payee starts with "Transfer : ", OR
 * - payee_name != import_payee_name (manually changed by user)
 *
 * @param id Transaction ID
 * @param importPayeeName Import payee name
 * @param payeeName Payee name (if different from import_payee_name, transaction is skipped)
 * @param memo Existing memo content
 * @return Transaction object that should be skipped by splitting logic
 */
fun createSkippableTransaction(
    id: String,
    importPayeeName: String,
    payeeName: String? = importPayeeName,
    memo: String? = null
): Transaction {
    return createTransaction(
        id = id,
        importPayeeName = importPayeeName,
        payeeName = payeeName,
        memo = memo
    )
}

/**
 * Builds a YNAB API response JSON for GET /v1/budgets/{id}/transactions endpoint.
 *
 * Format:
 * ```json
 * {
 *   "data": {
 *     "transactions": [...],
 *     "server_knowledge": 123
 *   }
 * }
 * ```
 *
 * @param transactions List of Transaction objects to include in response
 * @param serverKnowledge Server knowledge value (defaults to 12345)
 * @return JSON string in YNAB API response format
 */
fun buildBudgetTransactionsResponse(
    transactions: List<Transaction>,
    serverKnowledge: Long = 12345
): String {
    val json = Json { prettyPrint = false }
    val transactionsJson = transactions.joinToString(",") { json.encodeToString(Transaction.serializer(), it) }
    return """
        {
          "data": {
            "transactions": [$transactionsJson],
            "server_knowledge": $serverKnowledge
          }
        }
    """.trimIndent()
}

/**
 * Parses a WireMock captured PATCH request body into PatchTransactionsWrapper.
 *
 * Use this to verify the content of PATCH /v1/budgets/{id}/transactions requests
 * made by the CLI during E2E tests.
 *
 * @param requestBody WireMock LoggedRequest body string
 * @return PatchTransactionsWrapper with deserialized transactions
 * @throws kotlinx.serialization.SerializationException if JSON is invalid
 */
fun parseRequestBody(requestBody: String): PatchTransactionsWrapper {
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromString(PatchTransactionsWrapper.serializer(), requestBody)
}
