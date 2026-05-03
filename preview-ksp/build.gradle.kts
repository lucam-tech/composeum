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

// kotlin-compile-testing 1.6.0 embeds the Kotlin 1.9.24 compiler/KSP runtime.
// Without pinning the preview-ksp test classpath to that line, Gradle upgrades stdlib/reflect
// to this module's Kotlin 2.3.x versions and every compile-testing case fails before assertions run.
configurations.matching { it.name.startsWith("test") }.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlin:kotlin-stdlib:1.9.24",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.24",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24",
        "org.jetbrains.kotlin:kotlin-reflect:1.9.24",
        "org.jetbrains.kotlin:kotlin-script-runtime:1.9.24",
        "org.jetbrains.kotlin:kotlin-daemon-embeddable:1.9.24",
        "org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.24",
        "org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:1.9.24",
    )
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
