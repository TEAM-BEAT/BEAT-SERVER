package com.beat.global.auth.redis;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash(value = "refreshToken", timeToLive = 1209600)
@Getter
@Builder
public class Token {

	@Id
	private Long id;

	@Indexed
	private String refreshToken;

	public static Token of(final Long id, final String refreshToken) {
		return Token.builder()
			.id(id)
			.refreshToken(refreshToken)
			.build();
	}
}
