package com.beat.apis.booking.application.exception

import com.beat.global.support.exception.base.BaseErrorCode

enum class BookingApplicationErrorCode(
    private val status: Int,
    private val message: String,
) : BaseErrorCode {
    REQUIRED_DATA_MISSING(400, "필수 데이터가 누락되었습니다."),
    INVALID_REQUEST_FORMAT(400, "잘못된 요청 형식입니다."),
    NO_BOOKING_FOUND(404, "입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요."),
    NO_PERFORMANCE_FOUND(404, "공연을 찾을 수 없습니다."),
    NO_SCHEDULE_FOUND(404, "회차를 찾을 수 없습니다."),
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
