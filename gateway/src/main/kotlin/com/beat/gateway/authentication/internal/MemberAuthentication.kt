package com.beat.gateway.authentication.internal

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * `ROLE_MEMBER` 인증 토큰. principal은 항상 사용자 memberId다.
 */
class MemberAuthentication(
    memberId: Long,
    authorities: Collection<GrantedAuthority>,
) : UsernamePasswordAuthenticationToken(memberId, null, authorities)
