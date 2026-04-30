package com.beat.apis.performance.application.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class PerformanceApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    PERFORMANCE_NOT_FOUND(404, "해당 공연 정보를 찾을 수 없습니다."),
    SCHEDULE_LIST_NOT_FOUND(404, "스케쥴 리스트에 스케쥴이 없습니다.")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
