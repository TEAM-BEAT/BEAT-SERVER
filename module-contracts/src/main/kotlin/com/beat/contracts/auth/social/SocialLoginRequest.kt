package com.beat.contracts.auth.social


data class SocialLoginRequest(
    val authorizationCode: String,
    val socialType: SocialLoginType,
)
