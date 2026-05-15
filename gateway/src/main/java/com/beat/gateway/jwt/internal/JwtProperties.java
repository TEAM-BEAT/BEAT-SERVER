package com.beat.gateway.jwt.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
	String secret,
	long accessTokenExpireTime,
	long refreshTokenExpireTime,
	String keyId
) {
}
