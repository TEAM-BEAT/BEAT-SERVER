package com.beat.global.common.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public class BadRequestException extends BeatException {
    public BadRequestException(final BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
