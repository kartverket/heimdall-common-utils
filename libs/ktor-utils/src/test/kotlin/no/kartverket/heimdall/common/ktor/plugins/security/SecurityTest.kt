package no.kartverket.heimdall.common.ktor.plugins.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.auth.Authentication
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.get

class SecurityTest {
    @Test
    fun `should provide a test principal when disabled`() {
        val security = Security(
            Security.AuthProvider(
                name = "azuread",
                jwksConfig = Security.JwksConfig.JwksUrl("http://test.uri", "test"),
                tokenLocation = Security.TokenLocation.Header("Authorization")
            )
        )
        testApplication {
            install(Authentication) {
                security.setupMock()
            }
            routing {
                authenticate("azuread") {
                    get {
                        val principal = call.principal<JWTPrincipal>()
                        call.respond("Hello ${principal?.payload?.subject}")
                    }
                }
            }

            val response = client.get("")
            assertThat(response.bodyAsText()).isEqualTo("Hello fake-user")
        }
    }
}