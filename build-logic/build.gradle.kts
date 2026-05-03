plugins {
    `kotlin-dsl`
}

// /workspace is a Windows DrvFs (9p) filesystem that does not support chmod.
// Build outputs that require chmod (e.g. META-INF directory permissions) must land
// on a POSIX-capable filesystem.  Redirect build-logic outputs to the user home.
layout.buildDirectory.set(
    file(
        providers.systemProperty("user.home").get() + "/.composeum-build/build-logic"
    )
)

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.composePlugin)
}
