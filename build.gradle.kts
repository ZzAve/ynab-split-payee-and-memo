import groovy.xml.dom.DOMCategory.attributes

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
    jvmToolchain(21)
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    mergeServiceFiles()
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

    // Kotlinx DateTime
    implementation(libs.kotlinx.datetime)

    // Clikt for command-line interface
    implementation(libs.clikt)
}
