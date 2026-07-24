package com.beat.apis.performance.api.response

import com.beat.apis.performance.application.result.StaffResult
import com.beat.global.support.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class StaffModifyResponse private constructor(
    val staffId: Long?,
    val staffName: String?,
    val staffRole: String?,
    @field:CdnImageUrl val staffPhoto: String?,
) {
    companion object {
        fun from(result: StaffResult): StaffModifyResponse = StaffModifyResponse(result.id, result.name, result.role, result.photo)
    }
}
