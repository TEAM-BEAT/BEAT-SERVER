package com.beat.apis.booking.exception

import com.beat.apis.exception.ApplicationErrorCode
import com.beat.apis.exception.ApplicationErrorType

enum class BookingApplicationErrorCode(
    private val code: String,
    private val type: ApplicationErrorType,
    private val message: String,
) : ApplicationErrorCode {
    REQUIRED_DATA_MISSING("BOOKING_REQUIRED_DATA_MISSING", ApplicationErrorType.INVALID_INPUT, "필수 데이터가 누락되었습니다."),
    INVALID_REQUEST_FORMAT("BOOKING_INVALID_REQUEST_FORMAT", ApplicationErrorType.INVALID_INPUT, "잘못된 요청 형식입니다."),
    AUTHENTICATION_REQUIRED(
        "BOOKING_AUTHENTICATION_REQUIRED",
        ApplicationErrorType.UNAUTHENTICATED,
        "예매자 인증이 필요합니다.",
    ),
    GUEST_ACCESS_RATE_LIMITED(
        "BOOKING_GUEST_ACCESS_RATE_LIMITED",
        ApplicationErrorType.RATE_LIMITED,
        "비회원 예매 조회 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
    ),
    NO_BOOKING_FOUND(
        "BOOKING_NOT_FOUND",
        ApplicationErrorType.NOT_FOUND,
        "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
    ),
    NO_PERFORMANCE_FOUND("BOOKING_PERFORMANCE_NOT_FOUND", ApplicationErrorType.NOT_FOUND, "공연을 찾을 수 없습니다."),
    NO_SCHEDULE_FOUND("BOOKING_SCHEDULE_NOT_FOUND", ApplicationErrorType.NOT_FOUND, "회차를 찾을 수 없습니다."),
    INSUFFICIENT_TICKETS(
        "BOOKING_INSUFFICIENT_TICKETS",
        ApplicationErrorType.INVALID_INPUT,
        "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요.",
    ),
    TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE(
        "BOOKING_TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE",
        ApplicationErrorType.INVALID_INPUT,
        "결제 금액이 허용 범위를 초과했습니다.",
    ),
    STORED_TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE(
        "BOOKING_STORED_TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE",
        ApplicationErrorType.INTERNAL_ERROR,
        "저장된 예매 금액을 처리할 수 없습니다.",
    ),
    ;

    override fun getCode(): String = code

    override fun getType(): ApplicationErrorType = type

    override fun getMessage(): String = message
}
