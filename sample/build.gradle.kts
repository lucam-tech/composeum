plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "tech.lucam.composeum.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "tech.lucam.composeum.sample"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":preview-runtime"))
    ksp(project(":preview-ksp"))

    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material3)

    debugImplementation(libs.compose.ui.tooling)
}
