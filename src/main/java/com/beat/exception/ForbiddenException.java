package com.beat.exception;

import com.beat.exception.message.ErrorMessage;

public class ForbiddenException extends beatException {
    public ForbiddenException(final ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
