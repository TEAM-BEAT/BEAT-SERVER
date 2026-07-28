package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.beat.apis.support.AbstractIntegrationTest;
import com.beat.contracts.auth.guest.GuestAccessThrottlePort;
import com.beat.contracts.auth.guest.GuestSessionPort;

class GuestAuthRedisIntegrationTest extends AbstractIntegrationTest {

	private static final long USER_ID = 1L;
	private static final String LEGACY_TOKEN = "legacy-guest-token";
	private static final String LEGACY_TYPE = "com.beat.gateway.guest.internal.store.GuestSession";
	private static final String THROTTLE_KEY_MATERIAL = "guest-access-key";

	@Autowired
	private GuestSessionPort guestSessionPort;

	@Autowired
	private GuestAccessThrottlePort guestAccessThrottlePort;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@AfterEach
	void tearDown() {
		redisTemplate.delete(guestSessionKey(LEGACY_TOKEN));
		guestAccessThrottlePort.reset(THROTTLE_KEY_MATERIAL);
	}

	@Test
	void issuedGuestSessionKeepsLegacyTypeAliasAndRoundTrips() {
		String token = guestSessionPort.issue(USER_ID);
		String redisKey = guestSessionKey(token);
		try {
			assertEquals(USER_ID, guestSessionPort.findUserId(token).orElseThrow());
			assertEquals(LEGACY_TYPE, redisTemplate.<String, String>opsForHash().get(redisKey, "_class"));
		} finally {
			redisTemplate.delete(redisKey);
		}
	}

	@Test
	void legacyGatewayGuestSessionHashCanBeReadByInfraAdapter() {
		String redisKey = guestSessionKey(LEGACY_TOKEN);
		String tokenHash = hashToBase64Url(LEGACY_TOKEN);
		redisTemplate.<String, String>opsForHash().putAll(redisKey, Map.of(
			"_class", LEGACY_TYPE,
			"tokenHash", tokenHash,
			"userId", Long.toString(USER_ID)
		));

		assertEquals(USER_ID, guestSessionPort.findUserId(LEGACY_TOKEN).orElseThrow());
	}

	@Test
	void guestAccessThrottleKeepsExistingLimitAndResetBehavior() {
		assertFalse(guestAccessThrottlePort.isBlocked(THROTTLE_KEY_MATERIAL));

		for (int attempt = 0; attempt < 5; attempt++) {
			guestAccessThrottlePort.recordFailure(THROTTLE_KEY_MATERIAL);
		}

		assertTrue(guestAccessThrottlePort.isBlocked(THROTTLE_KEY_MATERIAL));

		guestAccessThrottlePort.reset(THROTTLE_KEY_MATERIAL);
		assertFalse(guestAccessThrottlePort.isBlocked(THROTTLE_KEY_MATERIAL));
	}

	private String guestSessionKey(String token) {
		return "guestSession:" + hashToBase64Url(token);
	}

	private String hashToBase64Url(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
