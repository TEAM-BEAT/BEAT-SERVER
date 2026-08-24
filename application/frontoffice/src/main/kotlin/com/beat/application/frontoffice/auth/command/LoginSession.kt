package com.beat.application.frontoffice.auth.command

internal data class LoginSession(
    val accessToken: String,
    val refreshToken: String,
)
