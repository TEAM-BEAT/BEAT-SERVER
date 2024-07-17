package com.beat.domain.cast.exception;

import com.beat.global.common.exception.base.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CastErrorCode implements BaseErrorCode {
    CAST_NOT_FOUND(404, "등장인물이 존재하지 않습니다.")
    ;

    private final int status;
    private final String message;
}
