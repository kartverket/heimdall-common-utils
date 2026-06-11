package no.kartverket.heimdall.common.ktor.plugins

import assertk.assertThat
import assertk.assertions.*
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test

class MetricsTest {
    @Test
    fun `should mount a route at root without configuration`() {
        testApplication {
            install(Metrics.Plugin)
            val response = client.get("/internal/metrics")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).isNotEmpty()
        }
    }

    @Test
    fun `should mount at correct contextpath`() {
        testApplication {
            install(Metrics.Plugin) {
                contextPath = "myapp"
            }

            var response = client.get("/internal/metrics")
            assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)

            response = client.get("myapp/internal/metrics")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).isNotEmpty()
        }
    }

    @Test
    fun `should expose metrics registered`() {
        testApplication {
            install(Metrics.Plugin)

            val counter = Metrics.Registry.counter("testcounter")
            counter.increment(13.0)


            val response = client.get("/internal/metrics")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains("testcounter_total 13.0")
        }
    }
}