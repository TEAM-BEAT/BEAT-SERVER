package com.beat.application.frontoffice.security

data class TokenSubject(
    val memberId: Long,
    val roleName: String,
)
