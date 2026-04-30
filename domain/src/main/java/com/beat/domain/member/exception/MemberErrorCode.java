package com.beat.domain.member.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public enum MemberErrorCode implements BaseErrorCode {
	/*
	400 BadRequest
	*/
	SOCIAL_TYPE_BAD_REQUEST(400, "로그인 요청이 유효하지 않습니다."),

	/*
	404 NotFound
	*/
	MEMBER_NOT_FOUND(404, "회원이 없습니다");

	private final int status;
	private final String message;

	MemberErrorCode(int status, String message) {
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
