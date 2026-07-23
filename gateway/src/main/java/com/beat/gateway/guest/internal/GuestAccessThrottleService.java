package com.beat.gateway.guest.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.beat.contracts.auth.guest.GuestAccessThrottlePort;

public final class GuestAccessThrottleService implements GuestAccessThrottlePort {

	private static final long MAX_FAILURES = 5;
	private static final Duration WINDOW = Duration.ofMinutes(10);
	private static final String KEY_PREFIX = "guest-access-failure:";
	private static final DefaultRedisScript<Long> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>(
		"local value = redis.call('INCR', KEYS[1]); "
			+ "if value == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
			+ "return value;",
		Long.class
	);

	private final StringRedisTemplate redisTemplate;

	public GuestAccessThrottleService(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public boolean isBlocked(String keyMaterial) {
		String attempts = redisTemplate.opsForValue().get(key(keyMaterial));
		return attempts != null && Long.parseLong(attempts) >= MAX_FAILURES;
	}

	@Override
	public void recordFailure(String keyMaterial) {
		redisTemplate.execute(
			RECORD_FAILURE_SCRIPT,
			List.of(key(keyMaterial)),
			Long.toString(WINDOW.toSeconds())
		);
	}

	@Override
	public void reset(String keyMaterial) {
		redisTemplate.delete(key(keyMaterial));
	}

	private String key(String keyMaterial) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
			return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
