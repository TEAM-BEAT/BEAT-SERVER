package com.beat.apis.ticket.application.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class TicketApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED(400, "이미 결제가 완료된 티켓의 상태는 변경할 수 없습니다."),
    SEARCH_WORD_TOO_SHORT(400, "검색어는 최소 2글자 이상이어야 합니다."),
    DELETED_TICKET_RETRIEVE_NOT_ALLOWED(400, "삭제된 예매자를 조회할 수 없습니다."),
    NO_TICKETS_FOUND(404, "입력하신 정보와 일치하는 예매자 목록이 없습니다."),
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
