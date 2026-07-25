package com.beat.contracts.auth

import java.util.OptionalLong

interface RefreshTokenPort {

    fun saveRefreshToken(memberId: Long, refreshToken: String)

    fun findMemberIdByRefreshToken(refreshToken: String): OptionalLong

    fun deleteRefreshToken(memberId: Long): Boolean
}
