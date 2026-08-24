package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.StaffResult
import com.beat.apps.api.web.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class StaffResponse private constructor(
    val staffId: Long?,
    val staffName: String?,
    val staffRole: String?,
    @field:CdnImageUrl val staffPhoto: String?,
) {
    companion object {
        fun from(result: StaffResult): StaffResponse = StaffResponse(result.id, result.name, result.role, result.photo)
    }
}
