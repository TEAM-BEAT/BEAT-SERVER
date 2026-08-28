package com.beat.support.security.authentication.internal

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/** `ROLE_ADMIN` 인증 토큰. principal은 항상 관리자 memberId다. */
internal class AdminAuthentication(
    memberId: Long,
    authorities: Collection<GrantedAuthority>,
) : UsernamePasswordAuthenticationToken(memberId, null, authorities)
