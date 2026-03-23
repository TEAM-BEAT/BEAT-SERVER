package com.beat.gateway.redis;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class LettuceLockRepository {

	private final RedisTemplate<String, Object> redisTemplate;

	public Boolean lock(String token, String lockType) {
		return redisTemplate
			.opsForValue()
			.setIfAbsent(token, lockType, Duration.ofSeconds(3L));
	}

	public void unlock(String token) {
		redisTemplate.delete(token);
	}
}
