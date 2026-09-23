package no.kartverket.heimdall.common.featureflags

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.startsWith
import com.posthog.internal.FeatureFlag
import com.posthog.internal.FeatureFlagMetadata
import com.posthog.internal.FlagConditionGroup
import com.posthog.internal.FlagDefinition
import com.posthog.internal.FlagFilters
import com.posthog.internal.FlagProperty
import com.posthog.internal.LocalEvaluationResponse
import com.posthog.internal.PostHogFlagsResponse
import com.posthog.internal.PostHogSerializer
import com.posthog.internal.PropertyOperator
import com.posthog.internal.PropertyType
import com.posthog.server.PostHogInterface
import io.mockk.mockk
import io.mockk.verify
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeatureToggleTest {

    private lateinit var server: MockWebServer
    private val serializer = PostHogSerializer(mockk())
    private val TEST_FLAGG = object : FeatureToggle.Flag {
        override val value: String = "test_flagg"

    }

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `henting og evaluering lokalt skal fungere`() {
        val service = FeatureToggle.localEvaluation(
            personalApiKey = "personal-key",
            host = server.url("").toString()
        )
        server.enqueue(
            localResponse(
                flags = listOf(
                    flagDefinition(
                        id = 1,
                        name = TEST_FLAGG.value,
                        releaseCondition = emptyList()
                    )
                )
            )
        )

        val enabled = service.isActive(TEST_FLAGG)

        assertThat(enabled).isTrue()
        assertThat(server.takeRequest().path).isNotNull().startsWith("/api/feature_flag/local_evaluation/")
    }

    @Test
    fun `losing connection to posthog keeps last previous flagDefinitions`() {
        val service = FeatureToggle.localEvaluation(
            personalApiKey = "personal-key",
            host = server.url("").toString(),
            customize = {
                pollIntervalSeconds(1)
            }
        )

        server.enqueue(
            localResponse(
                flags = listOf(
                    flagDefinition(
                        id = 1,
                        name = TEST_FLAGG.value,
                        releaseCondition = emptyList()
                    )
                )
            )
        )

        val enabled01 = service.isActive(TEST_FLAGG)

        // Simulate a failure to refresh flagDefinitions
        server.enqueue(
            MockResponse().setResponseCode(500)
        )
        Thread.sleep(1200)
        val enabled02 = service.isActive(TEST_FLAGG)

        // Simulate the connection is restored
        server.enqueue(
            localResponse(
                flags = listOf(
                    flagDefinition(
                        id = 1,
                        name = TEST_FLAGG.value,
                        releaseCondition = emptyList(),
                        active = false
                    )
                )
            )
        )
        Thread.sleep(1200)
        val enabled03 = service.isActive(TEST_FLAGG)

        assertThat(server.takeRequest().path).isNotNull().startsWith("/api/feature_flag/local_evaluation/")
        assertThat(server.getRequestCount()).isEqualTo(3)
        assertThat(enabled01).isTrue()
        assertThat(enabled02).isTrue()
        assertThat(enabled03).isFalse()
    }

    @Test
    fun `evaluering skal ta hensyn til global og lokal kontekst`() {
        val ctx = mapOf(
            FeatureToggle.CtxKeys.ENVIRONMENT to "betatest",
        )

        val service = FeatureToggle.localEvaluation(
            personalApiKey = "testkey",
            host = server.url("").toString(),
            globalContextProvider = { ctx }
        )
        server.enqueue(
            localResponse(
                listOf(
                    flagDefinition(
                        id = 1,
                        name = TEST_FLAGG.value,
                        releaseCondition = listOf(
                            releaseCondition(
                                property(FeatureToggle.CtxKeys.ENVIRONMENT, "betatest", PropertyOperator.EXACT),
                                property(FeatureToggle.CtxKeys.USERNAME, "test1", PropertyOperator.EXACT),
                            ),
                            releaseCondition(
                                property(FeatureToggle.CtxKeys.AREA, "0219", PropertyOperator.EXACT),
                            )
                        )
                    )
                )
            )
        )

        assertThat(service.isActive(TEST_FLAGG, mapOf(FeatureToggle.CtxKeys.USERNAME to "test1"))).isTrue()
        assertThat(service.isActive(TEST_FLAGG, mapOf(FeatureToggle.CtxKeys.AREA to "0219"))).isTrue()
        assertThat(server.getRequestCount()).isEqualTo(1)
    }

    @Test
    fun `henting og evaluering remote skal fungere`() {
        val service = FeatureToggle.remoteEvaluation(
            host = server.url("").toString()
        )
        server.enqueue(
            remoteResponse(
                TEST_FLAGG.value to true
            )
        )

        assertThat(service.isActive(TEST_FLAGG)).isTrue()
        assertThat(server.takeRequest().path).isNotNull().startsWith("/flags/?")
    }

    @Test
    fun `remote evaluering caches`() {
        val service = FeatureToggle.remoteEvaluation(
            host = server.url("").toString(),
            customize = {
                featureFlagCacheMaxAgeMs(500)
            }
        )

        server.enqueue(
            remoteResponse(
                TEST_FLAGG.value to true
            )
        )
        server.enqueue(
            remoteResponse(
                TEST_FLAGG.value to false
            )
        )

        val enabled01 = service.isActive(TEST_FLAGG)
        val enabled02 = service.isActive(TEST_FLAGG)

        Thread.sleep(500)
        val enabled03 = service.isActive(TEST_FLAGG)

        assertThat(server.getRequestCount()).isEqualTo(2)
        assertThat(enabled01).isTrue()
        assertThat(enabled02).isTrue()
        assertThat(enabled03).isFalse()
    }

    @Test
    fun `kall til fullstendig feil adresse skal bare returnere false som default`() {
        val service = FeatureToggle.remoteEvaluation(
            host = server.url("").toString()
        )

        server.enqueue(MockResponse().setResponseCode(500))

        val enabled = service.isActive(TEST_FLAGG)

        assertThat(enabled).isFalse()
    }

    @Test
    fun `close er idempotent`() {
        val posthog = mockk<PostHogInterface>(relaxed = true)
        val service = FeatureToggle.ServiceImpl(
            posthog = posthog,
            globalContextProvider = { emptyMap() },
        )

        service.close()
        service.close()

        verify(exactly = 1) { posthog.close() }
    }

    private fun MockResponse.json(data: Any): MockResponse {
        return this
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            // Må kalle lowercase() pga bug i serde-implementasjonen til PostHog
            .setBody(requireNotNull(serializer.serializeObject(data)).lowercase())
    }

    fun remoteResponse(
        vararg flags: Pair<String, Boolean>
    ): MockResponse {
        return MockResponse().json(
            PostHogFlagsResponse(
                featureFlags = null, // Legacy API
                featureFlagPayloads = null,
                flags = flags.associate {
                    val key = it.first
                    val value = FeatureFlag(
                        key = key,
                        enabled = it.second,
                        variant = null,
                        reason = null,
                        metadata = FeatureFlagMetadata(
                            id = 1,
                            payload = null,
                            version = 1
                        )
                    )
                    key to value
                },
                quotaLimited = null,
                requestId = null,
                evaluatedAt = null,
            )

        )
    }

    fun localResponse(flags: List<FlagDefinition>): MockResponse {
        return MockResponse().json(
            LocalEvaluationResponse(
                flags = flags,
                groupTypeMapping = null,
                cohorts = null
            )
        )
    }

    fun flagDefinition(
        id: Int,
        name: String,
        releaseCondition: List<FlagConditionGroup>,
        active: Boolean = true
    ): FlagDefinition {
        return FlagDefinition(
            id = id,
            key = name,
            name = "",
            active = active,
            version = 1,
            filters = FlagFilters(
                multivariate = null,
                payloads = null,
                aggregationGroupTypeIndex = null,
                groups = releaseCondition.ifEmpty { listOf(releaseCondition()) }
            )
        )
    }

    fun releaseCondition(vararg properties: FlagProperty): FlagConditionGroup {
        return FlagConditionGroup(
            properties = properties.toList(),
            rolloutPercentage = 100.0,
            variant = null
        )
    }

    fun property(key: FeatureToggle.CtxKeys, value: Any, operator: PropertyOperator) = FlagProperty(
        key = key.value,
        propertyValue = value,
        propertyOperator = operator,
        type = PropertyType.PERSON,
        negation = null,
        dependencyChain = null
    )

}