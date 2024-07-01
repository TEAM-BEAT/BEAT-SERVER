package com.beat.global.common.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public class UnauthorizedException extends BeatException {
    public UnauthorizedException(final BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
