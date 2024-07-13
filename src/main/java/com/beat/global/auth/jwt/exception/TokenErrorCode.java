package com.beat.global.auth.jwt.exception;

import com.beat.global.common.exception.base.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenErrorCode implements BaseErrorCode {

    AUTHENTICATION_CODE_EXPIRED(403, "토큰이 만료되었습니다"),
    REFRESH_TOKEN_NOT_FOUND(404, "리프레쉬토큰이 없습니다"),
    TOKEN_INCORRECT_ERROR(400, "잘못된 토큰입니다");

    private final int status;
    private final String message;
}