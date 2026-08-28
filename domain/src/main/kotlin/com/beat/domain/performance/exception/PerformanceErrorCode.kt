package com.beat.domain.performance.exception

import com.beat.domain.exception.DomainErrorCode
import com.beat.domain.exception.DomainErrorType

enum class PerformanceErrorCode(
    override val code: String,
    override val type: DomainErrorType,
    override val message: String,
) : DomainErrorCode {
    NON_POSITIVE_RUNNING_TIME(
        "PERFORMANCE_NON_POSITIVE_RUNNING_TIME",
        DomainErrorType.INVALID_INPUT,
        "러닝타임은 1분 이상이어야 합니다.",
    ),
    NEGATIVE_SCHEDULE_COUNT(
        "PERFORMANCE_NEGATIVE_SCHEDULE_COUNT",
        DomainErrorType.INVALID_INPUT,
        "공연 회차 수는 음수일 수 없습니다.",
    ),
    INVALID_PERFORMANCE_PERIOD(
        "PERFORMANCE_INVALID_PERIOD",
        DomainErrorType.INVALID_INPUT,
        "공연 기간이 올바르지 않습니다.",
    ),
    NEGATIVE_TICKET_PRICE(
        "PERFORMANCE_NEGATIVE_TICKET_PRICE",
        DomainErrorType.INVALID_INPUT,
        "티켓 가격은 음수일 수 없습니다.",
    ),
    PRICE_UPDATE_NOT_ALLOWED(
        "PERFORMANCE_PRICE_UPDATE_NOT_ALLOWED",
        DomainErrorType.STATE_CONFLICT,
        "예매자가 존재하여 가격을 수정할 수 없습니다.",
    ),
    DELETE_NOT_ALLOWED(
        "PERFORMANCE_DELETE_NOT_ALLOWED",
        DomainErrorType.STATE_CONFLICT,
        "예매자가 1명 이상 있을 경우, 공연을 삭제할 수 없습니다.",
    ),
    NEGATIVE_TICKET_QUANTITY(
        "PERFORMANCE_NEGATIVE_TICKET_QUANTITY",
        DomainErrorType.INVALID_INPUT,
        "티켓 수량은 음수일 수 없습니다.",
    ),
    INCOMPLETE_PAYMENT_ACCOUNT(
        "PERFORMANCE_INCOMPLETE_PAYMENT_ACCOUNT",
        DomainErrorType.INVALID_INPUT,
        "결제 계좌 정보는 모두 입력하거나 모두 비워야 합니다.",
    ),
    FREE_PERFORMANCE_PAYMENT_ACCOUNT_NOT_ALLOWED(
        "PERFORMANCE_FREE_PAYMENT_ACCOUNT_NOT_ALLOWED",
        DomainErrorType.INVALID_INPUT,
        "무료 공연에는 결제 계좌 정보를 입력할 수 없습니다.",
    ),
    PAID_PERFORMANCE_PAYMENT_ACCOUNT_REQUIRED(
        "PERFORMANCE_PAID_PAYMENT_ACCOUNT_REQUIRED",
        DomainErrorType.INVALID_INPUT,
        "유료 공연은 결제 계좌 정보를 입력해야 합니다.",
    ),
}
