package com.beat.application.frontoffice.auth.exception

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType

internal enum class TokenApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    REFRESH_TOKEN_NOT_FOUND(
        "REFRESH_TOKEN_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "리프레쉬 토큰이 존재하지 않습니다",
    ),
    INVALID_REFRESH_TOKEN_ERROR(
        "INVALID_REFRESH_TOKEN",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "잘못된 리프레쉬 토큰입니다",
    ),
    REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR(
        "REFRESH_TOKEN_MEMBER_ID_MISMATCH",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "리프레쉬 토큰의 사용자 정보가 일치하지 않습니다",
    ),
    REFRESH_TOKEN_EXPIRED_ERROR(
        "REFRESH_TOKEN_EXPIRED",
        FrontofficeApplicationErrorType.UNAUTHENTICATED,
        "리프레쉬 토큰이 만료되었습니다",
    ),
    REFRESH_TOKEN_SIGNATURE_ERROR(
        "REFRESH_TOKEN_INVALID_SIGNATURE",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "리프레쉬 토큰의 서명의 잘못 되었습니다",
    ),
    UNSUPPORTED_REFRESH_TOKEN_ERROR(
        "REFRESH_TOKEN_UNSUPPORTED",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "지원하지 않는 리프레쉬 토큰입니다",
    ),
    REFRESH_TOKEN_EMPTY_ERROR(
        "REFRESH_TOKEN_EMPTY",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "리프레쉬 토큰이 비어있습니다",
    ),
    UNKNOWN_REFRESH_TOKEN_ERROR(
        "REFRESH_TOKEN_INTERNAL_ERROR",
        FrontofficeApplicationErrorType.INTERNAL_ERROR,
        "알 수 없는 리프레쉬 토큰 오류가 발생했습니다",
    ),
}
