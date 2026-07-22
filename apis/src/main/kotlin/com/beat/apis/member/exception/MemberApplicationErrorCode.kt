package com.beat.apis.member.exception

import com.beat.apis.exception.ApplicationErrorCode
import com.beat.apis.exception.ApplicationErrorType

enum class MemberApplicationErrorCode(
    private val code: String,
    private val type: ApplicationErrorType,
    private val message: String,
) : ApplicationErrorCode {
    SOCIAL_TYPE_BAD_REQUEST(
        "MEMBER_SOCIAL_TYPE_INVALID",
        ApplicationErrorType.INVALID_INPUT,
        "로그인 요청이 유효하지 않습니다."
    ),
    AUTHENTICATION_CODE_EXPIRED(
        "MEMBER_AUTHENTICATION_CODE_EXPIRED",
        ApplicationErrorType.UNAUTHENTICATED,
        "인가코드가 만료되었습니다",
    ),
    SOCIAL_LOGIN_PROVIDER_FAILURE(
        "SOCIAL_LOGIN_PROVIDER_FAILURE",
        ApplicationErrorType.UPSTREAM_FAILURE,
        "소셜 로그인 서비스 응답을 처리할 수 없습니다.",
    ),
    SOCIAL_LOGIN_PROVIDER_UNAVAILABLE(
        "SOCIAL_LOGIN_PROVIDER_UNAVAILABLE",
        ApplicationErrorType.UPSTREAM_UNAVAILABLE,
        "소셜 로그인 서비스를 일시적으로 사용할 수 없습니다.",
    ),
    SOCIAL_LOGIN_PROVIDER_TIMEOUT(
        "SOCIAL_LOGIN_PROVIDER_TIMEOUT",
        ApplicationErrorType.UPSTREAM_TIMEOUT,
        "소셜 로그인 서비스 응답이 지연되고 있습니다.",
    ),
    MEMBER_NOT_FOUND(
        "MEMBER_NOT_FOUND",
        ApplicationErrorType.NOT_FOUND,
        "회원이 없습니다"
    );

    override fun getCode(): String = code

    override fun getType(): ApplicationErrorType = type

    override fun getMessage(): String = message
}
