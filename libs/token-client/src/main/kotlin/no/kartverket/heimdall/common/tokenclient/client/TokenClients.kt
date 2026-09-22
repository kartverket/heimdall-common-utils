package no.kartverket.heimdall.common.tokenclient.client

import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import no.kartverket.heimdall.common.tokenclient.TokenCache
import java.net.URI

class DownstreamApi(
    val cluster: String,
    val namespace: String,
    val application: String
) {
    companion object {
        @JvmStatic
        fun parse(value: String): DownstreamApi {
            val parts = value.split(":")
            check(parts.size == 3) { "DownstreamApi string must contain 3 parts" }

            val (cluster, namespace, application) = parts
            return DownstreamApi(cluster = cluster, namespace = namespace, application = application)
        }
    }

    fun tokenscope(): String = "api://$cluster.$namespace.$application/.default"
}

fun interface MachineToMachineTokenClient {
    fun createToken(scope: String): SignedJWT
    fun createToken(scope: DownstreamApi): SignedJWT = createToken(scope.tokenscope())
    fun bindTo(scope: String): BoundMachineToMachineTokenClient = object : BoundMachineToMachineTokenClient {
        override fun createToken(): SignedJWT = createToken(scope)
    }
    fun bindTo(scope: DownstreamApi): BoundMachineToMachineTokenClient = bindTo(scope.tokenscope())
}

fun interface BoundMachineToMachineTokenClient {
    fun createToken(): SignedJWT
}

fun interface OnBehalfOfTokenClient {
    fun exchangeToken(scope: String, accessToken: SignedJWT): SignedJWT
    fun exchangeToken(scope: DownstreamApi, accessToken: SignedJWT): SignedJWT = exchangeToken(scope.tokenscope(), accessToken)
    fun bindTo(scope: String): BoundOnBehalfOfTokenClient = object : BoundOnBehalfOfTokenClient {
        override fun exchangeToken(accessToken: SignedJWT): SignedJWT = exchangeToken(scope, accessToken)
    }
    fun bindTo(scope: DownstreamApi): BoundOnBehalfOfTokenClient = bindTo(scope.tokenscope())
}

fun interface BoundOnBehalfOfTokenClient {
    fun exchangeToken(accessToken: SignedJWT): SignedJWT
}

abstract class AbstractTokenClient(
    protected val clientId: String,
    tokenEndpoint: String,
    privateJwk: String,
    protected val tokenCache: TokenCache
) {
    protected val tokenEndpoint: URI = URI.create(tokenEndpoint)
    protected val privateJwkKeyId: String
    protected val assertionSigner: JWSSigner

    init {
        val rsaKey = RSAKey.parse(privateJwk)

        privateJwkKeyId = rsaKey.keyID
        assertionSigner = RSASSASigner(rsaKey)
    }
}
