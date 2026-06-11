package no.kartverket.heimdall.common.ktor.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EnvUtilsTest {
    val defaultValues = mutableMapOf<String, String?>(
        "FALLBACK" to "fallback_value"
    )

    @Test
    fun `should prioritize properties before environment variables`() {
        assertThat(EnvUtils.getConfig("PATH", defaultValues)).isNotEqualTo("testing")
        withProperty("PATH" to "testing") {
            assertThat(EnvUtils.getConfig("PATH", defaultValues)).isEqualTo("testing")
        }
    }

    @Test
    fun `should use default values if not found in properties or environment variables`() {
        assertThat(EnvUtils.getConfig("FALLBACK", defaultValues)).isEqualTo("fallback_value")
    }

    @Test
    fun `should return 'null' if no value is found`() {
        assertThat(EnvUtils.getConfig("UNKNOWN", defaultValues)).isNull()
    }

    @Test
    fun `should throw exception if required`() {
        assertThrows<IllegalStateException> {
            EnvUtils.getRequiredConfig("UNKNOWN", defaultValues)
        }
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