package com.beat.apis.performance.api.response

import com.beat.global.support.jackson.CdnImageUrl

data class CastModifyResponse(
    val castId: Long?,
    val castName: String?,
    val castRole: String?,
    @field:CdnImageUrl val castPhoto: String?,
)
