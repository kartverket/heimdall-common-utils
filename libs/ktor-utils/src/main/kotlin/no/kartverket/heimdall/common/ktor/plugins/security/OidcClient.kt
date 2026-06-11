package no.kartverket.heimdall.common.ktor.plugins.security

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI

internal class OidcClient(
    val wellKnownUrl: String,
    engine: HttpClientEngine = CIO_ENGINE
) {
    companion object {
        private val log = LoggerFactory.getLogger(OidcClient::class.java)
        private val CIO_ENGINE = CIO.create {
            val httpProxy = System.getenv("HTTP_PROXY")
            httpProxy?.let { proxy = ProxyBuilder.http(Url(it)) }
        }
    }

    private val httpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
    }

    @Serializable
    class OidcDiscoveryConfig(
        @SerialName("jwks_uri") val jwksUrl: String,
        @SerialName("issuer") val issuer: String,
        @SerialName("authorization_endpoint") val authorizationEndpoint: String,
        @SerialName("token_endpoint") val tokenEndpoint: String,
    )

    suspend fun fetch(): OidcDiscoveryConfig =
        httpClient
            .runCatching { get(URI(wellKnownUrl).toURL()).body<OidcDiscoveryConfig>() }
            .onFailure { log.error("COuld not fetch oidc-config from $wellKnownUrl", it) }
            .getOrThrow()
}