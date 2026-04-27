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
