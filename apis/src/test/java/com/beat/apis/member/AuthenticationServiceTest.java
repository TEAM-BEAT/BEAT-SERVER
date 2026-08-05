package com.beat.apis.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.member.application.command.AuthenticationCommandService;
import com.beat.contracts.auth.jwt.JwtTokenPort;
import com.beat.contracts.auth.jwt.JwtTokenType;
import com.beat.contracts.auth.refreshtoken.RefreshTokenPort;
import com.beat.apis.member.exception.TokenApplicationErrorCode;
import com.beat.contracts.auth.jwt.TokenValidationResult;
import com.beat.apis.exception.ApiApplicationException;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

	@Mock
	private JwtTokenPort jwtTokenPort;

	@Mock
	private RefreshTokenPort refreshTokenPort;

	private AuthenticationCommandService authenticationService;

	@BeforeEach
	void setUp() {
		authenticationService = new AuthenticationCommandService(jwtTokenPort, refreshTokenPort);
	}

	@Test
	void generateAccessTokenFromRefreshTokenShouldRejectUnknownRoleClaim() {
		String refreshToken = "refresh-token";

		when(jwtTokenPort.validateRefreshToken(refreshToken)).thenReturn(TokenValidationResult.VALID);
		when(jwtTokenPort.getMemberId(refreshToken, JwtTokenType.REFRESH)).thenReturn(1L);
		when(refreshTokenPort.findMemberIdByRefreshToken(refreshToken)).thenReturn(OptionalLong.of(1L));
		when(jwtTokenPort.getRoleName(refreshToken, JwtTokenType.REFRESH)).thenReturn("ROLE_UNKNOWN");

		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () -> authenticationService.generateAccessTokenFromRefreshToken(refreshToken));

		assertEquals(TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR, exception.getErrorCode());
		verify(jwtTokenPort).getRoleName(refreshToken, JwtTokenType.REFRESH);
	}

	@Test
	void generateAccessTokenFromRefreshTokenShouldTranslateMissingStoredToken() {
		String refreshToken = "refresh-token";

		when(jwtTokenPort.validateRefreshToken(refreshToken)).thenReturn(TokenValidationResult.VALID);
		when(jwtTokenPort.getMemberId(refreshToken, JwtTokenType.REFRESH)).thenReturn(1L);
		when(refreshTokenPort.findMemberIdByRefreshToken(refreshToken)).thenReturn(OptionalLong.empty());

		ApiApplicationException exception = assertThrows(ApiApplicationException.class,
			() -> authenticationService.generateAccessTokenFromRefreshToken(refreshToken));

		assertEquals(TokenApplicationErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());
	}

	@Test
	void generateAccessTokenFromRefreshTokenShouldRejectWhenValidationFailsAtBoundary() {
		String refreshToken = "refresh-token";

		// Missing or malformed required claims (memberId, role) are rejected during token
		// validation per RFC 8725 §3.3/§3.12, before any claim is extracted downstream.
		when(jwtTokenPort.validateRefreshToken(refreshToken)).thenReturn(TokenValidationResult.INVALID_TOKEN);

		ApiApplicationException exception = assertThrows(ApiApplicationException.class, () -> authenticationService.generateAccessTokenFromRefreshToken(refreshToken));

		assertEquals(TokenApplicationErrorCode.INVALID_REFRESH_TOKEN_ERROR, exception.getErrorCode());
		verify(jwtTokenPort, never()).getMemberId(refreshToken, JwtTokenType.REFRESH);
		verifyNoInteractions(refreshTokenPort);
	}

	@Test
	void signOutDeletesRefreshTokenThroughApplicationService() {
		when(refreshTokenPort.deleteRefreshToken(1L)).thenReturn(true);

		authenticationService.signOut(1L);

		verify(refreshTokenPort).deleteRefreshToken(1L);
	}

	@Test
	void signOutIsIdempotentWhenRefreshTokenIsMissing() {
		when(refreshTokenPort.deleteRefreshToken(1L)).thenReturn(false);

		authenticationService.signOut(1L);

		verify(refreshTokenPort).deleteRefreshToken(1L);
	}
}
