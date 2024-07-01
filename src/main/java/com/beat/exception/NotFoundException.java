package com.beat.exception;

import com.beat.exception.message.ErrorMessage;

public class NotFoundException extends BeatException {
    public NotFoundException(final ErrorMessage errorMessage) {
        super(errorMessage);
    }
}