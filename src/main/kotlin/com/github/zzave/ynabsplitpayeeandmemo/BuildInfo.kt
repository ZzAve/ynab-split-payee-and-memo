package com.github.zzave.ynabsplitpayeeandmemo

import java.util.Properties

object BuildInfo {
    private val props: Properties? = BuildInfo::class.java.getResourceAsStream("/build-info.properties")?.let { stream ->
        Properties().apply { load(stream) }
    }
    val VERSION: String = props?.getProperty("version") ?: "unknown"
    val IS_DEBUG_BUILD: Boolean = props?.getProperty("isDebugBuild")?.toBoolean() ?: false
}
