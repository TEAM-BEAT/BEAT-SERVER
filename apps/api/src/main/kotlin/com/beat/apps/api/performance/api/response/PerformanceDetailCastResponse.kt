package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.CastResult
import com.beat.apps.api.web.jackson.CdnImageUrl

@ConsistentCopyVisibility
data class PerformanceDetailCastResponse
private constructor(
    val castId: Long?,
    val castName: String?,
    val castRole: String?,
    @field:CdnImageUrl val castPhoto: String?,
) {
    companion object {
        fun from(result: CastResult): PerformanceDetailCastResponse =
            PerformanceDetailCastResponse(result.id, result.name, result.role, result.photo)
    }
}
