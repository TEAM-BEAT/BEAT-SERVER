package com.beat.exception;

import com.beat.exception.message.ErrorMessage;
import lombok.Getter;

@Getter
public class beatException extends RuntimeException {
    private ErrorMessage errorMessage;
    public beatException(ErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        this.errorMessage = errorMessage;
    }
}
