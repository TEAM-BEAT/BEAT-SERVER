package com.beat.support.security.token

interface TokenIssuer {

    fun issueAccessToken(subject: TokenSubject): String

    fun issueRefreshToken(subject: TokenSubject): String
}
