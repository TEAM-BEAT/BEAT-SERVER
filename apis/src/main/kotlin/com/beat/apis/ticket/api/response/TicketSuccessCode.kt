package com.beat.apis.ticket.api.response

import com.beat.global.common.exception.base.BaseSuccessCode

enum class TicketSuccessCode(
    private val status: Int,
    private val message: String,
) : BaseSuccessCode {
    TICKET_RETRIEVE_SUCCESS(200, "예매자 목록 조회가 성공적으로 완료되었습니다."),
    TICKET_UPDATE_SUCCESS(200, "예매자 입금여부 수정이 성공적으로 완료되었습니다."),
    TICKET_REFUND_SUCCESS(200, "예매 환불처리 요청이 성공했습니다."),
    TICKET_DELETE_SUCCESS(200, "예매자 삭제 요청이 성공했습니다."),
    TICKET_SEARCH_SUCCESS(200, "예매자 검색 결과 조회가 성공적으로 완료되었습니다.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
