package no.kartverket.heimdall.common.ktor.plugins.selftest

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

object Selftest {
    class Config(
        appName: String = "Not set",
        version: String = "Not set",
        var contextpath: String = "",
    ) : SelftestGenerator.Config(appName, version)

    val Plugin = createApplicationPlugin("Selftest", ::Config) {
        val config = pluginConfig
        val selftest = SelftestGenerator.getInstance(config)

        application.routing {
            route(config.contextpath) {
                route("internal") {
                    get("isAlive") {
                        if (selftest.isAlive()) {
                            call.respondText("Alive")
                        } else {
                            call.respondText("Not alive", status = HttpStatusCode.InternalServerError)
                        }
                    }

                    get("isReady") {
                        if (selftest.isReady()) {
                            call.respondText("Ready")
                        } else {
                            call.respondText("Not ready", status = HttpStatusCode.InternalServerError)
                        }
                    }

                    get("selftest") {
                        call.respondText(selftest.scrape())
                    }
                }
            }
        }
    }
}