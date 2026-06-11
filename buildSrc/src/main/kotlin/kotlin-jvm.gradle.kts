// The code in this file is a convention plugin - a Gradle mechanism for sharing reusable build logic.
// `buildSrc` is a Gradle-recognized directory and every plugin there will be easily available in the rest of the build.
package buildsrc.convention

import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin in JVM projects.
    `java-library`
    `maven-publish`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    // Use a specific Java version to make it easier to work in different environments.
    jvmToolchain(25)
}

tasks.withType<Test>().configureEach {
    // Configure all test Gradle tasks to use JUnitPlatform.
    useJUnitPlatform()

    // Log information about all test results, not only the failed ones.
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set("Shared Heimdall JVM utility library")
                url.set("https://github.com/kartverket/heimdall-common-utils")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/kartverket/heimdall-common-utils/blob/main/LICENSE")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/kartverket/heimdall-common-utils.git")
                    developerConnection.set("scm:git:ssh://git@github.com/kartverket/heimdall-common-utils.git")
                    url.set("https://github.com/kartverket/heimdall-common-utils")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kartverket/heimdall-common-utils")

            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
}
