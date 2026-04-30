package com.beat.domain.schedule.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class ScheduleErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),
    INSUFFICIENT_TICKETS(409, "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요."),
    EXCESS_TICKET_DELETE(409, "예매된 티켓 수 이상을 삭제할 수 없습니다."),
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
