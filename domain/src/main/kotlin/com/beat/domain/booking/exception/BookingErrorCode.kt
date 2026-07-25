package com.beat.domain.booking.exception

import com.beat.domain.exception.DomainErrorCode
import com.beat.domain.exception.DomainErrorType

enum class BookingErrorCode(
    override val code: String,
    override val type: DomainErrorType,
    override val message: String,
) : DomainErrorCode {
    INVALID_PURCHASE_TICKET_COUNT(
        "BOOKING_INVALID_PURCHASE_TICKET_COUNT",
        DomainErrorType.INVALID_INPUT,
        "구매 티켓 수량은 1개 이상이어야 합니다.",
    ),
    NEGATIVE_TOTAL_PAYMENT_AMOUNT(
        "BOOKING_NEGATIVE_TOTAL_PAYMENT_AMOUNT",
        DomainErrorType.INVALID_INPUT,
        "결제 금액은 음수일 수 없습니다.",
    ),
    INVALID_REFUND_ACCOUNT(
        "BOOKING_INVALID_REFUND_ACCOUNT",
        DomainErrorType.INVALID_INPUT,
        "환불 계좌 정보가 올바르지 않습니다.",
    ),
    PAYMENT_CONFIRMATION_NOT_ALLOWED(
        "BOOKING_PAYMENT_CONFIRMATION_NOT_ALLOWED",
        DomainErrorType.STATE_CONFLICT,
        "현재 예매 상태에서는 결제를 확정할 수 없습니다.",
    ),
    CONFIRMED_STATUS_CHANGE_NOT_ALLOWED(
        "BOOKING_CONFIRMED_STATUS_CHANGE_NOT_ALLOWED",
        DomainErrorType.STATE_CONFLICT,
        "이미 결제가 완료된 티켓의 상태는 변경할 수 없습니다.",
    ),
    STATUS_TRANSITION_NOT_ALLOWED(
        "BOOKING_STATUS_TRANSITION_NOT_ALLOWED",
        DomainErrorType.STATE_CONFLICT,
        "지원하지 않는 예매 상태 변경입니다.",
    ),
    REFUND_REQUEST_NOT_ALLOWED(
        "BOOKING_REFUND_REQUEST_NOT_ALLOWED",
        DomainErrorType.STATE_CONFLICT,
        "현재 예매 상태에서는 환불을 요청할 수 없습니다.",
    ),
    ;
}
