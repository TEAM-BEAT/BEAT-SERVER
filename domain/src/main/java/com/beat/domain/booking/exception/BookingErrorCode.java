package com.beat.domain.booking.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public enum BookingErrorCode implements BaseErrorCode {
	/*
	400 BadRequest
	*/
	REQUIRED_DATA_MISSING(400, "필수 데이터가 누락되었습니다."),
	INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),
	INVALID_REQUEST_FORMAT(400, "잘못된 요청 형식입니다.");

	private final int status;
	private final String message;

	BookingErrorCode(int status, String message) {
		this.status = status;
		this.message = message;
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
}