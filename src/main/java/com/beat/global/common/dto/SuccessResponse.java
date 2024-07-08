package com.beat.global.common.dto;

import com.beat.global.common.exception.base.BaseSuccessCode;

public record SuccessResponse<T>(
        int status,
        String message,
        T data
) {
    public static <T> SuccessResponse of(final BaseSuccessCode baseSuccessCode, final T data) {
        return new SuccessResponse(baseSuccessCode.getStatus(), baseSuccessCode.getMessage(), data);
    }

    public static SuccessResponse of(final BaseSuccessCode baseSuccessCode) {
        return new SuccessResponse(baseSuccessCode.getStatus(), baseSuccessCode.getMessage(), null);
    }
}