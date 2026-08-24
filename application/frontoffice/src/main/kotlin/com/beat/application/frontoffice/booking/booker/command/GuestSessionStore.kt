package com.beat.application.frontoffice.booking.booker.command

interface GuestSessionStore {
    fun issue(userId: Long): String

    fun findUserId(token: String): Long?
}
