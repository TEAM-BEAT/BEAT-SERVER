package com.beat.domain.member.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.beat.contracts.auth.JwtTokenPort;
import com.beat.contracts.auth.RefreshTokenPort;
import com.beat.contracts.auth.TokenErrorCode;
import com.beat.contracts.auth.TokenValidationResult;
import com.beat.global.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
	void generateAccessTokenFromRefreshTokenShouldRejectUnknownRoleClaim() {
		String refreshToken = "refresh-token";

		when(jwtTokenPort.validateToken(refreshToken)).thenReturn(TokenValidationResult.VALID);
		when(jwtTokenPort.getMemberId(refreshToken)).thenReturn(1L);
		when(refreshTokenPort.findMemberIdByRefreshToken(refreshToken)).thenReturn(1L);
		when(jwtTokenPort.getRoleName(refreshToken)).thenReturn("ROLE_UNKNOWN");

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> authenticationService.generateAccessTokenFromRefreshToken(refreshToken)
		);

		assertEquals(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR, exception.getBaseErrorCode());
		verify(jwtTokenPort).getRoleName(refreshToken);
	}

	@Test
	void generateAccessTokenFromRefreshTokenShouldRejectMissingRoleClaim() {
		String refreshToken = "refresh-token";

		when(jwtTokenPort.validateToken(refreshToken)).thenReturn(TokenValidationResult.VALID);
		when(jwtTokenPort.getMemberId(refreshToken)).thenReturn(1L);
		when(refreshTokenPort.findMemberIdByRefreshToken(refreshToken)).thenReturn(1L);
		when(jwtTokenPort.getRoleName(refreshToken)).thenReturn(null);

		BadRequestException exception = assertThrows(
			BadRequestException.class,
			() -> authenticationService.generateAccessTokenFromRefreshToken(refreshToken)
		);

		assertEquals(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR, exception.getBaseErrorCode());
		verify(jwtTokenPort).getRoleName(refreshToken);
	}
}
