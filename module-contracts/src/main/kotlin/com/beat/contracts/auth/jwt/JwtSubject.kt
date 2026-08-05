package com.beat.contracts.auth.jwt

data class JwtSubject(
    val memberId: Long,
    val roleName: String,
)
