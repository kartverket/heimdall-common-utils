# Ktor Utils

Inneholder små funksjoner, moduler og plugins for forenkling av oppsett av ktor apper


## Metrics

**Oppsett:**
```kotlin
application {
    install(Metrics.Plugin) {
        contextpath = "path-to-app-if-any (optional)" // Optional, default: ""
        // Konfiguration for  MicrometerMetrics plugin er tilgjengeligjort
        meterBinders = emptyList() // e.g skru av alle standard metrikker
    }
}
```

**Lage en egne metrikker:**
```kotlin
val counter = Metrics.Registry.counter("my-counter")
val timer = Metrics.Registry.timer("my-timer")
val gauge = Metrics.Registry.gauge("my-gauge")
```

# Security

**Oppsett:**
```kotlin
application {
    val security = Security(
        AuthProvider(
            name = "FirstIDP",
            jwksConfig = Security.JwksConfig.JwksUrl("http://test.uri", "test"),
            tokenLocation = Security.TokenLocation.Cookie("Auth"),
        ),
        AuthProvider(
            name = "SecondIDP",
            jwksConfig = Security.JwksConfig.OidcWellkownUrl("http://test.uri/.well-known"),
            tokenLocation = Security.TokenLocation.Header("Authorization"),
        )
    )
    
    install(Authentication) {
        if (useMock) {
            security.setupMock("fakeuser")
        } else {
            security.setupJWT()
        }
    }
}
```

**Beskytt ett endepunkt:**
```kotlin
routing {
    authenticate(*security.authproviders) {
        // your route config
    }
}
```

**NB!** Om man bare har en `AuthProvider` kan man sette `name = null` og deretter ikke sende med `*security.authproviders` til `authenticate`.

**NB!** Om man har flere `AuthProviderConfig` vil plugin'en teste hver enkelt inntil den finner en gyldig innlogging. Den vil derfor ikke nødvendigvis sikre at bruker er logget inn flere steder.


## Nais endepunkter og selftest

**Oppsett:**
```kotlin
application {
    install(Selftest.Plugin) {
        appname = "your app name" // Optional, default: "Not set"
        version = "version of your app" // Optional, default: "Not set"
        contextpath = "/my-app" // Optional, default: ""
    }
}
```

**Registrere avhengigheter:**
```kotlin
class DummyService {
    val reporter = Selftest.Reporter(name = "DummyService", critical = false)
    
    init {
        fixedRateTimer("ping", daemon = true, period = 10.seconds.inWholeMilliseconds) {
            runBlocking {
                reporter.ping {
                    // TODO implementer sjekk som kaster Exception ved feil her
                }
            }
        }
    }
}
```

Om man ikke ønsker å ha en periodisk sjekk av helsetilstanden til en avhengighet, kan man bruke en optimistisk tilnærming.
```kotlin
class DummyService {
    val reporter = Selftest.Reporter(name = "DummyService", critical = false)
        .also { it.reportOk() }

    suspend fun hentData(): String {
        return runCatching { "Data hentet fra ett sted" }
            .onSuccess { reporter.reportOk() }
            .onFailure { reporter.reportError(it) }
            .getOrThrow()
    }
}
```

Kallet til `reportOk` er her påkrevd for at appen skal bli satt som `ready` og motta trafikk etter oppstart.
Oppstår det generelle feil vil disse så bli propagert til selftesten. Det er også mulig å bruke en kombinasjon av optimistisk sjekk, og periodisk sjekk.