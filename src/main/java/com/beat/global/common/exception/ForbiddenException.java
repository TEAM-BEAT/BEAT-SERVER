package com.beat.global.common.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public class ForbiddenException extends BeatException {
    public ForbiddenException(final BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
