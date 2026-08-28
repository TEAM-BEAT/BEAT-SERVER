package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.StaffResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 상세정보의 스태프 정보")
@ConsistentCopyVisibility
data class PerformanceDetailStaffResponse
private constructor(
    @field:Schema(
        description = "스태프 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val staffId: Long,
    @field:Schema(
        description = "스태프 이름",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "김기획",
    )
    val staffName: String,
    @field:Schema(
        description = "스태프 역할",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "연출",
    )
    val staffRole: String,
    @field:Schema(
        description = "스태프 사진의 CDN URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "https://cdn.example.com/prod/staff/staff-1.jpg",
    )
    @field:CdnImageUrl
    val staffPhoto: String,
) {
    companion object {
        fun from(result: StaffResult): PerformanceDetailStaffResponse =
            PerformanceDetailStaffResponse(
                staffId =
                    requireNotNull(result.id) {
                        "PerformanceDetailStaffResponse.staffId must be present for a persisted staff"
                    },
                staffName =
                    requireNotNull(result.name) {
                        "PerformanceDetailStaffResponse.staffName must be present"
                    },
                staffRole =
                    requireNotNull(result.role) {
                        "PerformanceDetailStaffResponse.staffRole must be present"
                    },
                staffPhoto =
                    requireNotNull(result.photo) {
                        "PerformanceDetailStaffResponse.staffPhoto must be present"
                    },
            )
    }
}
