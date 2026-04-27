plugins {
    id("kotlin-library")
    id("publish-library")
    id("com.gradleup.nmcp")
}

description = "Compose Preview — KSP annotation processor"

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

// KMP consumers: apply KSP per target in your build.gradle.kts, e.g.
//   kspAndroid(project(":preview-ksp"))
//   kspWasmJs(project(":preview-ksp"))
// The processor runs JVM-only and resolves the jvm() variant of :preview-annotation.
dependencies {
    compileOnly(project(":preview-annotation"))
    compileOnly(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(libs.junit)
    testImplementation(libs.compile.testing.ksp)
}

nmcp {
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
}
