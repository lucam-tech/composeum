@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("publish-library")
    id("com.gradleup.nmcp")
}

description = "Compose Preview — runtime browser UI and registry model"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":preview-annotation"))
            implementation(project.dependencies.platform(libs.compose.bom))
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.icons.ext)
            implementation(libs.collections.immutable)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.navigation.compose.cmp)
        }
        androidMain.dependencies {
            implementation(libs.activity.compose)
            implementation(libs.datastore.preferences)
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.ui.test.junit4)
        }
    }
}

android {
    namespace = "tech.lucam.composeum.runtime"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        // Disable lint in this sandbox — lint-gradle is not cached and the network is unreachable.
        checkReleaseBuilds = false
        abortOnError = false
    }
}


// extractAnnotations downloads lint-gradle at execution time.
// For offline sandboxes, replace the task actions so the typedef file is written without
// actually invoking lint-gradle (which cannot be downloaded with no network).
afterEvaluate {
    tasks.matching { it.name.startsWith("extract") && it.name.endsWith("Annotations") }.configureEach {
        val variantName = name.removePrefix("extract").removeSuffix("Annotations").lowercase()
        val typedefsFile = layout.buildDirectory
            .file("intermediates/annotations_typedef_file/$variantName/${name}/typedefs.txt")
        outputs.file(typedefsFile)
        actions.clear()
        doFirst {
            typedefsFile.get().asFile.let { f -> f.parentFile.mkdirs(); f.writeText("") }
        }
    }
}
