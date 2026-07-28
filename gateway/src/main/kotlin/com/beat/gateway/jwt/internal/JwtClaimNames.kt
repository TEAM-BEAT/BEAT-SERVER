package com.beat.gateway.jwt.internal

/**
 * JWT claim 이름 상수. 발급([JwtTokenIssuer])과 파싱([JwtTokenParser])이 같은 계약을 공유한다.
 */
object JwtClaimNames {

    const val MEMBER_ID = "memberId"
    const val ROLE = "role"
    const val TOKEN_TYPE = "tokenType"
}
