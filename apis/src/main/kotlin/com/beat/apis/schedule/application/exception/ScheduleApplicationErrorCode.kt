package com.beat.apis.schedule.application.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class ScheduleApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),
    SCHEDULE_NOT_BELONG_TO_PERFORMANCE(403, "해당 스케줄은 해당 공연에 속해 있지 않습니다."),
    NO_SCHEDULE_FOUND(404, "해당 회차를 찾을 수 없습니다."),
    INSUFFICIENT_TICKETS(409, "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다. 다른 수량을 선택해 주세요.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
