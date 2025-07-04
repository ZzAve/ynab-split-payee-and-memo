import co.touchlab.kermit.Logger

fun main(args: Array<String>) {
    Logger.setMinSeverity(co.touchlab.kermit.Severity.Info)
    YnabSplitPayeeAndMemo().main(args)

}
