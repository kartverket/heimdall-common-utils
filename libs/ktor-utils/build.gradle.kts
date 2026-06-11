plugins {
    id("buildsrc.convention.kotlin-jvm")
}

// Dependencies that should be provided by the application using the library
val providedDeps = listOf(
    libs.slf4j,
    ktorLibs.server.core,
    ktorLibs.server.auth.jwt,
    ktorLibs.server.metrics.micrometer,
    ktorLibs.client.core,
    ktorLibs.client.cio,
    ktorLibs.client.contentNegotiation,
    ktorLibs.serialization.kotlinx.json,
    libs.micrometerPrometheus,
)

dependencies {
    providedDeps.forEach {
        compileOnly(it)
        testImplementation(it)
    }

    testImplementation(libs.bundles.testEcosystem)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.client.mock)
}