package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.beat.apis.support.AbstractIntegrationTest;
import com.beat.contracts.auth.RefreshTokenPort;

class RefreshTokenRepositoryIntegrationTest extends AbstractIntegrationTest {

	private static final Long MEMBER_ID = 1L;
	private static final String REFRESH_TOKEN = "refresh-token";

	@Autowired
	private RefreshTokenPort refreshTokenPort;

	@AfterEach
	void tearDown() {
		refreshTokenPort.deleteRefreshToken(MEMBER_ID);
	}

	@Test
	void refreshTokenPortRoundTripWorksWithRedisBackedGatewayImplementation() {
		refreshTokenPort.saveRefreshToken(MEMBER_ID, REFRESH_TOKEN);

		long loadedMemberId = refreshTokenPort.findMemberIdByRefreshToken(REFRESH_TOKEN).orElseThrow();

		assertEquals(MEMBER_ID, loadedMemberId);

		refreshTokenPort.deleteRefreshToken(MEMBER_ID);

		assertFalse(refreshTokenPort.findMemberIdByRefreshToken(REFRESH_TOKEN).isPresent());
	}
}
