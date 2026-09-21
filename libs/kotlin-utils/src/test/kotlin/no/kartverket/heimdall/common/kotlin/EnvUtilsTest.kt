package no.kartverket.heimdall.common.kotlin

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import java.util.Properties

class EnvUtilsTest {
    val defaultValues = mutableMapOf<String, String?>(
        "FALLBACK" to "fallback_value"
    )

    @Test
    fun `should prioritize properties before environment variables`() {
        assertThat(EnvUtils.getConfigOrNull("PATH", defaultValues)).isNotEqualTo("testing")
        withProperty("PATH" to "testing") {
            assertThat(EnvUtils.getConfigOrNull("PATH", defaultValues)).isEqualTo("testing")
        }
    }

    @Test
    fun `should use default values if not found in properties or environment variables`() {
        assertThat(EnvUtils.getConfigOrNull("FALLBACK", defaultValues)).isEqualTo("fallback_value")
    }

    @Test
    fun `should return 'null' if no value is found`() {
        assertThat(EnvUtils.getConfigOrNull("UNKNOWN", defaultValues)).isNull()
    }

    @Test
    fun `should throw exception if required`() {
        assertThrows<IllegalStateException> {
            EnvUtils.getConfig("UNKNOWN", defaultValues)
        }
    }

    @Test
    fun `loading properties file into jvm properties`() {
        val resource = checkNotNull(javaClass.classLoader.getResource("test-load.properties")) {
            "test-load.properties fixture must be on the test classpath"
        }
        val testProps = Properties()
        EnvUtils.load(Paths.get(resource.toURI()).toString(), testProps)

        assertThat(testProps.getProperty("loaded.key")).isEqualTo("loaded_value")
    }

    private fun withProperty(entry: Pair<String, String?>, block: () -> Unit) {
        val (key, value) = entry
        val originalValue = System.getProperty(key)
        nullableSetProperty(key, value)
        block()
        nullableSetProperty(key, originalValue)
    }

    private fun nullableSetProperty(key: String, value: String?) {
        if (value == null) {
            System.clearProperty(key)
        } else {
            System.setProperty(key, value)
        }
    }
}