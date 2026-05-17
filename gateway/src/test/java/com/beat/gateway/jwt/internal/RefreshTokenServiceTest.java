package com.beat.gateway.jwt.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.contracts.auth.TokenErrorCode;
import com.beat.gateway.jwt.internal.store.RefreshToken;
import com.beat.gateway.jwt.internal.store.RefreshTokenRepository;
import com.beat.global.support.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		refreshTokenService = new RefreshTokenService(refreshTokenRepository);
	}

	@Test
	void saveRefreshTokenStoresRefreshTokenAggregate() {
		refreshTokenService.saveRefreshToken(1L, "refresh-token");

		verify(refreshTokenRepository).save(argThat(token ->
			token.getId().equals(1L) && token.getRefreshToken().equals("refresh-token")
		));
	}

	@Test
	void findMemberIdByRefreshTokenReturnsMemberIdWhenTokenExists() {
		when(refreshTokenRepository.findByRefreshToken("refresh-token"))
			.thenReturn(Optional.of(RefreshToken.of(1L, "refresh-token")));

		Long memberId = refreshTokenService.findMemberIdByRefreshToken("refresh-token");

		assertEquals(1L, memberId);
	}

	@Test
	void findMemberIdByRefreshTokenThrowsWhenTokenIsMissing() {
		when(refreshTokenRepository.findByRefreshToken("missing")).thenReturn(Optional.empty());

		NotFoundException exception = assertThrows(
			NotFoundException.class,
			() -> refreshTokenService.findMemberIdByRefreshToken("missing")
		);

		assertEquals(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getBaseErrorCode());
	}

	@Test
	void deleteRefreshTokenDeletesLoadedToken() {
		RefreshToken token = RefreshToken.of(1L, "refresh-token");
		when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(token));

		refreshTokenService.deleteRefreshToken(1L);

		verify(refreshTokenRepository).delete(token);
	}

	@Test
	void deleteRefreshTokenThrowsWhenTokenIsMissing() {
		when(refreshTokenRepository.findById(1L)).thenReturn(Optional.empty());

		NotFoundException exception = assertThrows(
			NotFoundException.class,
			() -> refreshTokenService.deleteRefreshToken(1L)
		);

		assertEquals(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getBaseErrorCode());
	}
}
