package com.beat.application.frontoffice.booking.booker

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType

enum class BookingApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    REQUIRED_DATA_MISSING(
        "BOOKING_REQUIRED_DATA_MISSING",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "필수 데이터가 누락되었습니다.",
    ),
    INVALID_REQUEST_FORMAT(
        "BOOKING_INVALID_REQUEST_FORMAT",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "잘못된 요청 형식입니다.",
    ),
    AUTHENTICATION_REQUIRED(
        "BOOKING_AUTHENTICATION_REQUIRED",
        FrontofficeApplicationErrorType.UNAUTHENTICATED,
        "예매자 인증이 필요합니다.",
    ),
    GUEST_ACCESS_RATE_LIMITED(
        "BOOKING_GUEST_ACCESS_RATE_LIMITED",
        FrontofficeApplicationErrorType.RATE_LIMITED,
        "비회원 예매 조회 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
    ),
    NO_BOOKING_FOUND(
        "BOOKING_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
    ),
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", FrontofficeApplicationErrorType.NOT_FOUND, "회원이 없습니다"),
    PERFORMANCE_NOT_FOUND(
        "PERFORMANCE_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "해당 공연 정보를 찾을 수 없습니다.",
    ),
    SCHEDULE_NOT_FOUND(
        "SCHEDULE_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "해당 회차를 찾을 수 없습니다.",
    ),
    BOOKING_CLOSED(
        "SCHEDULE_BOOKING_CLOSED",
        FrontofficeApplicationErrorType.STATE_CONFLICT,
        "예매가 마감된 회차입니다.",
    ),
    TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE(
        "BOOKING_TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "결제 금액이 허용 범위를 초과했습니다.",
    ),
    STORED_TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE(
        "BOOKING_STORED_TOTAL_PAYMENT_AMOUNT_OUT_OF_RANGE",
        FrontofficeApplicationErrorType.INTERNAL_ERROR,
        "저장된 예매 금액을 처리할 수 없습니다.",
    ),
}
