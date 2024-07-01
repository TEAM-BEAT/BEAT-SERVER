package com.beat.exception;

import com.beat.exception.message.ErrorMessage;

public class UnauthorizedException extends BeatException {
    public UnauthorizedException(final ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
