package com.beat.application.admin.user.query

data class AdminUserResults(val users: List<AdminUserResult>) {
    data class AdminUserResult(
        val id: Long,
        val role: String,
    )
}
