package com.beat.apis.schedule.api.response

import com.beat.global.support.exception.base.BaseSuccessCode

enum class ScheduleSuccessCode(
    private val status: Int,
    private val message: String,
) : BaseSuccessCode {
    TICKET_AVAILABILITY_RETRIEVAL_SUCCESS(200, "티켓 수량 조회가 성공적으로 완료되었습니다.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
