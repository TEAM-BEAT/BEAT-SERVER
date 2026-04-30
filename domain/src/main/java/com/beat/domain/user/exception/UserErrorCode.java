package com.beat.domain.user.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public enum UserErrorCode implements BaseErrorCode {
	/*
	404 NotFound
	*/
	USER_NOT_FOUND(404, "유저가 없습니다");

	private final int status;
	private final String message;

	UserErrorCode(int status, String message) {
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
