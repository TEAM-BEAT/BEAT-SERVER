package com.beat.apps.api.performance.api.response

import com.beat.application.frontoffice.performance.maker.query.PerformanceEditResult
import com.beat.apps.api.performance.api.type.BankNameType
import com.beat.apps.api.performance.api.type.GenreType
import com.beat.apps.api.web.jackson.CdnImageUrl
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 수정 화면에 표시할 공연 및 구성 요소 정보")
@ConsistentCopyVisibility
data class PerformanceModifyDetailResponse
private constructor(
    @field:Schema(
        description = "공연을 등록한 회원 식별자",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val userId: Long?,
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
        description = "공연 장르",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BAND",
    )
    val genre: GenreType?,
    @field:Schema(
        description = "공연 러닝타임(분)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "90",
    )
    val runningTime: Int,
    @field:Schema(
        description = "공연 소개",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "가을밤의 라이브 공연을 소개합니다.",
    )
    val performanceDescription: String?,
    @field:Schema(
        description = "공연 유의사항",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "공연 시작 10분 전까지 입장해 주세요.",
    )
    val performanceAttentionNote: String?,
    @field:Schema(
        description = "유료 공연의 입금 은행이며, 무료 공연(ticketPrice = 0)에서는 null입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "KAKAOBANK",
    )
    val bankName: BankNameType?,
    @field:Schema(
        description = "유료 공연의 입금 계좌번호이며, 무료 공연(ticketPrice = 0)에서는 null입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "3333-01-1234567",
    )
    val accountNumber: String?,
    @field:Schema(
        description = "유료 공연의 입금 계좌 예금주이며, 무료 공연(ticketPrice = 0)에서는 null입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BEAT 운영팀",
    )
    val accountHolder: String?,
    @field:Schema(
        description = "포스터 이미지의 CDN URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "https://cdn.example.com/prod/poster/performance-1.jpg",
    )
    @field:CdnImageUrl
    val posterImage: String?,
    @field:Schema(
        description = "공연 팀명",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BEAT 밴드",
    )
    val performanceTeamName: String?,
    @field:Schema(
        description = "공연 장소명",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "홍대 라이브홀",
    )
    val performanceVenue: String?,
    @field:Schema(
        description = "공연 장소 도로명 주소",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "서울특별시 마포구 양화로 123",
    )
    val roadAddressName: String?,
    @field:Schema(
        description = "공연 장소 상세 주소",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "지하 1층",
    )
    val placeDetailAddress: String?,
    @field:Schema(
        description = "공연 장소 위도(십진수 문자열)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "37.5665",
    )
    val latitude: String?,
    @field:Schema(
        description = "공연 장소 경도(십진수 문자열)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "126.9780",
    )
    val longitude: String?,
    @field:Schema(
        description = "공연 문의 연락처",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "010-1234-5678",
    )
    val performanceContact: String?,
    @field:Schema(
        description = "회차 날짜의 최솟값과 최댓값으로 계산한 공연 기간(yyyy.MM.dd 또는 yyyy.MM.dd~yyyy.MM.dd)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "2026.09.01~2026.09.03",
    )
    val performancePeriod: String?,
    @field:Schema(
        description = "티켓 가격(원)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "30000",
    )
    val ticketPrice: Int,
    @field:Schema(
        description = "저장된 공연 회차 목록의 개수로 계산한 전체 회차 수",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "3",
    )
    val totalScheduleCount: Int,
    @field:Schema(
        description = "활성 예매 내역의 존재 여부. true이면 예매가 있어 일부 수정·삭제가 제한될 수 있습니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "false",
    )
    @get:JsonProperty("isBookerExist")
    val isBookerExist: Boolean,
    @field:Schema(
        description = "공연 회차 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val scheduleList: List<ScheduleResponse>,
    @field:Schema(
        description = "공연 출연진 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val castList: List<CastResponse>,
    @field:Schema(
        description = "공연 스태프 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val staffList: List<StaffResponse>,
    @field:Schema(
        description = "공연 상세 이미지 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val performanceImageList: List<PerformanceImageResponse>,
) {
    companion object {
        fun from(result: PerformanceEditResult): PerformanceModifyDetailResponse {
            val performance = result.performance
            return PerformanceModifyDetailResponse(
                userId = performance.userId,
                performanceId = performance.performanceId,
                performanceTitle = performance.performanceTitle,
                genre = performance.genre?.let(GenreType::valueOf),
                runningTime = performance.runningTime,
                performanceDescription = performance.performanceDescription,
                performanceAttentionNote = performance.performanceAttentionNote,
                bankName = performance.bankName?.let(BankNameType::valueOf),
                accountNumber = performance.accountNumber,
                accountHolder = performance.accountHolder,
                posterImage = performance.posterImage,
                performanceTeamName = performance.performanceTeamName,
                performanceVenue = performance.performanceVenue,
                roadAddressName = performance.roadAddressName,
                placeDetailAddress = performance.placeDetailAddress,
                latitude = performance.latitude,
                longitude = performance.longitude,
                performanceContact = performance.performanceContact,
                performancePeriod = performance.performancePeriod,
                ticketPrice = performance.ticketPrice,
                totalScheduleCount = performance.totalScheduleCount,
                isBookerExist = result.isBookerExist,
                scheduleList = performance.schedules.map(ScheduleResponse::from),
                castList = performance.casts.map(CastResponse::from),
                staffList = performance.staffs.map(StaffResponse::from),
                performanceImageList = performance.images.map(PerformanceImageResponse::from),
            )
        }
    }
}
