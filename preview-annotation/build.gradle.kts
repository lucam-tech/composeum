plugins {
    kotlin("multiplatform")
}

description = "Compose Preview — @ComposePreview and @PreviewParam annotations"

kotlin {
    jvmToolchain(17)
    jvm()
    wasmJs { browser() }
}
