package com.beat.apis.schedule.application.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class ScheduleApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    NO_SCHEDULE_FOUND(404, "해당 회차를 찾을 수 없습니다.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
