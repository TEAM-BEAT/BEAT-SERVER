package com.beat.contracts.auth


interface JwtTokenPort {

    fun issueAccessToken(subject: JwtSubject): String

    fun issueRefreshToken(subject: JwtSubject): String

    fun validateAccessToken(token: String): TokenValidationResult

    fun validateRefreshToken(token: String): TokenValidationResult

    fun getMemberId(token: String, expectedType: JwtTokenType): Long?

    fun getRoleName(token: String, expectedType: JwtTokenType): String?
}
