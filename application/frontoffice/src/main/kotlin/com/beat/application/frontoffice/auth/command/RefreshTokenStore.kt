package com.beat.application.frontoffice.auth.command

interface RefreshTokenStore {
    fun save(memberId: Long, refreshToken: String)

    fun findMemberIdByRefreshToken(refreshToken: String): Long?

    fun delete(memberId: Long): Boolean
}
