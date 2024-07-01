package com.beat.exception;

import com.beat.exception.message.ErrorMessage;

public class ForbiddenException extends BeatException {
    public ForbiddenException(final ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
