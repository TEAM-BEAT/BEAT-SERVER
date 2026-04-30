package com.beat.domain.booking.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public enum BookingErrorCode implements BaseErrorCode {
	/*
	400 BadRequest
	*/
	INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다.");

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
