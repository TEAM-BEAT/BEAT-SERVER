package com.beat.application.frontoffice.ticket.exception

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType

enum class TicketApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED(
        "TICKET_PAYMENT_COMPLETED_UPDATE_NOT_ALLOWED",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "이미 결제가 완료된 티켓의 상태는 변경할 수 없습니다.",
    ),
    INVALID_BOOKING_STATUS_TRANSITION(
        "TICKET_INVALID_BOOKING_STATUS_TRANSITION",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "지원하지 않는 예매 상태 변경입니다.",
    ),
    DUPLICATE_BOOKING_ID(
        "TICKET_DUPLICATE_BOOKING_ID",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "중복된 예매 식별자는 한 번만 요청할 수 있습니다.",
    ),
    SEARCH_WORD_TOO_SHORT(
        "TICKET_SEARCH_WORD_TOO_SHORT",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "검색어는 최소 2글자 이상이어야 합니다.",
    ),
    DELETED_TICKET_RETRIEVE_NOT_ALLOWED(
        "TICKET_DELETED_RETRIEVE_NOT_ALLOWED",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "삭제된 예매자를 조회할 수 없습니다.",
    ),
    NO_TICKETS_FOUND(
        "TICKET_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "입력하신 정보와 일치하는 예매자 목록이 없습니다.",
    ),
    MEMBER_NOT_FOUND(
        "TICKET_MEMBER_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "회원이 없습니다",
    ),
    PERFORMANCE_NOT_FOUND(
        "TICKET_PERFORMANCE_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "해당 공연 정보를 찾을 수 없습니다.",
    ),
    NOT_PERFORMANCE_OWNER(
        "TICKET_PERFORMANCE_FORBIDDEN",
        FrontofficeApplicationErrorType.FORBIDDEN,
        "해당 공연의 메이커가 아닙니다.",
    ),
    NO_BOOKING_FOUND(
        "TICKET_BOOKING_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.",
    ),
    NO_SCHEDULE_FOUND(
        "TICKET_SCHEDULE_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "해당 회차를 찾을 수 없습니다.",
    ),
    SCHEDULE_NOT_BELONG_TO_PERFORMANCE(
        "TICKET_SCHEDULE_NOT_BELONG_TO_PERFORMANCE",
        FrontofficeApplicationErrorType.FORBIDDEN,
        "해당 스케줄은 해당 공연에 속해 있지 않습니다.",
    ),
    REFUND_COMPLETION_NOT_ALLOWED(
        "TICKET_REFUND_COMPLETION_NOT_ALLOWED",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "환불 요청 상태인 예매만 환불 완료 처리할 수 있습니다.",
    ),
    DELETION_NOT_ALLOWED(
        "TICKET_DELETION_NOT_ALLOWED",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "미입금, 무료 확정 또는 취소 완료 예매만 삭제할 수 있습니다.",
    ),
    ;
}
