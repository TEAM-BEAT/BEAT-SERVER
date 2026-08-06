package com.beat.admin.user.application.result

@JvmRecord
data class AdminUserResults(
    val users: List<AdminUserResult>,
) {
    @JvmRecord
    data class AdminUserResult(
        val id: Long,
        val role: String,
    )
}