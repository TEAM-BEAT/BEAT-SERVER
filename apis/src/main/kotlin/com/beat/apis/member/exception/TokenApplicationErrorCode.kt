package com.beat.apis.member.exception

import com.beat.apis.exception.ApplicationErrorCode
import com.beat.apis.exception.ApplicationErrorType

enum class TokenApplicationErrorCode(
    private val code: String,
    private val type: ApplicationErrorType,
    private val message: String,
) : ApplicationErrorCode {
    REFRESH_TOKEN_NOT_FOUND("REFRESH_TOKEN_NOT_FOUND", ApplicationErrorType.NOT_FOUND, "리프레쉬 토큰이 존재하지 않습니다"),
    INVALID_REFRESH_TOKEN_ERROR("INVALID_REFRESH_TOKEN", ApplicationErrorType.INVALID_INPUT, "잘못된 리프레쉬 토큰입니다"),
    REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR(
        "REFRESH_TOKEN_MEMBER_ID_MISMATCH",
        ApplicationErrorType.INVALID_INPUT,
        "리프레쉬 토큰의 사용자 정보가 일치하지 않습니다",
    ),
    REFRESH_TOKEN_EXPIRED_ERROR("REFRESH_TOKEN_EXPIRED", ApplicationErrorType.UNAUTHENTICATED, "리프레쉬 토큰이 만료되었습니다"),
    REFRESH_TOKEN_SIGNATURE_ERROR(
        "REFRESH_TOKEN_INVALID_SIGNATURE",
        ApplicationErrorType.INVALID_INPUT,
        "리프레쉬 토큰의 서명의 잘못 되었습니다",
    ),
    UNSUPPORTED_REFRESH_TOKEN_ERROR(
        "REFRESH_TOKEN_UNSUPPORTED",
        ApplicationErrorType.INVALID_INPUT,
        "지원하지 않는 리프레쉬 토큰입니다",
    ),
    REFRESH_TOKEN_EMPTY_ERROR("REFRESH_TOKEN_EMPTY", ApplicationErrorType.INVALID_INPUT, "리프레쉬 토큰이 비어있습니다"),
    UNKNOWN_REFRESH_TOKEN_ERROR(
        "REFRESH_TOKEN_INTERNAL_ERROR",
        ApplicationErrorType.INTERNAL_ERROR,
        "알 수 없는 리프레쉬 토큰 오류가 발생했습니다",
    ),
    ;

    override fun getCode(): String = code

    override fun getType(): ApplicationErrorType = type

    override fun getMessage(): String = message
}
