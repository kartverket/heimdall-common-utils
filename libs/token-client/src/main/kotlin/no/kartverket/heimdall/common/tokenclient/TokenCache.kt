package no.kartverket.heimdall.common.tokenclient

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.SignedJWT
import java.time.Duration

fun interface TokenCache {
    fun getFromCacheOrTryProvider(cacheKey: String, tokenProvider: () -> SignedJWT): SignedJWT
}

class CaffeineTokenCache(
    private val earlyRefreshThreshold: Duration = Duration.ofSeconds(30),
    private val cache: Cache<String, SignedJWT> = Caffeine
        .newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build()
) : TokenCache {
    companion object {
        fun shouldRefreshToken(token: JWT, earlyRefreshThreshold: Duration): Boolean {
            return runCatching {
                val expiration = token.jwtClaimsSet.expirationTime
                if (expiration == null) return true

                val earlyExpiration = expiration.time - earlyRefreshThreshold.toMillis()

                return System.currentTimeMillis() > earlyExpiration
            }.getOrDefault(true)
        }
    }

    override fun getFromCacheOrTryProvider(cacheKey: String, tokenProvider: () -> SignedJWT): SignedJWT {
        // Use ConcurrentMap#compute for an atomic check-and-refresh so concurrent callers
        // for the same cacheKey don't each trigger their own token request (cache stampede).
        return requireNotNull(
            cache.asMap().compute(cacheKey) { _, existing ->
                if (existing == null) tokenProvider()
                else if (shouldRefreshToken(existing, earlyRefreshThreshold)) tokenProvider()
                else existing
            }
        )
    }
}
