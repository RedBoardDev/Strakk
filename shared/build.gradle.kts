import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// ---------------------------------------------------------------------------
// Read Supabase credentials from local.properties (gitignored)
// and generate a Kotlin object so they never appear in source control.
// ---------------------------------------------------------------------------

abstract class GenerateSupabaseConfigTask : DefaultTask() {
    @get:org.gradle.api.tasks.Input
    abstract val url: Property<String>

    @get:org.gradle.api.tasks.Input
    abstract val key: Property<String>

    @get:org.gradle.api.tasks.OutputDirectory
    abstract val outputDir: DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun generate() {
        val dir = outputDir.get().asFile.resolve("com/strakk/shared/data/remote")
        dir.mkdirs()
        dir.resolve("SupabaseConfig.kt").writeText(
            """
            |package com.strakk.shared.data.remote
            |
            |/**
            | * Auto-generated from local.properties — do NOT edit manually.
            | * Values are injected at build time and never committed to source control.
            | */
            |internal object SupabaseConfig {
            |    const val URL = "${url.get()}"
            |    const val ANON_KEY = "${key.get()}"
            |}
            """.trimMargin()
        )
    }
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

val generateSupabaseConfig by tasks.registering(GenerateSupabaseConfigTask::class) {
    url.set(localProps.getProperty("supabase.url", ""))
    key.set(localProps.getProperty("supabase.key", ""))
    outputDir.set(layout.buildDirectory.dir("generated/supabaseConfig"))
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateSupabaseConfig.map { layout.buildDirectory.dir("generated/supabaseConfig") })
        }

        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.functions)
            implementation(libs.supabase.storage)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.noarg)
            implementation(libs.multiplatform.settings.serialization)
            implementation(libs.multiplatform.settings.coroutines)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.strakk.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
