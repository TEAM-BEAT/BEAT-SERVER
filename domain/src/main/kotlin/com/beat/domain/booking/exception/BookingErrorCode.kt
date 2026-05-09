package com.beat.domain.booking.exception

import com.beat.global.support.exception.base.BaseErrorCode

enum class BookingErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
