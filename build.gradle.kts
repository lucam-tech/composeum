plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Pin androidx.lifecycle to the version cached in this sandbox (no network access).
// 2.8.3 is API-compatible with the 2.8.5 demanded by Compose BOM 2024.12.01.
subprojects {
    configurations.all {
        resolutionStrategy.force("androidx.lifecycle:lifecycle-common-jvm:2.8.3")
    }
}

// Use system Node.js and Yarn — the sandbox has no internet access to download them.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download = false
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().download = false
}
