package com.beat.apis.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.apis.member.application.AuthenticationService;
import com.beat.contracts.auth.JwtTokenPort;
import com.beat.contracts.auth.JwtTokenType;
import com.beat.contracts.auth.RefreshTokenPort;
import com.beat.contracts.auth.TokenErrorCode;
import com.beat.contracts.auth.TokenValidationResult;
import com.beat.contracts.auth.social.SocialMemberInfo;
import com.beat.global.support.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

	@Mock
	private JwtTokenPort jwtTokenPort;

	@Mock
	private RefreshTokenPort refreshTokenPort;

	private AuthenticationService authenticationService;

	@BeforeEach
	void setUp() {
		authenticationService = new AuthenticationService(jwtTokenPort, refreshTokenPort);
	}

	@Test
	void generateLoginSuccessResponseShouldRejectUnknownRoleNameBeforeIssuingToken() {
		SocialMemberInfo socialMemberInfo = new SocialMemberInfo(123L, "nickname", "email@test.com");

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> authenticationService.generateLoginSuccessResponse(1L, "ROLE_UNKNOWN", socialMemberInfo)
		);

		assertEquals(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR, exception.getBaseErrorCode());
		verifyNoInteractions(jwtTokenPort, refreshTokenPort);
	}

	@Test
	void generateAccessTokenFromRefreshTokenShouldRejectUnknownRoleClaim() {
		String refreshToken = "refresh-token";

		when(jwtTokenPort.validateRefreshToken(refreshToken)).thenReturn(TokenValidationResult.VALID);
		when(jwtTokenPort.getMemberId(refreshToken, JwtTokenType.REFRESH)).thenReturn(1L);
		when(refreshTokenPort.findMemberIdByRefreshToken(refreshToken)).thenReturn(1L);
		when(jwtTokenPort.getRoleName(refreshToken, JwtTokenType.REFRESH)).thenReturn("ROLE_UNKNOWN");

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> authenticationService.generateAccessTokenFromRefreshToken(refreshToken)
		);

		assertEquals(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR, exception.getBaseErrorCode());
		verify(jwtTokenPort).getRoleName(refreshToken, JwtTokenType.REFRESH);
	}

	@Test
	void generateAccessTokenFromRefreshTokenShouldRejectMissingMemberIdClaim() {
		String refreshToken = "refresh-token";

		when(jwtTokenPort.validateRefreshToken(refreshToken)).thenReturn(TokenValidationResult.VALID);
		when(jwtTokenPort.getMemberId(refreshToken, JwtTokenType.REFRESH)).thenReturn(null);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> authenticationService.generateAccessTokenFromRefreshToken(refreshToken)
		);

		assertEquals(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR, exception.getBaseErrorCode());
		verify(refreshTokenPort, never()).findMemberIdByRefreshToken(refreshToken);
	}

	@Test
	void signOutDeletesRefreshTokenThroughApplicationService() {
		authenticationService.signOut(1L);

		verify(refreshTokenPort).deleteRefreshToken(1L);
	}

	@Test
	void generateAccessTokenFromRefreshTokenShouldRejectMissingRoleClaim() {
		String refreshToken = "refresh-token";

		when(jwtTokenPort.validateRefreshToken(refreshToken)).thenReturn(TokenValidationResult.VALID);
		when(jwtTokenPort.getMemberId(refreshToken, JwtTokenType.REFRESH)).thenReturn(1L);
		when(refreshTokenPort.findMemberIdByRefreshToken(refreshToken)).thenReturn(1L);
		when(jwtTokenPort.getRoleName(refreshToken, JwtTokenType.REFRESH)).thenReturn(null);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> authenticationService.generateAccessTokenFromRefreshToken(refreshToken)
		);

		assertEquals(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR, exception.getBaseErrorCode());
		verify(jwtTokenPort).getRoleName(refreshToken, JwtTokenType.REFRESH);
	}
}
