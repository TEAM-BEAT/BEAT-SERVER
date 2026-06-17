package com.beat.contracts.auth

interface RefreshTokenPort {

    fun saveRefreshToken(memberId: Long, refreshToken: String)

    fun findMemberIdByRefreshToken(refreshToken: String): Long

    fun deleteRefreshToken(memberId: Long)
}
