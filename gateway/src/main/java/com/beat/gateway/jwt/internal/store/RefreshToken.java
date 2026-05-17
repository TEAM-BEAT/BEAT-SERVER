package com.beat.gateway.jwt.internal.store;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RedisHash(value = "refreshToken", timeToLive = 1209600)
public class RefreshToken {

	@Id
	private Long id;

	@Indexed
	private String refreshToken;

	public static RefreshToken of(Long id, String refreshToken) {
		return RefreshToken.builder()
			.id(id)
			.refreshToken(refreshToken)
			.build();
	}
}
