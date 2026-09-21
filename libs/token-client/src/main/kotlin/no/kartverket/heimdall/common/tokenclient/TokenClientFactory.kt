package no.kartverket.heimdall.common.tokenclient

import no.kartverket.heimdall.common.tokenclient.client.*
import no.kartverket.heimdall.common.tokenclient.utils.AzureAdEnvironmentVariables as AzureEnv
import no.kartverket.heimdall.common.tokenclient.utils.MaskinportenEnvironmentVariables as MaskinportenEnv

object TokenClientFactory {
    object MachineToMachine {
        @JvmStatic
        fun maskinporten(): MachineToMachineTokenClient = maskinporten(CaffeineTokenCache())

        @JvmStatic
        fun maskinporten(tokenCache: TokenCache): MachineToMachineTokenClient = MaskinportenMachineToMachineTokenClient(
            clientId = getEnv(MaskinportenEnv.CLIENT_ID),
            tokenEndpoint = getEnv(MaskinportenEnv.TOKEN_ENDPOINT),
            privateJwk = getEnv(MaskinportenEnv.CLIENT_JWK),
            tokenCache = tokenCache
        )

        @JvmStatic
        fun azuread(): MachineToMachineTokenClient = azuread(CaffeineTokenCache())

        @JvmStatic
        fun azuread(tokenCache: TokenCache): MachineToMachineTokenClient =
            AzureAdMachineToMachineTokenClient(
                clientId = getEnv(AzureEnv.CLIENT_ID),
                tokenEndpoint = getEnv(AzureEnv.OPENID_CONFIG_TOKEN_ENDPOINT),
                privateJwk = getEnv(AzureEnv.APP_JWK),
                tokenCache = tokenCache
            )
    }

    object OnBehalfOf {
        @JvmStatic
        fun azureAd(): OnBehalfOfTokenClient = azureAd(CaffeineTokenCache())

        @JvmStatic
        fun azureAd(tokenCache: TokenCache): OnBehalfOfTokenClient =
            AzureAdOnBehalfOfTokenClient(
                clientId = getEnv(AzureEnv.CLIENT_ID),
                tokenEndpoint = getEnv(AzureEnv.OPENID_CONFIG_TOKEN_ENDPOINT),
                privateJwk = getEnv(AzureEnv.APP_JWK),
                tokenCache = tokenCache
            )
    }

    private fun getEnv(name: String): String {
        val value = System.getProperty(name) ?: System.getenv(name)

        check(!value.isNullOrBlank()) {
            "$name must have a value"
        }

        return value
    }
}
