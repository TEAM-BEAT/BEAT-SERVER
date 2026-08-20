package com.beat.support.security.jwt.internal

import com.beat.support.security.token.TokenSubject
import io.jsonwebtoken.Jwts
import java.time.Instant
import java.util.Date

/**
 * JWT 발급만 담당한다.
 */
internal class JwtTokenIssuer(
    private val signingKeyHolder: JwtSigningKeyHolder,
) {

    fun issue(subject: TokenSubject, expireTimeMillis: Long, tokenType: JwtTokenType): String {
        val issuedAt = Instant.now()
        val expiration = issuedAt.plusMillis(expireTimeMillis)

        return Jwts.builder()
            .header()
            .keyId(signingKeyHolder.keyId)
            .and()
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiration))
            .claim(JwtClaimNames.MEMBER_ID, subject.memberId)
            .claim(JwtClaimNames.ROLE, subject.roleName)
            .claim(JwtClaimNames.TOKEN_TYPE, tokenType.name)
            .signWith(signingKeyHolder.signingKey)
            .compact()
    }
}
