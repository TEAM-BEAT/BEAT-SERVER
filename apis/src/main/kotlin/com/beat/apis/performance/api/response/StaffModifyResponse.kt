package com.beat.apis.performance.api.response

import com.beat.global.support.jackson.CdnImageUrl

data class StaffModifyResponse(
    val staffId: Long?,
    val staffName: String?,
    val staffRole: String?,
    @field:CdnImageUrl val staffPhoto: String?,
)
