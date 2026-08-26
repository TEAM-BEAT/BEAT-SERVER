package com.beat.apps.api.home.api.response

import com.beat.application.frontoffice.home.booker.query.HomePerformanceResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "홈 화면에 표시할 공연 요약 정보입니다.")
@ConsistentCopyVisibility
data class HomePerformanceDetail
private constructor(
    @field:Schema(
        description = "공연 식별자입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "11",
    )
    val performanceId: Long?,
    @field:Schema(
        description = "공연 제목입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "title",
    )
    val performanceTitle: String?,
    @field:Schema(
        description = "공연 기간입니다. 단일 날짜는 yyyy.MM.dd, 여러 날짜는 시작일~종료일 형식입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "2026.08.25",
    )
    val performancePeriod: String?,
    @field:Schema(
        description = "공연 티켓 가격입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "30000",
    )
    val ticketPrice: Int,
    @field:Schema(
        description = "공연일까지 남은 일수입니다. 공연일이 지나면 음수이며 공연일이 없으면 매우 큰 값으로 표시됩니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "3",
    )
    val dueDate: Int,
    @field:Schema(
        description = "공연 장르입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BAND",
    )
    val genre: String?,
    @field:Schema(
        description = "공연 포스터 이미지 경로입니다. 응답 시 CDN 설정에 따라 CDN URL로 직렬화됩니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "poster.png",
    )
    @field:CdnImageUrl
    val posterImage: String?,
    @field:Schema(
        description = "공연 장소입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "venue",
    )
    val performanceVenue: String?,
) {
    companion object {
        fun from(result: HomePerformanceResult): HomePerformanceDetail =
            HomePerformanceDetail(
                performanceId = result.performanceId,
                performanceTitle = result.performanceTitle,
                performancePeriod = result.performancePeriod,
                ticketPrice = result.ticketPrice,
                dueDate = result.dueDate,
                genre = result.genre,
                posterImage = result.posterImage,
                performanceVenue = result.performanceVenue,
            )
    }
}
