package com.beat.application.frontoffice.security

interface TokenIssuer {
    fun issueAccessToken(subject: TokenSubject): String
    fun issueRefreshToken(subject: TokenSubject): String
}
