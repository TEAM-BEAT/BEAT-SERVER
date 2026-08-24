package com.beat.application.frontoffice.member.exception

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType

internal enum class MemberApplicationErrorCode(
    override val code: String,
    override val type: FrontofficeApplicationErrorType,
    override val message: String,
) : FrontofficeApplicationErrorCode {
    SOCIAL_TYPE_BAD_REQUEST(
        "MEMBER_SOCIAL_TYPE_INVALID",
        FrontofficeApplicationErrorType.INVALID_INPUT,
        "로그인 요청이 유효하지 않습니다."
    ),
    AUTHENTICATION_CODE_EXPIRED(
        "MEMBER_AUTHENTICATION_CODE_EXPIRED",
        FrontofficeApplicationErrorType.UNAUTHENTICATED,
        "인가코드가 만료되었습니다",
    ),
    SOCIAL_LOGIN_PROVIDER_FAILURE(
        "SOCIAL_LOGIN_PROVIDER_FAILURE",
        FrontofficeApplicationErrorType.UPSTREAM_FAILURE,
        "소셜 로그인 서비스 응답을 처리할 수 없습니다.",
    ),
    SOCIAL_LOGIN_PROVIDER_UNAVAILABLE(
        "SOCIAL_LOGIN_PROVIDER_UNAVAILABLE",
        FrontofficeApplicationErrorType.UPSTREAM_UNAVAILABLE,
        "소셜 로그인 서비스를 일시적으로 사용할 수 없습니다.",
    ),
    SOCIAL_LOGIN_PROVIDER_TIMEOUT(
        "SOCIAL_LOGIN_PROVIDER_TIMEOUT",
        FrontofficeApplicationErrorType.UPSTREAM_TIMEOUT,
        "소셜 로그인 서비스 응답이 지연되고 있습니다.",
    ),
    MEMBER_NOT_FOUND(
        "MEMBER_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "회원이 없습니다"
    ),
    USER_NOT_FOUND(
        "USER_NOT_FOUND",
        FrontofficeApplicationErrorType.NOT_FOUND,
        "유저가 없습니다"
    );

}
