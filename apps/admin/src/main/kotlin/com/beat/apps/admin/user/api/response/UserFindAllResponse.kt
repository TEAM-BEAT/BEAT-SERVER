package com.beat.apps.admin.user.api.response

import com.beat.application.admin.user.query.AdminUserResults
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 사용자 전체 조회 응답")
data class UserFindAllResponse(
    @field:Schema(
        description = "시스템에 등록된 사용자 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = """[{"id":1,"role":"ROLE_USER"}]""",
    )
    @get:JsonProperty("users")
    val userResponses: List<UserFindResponse>
) {
    constructor(
        results: AdminUserResults
    ) : this(results.users.map { UserFindResponse(it.id, it.role) })

    @Schema(description = "사용자 식별자와 권한 정보")
    data class UserFindResponse(
        @field:Schema(
            description = "사용자 식별자",
            format = "int64",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1",
        )
        val id: Long,
        @field:Schema(
            description = "사용자 권한",
            type = "string",
            allowableValues = ["ROLE_USER", "ROLE_MEMBER", "ROLE_ADMIN"],
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ROLE_USER",
        )
        val role: String,
    )
}
