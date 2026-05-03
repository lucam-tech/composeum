plugins {
    `maven-publish`
    signing
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

// Empty javadoc jar satisfies Maven Central's requirement for JVM/metadata publications.
// Replace with Dokka once it is wired up.
val javadocJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
}

// kotlin-jvm modules (e.g. :preview-ksp) need an explicit publication and sources jar.
// kotlin-multiplatform modules configure their own publications automatically.
plugins.withId("org.jetbrains.kotlin.jvm") {
    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                artifact(javadocJar)
            }
        }
    }
}

afterEvaluate {
    // Attach the javadoc jar to every KMP publication so Maven Central validation passes.
    if (plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        publishing {
            publications.withType<MavenPublication>().configureEach {
                artifact(javadocJar)
            }
        }
    }

    publishing {
        publications.withType<MavenPublication>().configureEach {
            pom {
                name.set(project.name)
                description.set(project.description ?: "")
                url.set("https://github.com/lucam-tech/composeum")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("lucam-tech")
                        name.set("Luca Moser")
                        email.set("luca.m.moser@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/lucam-tech/composeum.git")
                    developerConnection.set("scm:git:ssh://github.com/lucam-tech/composeum.git")
                    url.set("https://github.com/lucam-tech/composeum")
                }
            }
        }
    }

    signing {
        val signingKeyId = providers.gradleProperty("signingKeyId")
            .orElse(providers.environmentVariable("GPG_KEY_ID"))
            .orNull
        val signingKey = providers.gradleProperty("signingKey")
            .orElse(providers.environmentVariable("GPG_SIGNING_KEY"))
            .orNull
        val signingPassword = providers.gradleProperty("signingPassword")
            .orElse(providers.environmentVariable("GPG_SIGNING_PASSWORD"))
            .orNull
        if (signingKey != null) {
            // Export your key with: gpg --armor --export-secret-keys <KEY_ID>
            // Set signingKey/signingPassword in ~/.gradle/gradle.properties or
            // GPG_SIGNING_KEY/GPG_SIGNING_PASSWORD in the environment.
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            sign(publishing.publications)
        }
    }

    // Gradle 8 implicit-dependency check: the kotlinMultiplatform publication bundles
    // artifacts from all targets, so nmcp's publish tasks read .asc files produced by
    // the per-target Sign tasks without an explicit dependency.  Wire them all here.
    tasks.withType<AbstractPublishToMaven>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
}
