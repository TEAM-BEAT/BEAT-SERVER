package com.beat.global.common.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public class BeatException extends RuntimeException {
	private final BaseErrorCode baseErrorCode;

	public BeatException(BaseErrorCode baseErrorCode) {
		super(baseErrorCode.getMessage());
		this.baseErrorCode = baseErrorCode;
	}

	public BaseErrorCode getBaseErrorCode() {
		return baseErrorCode;
	}
}
