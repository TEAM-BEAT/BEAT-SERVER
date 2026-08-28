package com.beat.apps.api.booking.api.response

import com.beat.application.frontoffice.booking.booker.command.result.BookingCreationResult
import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.apps.api.performance.api.type.BankNameType
import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@ConsistentCopyVisibility
@Schema(description = "비회원 예매 생성 결과입니다. 무료 공연은 계좌 필드가 null이고, 유료 공연은 공연에 등록된 계좌 값이 반환됩니다.")
data class GuestBookingResponse
private constructor(
    @field:Schema(
        description = "생성된 예매의 식별자입니다.",
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
        description = "예매와 연결된 사용자 식별자입니다.",
        example = "3001",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val userId: Long,
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
        description = "예매자 전화번호입니다.",
        example = "010-1234-5678",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookerPhoneNumber: String,
    @field:Schema(
        description = "예매 상태입니다. 무료 공연은 BOOKING_CONFIRMED, 유료 공연은 CHECKING_PAYMENT로 생성됩니다.",
        example = "BOOKING_CONFIRMED",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val bookingStatus: BookingStatusType,
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
        description = "예매 총 결제 금액입니다.",
        example = "10000",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val totalPaymentAmount: Int,
    @field:Schema(
        description = "예매 생성 시각입니다(ISO-8601 형식).",
        example = "2024-01-01T19:00:00",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(result: BookingCreationResult): GuestBookingResponse =
            GuestBookingResponse(
                bookingId = result.bookingId,
                scheduleId = result.scheduleId,
                userId = result.userId,
                purchaseTicketCount = result.purchaseTicketCount,
                scheduleNumber = ScheduleNumberType.valueOf(result.scheduleNumber),
                bookerName = result.bookerName,
                bookerPhoneNumber = result.bookerPhoneNumber,
                bookingStatus = BookingStatusType.valueOf(result.bookingStatus),
                bankName = result.bankName?.let(BankNameType::valueOf),
                accountNumber = result.accountNumber,
                totalPaymentAmount = result.totalPaymentAmount,
                createdAt = result.createdAt,
            )
    }
}
