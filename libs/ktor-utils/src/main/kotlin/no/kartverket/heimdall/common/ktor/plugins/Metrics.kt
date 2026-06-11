package no.kartverket.heimdall.common.ktor.plugins

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object Metrics {
    val Registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    class Config {
        var contextPath: String = ""
        var metricName: String = "ktor.http.server.requests"
        var meterBinders: List<MeterBinder> =
            listOf(
                ClassLoaderMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics(),
                JvmThreadMetrics(),
                FileDescriptorMetrics(),
            )
        var distributionStatisticConfig: DistributionStatisticConfig =
            DistributionStatisticConfig.Builder().percentiles(0.5, 0.9, 0.95, 0.99).build()

        internal var timerBuilder: Timer.Builder.(ApplicationCall, Throwable?) -> Unit = { _, _ -> }

        fun timers(block: Timer.Builder.(ApplicationCall, Throwable?) -> Unit) {
            timerBuilder = block
        }
    }

    val Plugin = createApplicationPlugin("Metrics", ::Config) {
        val config = pluginConfig
        application.install(MicrometerMetrics) {
            this.registry = Metrics.Registry
            this.metricName = config.metricName
            this.meterBinders = config.meterBinders
            this.distributionStatisticConfig = config.distributionStatisticConfig
            this.timers(block = config.timerBuilder)
        }

        application.routing {
            route(config.contextPath) {
                route("internal") {
                    get("metrics") {
                        call.respondText(Registry.scrape())
                    }
                }
            }
        }
    }
}