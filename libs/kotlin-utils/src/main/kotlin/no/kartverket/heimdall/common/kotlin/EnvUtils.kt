package no.kartverket.heimdall.common.kotlin

import java.util.Properties
import kotlin.io.path.Path
import kotlin.io.path.reader

object EnvUtils {
    fun getConfigOrNull(
        name: String,
        defaultValues: Map<String, String?> = emptyMap()
    ): String? {
        return (System.getProperty(name) ?: System.getenv(name) ?: defaultValues[name])
            ?.takeIf { it.isNotBlank() }
    }

    fun getConfig(
        name: String,
        defaultValues: Map<String, String?> = emptyMap()
    ): String {
        return checkNotNull(getConfigOrNull(name, defaultValues)) {
            "$name must be defined in java properties or environment"
        }
    }

    fun load(file: String, into: Properties = System.getProperties()) {
        val env = Properties()
        Path(file).reader().use { env.load(it) }

        env.entries.forEach { entry ->
            into.setProperty(entry.key.toString(), entry.value.toString())
        }
    }
}