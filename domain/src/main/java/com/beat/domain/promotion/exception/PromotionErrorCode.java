package com.beat.domain.promotion.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public enum PromotionErrorCode implements BaseErrorCode {
	/*
	404 NotFound
	*/
	PROMOTION_NOT_FOUND(404, "해당 홍보 정보를 찾을 수 없습니다.");
	private final int status;
	private final String message;

	PromotionErrorCode(int status, String message) {
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