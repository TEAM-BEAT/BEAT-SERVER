package com.beat.global.common.dto;

import com.beat.global.common.exception.base.BaseErrorCode;

public record  ErrorResponse(
        int status,
        String message
) {
    public static ErrorResponse of(final int status, final String message) {
        return new ErrorResponse(status, message);
    }

    public static ErrorResponse of(final BaseErrorCode baseErrorCode) {
        return new ErrorResponse(baseErrorCode.getStatus(), baseErrorCode.getMessage());
    }
}
