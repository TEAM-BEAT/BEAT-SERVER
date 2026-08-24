package com.beat.support.security.token

data class TokenSubject(
    val memberId: Long,
    val roleName: String,
)
