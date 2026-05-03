package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.beat.apis.support.AbstractIntegrationTest;
import com.beat.contracts.auth.RefreshTokenPort;
import com.beat.contracts.auth.TokenErrorCode;
import com.beat.global.common.exception.NotFoundException;

class RefreshTokenRepositoryIntegrationTest extends AbstractIntegrationTest {

	private static final Long MEMBER_ID = 1L;
	private static final String REFRESH_TOKEN = "refresh-token";

	@Autowired
	private RefreshTokenPort refreshTokenPort;

	@AfterEach
	void tearDown() {
		try {
			refreshTokenPort.deleteRefreshToken(MEMBER_ID);
		} catch (NotFoundException ignored) {
			// Already deleted by the test path.
		}
	}

	@Test
	void refreshTokenPortRoundTripWorksWithRedisBackedGatewayImplementation() {
		refreshTokenPort.saveRefreshToken(MEMBER_ID, REFRESH_TOKEN);

		Long loadedMemberId = refreshTokenPort.findMemberIdByRefreshToken(REFRESH_TOKEN);

		assertEquals(MEMBER_ID, loadedMemberId);

		refreshTokenPort.deleteRefreshToken(MEMBER_ID);

		NotFoundException exception = assertThrows(
			NotFoundException.class,
			() -> refreshTokenPort.findMemberIdByRefreshToken(REFRESH_TOKEN)
		);
		assertEquals(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getBaseErrorCode());
	}
}
