package com.beat.apis.performance.api.response

import com.beat.application.frontoffice.performance.CastResult
import com.beat.apis.web.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class CastResponse private constructor(
    val castId: Long?,
    val castName: String?,
    val castRole: String?,
    @field:CdnImageUrl val castPhoto: String?,
) {
    companion object {
        fun from(result: CastResult): CastResponse = CastResponse(result.id, result.name, result.role, result.photo)
    }
}
