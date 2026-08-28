package com.beat.support.security.jwt.internal

import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import javax.crypto.SecretKey

/**
 * 서명 키 소유자. Base64 디코딩과 HMAC 키 파생은 비용이 있으므로 한 번만 수행하고 재사용한다.
 *
 * 값 자체의 존재/범위 검증은 [JwtProperties]의 Bean Validation이 담당하고, 이 클래스는 "그 값으로 실제 서명 키를 만들 수 있는지"만 기동 시점에
 * 검증한다.
 */
internal class JwtSigningKeyHolder(private val jwtProperties: JwtProperties) {

    internal val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret))
    }

    val keyId: String
        get() = jwtProperties.keyId

    @PostConstruct
    fun validateSigningKey() {
        signingKey
    }
}
