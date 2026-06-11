package no.kartverket.heimdall.common.ktor.plugins.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.engine.mock.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class OidcClientTest {
    @Test
    fun `should be able to deserialize well-known json`() {
        val mockEngine = MockEngine {
            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType, "application/json"
                ),
                content = """
                    {
                    "issuer":"wk-issuer",
                    "jwks_uri":"http://jwks.uri",
                    "response_types_supported":["code", "id_token"],
                    "authorization_endpoint":"http://auth.uri",
                    "token_endpoint":"http://token.uri"
                    }
                """.trimIndent(),
            )
        }

        val client = OidcClient("http://dummy.test", mockEngine)
        val config = runBlocking { client.fetch() }
        assertThat(config.issuer).isEqualTo("wk-issuer")
    }
}