package com.beat.domain.booking.exception;

import com.beat.global.common.exception.base.BaseSuccessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingSuccessCode implements BaseSuccessCode {
	/*
	200 Ok
	*/
	MEMBER_BOOKING_RETRIEVE_SUCCESS(200, "회원 예매 조회가 성공적으로 완료되었습니다."),
	GUEST_BOOKING_RETRIEVE_SUCCESS(200, "비회원 예매 조회가 성공적으로 완료되었습니다."),
	BOOKING_REFUND_SUCCESS(200, "예매자의 환불요청이 성공했습니다."),
	BOOKING_CANCEL_SUCCESS(200, "예매자의 취소요청이 성공했습니다."),

	/*
	201 Created
	*/
	MEMBER_BOOKING_SUCCESS(201, "회원 예매가 성공적으로 완료되었습니다"),
	GUEST_BOOKING_SUCCESS(201, "비회원 예매가 성공적으로 완료되었습니다");

	private final int status;
	private final String message;
}
