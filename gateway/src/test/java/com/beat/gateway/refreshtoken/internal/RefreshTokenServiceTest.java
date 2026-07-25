package com.beat.gateway.refreshtoken.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.beat.gateway.refreshtoken.internal.store.RefreshToken;
import com.beat.gateway.refreshtoken.internal.store.RefreshTokenRepository;

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

		long memberId = refreshTokenService.findMemberIdByRefreshToken("refresh-token").orElseThrow();

		assertEquals(1L, memberId);
	}

	@Test
	void findMemberIdByRefreshTokenReturnsEmptyWhenTokenIsMissing() {
		when(refreshTokenRepository.findByRefreshToken("missing")).thenReturn(Optional.empty());

		assertFalse(refreshTokenService.findMemberIdByRefreshToken("missing").isPresent());
	}

	@Test
	void deleteRefreshTokenDeletesLoadedToken() {
		RefreshToken token = RefreshToken.of(1L, "refresh-token");
		when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(token));

		boolean deleted = refreshTokenService.deleteRefreshToken(1L);

		assertTrue(deleted);
		verify(refreshTokenRepository).delete(token);
	}

	@Test
	void deleteRefreshTokenIsIdempotentWhenTokenIsMissing() {
		when(refreshTokenRepository.findById(1L)).thenReturn(Optional.empty());

		boolean deleted = refreshTokenService.deleteRefreshToken(1L);

		assertFalse(deleted);
		verify(refreshTokenRepository, never()).delete(any());
	}
}
