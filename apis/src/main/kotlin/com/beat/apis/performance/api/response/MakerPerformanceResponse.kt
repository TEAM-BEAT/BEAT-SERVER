package com.beat.apis.performance.api.response

import com.beat.application.frontoffice.performance.maker.query.MakerPerformanceListResult

@ConsistentCopyVisibility
data class MakerPerformanceResponse private constructor(
    val userId: Long?,
    val performances: List<MakerPerformanceDetailResponse>,
) {
    companion object {
        fun from(result: MakerPerformanceListResult): MakerPerformanceResponse =
            MakerPerformanceResponse(result.userId, result.performances.map(MakerPerformanceDetailResponse::from))
    }
}
