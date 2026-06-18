package com.beat.contracts.auth

data class JwtSubject(
    val memberId: Long,
    val roleName: String,
)
