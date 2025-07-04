plugins {
    kotlin("multiplatform") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
}

group = "nl.zzave.ynab-split-payee"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

kotlin {
    jvm {
//        jvmToolchain(21) // Use Java 21
//        withJava()

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }


    }
    // Target for macOS
    macosX64 {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    // Target for Linux
    linuxX64 {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    
    // Common source set for shared code
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin standard library
                implementation("org.jetbrains.kotlin:kotlin-stdlib")
                
                // Kotlinx Serialization for JSON
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                
                // Kotlinx Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                implementation("io.ktor:ktor-client-core:3.2.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.2.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.0")

                // Kermit for logging (replaces SLF4J/Logback)
                implementation("co.touchlab:kermit:2.0.2")

                // Kotlinx DateTime (replaces java.time)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

                // Clikt for command-line interface
                implementation("com.github.ajalt.clikt:clikt:4.2.1")
//                implementation("io.ktor:ktor-client-cio:3.2.0")
                implementation("io.ktor:ktor-client-logging:3.2.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("ch.qos.logback:logback-classic:1.4.11")
                implementation("io.ktor:ktor-client-cio-jvm:3.2.0")
            }
        }

        val macosX64Main by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.2.0")
            }
        }

        val linuxX64Main by getting {
            dependencies {
                implementation("io.ktor:ktor-client-curl:3.2.0")
            }
        }


    }
}

