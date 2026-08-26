package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.maker.query.MakerPerformanceResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원 등록 공연 목록의 개별 공연 정보")
@ConsistentCopyVisibility
data class MakerPerformanceDetailResponse
private constructor(
    @field:Schema(
        description = "공연 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val performanceId: Long?,
    @field:Schema(
        description = "공연 장르",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BAND",
    )
    val genre: String?,
    @field:Schema(
        description = "공연 제목",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "가을 밤 콘서트",
    )
    val performanceTitle: String?,
    @field:Schema(
        description = "포스터 이미지의 CDN URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "https://cdn.example.com/prod/poster/performance-1.jpg",
    )
    @field:CdnImageUrl
    val posterImage: String?,
    @field:Schema(
        description = "회차 날짜의 최솟값과 최댓값으로 계산한 공연 기간(yyyy.MM.dd 또는 yyyy.MM.dd~yyyy.MM.dd)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "2026.09.01~2026.09.03",
    )
    val performancePeriod: String?,
    @field:Schema(
        description = "대표 회차 공연일까지 남은 일수이며, 과거 회차는 음수입니다. 대표 회차가 없으면 2147483647입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "3",
    )
    val minDueDate: Int,
) {
    companion object {
        fun from(result: MakerPerformanceResult): MakerPerformanceDetailResponse =
            MakerPerformanceDetailResponse(
                performanceId = result.performanceId,
                genre = result.genre,
                performanceTitle = result.performanceTitle,
                posterImage = result.posterImage,
                performancePeriod = result.performancePeriod,
                minDueDate = result.minDueDate,
            )
    }
}
