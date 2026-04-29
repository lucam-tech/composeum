import org.gradle.kotlin.dsl.the

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.nmcp)
}

// Aggregate the three library modules into a single Maven Central deployment bundle.
// Run: ./gradlew publishAggregationToCentralPortal
//
// Credentials (set in ~/.gradle/gradle.properties, never commit):
//   mavenCentralUsername=<portal-token-username>
//   mavenCentralPassword=<portal-token-password>
//
// Generate a token at https://central.sonatype.com → Account → Generate User Token.
nmcpAggregation {
    centralPortal {
        username.set(
            providers.gradleProperty("mavenCentralUsername")
                .orElse(providers.environmentVariable("MAVEN_CENTRAL_USERNAME"))
        )
        password.set(
            providers.gradleProperty("mavenCentralPassword")
                .orElse(providers.environmentVariable("MAVEN_CENTRAL_PASSWORD"))
        )
        publishingType.set("USER_MANAGED")
    }
    // Discovers all subprojects that apply com.gradleup.nmcp (i.e. publish-library).
    // sample is excluded because it uses the android-application plugin, not publish-library.
    @Suppress("DEPRECATION")
    publishAllProjectsProbablyBreakingProjectIsolation()
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
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download.set(false)
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().download.set(false)
}
