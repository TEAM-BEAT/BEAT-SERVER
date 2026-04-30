package com.beat.domain.performance.exception

import com.beat.global.common.exception.base.BaseErrorCode

enum class PerformanceErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),
    NEGATIVE_TICKET_PRICE(400, "티켓 가격은 음수일 수 없습니다."),
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
