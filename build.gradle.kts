plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

group = "nl.zzave.ynab-split-payee"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

// Configure the application
application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(25)
}

val generateBuildInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/src/main/kotlin")
    val projectVersion = project.version.toString()
    outputs.dir(outputDir)
    inputs.property("version", projectVersion)

    doLast {
        val dir = outputDir.get().asFile.resolve("com/github/zzave/ynabsplitpayeeandmemo")
        dir.mkdirs()
        dir.resolve("BuildInfo.kt").writeText(
            """
            |package com.github.zzave.ynabsplitpayeeandmemo
            |
            |object BuildInfo {
            |    const val VERSION = "$projectVersion"
            |}
            """.trimMargin()
        )
    }
}

sourceSets.main {
    kotlin.srcDir(generateBuildInfo.map { it.outputs.files.singleFile })
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "MainKt"
        attributes["Implementation-Version"] = project.version
    }
    mergeServiceFiles()
}

tasks.register("printVersion") {
    val projectVersion = project.version.toString()
    doLast {
        println(projectVersion)
    }
}

dependencies {
    // Kotlin standard library
    implementation(libs.kotlin.stdlib)

    // Kotlinx Serialization for JSON
    implementation(libs.kotlinx.serialization)

    // Kotlinx Coroutines
    implementation(libs.kotlinx.coroutines)

    // Ktor client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)

    // Logging with SLF4J and Logback
    implementation(libs.logback)
    implementation(libs.janino)

    // Kotlinx DateTime
    implementation(libs.kotlinx.datetime)

    // Clikt for command-line interface
    implementation(libs.clikt)

    // Testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
