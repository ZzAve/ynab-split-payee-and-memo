package com.github.zzave.ynabsplitpayeeandmemo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models for the YNAB API responses.
 * Based on the YNAB API documentation: https://api.ynab.com/
 */

@Serializable
data class YnabResponse<T>(
    val data: T,
)

@Serializable
data class BudgetSummaryResponse(
    val budgets: List<BudgetSummary>,
    @SerialName("default_budget") val defaultBudget: BudgetSummary? = null,
)

@Serializable
data class BudgetSummary(
    val id: String,
    val name: String,
    @SerialName("last_modified_on") val lastModifiedOn: String? = null,
    @SerialName("first_month") val firstMonth: String? = null,
    @SerialName("last_month") val lastMonth: String? = null,
    @SerialName("date_format") val dateFormat: DateFormat? = null,
    @SerialName("currency_format") val currencyFormat: CurrencyFormat? = null,
)

@Serializable
data class DateFormat(
    val format: String,
)

@Serializable
data class CurrencyFormat(
    @SerialName("iso_code") val isoCode: String,
    @SerialName("example_format") val exampleFormat: String,
    @SerialName("decimal_digits") val decimalDigits: Int,
    @SerialName("decimal_separator") val decimalSeparator: String,
    @SerialName("symbol_first") val symbolFirst: Boolean,
    @SerialName("group_separator") val groupSeparator: String,
    @SerialName("currency_symbol") val currencySymbol: String,
    @SerialName("display_symbol") val displaySymbol: Boolean,
)

@Serializable
data class TransactionsResponse(
    val transactions: List<Transaction>,
    @SerialName("server_knowledge") val serverKnowledge: Long? = null,
)

@Serializable
data class TransactionResponse(
    val transaction: Transaction,
)

@Serializable
data class Transaction(
    val id: String,
    val date: String,
    val amount: Long,
    val cleared: String,
    val approved: Boolean,
    val deleted: Boolean = false,
    @SerialName("account_id") val accountId: String,
    @SerialName("account_name") val accountName: String? = null,
    @SerialName("payee_id") val payeeId: String? = null,
    @SerialName("payee_name") val payeeName: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    val memo: String? = null,
    @SerialName("flag_color") val flagColor: String? = null,
    @SerialName("import_id") val importId: String? = null,
    @SerialName("import_payee_name") val importPayeeName: String? = null,
    @SerialName("import_memo") val importMemo: String? = null,
)

@Serializable
data class TransactionUpdate(
    @SerialName("account_id") val accountId: String? = null,
    val date: String? = null,
    val amount: Long? = null,
    @SerialName("payee_id") val payeeId: String? = null,
    @SerialName("payee_name") val payeeName: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val memo: String? = null,
    val cleared: String? = null,
    val approved: Boolean? = null,
    @SerialName("flag_color") val flagColor: String? = null,
)

@Serializable
data class SaveTransactionWrapper(
    val transaction: TransactionUpdate,
)

@Serializable
data class PatchTransactionsWrapper(
    val transactions: List<SaveTransactionWithId>,
)

@Serializable
data class SaveTransactionWithId(
    val id: String? = null,
    @SerialName("account_id") val accountId: String,
    val date: String,
    val amount: Long,
    @SerialName("payee_id") val payeeId: String?,
    @SerialName("payee_name") val payeeName: String?,
    @SerialName("category_id") val categoryId: String?,
    val memo: String?,
    val cleared: String?,
    val approved: Boolean?,
    @SerialName("flag_color") val flagColor: String?,
)

@Serializable
data class SaveTransactionsResponse(
    val transactions: List<Transaction>,
    @SerialName("duplicate_import_ids") val duplicateImportIds: List<String>? = null,
    @SerialName("server_knowledge") val serverKnowledge: Long? = null,
)
