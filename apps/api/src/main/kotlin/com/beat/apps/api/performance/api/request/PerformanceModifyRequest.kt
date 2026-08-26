package com.beat.apps.api.performance.api.request

import com.beat.apps.api.performance.api.type.BankNameType
import com.beat.apps.api.performance.api.type.GenreType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

@Schema(description = "공연 수정 요청 정보")
data class PerformanceModifyRequest(
    @field:Schema(
        description = "수정할 공연 식별자",
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
        description = "공연 장르",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BAND",
    )
    val genre: GenreType,
    @field:Schema(
        description = "공연 러닝타임(분)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "90",
    )
    val runningTime: Int,
    @field:Size(max = 1500, message = "공연 소개는 1500자를 초과할 수 없습니다.")
    @field:Schema(
        description = "공연 소개(최대 1500자)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "가을밤의 라이브 공연을 소개합니다.",
    )
    val performanceDescription: String,
    @field:Size(max = 1500, message = "공연 유의사항은 1500자를 초과할 수 없습니다.")
    @field:Schema(
        description = "공연 유의사항(최대 1500자)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "공연 시작 10분 전까지 입장해 주세요.",
    )
    val performanceAttentionNote: String,
    @field:Schema(
        description = "유료 공연(ticketPrice > 0)의 입금 은행입니다. 무료 공연(ticketPrice = 0)은 null이어야 합니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "KAKAOBANK",
    )
    val bankName: BankNameType?,
    @field:Schema(
        description = "유료 공연(ticketPrice > 0)의 입금 계좌번호입니다. 무료 공연(ticketPrice = 0)은 null이어야 합니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "3333-01-1234567",
    )
    val accountNumber: String?,
    @field:Schema(
        description = "유료 공연(ticketPrice > 0)의 입금 계좌 예금주입니다. 무료 공연(ticketPrice = 0)은 null이어야 합니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "BEAT 운영팀",
    )
    val accountHolder: String?,
    @field:Schema(
        description = "업로드된 포스터 이미지의 이미지 key 또는 절대 URL",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "dev/poster/performance-1.jpg",
    )
    val posterImage: String,
    @field:Schema(
        description = "공연 팀명",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "BEAT 밴드",
    )
    val performanceTeamName: String,
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
        description = "공연 문의 연락처",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "010-1234-5678",
    )
    val performanceContact: String,
    @field:Schema(
        description =
            "scheduleModifyRequests의 회차 날짜로 서버가 계산하는 공연 기간입니다. 클라이언트 호환을 위해 허용되며 요청 값은 사용하지 않습니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "2026.09.01~2026.09.03",
    )
    val performancePeriod: String?,
    @field:Schema(
        description =
            "scheduleModifyRequests의 크기로 서버가 계산하는 전체 회차 수입니다. 클라이언트 호환을 위해 허용되며 요청 값은 사용하지 않습니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        example = "3",
    )
    val totalScheduleCount: Int?,
    @field:Schema(
        description =
            "티켓 가격(원). 0이면 무료 공연이며, 0보다 크면 bankName·accountNumber·accountHolder가 모두 필요합니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "30000",
    )
    val ticketPrice: Int,
    @field:Schema(
        description = "수정할 공연 회차 목록. 각 scheduleId가 null이면 신규 회차이고, 값이 있으면 기존 회차를 수정합니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    @field:Valid
    val scheduleModifyRequests: List<@Valid ScheduleModifyRequest>,
    @field:Schema(
        description = "수정할 공연 출연진 목록. 각 castId가 null이면 신규 출연진입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    @field:Valid
    val castModifyRequests: List<@Valid CastModifyRequest>,
    @field:Schema(
        description = "수정할 공연 스태프 목록. 각 staffId가 null이면 신규 스태프입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    @field:Valid
    val staffModifyRequests: List<@Valid StaffModifyRequest>,
    @field:Schema(
        description = "수정할 공연 이미지 목록. 각 performanceImageId가 null이면 신규 이미지입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    @field:Valid
    val performanceImageModifyRequests: List<@Valid PerformanceImageModifyRequest>,
)
