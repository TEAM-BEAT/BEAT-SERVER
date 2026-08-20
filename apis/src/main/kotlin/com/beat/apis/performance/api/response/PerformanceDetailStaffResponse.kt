package com.beat.apis.performance.api.response

import com.beat.application.frontoffice.performance.StaffResult
import com.beat.global.support.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class PerformanceDetailStaffResponse private constructor(
    val staffId: Long?,
    val staffName: String?,
    val staffRole: String?,
    @field:CdnImageUrl val staffPhoto: String?,
) {
    companion object {
        fun from(result: StaffResult): PerformanceDetailStaffResponse =
            PerformanceDetailStaffResponse(result.id, result.name, result.role, result.photo)
    }
}
