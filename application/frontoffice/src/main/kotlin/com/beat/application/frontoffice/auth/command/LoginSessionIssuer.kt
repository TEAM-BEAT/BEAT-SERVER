package com.beat.application.frontoffice.auth.command

interface LoginSessionIssuer {
    fun issueFor(memberId: Long, roleName: String): LoginSession
}
