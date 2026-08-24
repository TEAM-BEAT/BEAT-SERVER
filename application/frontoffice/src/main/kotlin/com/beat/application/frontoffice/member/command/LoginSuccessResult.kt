package com.beat.application.frontoffice.member.command

data class LoginSuccessResult(
    val accessToken: String,
    val refreshToken: String,
    val nickname: String,
    val role: String,
)
