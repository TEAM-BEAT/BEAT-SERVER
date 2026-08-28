package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.booker.query.PerformanceImageResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 상세정보의 이미지 정보")
@ConsistentCopyVisibility
data class PerformanceDetailImageResponse
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
        fun from(result: PerformanceImageResult): PerformanceDetailImageResponse =
            PerformanceDetailImageResponse(
                performanceImageId =
                    requireNotNull(result.id) {
                        "PerformanceDetailImageResponse.performanceImageId must be present for a persisted image"
                    },
                performanceImage =
                    requireNotNull(result.image) {
                        "PerformanceDetailImageResponse.performanceImage must be present"
                    },
            )
    }
}
