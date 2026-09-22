plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.slf4j)
    implementation(libs.okhttp)
    implementation(libs.caffeine)
    implementation(libs.oauthSdk)
    implementation(project(":libs:kotlin-utils"))

    testImplementation(libs.mockwebserver)
    testImplementation(libs.assertk)
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}