package com.beat.gateway;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GatewayConfigGroupTest {

	@Test
	void gatewayConfigGroupsExposeServletSecurityAndRefreshTokenStoreOnly() {
		assertArrayEquals(
			new GatewayConfigGroup[] {
				GatewayConfigGroup.SERVLET_SECURITY,
				GatewayConfigGroup.REFRESH_TOKEN_STORE
			},
			GatewayConfigGroup.values()
		);
	}

	@Test
	void servletSecurityGroupDoesNotImportRefreshTokenStore() throws IOException {
		assertEquals(
			"com.beat.gateway.internal.config.GatewayServletSecurityConfig",
			GatewayConfigGroup.SERVLET_SECURITY.getConfigClass().getName()
		);

		String source = source("src/main/java/com/beat/gateway/internal/config/GatewayServletSecurityConfig.java");

		assertTrue(source.contains("GatewayJwtConfig.class"));
		assertTrue(source.contains("GatewaySecurityServletConfig.class"));
		assertTrue(source.contains("GatewayWebMvcConfig.class"));
		assertFalse(source.contains("GatewayRefreshTokenConfig.class"));
		assertFalse(source.contains("GatewayRedisConfig.class"));
		assertFalse(source.contains("RefreshTokenService.class"));
	}

	@Test
	void refreshTokenStoreGroupOwnsRedisRepositoryAndRefreshTokenService() throws IOException {
		assertEquals(
			"com.beat.gateway.internal.config.GatewayRefreshTokenConfig",
			GatewayConfigGroup.REFRESH_TOKEN_STORE.getConfigClass().getName()
		);

		String source = source("src/main/java/com/beat/gateway/internal/config/GatewayRefreshTokenConfig.java");

		assertTrue(source.contains("GatewayRedisConfig.class"));
		assertTrue(source.contains("RefreshTokenService.class"));
	}

	@Test
	void jwtConfigImportsJwtProviderOnly() throws IOException {
		String source = source("src/main/java/com/beat/gateway/internal/config/GatewayJwtConfig.java");

		assertTrue(source.contains("JwtTokenProvider.class"));
		assertFalse(source.contains("RefreshTokenService.class"));
		assertFalse(source.contains("GatewayRedisConfig.class"));
	}

	private String source(String path) throws IOException {
		return Files.readString(Path.of(path));
	}
}
