import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

fun main(args: Array<String>) {
    Logger.setMinSeverity(Severity.Info)
    val logger = Logger.withTag("Main")
    logger.d { "d Starting YNAB Split Payee and Memo" }
    logger.i { "i Starting YNAB Split Payee and Memo" }
    logger.w { "w Starting YNAB Split Payee and Memo" }
    logger.e { "e Starting YNAB Split Payee and Memo" }

    YnabSplitPayeeAndMemo().main(args)
}
