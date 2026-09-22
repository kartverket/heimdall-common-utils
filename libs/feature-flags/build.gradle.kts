plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.posthog)
    implementation(libs.slf4j)
    implementation(libs.kotlinxCoroutines)

    testImplementation(libs.bundles.testEcosystem)
    testImplementation(libs.mockwebserver)
}