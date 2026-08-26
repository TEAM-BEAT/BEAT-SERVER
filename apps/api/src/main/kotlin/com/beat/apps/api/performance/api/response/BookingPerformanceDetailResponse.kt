package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.booker.query.BookingPerformanceDetailResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "예매 화면에 표시할 공연 상세정보")
@ConsistentCopyVisibility
data class BookingPerformanceDetailResponse
private constructor(
    @field:Schema(
        description = "공연 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val performanceId: Long?,
    @field:Schema(
        description = "공연 제목",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "가을 밤 콘서트",
    )
    val performanceTitle: String?,
    @field:Schema(
        description = "회차 날짜의 최솟값과 최댓값으로 계산한 공연 기간(yyyy.MM.dd 또는 yyyy.MM.dd~yyyy.MM.dd)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "2026.09.01~2026.09.03",
    )
    val performancePeriod: String?,
    @field:Schema(
        description = "공연 회차별 예매 정보 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val scheduleList: List<BookingPerformanceDetailScheduleResponse>,
    @field:Schema(
        description = "티켓 가격(원)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "30000",
    )
    val ticketPrice: Int,
    @field:Schema(
        description = "공연 장르",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BAND",
    )
    val genre: String?,
    @field:Schema(
        description = "포스터 이미지의 CDN URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "https://cdn.example.com/prod/poster/performance-1.jpg",
    )
    @field:CdnImageUrl
    val posterImage: String?,
    @field:Schema(
        description = "공연 장소명",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "홍대 라이브홀",
    )
    val performanceVenue: String?,
    @field:Schema(
        description = "공연 팀명",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BEAT 밴드",
    )
    val performanceTeamName: String?,
    @field:Schema(
        description = "유료 공연의 입금 은행이며, 무료 공연에서는 null입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "KAKAOBANK",
    )
    val bankName: String?,
    @field:Schema(
        description = "유료 공연의 입금 계좌번호이며, 무료 공연에서는 null입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "3333-01-1234567",
    )
    val accountNumber: String?,
    @field:Schema(
        description = "유료 공연의 입금 계좌 예금주이며, 무료 공연에서는 null입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BEAT 운영팀",
    )
    val accountHolder: String?,
) {
    companion object {
        fun from(result: BookingPerformanceDetailResult): BookingPerformanceDetailResponse =
            BookingPerformanceDetailResponse(
                performanceId = result.performanceId,
                performanceTitle = result.performanceTitle,
                performancePeriod = result.performancePeriod,
                scheduleList = result.schedules.map(BookingPerformanceDetailScheduleResponse::from),
                ticketPrice = result.ticketPrice,
                genre = result.genre,
                posterImage = result.posterImage,
                performanceVenue = result.performanceVenue,
                performanceTeamName = result.performanceTeamName,
                bankName = result.bankName,
                accountNumber = result.accountNumber,
                accountHolder = result.accountHolder,
            )
    }
}
