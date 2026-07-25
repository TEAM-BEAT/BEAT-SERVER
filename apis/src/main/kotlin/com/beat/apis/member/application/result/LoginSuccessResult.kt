package com.beat.apis.member.application.result

data class LoginSuccessResult(
    val accessToken: String?,
    val refreshToken: String?,
    val nickname: String?,
    val role: String?,
)
