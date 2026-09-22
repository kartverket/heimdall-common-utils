package no.kartverket.heimdall.common.tokenclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import no.kartverket.heimdall.common.tokenclient.client.MaskinportenMachineToMachineTokenClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MaskinportenMachineToMachineTokenClientTest {
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `validate request and response parsing`() {
        val m2mToken = TokenCreator.createToken("subject")
        server.enqueue(tokenResponse(m2mToken))

        val tokenClient = MaskinportenMachineToMachineTokenClient(
            clientId = "clientId",
            tokenEndpoint = server.url("/token").toString(),
            privateJwk = TokenCreator.jwk.toJSONString(),
            tokenCache = CaffeineTokenCache()
        )

        val token = tokenClient.createToken("test-scope")
        val recordedRequest = server.takeRequest()

        val body = parseFormdata(recordedRequest.body.readUtf8())

        assertThat(token.serialize()).isEqualTo(m2mToken.serialize())
        assertThat(recordedRequest.path).isEqualTo("/token")
        assertThat(recordedRequest.method).isEqualTo("POST")
        assertThat(body["grant_type"]).isEqualTo("urn:ietf:params:oauth:grant-type:jwt-bearer")
        assertThat(body["scope"]).isEqualTo("test-scope")
        assertThat(body["assertion"]).isNotNull()
    }

    @Test
    fun `should cache tokens`() {
        server.enqueue(tokenResponse(TokenCreator.createToken("subject1")))
        server.enqueue(tokenResponse(TokenCreator.createToken("subject2")))

        val tokenClient = MaskinportenMachineToMachineTokenClient(
            clientId = "clientId",
            tokenEndpoint = server.url("/token").toString(),
            privateJwk = TokenCreator.jwk.toJSONString(),
            tokenCache = CaffeineTokenCache()
        )

        tokenClient.createToken("scope-1")
        tokenClient.createToken("scope-1")

        assertThat(server.requestCount).isEqualTo(1)

        tokenClient.createToken("scope-2")
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Disabled // Uncomment to manually run
    @Test
    fun `fetches a real token from Maskinporten`() {
        val clientId = requireNotNull(System.getenv("MASKINPORTEN_CLIENT_ID")) { "MASKINPORTEN_CLIENT_ID is not set" }
        val tokenEndpoint = requireNotNull(System.getenv("MASKINPORTEN_TOKEN_ENDPOINT")) { "MASKINPORTEN_TOKEN_ENDPOINT is not set" }
        val privateJwk = requireNotNull(System.getenv("MASKINPORTEN_PRIVATE_JWK")) { "MASKINPORTEN_PRIVATE_JWK is not set" }
        val scope = requireNotNull(System.getenv("MASKINPORTEN_SCOPE")) { "MASKINPORTEN_SCOPE is not set" }

        val tokenClient = MaskinportenMachineToMachineTokenClient(
            clientId = clientId,
            tokenEndpoint = tokenEndpoint,
            privateJwk = privateJwk,
            tokenCache = CaffeineTokenCache()
        )

        val token = tokenClient.createToken(scope)

        assertThat(token).isNotNull()
    }
}
