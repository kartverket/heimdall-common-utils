package no.kartverket.heimdall.common.tokenclient

import com.nimbusds.oauth2.sdk.`as`.AuthorizationServerMetadata
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import net.minidev.json.JSONObject
import java.io.IOException
import java.net.URI

object OAuthDiscoveryClient {
    private fun fetchRaw(discoveryClient: String): JSONObject {
        val url = URI.create(discoveryClient)

        val request = HTTPRequest(HTTPRequest.Method.GET, url)
        request.connectTimeout = 10_000
        request.readTimeout = 5_000

        val response = request.send()

        if (!response.indicatesSuccess()) {
            throw IOException("Couldn't download metadata from $url. Response: ${response.statusCode}")
        }
        return response.bodyAsJSONObject
    }

    fun fetchOIDCMetadata(discoveryClient: String): OIDCProviderMetadata {
        return OIDCProviderMetadata.parse(fetchRaw(discoveryClient))
    }

    fun fetchAuthorizationServerMetadata(discoveryClient: String): AuthorizationServerMetadata {
        return AuthorizationServerMetadata.parse(fetchRaw(discoveryClient))
    }
}
