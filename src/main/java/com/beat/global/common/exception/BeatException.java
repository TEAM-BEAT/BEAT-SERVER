package com.beat.global.common.exception;

import com.beat.global.common.exception.base.BaseErrorCode;
import lombok.Getter;

@Getter
public class BeatException extends RuntimeException {
    private final BaseErrorCode baseErrorCode;

    public BeatException(BaseErrorCode baseErrorCode) {
        super(baseErrorCode.getMessage());
        this.baseErrorCode = baseErrorCode;
    }
}
