package com.beat.exception;

import com.beat.exception.message.ErrorMessage;

public class BadRequestException extends beatException {
    public BadRequestException(final ErrorMessage errorMessage) {
        super(errorMessage);
    }
}
