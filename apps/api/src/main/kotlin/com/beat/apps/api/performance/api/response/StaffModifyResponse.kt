package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.StaffResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 수정 결과의 스태프 정보")
@ConsistentCopyVisibility
data class StaffModifyResponse
private constructor(
    @field:Schema(
        description = "스태프 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val staffId: Long?,
    @field:Schema(
        description = "스태프 이름",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "김기획",
    )
    val staffName: String?,
    @field:Schema(
        description = "스태프 역할",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "연출",
    )
    val staffRole: String?,
    @field:Schema(
        description = "스태프 사진의 CDN URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "https://cdn.example.com/prod/staff/staff-1.jpg",
    )
    @field:CdnImageUrl
    val staffPhoto: String?,
) {
    companion object {
        fun from(result: StaffResult): StaffModifyResponse =
            StaffModifyResponse(result.id, result.name, result.role, result.photo)
    }
}
