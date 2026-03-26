package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.beat.apis.support.AbstractIntegrationTest;
import com.beat.gateway.jwt.store.RefreshToken;
import com.beat.gateway.jwt.store.RefreshTokenRepository;

class RefreshTokenRepositoryIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@AfterEach
	void tearDown() {
		refreshTokenRepository.deleteAll();
	}

	@Test
	void refreshTokenRepositoryRoundTripWorksWithRedisBackedInfrastructure() {
		RefreshToken refreshToken = RefreshToken.of(1L, "refresh-token");
		refreshTokenRepository.save(refreshToken);

		RefreshToken loaded = refreshTokenRepository.findByRefreshToken("refresh-token").orElseThrow();

		assertEquals(1L, loaded.getId());
		assertEquals("refresh-token", loaded.getRefreshToken());
		assertTrue(refreshTokenRepository.findById(1L).isPresent());

		refreshTokenRepository.delete(loaded);

		assertFalse(refreshTokenRepository.findById(1L).isPresent());
		assertFalse(refreshTokenRepository.findByRefreshToken("refresh-token").isPresent());
	}
}
