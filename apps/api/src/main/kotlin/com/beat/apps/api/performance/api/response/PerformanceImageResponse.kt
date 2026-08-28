package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.maker.command.result.PerformanceImageResult as CommandPerformanceImageResult
import com.beat.application.frontoffice.performance.maker.query.PerformanceImageResult as QueryPerformanceImageResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 생성 결과의 이미지 정보")
@ConsistentCopyVisibility
data class PerformanceImageResponse
private constructor(
    @field:Schema(
        description = "공연 이미지 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val imageId: Long,
    @field:Schema(
        description = "공연 이미지의 CDN URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "https://cdn.example.com/prod/performance/detail-1.jpg",
    )
    @field:CdnImageUrl
    val imageUrl: String,
) {
    companion object {
        fun from(result: CommandPerformanceImageResult): PerformanceImageResponse =
            PerformanceImageResponse(
                imageId =
                    requireNotNull(result.id) {
                        "PerformanceImageResponse.imageId must be present for a persisted image"
                    },
                imageUrl =
                    requireNotNull(result.image) {
                        "PerformanceImageResponse.imageUrl must be present"
                    },
            )

        fun from(result: QueryPerformanceImageResult): PerformanceImageResponse =
            PerformanceImageResponse(
                imageId =
                    requireNotNull(result.id) {
                        "PerformanceImageResponse.imageId must be present for a persisted image"
                    },
                imageUrl =
                    requireNotNull(result.image) {
                        "PerformanceImageResponse.imageUrl must be present"
                    },
            )
    }
}
