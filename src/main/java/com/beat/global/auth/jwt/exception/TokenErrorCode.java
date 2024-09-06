package com.beat.global.auth.jwt.exception;

import com.beat.global.common.exception.base.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenErrorCode implements BaseErrorCode {

    AUTHENTICATION_CODE_EXPIRED(401, "인가코드가 만료되었습니다"),
    REFRESH_TOKEN_NOT_FOUND(404, "리프레쉬 토큰이 존재하지 않습니다"),
    INVALID_REFRESH_TOKEN_ERROR(400, "잘못된 리프레쉬 토큰입니다"),
    REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR(400, "리프레쉬 토큰의 사용자 정보가 일치하지 않습니다"),
    REFRESH_TOKEN_EXPIRED_ERROR(401, "리프레쉬 토큰이 만료되었습니다"),
    REFRESH_TOKEN_SIGNATURE_ERROR(400, "리프레쉬 토큰의 서명의 잘못 되었습니다"),
    UNSUPPORTED_REFRESH_TOKEN_ERROR(400, "지원하지 않는 리프레쉬 토큰입니다"),
    REFRESH_TOKEN_EMPTY_ERROR(400, "리프레쉬 토큰이 비어있습니다"),
    UNKNOWN_REFRESH_TOKEN_ERROR(500, "알 수 없는 리프레쉬 토큰 오류가 발생했습니다")
    ;

    private final int status;
    private final String message;
}