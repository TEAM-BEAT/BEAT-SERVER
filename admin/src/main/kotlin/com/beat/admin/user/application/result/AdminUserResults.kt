package com.beat.admin.user.application.result

data class AdminUserResults(
    val users: List<AdminUserResult>,
) {
    data class AdminUserResult(
        val id: Long,
        val role: String,
    )
}