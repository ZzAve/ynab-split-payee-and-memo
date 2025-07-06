import groovy.xml.dom.DOMCategory.attributes

plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("com.google.cloud.tools.jib") version "3.4.5"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "nl.zzave.ynab-split-payee"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}


jib {
    from {
//        image = "eclipse-temurin:21-alpine"
    }
    to {
        image = "eu.gcr.io/my-gcp-project/my-app"
    }
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // Kotlinx Serialization for JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Kotlinx Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Ktor client
    implementation("io.ktor:ktor-client-core:3.2.0")
    implementation("io.ktor:ktor-client-cio-jvm:3.2.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.2.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.0")
    implementation("io.ktor:ktor-client-logging:3.2.0")

    // Kermit for logging
    implementation("co.touchlab:kermit:2.0.2")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Kotlinx DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // Clikt for command-line interface
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
}
