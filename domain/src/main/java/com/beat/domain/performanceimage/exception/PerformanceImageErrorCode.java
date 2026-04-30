package com.beat.domain.performanceimage.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public enum PerformanceImageErrorCode implements BaseErrorCode {
	/*
	403 Forbidden
	*/
	PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE(403, "해당 싱세이미지는 해당 공연에 속해 있지 않습니다."),

	/*
	404 NotFound
	*/
	PERFORMANCE_IMAGE_NOT_FOUND(404, "해당 공연 상세이미지를 찾을 수 없습니다.");

	private final int status;
	private final String message;

	PerformanceImageErrorCode(int status, String message) {
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
