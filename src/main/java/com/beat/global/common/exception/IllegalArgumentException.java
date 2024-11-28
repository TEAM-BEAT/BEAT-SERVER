package com.beat.global.common.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public class IllegalArgumentException extends BeatException {
	public IllegalArgumentException(final BaseErrorCode baseErrorCode) {
		super(baseErrorCode);
	}
}
