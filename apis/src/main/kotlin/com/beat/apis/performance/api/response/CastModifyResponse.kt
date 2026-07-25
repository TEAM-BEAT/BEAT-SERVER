package com.beat.apis.performance.api.response

import com.beat.apis.performance.application.result.CastResult
import com.beat.global.support.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class CastModifyResponse private constructor(
    val castId: Long?,
    val castName: String?,
    val castRole: String?,
    @field:CdnImageUrl val castPhoto: String?,
) {
    companion object {
        fun from(result: CastResult): CastModifyResponse = CastModifyResponse(result.id, result.name, result.role, result.photo)
    }
}
