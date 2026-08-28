package com.beat.apps.api.booking.api.response

import com.beat.application.frontoffice.booking.booker.result.BookingRetrieveResult
import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.apps.api.performance.api.type.BankNameType
import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import com.beat.apps.api.web.jackson.CdnImageUrl
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@ConsistentCopyVisibility
@Schema(
    description =
        "비회원 예매 내역 조회 결과입니다. 무료 공연은 계좌 필드가 null이고, 유료 공연은 공연에 등록된 은행·계좌번호·예금주 전체 값이 반환됩니다."
)
data class GuestBookingRetrieveResponse
private constructor(
    @field:Schema(
        description = "조회된 예매의 식별자입니다.",
        example = "1001",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookingId: Long,
    @field:Schema(
        description = "예매한 공연 회차의 식별자입니다.",
        example = "2001",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val scheduleId: Long,
    @field:Schema(
        description = "예매한 공연의 식별자입니다.",
        example = "3001",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val performanceId: Long,
    @field:Schema(
        description = "예매한 공연의 제목입니다.",
        example = "봄날의 콘서트",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val performanceTitle: String,
    @field:Schema(
        description = "예매한 공연 회차의 공연 일시입니다(ISO-8601 형식).",
        example = "2024-01-20T19:00:00",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val performanceDate: LocalDateTime,
    @field:Schema(
        description = "예매한 공연의 장소입니다.",
        example = "예술의전당",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val performanceVenue: String,
    @field:Schema(
        description = "예매한 티켓 수량입니다.",
        example = "2",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val purchaseTicketCount: Int,
    @field:Schema(
        description = "예매한 공연 회차 번호입니다.",
        example = "FIRST",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val scheduleNumber: ScheduleNumberType,
    @field:Schema(
        description = "예매자 이름입니다.",
        example = "홍길동",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookerName: String,
    @field:Schema(
        description = "공연 문의 연락처입니다.",
        example = "010-9876-5432",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val performanceContact: String,
    @field:Schema(
        description = "공연 입금 계좌의 은행입니다. 무료 공연은 null이고, 유료 공연은 공연에 등록된 값입니다.",
        types = ["string", "null"],
        example = "KAKAOBANK",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bankName: BankNameType?,
    @field:Schema(
        description = "공연 입금 계좌번호입니다. 무료 공연은 null이고, 유료 공연은 공연에 등록된 값입니다.",
        types = ["string", "null"],
        example = "123456789012",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val accountNumber: String?,
    @field:Schema(
        description = "공연 입금 계좌의 예금주명입니다. 무료 공연은 null이고, 유료 공연은 공연에 등록된 값입니다.",
        types = ["string", "null"],
        example = "홍길동",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val accountHolder: String?,
    @field:Schema(
        description = "오늘부터 공연일까지 남은 일수입니다. 공연일이 지나면 음수일 수 있습니다.",
        example = "30",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val dueDate: Int,
    @field:Schema(
        description = "예매 상태입니다.",
        example = "BOOKING_CONFIRMED",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookingStatus: BookingStatusType,
    @field:Schema(
        description = "예매 생성 시각입니다(ISO-8601 형식).",
        example = "2024-01-01T19:00:00",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val createdAt: LocalDateTime,
    @field:Schema(
        description = "공연 포스터 이미지 URL입니다.",
        example = "https://cdn.example.com/poster.jpg",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:CdnImageUrl
    val posterImage: String,
    @field:Schema(
        description = "예매 총 결제 금액입니다.",
        example = "10000",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val totalPaymentAmount: Int,
) {
    companion object {
        fun from(result: BookingRetrieveResult): GuestBookingRetrieveResponse =
            GuestBookingRetrieveResponse(
                bookingId = result.bookingId,
                scheduleId = result.scheduleId,
                performanceId = result.performanceId,
                performanceTitle = result.performanceTitle,
                performanceDate = result.performanceDate,
                performanceVenue = result.performanceVenue,
                purchaseTicketCount = result.purchaseTicketCount,
                scheduleNumber = ScheduleNumberType.valueOf(result.scheduleNumber),
                bookerName = result.bookerName,
                performanceContact = result.performanceContact,
                bankName = result.bankName?.let(BankNameType::valueOf),
                accountNumber = result.accountNumber,
                accountHolder = result.accountHolder,
                dueDate = result.dueDate,
                bookingStatus = BookingStatusType.valueOf(result.bookingStatus),
                createdAt = result.createdAt,
                posterImage = result.posterImage,
                totalPaymentAmount = result.totalPaymentAmount,
            )
    }
}
