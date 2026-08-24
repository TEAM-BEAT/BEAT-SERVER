package com.beat.application.frontoffice.member.command

data class SocialLoginRequest(
    val authorizationCode: String,
    val socialType: SocialLoginType,
)
