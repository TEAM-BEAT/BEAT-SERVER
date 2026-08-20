package com.beat.application.frontoffice.exception

import com.beat.application.frontoffice.auth.exception.TokenApplicationErrorCode
import com.beat.application.frontoffice.member.exception.MemberApplicationErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplicationErrorCodeContractTest {
    @Test
    fun `member and token application error codes stay explicit and unique`() {
        val codes = MemberApplicationErrorCode.entries.map { it.code } +
            TokenApplicationErrorCode.entries.map { it.code }

        assertEquals(codes.size, codes.distinct().size)
        assertTrue(codes.all { it.matches(Regex("[A-Z][A-Z0-9_]+")) })
    }

    @Test
    fun `member application error code names types and messages stay stable`() {
        assertContract(
            MemberApplicationErrorCode.entries,
            listOf(
                Expected("SOCIAL_TYPE_BAD_REQUEST", "MEMBER_SOCIAL_TYPE_INVALID", FrontofficeApplicationErrorType.INVALID_INPUT, "로그인 요청이 유효하지 않습니다."),
                Expected("AUTHENTICATION_CODE_EXPIRED", "MEMBER_AUTHENTICATION_CODE_EXPIRED", FrontofficeApplicationErrorType.UNAUTHENTICATED, "인가코드가 만료되었습니다"),
                Expected("SOCIAL_LOGIN_PROVIDER_FAILURE", "SOCIAL_LOGIN_PROVIDER_FAILURE", FrontofficeApplicationErrorType.UPSTREAM_FAILURE, "소셜 로그인 서비스 응답을 처리할 수 없습니다."),
                Expected("SOCIAL_LOGIN_PROVIDER_UNAVAILABLE", "SOCIAL_LOGIN_PROVIDER_UNAVAILABLE", FrontofficeApplicationErrorType.UPSTREAM_UNAVAILABLE, "소셜 로그인 서비스를 일시적으로 사용할 수 없습니다."),
                Expected("SOCIAL_LOGIN_PROVIDER_TIMEOUT", "SOCIAL_LOGIN_PROVIDER_TIMEOUT", FrontofficeApplicationErrorType.UPSTREAM_TIMEOUT, "소셜 로그인 서비스 응답이 지연되고 있습니다."),
                Expected("MEMBER_NOT_FOUND", "MEMBER_NOT_FOUND", FrontofficeApplicationErrorType.NOT_FOUND, "회원이 없습니다"),
                Expected("USER_NOT_FOUND", "USER_NOT_FOUND", FrontofficeApplicationErrorType.NOT_FOUND, "유저가 없습니다"),
            ),
        )
    }

    @Test
    fun `token application error code names types and messages stay stable`() {
        assertContract(
            TokenApplicationErrorCode.entries,
            listOf(
                Expected("REFRESH_TOKEN_NOT_FOUND", "REFRESH_TOKEN_NOT_FOUND", FrontofficeApplicationErrorType.NOT_FOUND, "리프레쉬 토큰이 존재하지 않습니다"),
                Expected("INVALID_REFRESH_TOKEN_ERROR", "INVALID_REFRESH_TOKEN", FrontofficeApplicationErrorType.INVALID_INPUT, "잘못된 리프레쉬 토큰입니다"),
                Expected("REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR", "REFRESH_TOKEN_MEMBER_ID_MISMATCH", FrontofficeApplicationErrorType.INVALID_INPUT, "리프레쉬 토큰의 사용자 정보가 일치하지 않습니다"),
                Expected("REFRESH_TOKEN_EXPIRED_ERROR", "REFRESH_TOKEN_EXPIRED", FrontofficeApplicationErrorType.UNAUTHENTICATED, "리프레쉬 토큰이 만료되었습니다"),
                Expected("REFRESH_TOKEN_SIGNATURE_ERROR", "REFRESH_TOKEN_INVALID_SIGNATURE", FrontofficeApplicationErrorType.INVALID_INPUT, "리프레쉬 토큰의 서명의 잘못 되었습니다"),
                Expected("UNSUPPORTED_REFRESH_TOKEN_ERROR", "REFRESH_TOKEN_UNSUPPORTED", FrontofficeApplicationErrorType.INVALID_INPUT, "지원하지 않는 리프레쉬 토큰입니다"),
                Expected("REFRESH_TOKEN_EMPTY_ERROR", "REFRESH_TOKEN_EMPTY", FrontofficeApplicationErrorType.INVALID_INPUT, "리프레쉬 토큰이 비어있습니다"),
                Expected("UNKNOWN_REFRESH_TOKEN_ERROR", "REFRESH_TOKEN_INTERNAL_ERROR", FrontofficeApplicationErrorType.INTERNAL_ERROR, "알 수 없는 리프레쉬 토큰 오류가 발생했습니다"),
            ),
        )
    }

    private fun assertContract(
        actual: List<FrontofficeApplicationErrorCode>,
        expected: List<Expected>,
    ) {
        assertEquals(expected.map { it.name }, actual.map { (it as Enum<*>).name })
        assertEquals(expected.map { it.code }, actual.map { it.code })
        assertEquals(expected.map { it.type }, actual.map { it.type })
        assertEquals(expected.map { it.message }, actual.map { it.message })
    }

    private data class Expected(
        val name: String,
        val code: String,
        val type: FrontofficeApplicationErrorType,
        val message: String,
    )
}
