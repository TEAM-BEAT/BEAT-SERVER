package com.beat.apis.booking.api.response

import com.beat.global.support.exception.base.BaseSuccessCode

enum class BookingSuccessCode(
    private val status: Int,
    private val message: String,
) : BaseSuccessCode {
    MEMBER_BOOKING_RETRIEVE_SUCCESS(200, "회원 예매 조회가 성공적으로 완료되었습니다."),
    GUEST_BOOKING_RETRIEVE_SUCCESS(200, "비회원 예매 조회가 성공적으로 완료되었습니다."),
    BOOKING_REFUND_SUCCESS(200, "예매자의 환불요청이 성공했습니다."),
    BOOKING_CANCEL_SUCCESS(200, "예매자의 취소요청이 성공했습니다."),
    MEMBER_BOOKING_SUCCESS(201, "회원 예매가 성공적으로 완료되었습니다"),
    GUEST_BOOKING_SUCCESS(201, "비회원 예매가 성공적으로 완료되었습니다")
    ;

    override fun getStatus(): Int = status

    override fun getMessage(): String = message
}
