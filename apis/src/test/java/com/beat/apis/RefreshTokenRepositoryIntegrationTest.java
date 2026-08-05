package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.beat.apis.support.AbstractIntegrationTest;
import com.beat.contracts.auth.refreshtoken.RefreshTokenPort;

class RefreshTokenRepositoryIntegrationTest extends AbstractIntegrationTest {

	private static final Long MEMBER_ID = 1L;
	private static final String REFRESH_TOKEN = "refresh-token";
	private static final String LEGACY_TYPE = "com.beat.gateway.refreshtoken.internal.store.RefreshToken";
	private static final String REDIS_KEYSPACE_KEY = "refreshToken";
	private static final String REDIS_KEY = "refreshToken:" + MEMBER_ID;
	private static final String REDIS_INDEX_KEY = "refreshToken:refreshToken:" + REFRESH_TOKEN;
	private static final String REDIS_INDEX_METADATA_KEY = REDIS_KEY + ":idx";

	@Autowired
	private RefreshTokenPort refreshTokenPort;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@AfterEach
	void tearDown() {
		refreshTokenPort.deleteRefreshToken(MEMBER_ID);
		redisTemplate.delete(REDIS_KEY);
		redisTemplate.delete(REDIS_KEYSPACE_KEY);
		redisTemplate.delete(REDIS_INDEX_KEY);
		redisTemplate.delete(REDIS_INDEX_METADATA_KEY);
	}

	@Test
	void refreshTokenPortRoundTripWorksWithRedisBackedInfraImplementation() {
		refreshTokenPort.saveRefreshToken(MEMBER_ID, REFRESH_TOKEN);

		long loadedMemberId = refreshTokenPort.findMemberIdByRefreshToken(REFRESH_TOKEN).orElseThrow();

		assertEquals(MEMBER_ID, loadedMemberId);
		assertEquals(LEGACY_TYPE, redisTemplate.<String, String>opsForHash().get(REDIS_KEY, "_class"));

		refreshTokenPort.deleteRefreshToken(MEMBER_ID);

		assertFalse(refreshTokenPort.findMemberIdByRefreshToken(REFRESH_TOKEN).isPresent());
	}

	@Test
	void legacyGatewayHashCanBeReadAndDeletedByInfraAdapter() {
		redisTemplate.<String, String>opsForHash().putAll(REDIS_KEY, Map.of(
			"_class", LEGACY_TYPE,
			"id", MEMBER_ID.toString(),
			"refreshToken", REFRESH_TOKEN
		));
		redisTemplate.opsForSet().add(REDIS_KEYSPACE_KEY, MEMBER_ID.toString());
		redisTemplate.opsForSet().add(REDIS_INDEX_KEY, MEMBER_ID.toString());
		redisTemplate.opsForSet().add(REDIS_INDEX_METADATA_KEY, REDIS_INDEX_KEY);

		assertEquals(MEMBER_ID, refreshTokenPort.findMemberIdByRefreshToken(REFRESH_TOKEN).orElseThrow());
		assertTrue(refreshTokenPort.deleteRefreshToken(MEMBER_ID));
		assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_KEY)));
	}
}
