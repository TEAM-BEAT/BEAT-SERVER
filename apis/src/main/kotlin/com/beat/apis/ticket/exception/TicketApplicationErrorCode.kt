package com.beat.apis.ticket.exception

import com.beat.apis.exception.ApplicationErrorCode
import com.beat.apis.exception.ApplicationErrorType

enum class TicketApplicationErrorCode(
    private val code: String,
    private val type: ApplicationErrorType,
    private val message: String,
) : ApplicationErrorCode {
    PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED(
        "TICKET_PAYMENT_COMPLETED_UPDATE_NOT_ALLOWED",
        ApplicationErrorType.INVALID_INPUT,
        "이미 결제가 완료된 티켓의 상태는 변경할 수 없습니다.",
    ),
    INVALID_BOOKING_STATUS_TRANSITION(
        "TICKET_INVALID_BOOKING_STATUS_TRANSITION",
        ApplicationErrorType.INVALID_INPUT,
        "지원하지 않는 예매 상태 변경입니다.",
    ),
    DUPLICATE_BOOKING_ID(
        "TICKET_DUPLICATE_BOOKING_ID",
        ApplicationErrorType.INVALID_INPUT,
        "중복된 예매 식별자는 한 번만 요청할 수 있습니다.",
    ),
    SEARCH_WORD_TOO_SHORT(
        "TICKET_SEARCH_WORD_TOO_SHORT",
        ApplicationErrorType.INVALID_INPUT,
        "검색어는 최소 2글자 이상이어야 합니다.",
    ),
    DELETED_TICKET_RETRIEVE_NOT_ALLOWED(
        "TICKET_DELETED_RETRIEVE_NOT_ALLOWED",
        ApplicationErrorType.INVALID_INPUT,
        "삭제된 예매자를 조회할 수 없습니다.",
    ),
    NO_TICKETS_FOUND(
        "TICKET_NOT_FOUND",
        ApplicationErrorType.NOT_FOUND,
        "입력하신 정보와 일치하는 예매자 목록이 없습니다.",
    ),
    ;

    override fun getCode(): String = code

    override fun getType(): ApplicationErrorType = type

    override fun getMessage(): String = message
}
