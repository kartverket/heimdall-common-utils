package no.kartverket.heimdall.common.ktor.plugins.selftest

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.time.Duration.Companion.milliseconds

class SelftestTest {
    @Test
    fun `should mount a route at root without configuration`() {
        testApplication {
            install(Selftest.Plugin) {
                appname = "testapp"
                version = "1.0.0"
            }

            assertNaisRoutes(
                heading = "Should be ok if no reporters are registered",
                isAlive = HttpStatusCode.OK,
                isReady = HttpStatusCode.OK,
            )

            assertSelftestContent(
                content =
                    """
                    Appname: testapp
                    Version: 1.0.0
                    
                    Status:
                    """.trimIndent(),
            )
        }
    }


    @Test
    fun `should mount a route at correct contextpath`() {
        testApplication {
            install(Selftest.Plugin) {
                appname = "testapp"
                version = "1.0.0"
                contextpath = "myapp"
            }

            assertNaisRoutes(
                heading = "Should be ok if no reporters are registered",
                contextpath = "myapp",
                isAlive = HttpStatusCode.OK,
                isReady = HttpStatusCode.OK,
            )

            assertSelftestContent(
                contextpath = "myapp",
                content =
                    """
                    Appname: testapp
                    Version: 1.0.0
                    
                    Status:
                    """.trimIndent(),
            )
        }
    }

    private suspend fun ApplicationTestBuilder.assertNaisRoutes(
        heading: String? = null,
        contextpath: String? = null,
        isAlive: HttpStatusCode,
        isReady: HttpStatusCode,
    ) {
        delay(50.milliseconds) // Allow changes to propagate to selftest
        val base = if (contextpath != null) "$contextpath/" else ""
        assertAll(
            heading = heading,
            {
                runBlocking {
                    client
                        .get("${base}internal/isAlive")
                        .assertEquals(
                            status = isAlive,
                            content = if (isAlive == HttpStatusCode.OK) "Alive" else "Not alive",
                        )
                }
            },
            {
                runBlocking {
                    client
                        .get("${base}internal/isReady")
                        .assertEquals(
                            status = isReady,
                            content = if (isReady == HttpStatusCode.OK) "Ready" else "Not ready",
                        )
                }
            },
        )
    }

    private suspend fun ApplicationTestBuilder.assertSelftestContent(
        contextpath: String? = null,
        content: String,
    ) {
        delay(50.milliseconds) // Allow changes to propagate to selftest
        val base = if (contextpath != null) "$contextpath/" else ""
        client
            .get("${base}internal/selftest")
            .assertEquals(
                status = HttpStatusCode.OK,
                content = content,
            )
    }

    private suspend fun HttpResponse.assertEquals(
        status: HttpStatusCode,
        content: String,
    ) {
        assertEquals(status, this.status)
        assertEquals(content, bodyAsText().trim())
    }
}