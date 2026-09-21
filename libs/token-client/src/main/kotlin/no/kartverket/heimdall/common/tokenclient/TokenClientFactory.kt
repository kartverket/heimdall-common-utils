package no.kartverket.heimdall.common.tokenclient

import no.kartverket.heimdall.common.kotlin.EnvUtils
import no.kartverket.heimdall.common.tokenclient.client.*
import no.kartverket.heimdall.common.tokenclient.utils.AzureAdEnvironmentVariables as AzureEnv
import no.kartverket.heimdall.common.tokenclient.utils.MaskinportenEnvironmentVariables as MaskinportenEnv

object TokenClientFactory {
    object MachineToMachine {
        @JvmStatic
        fun maskinporten(): MachineToMachineTokenClient = maskinporten(CaffeineTokenCache())

        @JvmStatic
        fun maskinporten(tokenCache: TokenCache): MachineToMachineTokenClient = MaskinportenMachineToMachineTokenClient(
            clientId = EnvUtils.getConfig(MaskinportenEnv.CLIENT_ID),
            tokenEndpoint = EnvUtils.getConfig(MaskinportenEnv.TOKEN_ENDPOINT),
            privateJwk = EnvUtils.getConfig(MaskinportenEnv.CLIENT_JWK),
            tokenCache = tokenCache
        )

        @JvmStatic
        fun azureAd(): MachineToMachineTokenClient = azureAd(CaffeineTokenCache())

        @JvmStatic
        fun azureAd(tokenCache: TokenCache): MachineToMachineTokenClient =
            AzureAdMachineToMachineTokenClient(
                clientId = EnvUtils.getConfig(AzureEnv.CLIENT_ID),
                tokenEndpoint = EnvUtils.getConfig(AzureEnv.OPENID_CONFIG_TOKEN_ENDPOINT),
                privateJwk = EnvUtils.getConfig(AzureEnv.APP_JWK),
                tokenCache = tokenCache
            )
    }

    object OnBehalfOf {
        @JvmStatic
        fun azureAd(): OnBehalfOfTokenClient = azureAd(CaffeineTokenCache())

        @JvmStatic
        fun azureAd(tokenCache: TokenCache): OnBehalfOfTokenClient =
            AzureAdOnBehalfOfTokenClient(
                clientId = EnvUtils.getConfig(AzureEnv.CLIENT_ID),
                tokenEndpoint = EnvUtils.getConfig(AzureEnv.OPENID_CONFIG_TOKEN_ENDPOINT),
                privateJwk = EnvUtils.getConfig(AzureEnv.APP_JWK),
                tokenCache = tokenCache
            )
    }
}
