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
	void gatewayConfigGroupsExposeOptionalStatefulSecurityServices() {
		assertArrayEquals(
			new GatewayConfigGroup[] {
				GatewayConfigGroup.REFRESH_TOKEN_STORE,
				GatewayConfigGroup.GUEST_ACCESS
			},
			GatewayConfigGroup.values()
		);
	}

	@Test
	void guestAccessGroupOwnsGuestSessionPasswordAndThrottleServices() throws IOException {
		assertEquals(
			"com.beat.gateway.guest.internal.config.GuestAccessConfig",
			GatewayConfigGroup.GUEST_ACCESS.getConfigClass().getName()
		);

		String source = source("src/main/java/com/beat/gateway/guest/internal/config/GuestAccessConfig.java");

		assertTrue(source.contains("RedisConfig.class"));
		assertTrue(source.contains("GuestSessionService.class"));
		assertTrue(source.contains("GuestPasswordHashService.class"));
		assertTrue(source.contains("GuestAccessThrottleService.class"));
	}

	@Test
	void publicServletSecurityAnnotationIsStaticImportSurfaceForExecutableModules() throws IOException {
		String annotationSource = source("src/main/java/com/beat/gateway/EnableGatewayServletSecurity.java");
		String source = source("src/main/java/com/beat/gateway/authentication/internal/config/ServletSecurityConfig.java");

		assertTrue(annotationSource.contains("@Import(ServletSecurityConfig.class)"));
		assertTrue(source.contains("JwtConfig.class"));
		assertTrue(source.contains("SecurityFilterConfig.class"));
		assertTrue(source.contains("WebMvcConfig.class"));
		assertFalse(source.contains("RefreshTokenConfig.class"));
		assertFalse(source.contains("RedisConfig.class"));
		assertFalse(source.contains("RefreshTokenService.class"));
	}

	@Test
	void securityMdcFilterIsPartOfServletSecurityGroupAndAutoRegistrationIsDisabled() throws IOException {
		String servletSource =
			source("src/main/java/com/beat/gateway/authentication/internal/config/SecurityFilterConfig.java");

		assertTrue(servletSource.contains("gatewaySecurityMdcLoggingFilter"));
		assertTrue(servletSource.contains("FilterRegistrationBean<SecurityMdcLoggingFilter>"));
		assertTrue(servletSource.contains("registration.setEnabled(false)"));
	}

	@Test
	void refreshTokenStoreGroupOwnsRedisRepositoryAndRefreshTokenService() throws IOException {
		assertEquals(
			"com.beat.gateway.refreshtoken.internal.config.RefreshTokenConfig",
			GatewayConfigGroup.REFRESH_TOKEN_STORE.getConfigClass().getName()
		);

		String source = source("src/main/java/com/beat/gateway/refreshtoken/internal/config/RefreshTokenConfig.java");

		assertTrue(source.contains("RedisConfig.class"));
		assertTrue(source.contains("RefreshTokenService.class"));
	}

	@Test
	void jwtFilterEnrichesAlreadyInitializedMdcWithAuthenticatedUser() throws IOException {
		String source = source("src/main/java/com/beat/gateway/authentication/internal/JwtAuthenticationFilter.java");

		assertTrue(source.contains("MDC.put(BaseMdcLoggingFilter.USER_ID_KEY, Long.toString(memberId))"));
	}

	@Test
	void jwtConfigRegistersJwtProviderOnly() throws IOException {
		String source = source("src/main/java/com/beat/gateway/jwt/internal/config/JwtConfig.java");

		assertTrue(source.contains("JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties)"));
		assertFalse(source.contains("RefreshTokenService.class"));
		assertFalse(source.contains("RedisConfig.class"));
	}

	private String source(String path) throws IOException {
		return Files.readString(Path.of(path));
	}
}
