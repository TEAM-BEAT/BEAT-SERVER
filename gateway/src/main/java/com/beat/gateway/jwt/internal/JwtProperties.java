package com.beat.gateway.jwt.internal;

import java.time.Instant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
	String secret,
	String legacySecret,
	long accessTokenExpireTime,
	long refreshTokenExpireTime,
	String keyId,
	Instant legacyAccessTokenVerifyUntil
) {
}
