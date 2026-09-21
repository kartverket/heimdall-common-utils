plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.slf4j)
    implementation(libs.okhttp)
    implementation(libs.caffeine)
    implementation(libs.oauthSdk)

    testImplementation(libs.mockwebserver)
    testImplementation(libs.assertk)
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}