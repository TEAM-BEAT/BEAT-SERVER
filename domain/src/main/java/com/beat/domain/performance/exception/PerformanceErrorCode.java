package com.beat.domain.performance.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public enum PerformanceErrorCode implements BaseErrorCode {
	/*
	400 BadRequest
	*/
	INVALID_DATA_FORMAT(400, "잘못된 데이터 형식입니다."),
	NEGATIVE_TICKET_PRICE(400, "티켓 가격은 음수일 수 없습니다.");

	private final int status;
	private final String message;

	PerformanceErrorCode(int status, String message) {
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
