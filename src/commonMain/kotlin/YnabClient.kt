import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

/**
 * Client for the YNAB API.
 * Based on the YNAB API documentation: https://api.ynab.com/
 */
class YnabClient(private val token: String) {
    private val logger = Logger.withTag("YnabClient")
    private val baseUrl = "https://api.ynab.com/v1"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                explicitNulls = false
            })

        }
        install(Logging) {
            this.logger = io.ktor.client.plugins.logging.Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

    /**
     * Get the default budget ID.
     * If no default budget is found, returns the first budget in the list.
     */
    suspend fun getDefaultBudgetId(): String {
        logger.i { "Fetching budgets" }

        val response = client.get("$baseUrl/budgets") {
            header("Authorization", "Bearer $token")
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Failed to fetch budgets: ${response.status}")
        }

        val ynabResponse: YnabResponse<BudgetSummaryResponse> = response.body()

        val budgets = ynabResponse.data.budgets
        if (budgets.isEmpty()) {
            throw IllegalStateException("No budgets found")
        }

        // Use default budget if available, otherwise use the first budget
        val defaultBudget = ynabResponse.data.defaultBudget
        val budgetId = defaultBudget?.id ?: budgets.first().id

        logger.i { "Using budget: ${defaultBudget?.name ?: budgets.first().name} ($budgetId)" }

        return budgetId
    }

    /**
     * Get transactions for a budget.
     * 
     * @param budgetId The budget ID
     * @param accountId Optional account ID to filter transactions
     * @param sinceDate Optional date to filter transactions since
     * @return List of transactions
     */
    suspend fun getTransactions(
        budgetId: String,
        accountId: String?,
        sinceDate: LocalDate?,
        onlyUnapproved: Boolean
    ): List<Transaction> {
        logger.i { "Fetching transactions for budget $budgetId" }

        val url = if (accountId != null) {
            "$baseUrl/budgets/$budgetId/accounts/$accountId/transactions"
        } else {
            "$baseUrl/budgets/$budgetId/transactions"
        }

        val response = client.get(url) {
            header("Authorization", "Bearer $token")
            if (sinceDate != null) {
                parameter("since_date", sinceDate.toString())
            }

            if (onlyUnapproved)
                parameter("type", "unapproved")
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Failed to fetch transactions: ${response.status}")
        }

        val ynabResponse: YnabResponse<TransactionsResponse> = response.body()

        return ynabResponse.data.transactions.filter { !it.deleted }
    }

    /**
     * Update a transaction.
     * 
     * @param budgetId The budget ID
     * @param transactionId The transaction ID
     * @param payeeName The new payee name
     * @param memo The new memo
     */
    suspend fun updateTransaction(
        budgetId: String,
        transactionId: String,
        payeeName: String,
        memo: String?
    ): Transaction {
        logger.i { "Updating transaction $transactionId" }

        val update = TransactionUpdate(
            payeeName = payeeName,
            memo = memo
        )

        val wrapper = SaveTransactionWrapper(update)

        val response = client.put("$baseUrl/budgets/$budgetId/transactions/$transactionId") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(wrapper)
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Failed to update transaction: ${response.status}")
        }

        val ynabResponse: YnabResponse<TransactionResponse> = response.body()

        return ynabResponse.data.transaction
    }

    /**
     * Update multiple transactions in a batch.
     * 
     * @param budgetId The budget ID
     * @param transactions List of transactions to update
     * @return List of updated transactions
     */
    suspend fun updateTransactions(
        budgetId: String,
        transactions: List<SaveTransactionWithId>
    ): List<Transaction> {
        logger.i { "Batch updating ${transactions.size} transactions" }

        val wrapper = PatchTransactionsWrapper(transactions)

        val response = client.patch("$baseUrl/budgets/$budgetId/transactions") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(wrapper)
            println(this.body)
        }

        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.fromValue(209)) {
            throw Exception("Failed to update transactions: ${response.status}, ${response.bodyAsText()}")
        }

        val ynabResponse: YnabResponse<SaveTransactionsResponse> = response.body()

        return ynabResponse.data.transactions
    }
}
