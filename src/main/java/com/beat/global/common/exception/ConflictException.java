package com.beat.global.common.exception;

import com.beat.global.common.exception.base.BaseErrorCode;

public class ConflictException extends BeatException{
    public ConflictException(final BaseErrorCode baseErrorCode) {super(baseErrorCode);}
}