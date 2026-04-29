@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform")
    id("publish-library")
    id("com.gradleup.nmcp")
}

description = "Compose Preview — @ComposePreview and @PreviewParam annotations"

kotlin {
    jvmToolchain(17)
    jvm()
    wasmJs { browser() }
}
