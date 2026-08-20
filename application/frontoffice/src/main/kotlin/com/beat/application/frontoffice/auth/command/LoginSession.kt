package com.beat.application.frontoffice.auth.command

data class LoginSession(
    val accessToken: String,
    val refreshToken: String,
)
