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
    PURCHASE_TICKET_COUNT_EXCEEDED(
        "BOOKING_PURCHASE_TICKET_COUNT_EXCEEDED",
        DomainErrorType.INVALID_INPUT,
        "한 번에 최대 10매까지 예매할 수 있습니다.",
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
    CANCELLATION_NOT_ALLOWED(
        "BOOKING_CANCELLATION_NOT_ALLOWED",
        DomainErrorType.STATE_CONFLICT,
        "입금이 확인된 예매와 환불 처리 중인 예매는 바로 취소할 수 없습니다.",
    ),
    REFUND_COMPLETION_NOT_ALLOWED(
        "BOOKING_REFUND_COMPLETION_NOT_ALLOWED",
        DomainErrorType.STATE_CONFLICT,
        "환불 요청 상태인 예매만 환불 완료 처리할 수 있습니다.",
    ),
    DELETION_NOT_ALLOWED(
        "BOOKING_DELETION_NOT_ALLOWED",
        DomainErrorType.STATE_CONFLICT,
        "미입금, 무료 확정 또는 취소 완료 예매만 삭제할 수 있습니다.",
    ),
}
