package com.beat.application.frontoffice.auth.command

internal interface LoginSessionIssuer {
    fun issueFor(memberId: Long, roleName: String): LoginSession
}
