package com.beat.admin.user.api.response

import com.beat.application.admin.user.query.AdminUserResults
import com.fasterxml.jackson.annotation.JsonProperty

data class UserFindAllResponse(
    @get:JsonProperty("users")
    val userResponses: List<UserFindResponse>,
) {
    constructor(results: AdminUserResults) : this(results.users.map { UserFindResponse(it.id, it.role) })

    data class UserFindResponse(
        val id: Long,
        val role: String,
    )
}
