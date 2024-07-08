package com.beat.global.common.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public class NotFoundException extends BeatException {
    public NotFoundException(final BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}