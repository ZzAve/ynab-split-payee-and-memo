import com.github.ajalt.clikt.core.main
import com.github.zzave.ynabsplitpayeeandmemo.YnabSplitPayeeAndMemo

fun main(args: Array<String>) {
    if (args.any { it == "--verbose" || it == "-v" } || System.getenv("YNAB_VERBOSE") == "true") {
        System.setProperty("org.slf4j.simpleLogger.log.com.github.zzave.ynabsplitpayeeandmemo", "debug")
    }
    YnabSplitPayeeAndMemo().main(args)
}
