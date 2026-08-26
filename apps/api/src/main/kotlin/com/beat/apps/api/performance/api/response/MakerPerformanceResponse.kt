package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.maker.query.MakerPerformanceListResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원이 등록한 공연 목록 응답")
@ConsistentCopyVisibility
data class MakerPerformanceResponse
private constructor(
    @field:Schema(
        description = "공연을 등록한 회원 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val userId: Long?,
    @field:Schema(
        description = "회원이 등록한 공연 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val performances: List<MakerPerformanceDetailResponse>,
) {
    companion object {
        fun from(result: MakerPerformanceListResult): MakerPerformanceResponse =
            MakerPerformanceResponse(
                result.userId,
                result.performances.map(MakerPerformanceDetailResponse::from),
            )
    }
}
