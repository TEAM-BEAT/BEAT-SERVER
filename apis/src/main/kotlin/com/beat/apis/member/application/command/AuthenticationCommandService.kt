package com.beat.apis.member.application.command

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.member.application.result.AccessTokenResult
import com.beat.apis.member.exception.TokenApplicationErrorCode
import com.beat.contracts.auth.jwt.JwtTokenPort
import com.beat.contracts.auth.jwt.JwtSubject
import com.beat.contracts.auth.jwt.JwtTokenType
import com.beat.contracts.auth.refreshtoken.RefreshTokenPort
import com.beat.contracts.auth.jwt.TokenValidationResult
import com.beat.domain.user.model.Role
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthenticationCommandService(
    private val jwtTokenPort: JwtTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
) {
    @Transactional
    fun generateAccessTokenFromRefreshToken(refreshToken: String): AccessTokenResult {
        validateRefreshToken(refreshToken)
        val memberId = jwtTokenPort.getMemberId(refreshToken, JwtTokenType.REFRESH)
        verifyStoredTokenOwner(refreshToken, memberId)
        val role = mapRole(jwtTokenPort.getRoleName(refreshToken, JwtTokenType.REFRESH))
        return AccessTokenResult(
            jwtTokenPort.issueAccessToken(
                JwtSubject(
                    memberId = memberId,
                    roleName = role.roleName,
                ),
            ),
        )
    }

    @Transactional
    fun signOut(memberId: Long) {
        refreshTokenPort.deleteRefreshToken(memberId)
    }

    private fun validateRefreshToken(refreshToken: String) {
        val errorCode = when (jwtTokenPort.validateRefreshToken(refreshToken)) {
            TokenValidationResult.VALID -> return
            TokenValidationResult.EXPIRED -> TokenApplicationErrorCode.REFRESH_TOKEN_EXPIRED_ERROR
            TokenValidationResult.INVALID_TOKEN -> TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR
            TokenValidationResult.INVALID_SIGNATURE -> TokenApplicationErrorCode.REFRESH_TOKEN_SIGNATURE_ERROR
            TokenValidationResult.UNSUPPORTED -> TokenApplicationErrorCode.UNSUPPORTED_REFRESH_TOKEN_ERROR
            TokenValidationResult.EMPTY -> TokenApplicationErrorCode.REFRESH_TOKEN_EMPTY_ERROR
        }
        throw ApiApplicationException(errorCode)
    }

    private fun verifyStoredTokenOwner(refreshToken: String, memberId: Long) {
        val storedMemberId = refreshTokenPort.findMemberIdByRefreshToken(refreshToken)
            .orElseThrow { ApiApplicationException(TokenApplicationErrorCode.REFRESH_TOKEN_NOT_FOUND) }
        if (memberId != storedMemberId) {
            log.error { "MemberId mismatch: token does not match the stored refresh token" }
            throw ApiApplicationException(TokenApplicationErrorCode.REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR)
        }
    }

    private fun mapRole(roleName: String?): Role {
        if (roleName.isNullOrBlank()) {
            throw ApiApplicationException(TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR)
        }
        return try {
            Role.valueOf(roleName.removePrefix("ROLE_").uppercase())
        } catch (exception: IllegalArgumentException) {
            log.error(exception) { "Refresh token role claim is invalid: ${roleName}" }
            throw ApiApplicationException(TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR)
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
