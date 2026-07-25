package com.beat.contracts.auth.guest

import java.util.OptionalLong

interface GuestSessionPort {

    fun issue(userId: Long): String

    fun findUserId(token: String): OptionalLong
}
