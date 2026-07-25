package com.beat.gateway.guest.internal.store;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "guestSession", timeToLive = 1800)
public class GuestSession {

	@Id
	private String tokenHash;

	private Long userId;

	protected GuestSession() {
	}

	private GuestSession(String tokenHash, Long userId) {
		this.tokenHash = tokenHash;
		this.userId = userId;
	}

	public static GuestSession of(String tokenHash, Long userId) {
		return new GuestSession(tokenHash, userId);
	}

	public Long getUserId() {
		return userId;
	}
}
