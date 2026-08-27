package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.booker.query.PerformanceDetailResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 상세 페이지의 공연 정보")
@ConsistentCopyVisibility
data class PerformanceDetailResponse
private constructor(
    @field:Schema(
        description = "공연 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val performanceId: Long,
    @field:Schema(
        description = "공연 제목",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "가을 밤 콘서트",
    )
    val performanceTitle: String,
    @field:Schema(
        description = "회차 날짜의 최솟값과 최댓값으로 계산한 공연 기간(yyyy.MM.dd 또는 yyyy.MM.dd~yyyy.MM.dd)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "2026.09.01~2026.09.03",
    )
    val performancePeriod: String,
    @field:Schema(
        description = "공연 회차별 예매 상태 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val scheduleList: List<PerformanceDetailScheduleResponse>,
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
    val genre: String,
    @field:Schema(
        description = "포스터 이미지의 CDN URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "https://cdn.example.com/prod/poster/performance-1.jpg",
    )
    @field:CdnImageUrl
    val posterImage: String,
    @field:Schema(
        description = "공연 러닝타임(분)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "90",
    )
    val runningTime: Int,
    @field:Schema(
        description = "공연 장소명",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "홍대 라이브홀",
    )
    val performanceVenue: String,
    @field:Schema(
        description = "공연 장소 도로명 주소",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "서울특별시 마포구 양화로 123",
    )
    val roadAddressName: String,
    @field:Schema(
        description = "공연 장소 상세 주소",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "지하 1층",
    )
    val placeDetailAddress: String,
    @field:Schema(
        description = "공연 장소 위도(십진수 문자열)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "37.5665",
    )
    val latitude: String,
    @field:Schema(
        description = "공연 장소 경도(십진수 문자열)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "126.9780",
    )
    val longitude: String,
    @field:Schema(
        description = "공연 소개",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "가을밤의 라이브 공연을 소개합니다.",
    )
    val performanceDescription: String,
    @field:Schema(
        description = "공연 유의사항",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "공연 시작 10분 전까지 입장해 주세요.",
    )
    val performanceAttentionNote: String,
    @field:Schema(
        description = "공연 문의 연락처",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "010-1234-5678",
    )
    val performanceContact: String,
    @field:Schema(
        description = "공연 팀명",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BEAT 밴드",
    )
    val performanceTeamName: String,
    @field:Schema(
        description = "공연 출연진 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val castList: List<PerformanceDetailCastResponse>,
    @field:Schema(
        description = "공연 스태프 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val staffList: List<PerformanceDetailStaffResponse>,
    @field:Schema(
        description = "전체 회차 중 가장 가까운 공연일까지 남은 일수이며, 과거 회차는 음수입니다. 회차가 없으면 2147483647입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "3",
    )
    val minDueDate: Int,
    @field:Schema(
        description = "공연 상세 이미지 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val performanceImageList: List<PerformanceDetailImageResponse>,
) {
    companion object {
        fun from(result: PerformanceDetailResult): PerformanceDetailResponse =
            PerformanceDetailResponse(
                performanceId =
                    requireNotNull(result.performanceId) {
                        "PerformanceDetailResponse.performanceId must be present for a persisted performance"
                    },
                performanceTitle =
                    requireNotNull(result.performanceTitle) {
                        "PerformanceDetailResponse.performanceTitle must be present"
                    },
                performancePeriod =
                    requireNotNull(result.performancePeriod) {
                        "PerformanceDetailResponse.performancePeriod must be present"
                    },
                scheduleList = result.schedules.map(PerformanceDetailScheduleResponse::from),
                ticketPrice = result.ticketPrice,
                genre =
                    requireNotNull(result.genre) {
                        "PerformanceDetailResponse.genre must be present"
                    },
                posterImage =
                    requireNotNull(result.posterImage) {
                        "PerformanceDetailResponse.posterImage must be present"
                    },
                runningTime = result.runningTime,
                performanceVenue =
                    requireNotNull(result.performanceVenue) {
                        "PerformanceDetailResponse.performanceVenue must be present"
                    },
                roadAddressName =
                    requireNotNull(result.roadAddressName) {
                        "PerformanceDetailResponse.roadAddressName must be present"
                    },
                placeDetailAddress =
                    requireNotNull(result.placeDetailAddress) {
                        "PerformanceDetailResponse.placeDetailAddress must be present"
                    },
                latitude =
                    requireNotNull(result.latitude) {
                        "PerformanceDetailResponse.latitude must be present"
                    },
                longitude =
                    requireNotNull(result.longitude) {
                        "PerformanceDetailResponse.longitude must be present"
                    },
                performanceDescription =
                    requireNotNull(result.performanceDescription) {
                        "PerformanceDetailResponse.performanceDescription must be present"
                    },
                performanceAttentionNote =
                    requireNotNull(result.performanceAttentionNote) {
                        "PerformanceDetailResponse.performanceAttentionNote must be present"
                    },
                performanceContact =
                    requireNotNull(result.performanceContact) {
                        "PerformanceDetailResponse.performanceContact must be present"
                    },
                performanceTeamName =
                    requireNotNull(result.performanceTeamName) {
                        "PerformanceDetailResponse.performanceTeamName must be present"
                    },
                castList = result.casts.map(PerformanceDetailCastResponse::from),
                staffList = result.staffs.map(PerformanceDetailStaffResponse::from),
                minDueDate = result.minDueDate,
                performanceImageList = result.images.map(PerformanceDetailImageResponse::from),
            )
    }
}
