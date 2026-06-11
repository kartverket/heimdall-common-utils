package no.kartverket.heimdall.common.ktor.utils

object EnvUtils {
    fun getConfig(
        name: String,
        defaultValues: Map<String, String?> = emptyMap()
    ): String? {
        return System.getProperty(name)
            ?: System.getenv(name)
            ?: defaultValues[name]
    }
    fun getRequiredConfig(
        name: String,
        defaultValues: Map<String, String?> = emptyMap()
    ): String {
        return checkNotNull(getConfig(name, defaultValues)) {
            "$name must be defined in java properties or environment"
        }
    }
}