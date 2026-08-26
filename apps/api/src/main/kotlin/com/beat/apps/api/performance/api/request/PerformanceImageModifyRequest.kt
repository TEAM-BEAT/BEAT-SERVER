package com.beat.apps.api.performance.api.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 수정 시 공연 이미지 한 장의 정보")
data class PerformanceImageModifyRequest(
    @field:Schema(
        description = "기존 공연 이미지의 식별자이며, 신규 이미지를 추가할 때는 null입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "1",
    )
    val performanceImageId: Long?,
    @field:Schema(
        description = "업로드된 공연 이미지의 이미지 key 또는 절대 URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "dev/performance/detail-1.jpg",
    )
    val performanceImage: String,
)
