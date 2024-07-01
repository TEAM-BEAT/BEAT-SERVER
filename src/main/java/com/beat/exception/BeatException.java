package com.beat.exception;

import com.beat.exception.message.ErrorMessage;
import lombok.Getter;

@Getter
public class BeatException extends RuntimeException {
    private ErrorMessage errorMessage;
    public BeatException(ErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        this.errorMessage = errorMessage;
    }
}
