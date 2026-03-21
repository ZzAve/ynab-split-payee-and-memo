import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
    `jvm-test-suite`
}

group = "nl.zzave.ynab-split-payee"
version = "${if (version != "unspecified") version else "0-SNAPSHOT"}"

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

val generateBuildInfoProperties by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources/prod")
    val projectVersion = project.version.toString()
    outputs.dir(outputDir)
    inputs.property("version", projectVersion)
    doLast {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        dir.resolve("build-info.properties").writeText(
            "version=$projectVersion\n",
        )
    }
}

val generateDebugBuildInfoProperties by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources/debug")
    val projectVersion = project.version.toString()
    outputs.dir(outputDir)
    inputs.property("version", projectVersion)
    doLast {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        dir.resolve("build-info.properties").writeText(
            "version=$projectVersion\nisDebugBuild=true\n",
        )
    }
}

tasks.shadowJar {
    from(generateBuildInfoProperties)
    manifest {
        attributes["Main-Class"] = "MainKt"
        attributes["Implementation-Version"] = project.version
    }
    mergeServiceFiles()
}

val debugShadowJar by tasks.registering(ShadowJar::class) {
    archiveClassifier.set("debug-all")
    from(sourceSets.main.get().output)
    from(generateDebugBuildInfoProperties)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    manifest {
        attributes["Main-Class"] = "MainKt"
        attributes["Implementation-Version"] = project.version
    }
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
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
    implementation(libs.logstash.logback.encoder)

    // Kotlinx DateTime
    implementation(libs.kotlinx.datetime)

    // Clikt for command-line interface
    implementation(libs.clikt)

    // Testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.clikt.testing)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotest.assertions.core)
                implementation(libs.wiremock)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.serialization)
            }
            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(tasks.test)
                        dependsOn(debugShadowJar)
                    }
                }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        },
    )
}
