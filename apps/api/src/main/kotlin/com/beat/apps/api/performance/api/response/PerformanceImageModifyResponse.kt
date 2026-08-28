package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.maker.command.result.PerformanceImageResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 수정 결과의 이미지 정보")
@ConsistentCopyVisibility
data class PerformanceImageModifyResponse
private constructor(
    @field:Schema(
        description = "공연 이미지 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val performanceImageId: Long,
    @field:Schema(
        description = "공연 이미지의 CDN URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "https://cdn.example.com/prod/performance/detail-1.jpg",
    )
    @field:CdnImageUrl
    val performanceImage: String,
) {
    companion object {
        fun from(result: PerformanceImageResult): PerformanceImageModifyResponse =
            PerformanceImageModifyResponse(
                performanceImageId =
                    requireNotNull(result.id) {
                        "PerformanceImageModifyResponse.performanceImageId must be present for a persisted image"
                    },
                performanceImage =
                    requireNotNull(result.image) {
                        "PerformanceImageModifyResponse.performanceImage must be present"
                    },
            )
    }
}
