package no.kartverket.heimdall.common.featureflags

import com.posthog.server.PostHog
import com.posthog.server.PostHogConfig
import com.posthog.server.PostHogEvaluateFlagsOptions
import com.posthog.server.PostHogInterface
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object FeatureToggle {
    val DEFAULT_POSTHOG_API_KEY: String = "phc_jFo0vYJBRHr8NolV09X5HJy6uh3dnVV5IgoA148hUfj"
    val DEFAULT_POSTHOG_HOST: String = "https://ph.atkv3-prod.kartverket.cloud"

    enum class CtxKeys(val value: String) {
        USERNAME("username"),
        ENVIRONMENT("environment"),
        AREA("area_id"),
    }

    fun interface ContextProvider {
        fun get(): Map<CtxKeys, Any?>
    }

    interface Service {
        fun isActive(flag: String): Boolean
        fun isActive(flag: String, contextProvider: Map<CtxKeys, Any?> = emptyMap()): Boolean
        fun close()
    }

    class ServiceImpl(
        private val posthog: PostHogInterface,
        private val globalContextProvider: ContextProvider,
    ) : Service {

        constructor(
            config: PostHogConfig,
            globalContextProvider: ContextProvider,
        ) : this(
            PostHog.with(config),
            globalContextProvider
        )
        private val logger: Logger = LoggerFactory.getLogger("FeatureToggleService")
        private val closed = AtomicBoolean(false)
        override fun isActive(flag: String): Boolean = isActive(flag, emptyMap())

        override fun isActive(flag: String, contextProvider: Map<CtxKeys, Any?>): Boolean {
            return runBlocking {
                runCatching {
                    withTimeout(5.seconds) {
                        val ctx = globalContextProvider.get() + contextProvider
                        val distinctId = ctx[CtxKeys.USERNAME]?.toString() ?: "anonymous"

                        val optionsBuilder = PostHogEvaluateFlagsOptions.builder()
                            .flagKeys(listOf(flag))
                        ctx.forEach { (key, value) ->
                            optionsBuilder.personProperty(key.value, value)
                        }

                        val flags = posthog.evaluateFlags(distinctId, optionsBuilder.build())
                        flags.isEnabled(flag)
                    }
                }.getOrElse { exception ->
                    logger.error("Feil ved evaluering av feature toggle", exception)
                    false
                }
            }
        }

        override fun close() {
            if (closed.compareAndSet(false, true)) {
                posthog.close()
            }
        }
    }

    class MockImpl : Service {
        private val mocks = mutableMapOf<String, Boolean>()

        fun setFlagStatus(flag: String, enabled: Boolean?) {
            if (enabled == null) {
                mocks.remove(flag)
            } else {
                mocks[flag] = enabled
            }
        }

        fun <T> withFlagStatus(flags: String, enabled: Boolean, fn: () -> T): T {
            val current = mocks[flags]
            setFlagStatus(flags, enabled)
            val result = fn()
            setFlagStatus(flags, current)
            return result
        }

        override fun isActive(flag: String): Boolean {
            return mocks[flag] ?: false
        }

        override fun isActive(
            flag: String,
            contextProvider: Map<CtxKeys, Any?>
        ): Boolean {
            return mocks[flag] ?: false
        }

        override fun close() {}
    }

    fun remoteEvaluation(
        apiKey: String = DEFAULT_POSTHOG_API_KEY,
        host: String = DEFAULT_POSTHOG_HOST,
        globalContextProvider: ContextProvider = ContextProvider { emptyMap() },
        customize: PostHogConfig.Builder.() -> Unit = {}
    ): Service {
        val config = PostHogConfig
            .builder(apiKey)
            .host(host)
            .localEvaluation(false)
            // Styrer hvor flag-evaluering invalideres i cache
            .featureFlagCacheMaxAgeMs(1.minutes.inWholeMilliseconds.toInt())
            .apply(customize)
            .build()

        return ServiceImpl(config, globalContextProvider)
    }

    fun localEvaluation(
        personalApiKey: String,
        apiKey: String = DEFAULT_POSTHOG_API_KEY,
        host: String = DEFAULT_POSTHOG_HOST,
        globalContextProvider: ContextProvider = ContextProvider { emptyMap() },
        customize: PostHogConfig.Builder.() -> Unit = {}
    ): Service {
        val config = PostHogConfig
            .builder(apiKey)
            .host(host)
            .personalApiKey(personalApiKey)
            .localEvaluation(true)
            // Styrer hvor ofte vi henter flagg-definisjoner fra PostHog
            .pollIntervalSeconds(30.seconds.inWholeSeconds.toInt())
            .apply(customize)
            .build()

        return ServiceImpl(config, globalContextProvider)
    }
}

