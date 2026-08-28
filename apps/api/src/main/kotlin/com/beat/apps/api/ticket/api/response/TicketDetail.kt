package com.beat.apps.api.ticket.api.response

import com.beat.application.frontoffice.ticket.maker.query.TicketDetailResult
import com.beat.apps.api.booking.api.type.BookingStatusType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "메이커가 조회한 예매자 한 명의 상세 정보입니다.")
@ConsistentCopyVisibility
data class TicketDetail
private constructor(
    @field:Schema(
        description = "예매 식별자입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val bookingId: Long,
    @field:Schema(
        description = "예매자 이름입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "booker",
    )
    val bookerName: String,
    @field:Schema(
        description = "예매자 전화번호입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "010-0000-0000",
    )
    val bookerPhoneNumber: String,
    @field:Schema(
        description = "예매가 연결된 회차 식별자입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val scheduleId: Long,
    @field:Schema(
        description = "구매한 티켓 수량입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "2",
    )
    val purchaseTicketCount: Int,
    @field:Schema(
        description = "예매 생성 일시입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "2026-04-01T12:00:00",
    )
    val createdAt: LocalDateTime,
    @field:Schema(
        description = "예매 상태입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "CHECKING_PAYMENT",
    )
    val bookingStatus: BookingStatusType,
    @field:Schema(
        description = "예매가 연결된 회차 번호입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "FIRST",
    )
    val scheduleNumber: String,
    @field:Schema(
        description = "환불 계좌 은행명입니다. 환불 계좌가 없으면 빈 문자열입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "카카오뱅크",
    )
    val bankName: String,
    @field:Schema(
        description = "환불 계좌번호입니다. 환불 계좌가 없으면 빈 문자열입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "123-456",
    )
    val accountNumber: String,
    @field:Schema(
        description = "환불 계좌 예금주입니다. 환불 계좌가 없으면 빈 문자열입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "예금주",
    )
    val accountHolder: String,
    @field:Schema(
        description =
            "메이커가 해당 예매를 선택해 삭제할 수 있는지 여부입니다. 결제 금액이 0인 무료 예매는 REFUND_REQUESTED가 아니면, 결제 금액이 0보다 큰 유료 예매는 CHECKING_PAYMENT(입금 확인 전)·BOOKING_CANCELLED(취소 완료)·BOOKING_DELETED 상태이면 삭제할 수 있습니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "true",
    )
    val deletable: Boolean,
) {
    companion object {
        fun from(result: TicketDetailResult): TicketDetail =
            TicketDetail(
                bookingId = result.bookingId,
                bookerName = result.bookerName,
                bookerPhoneNumber = result.bookerPhoneNumber,
                scheduleId = result.scheduleId,
                purchaseTicketCount = result.purchaseTicketCount,
                createdAt = result.createdAt,
                bookingStatus = BookingStatusType.valueOf(result.bookingStatus),
                scheduleNumber = result.scheduleNumber,
                bankName = result.bankName,
                accountNumber = result.accountNumber,
                accountHolder = result.accountHolder,
                deletable = result.deletable,
            )
    }
}
