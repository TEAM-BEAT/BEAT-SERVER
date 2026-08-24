package com.beat.application.frontoffice.auth.command

import com.beat.application.frontoffice.auth.exception.TokenApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.security.RefreshTokenAuthenticator
import com.beat.application.frontoffice.security.TokenAuthenticationFailure
import com.beat.application.frontoffice.security.TokenAuthenticationResult
import com.beat.application.frontoffice.security.TokenIssuer
import com.beat.application.frontoffice.security.TokenSubject
import com.beat.domain.user.model.Role
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthenticationCommandService internal constructor(
    private val tokenIssuer: TokenIssuer,
    private val refreshTokenAuthenticator: RefreshTokenAuthenticator,
    private val refreshTokenStore: RefreshTokenStore,
) {
    @Transactional
    fun generateAccessTokenFromRefreshToken(refreshToken: String): AccessTokenResult {
        return translateDomainFailure {
            val subject = authenticateRefreshToken(refreshToken)
            verifyStoredTokenOwner(refreshToken, subject.memberId)
            val role = mapRole(subject.roleName)
            AccessTokenResult(
                tokenIssuer.issueAccessToken(TokenSubject(subject.memberId, role.roleName)),
            )
        }
    }

    @Transactional
    fun signOut(memberId: Long) {
        translateDomainFailure {
            refreshTokenStore.delete(memberId)
        }
    }

    private fun authenticateRefreshToken(refreshToken: String) = when (
        val result = refreshTokenAuthenticator.authenticateRefreshToken(refreshToken)
    ) {
        is TokenAuthenticationResult.Authenticated -> result.subject
        is TokenAuthenticationResult.Rejected -> throw FrontofficeApplicationException(result.failure.toErrorCode())
    }

    private fun verifyStoredTokenOwner(refreshToken: String, memberId: Long) {
        val storedMemberId = refreshTokenStore.findMemberIdByRefreshToken(refreshToken)
            ?: throw FrontofficeApplicationException(TokenApplicationErrorCode.REFRESH_TOKEN_NOT_FOUND)
        if (memberId != storedMemberId) {
            log.error { "MemberId mismatch: token does not match the stored refresh token" }
            throw FrontofficeApplicationException(TokenApplicationErrorCode.REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR)
        }
    }

    private fun mapRole(roleName: String?): Role {
        if (roleName.isNullOrBlank()) {
            throw FrontofficeApplicationException(TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR)
        }
        return try {
            Role.valueOf(roleName.removePrefix("ROLE_").uppercase())
        } catch (exception: IllegalArgumentException) {
            log.error(exception) { "Refresh token role claim is invalid: ${roleName}" }
            throw FrontofficeApplicationException(TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR)
        }
    }

    private fun TokenAuthenticationFailure.toErrorCode(): TokenApplicationErrorCode = when (this) {
        TokenAuthenticationFailure.EXPIRED -> TokenApplicationErrorCode.REFRESH_TOKEN_EXPIRED_ERROR
        TokenAuthenticationFailure.INVALID_TOKEN -> TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR
        TokenAuthenticationFailure.INVALID_SIGNATURE -> TokenApplicationErrorCode.REFRESH_TOKEN_SIGNATURE_ERROR
        TokenAuthenticationFailure.UNSUPPORTED -> TokenApplicationErrorCode.UNSUPPORTED_REFRESH_TOKEN_ERROR
        TokenAuthenticationFailure.EMPTY -> TokenApplicationErrorCode.REFRESH_TOKEN_EMPTY_ERROR
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
