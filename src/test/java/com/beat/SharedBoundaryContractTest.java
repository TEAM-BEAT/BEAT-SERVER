package com.beat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class SharedBoundaryContractTest {

	@Test
	void domainRoleNoLongerOwnsSpringSecurityAuthorityBridge() throws Exception {
		String roleSource = Files.readString(Path.of("domain/src/main/java/com/beat/domain/user/domain/Role.java"));
		String buildFile = Files.readString(Path.of("domain/build.gradle.kts"));

		assertFalse(roleSource.contains("GrantedAuthority"));
		assertFalse(roleSource.contains("SimpleGrantedAuthority"));
		assertFalse(buildFile.contains("spring.security.core"));
	}

	@Test
	void globalUtilsBuildFileMustRemainStandalone() throws Exception {
		String buildFile = Files.readString(Path.of("global-utils/build.gradle.kts"));

		assertFalse(buildFile.contains("project(\":"));
		assertFalse(buildFile.contains("org.springframework"));
	}

	@Test
	void redisWiringLeavesOnlyRefreshTokenRepositoryContractInGateway() throws Exception {
		String gatewayRedisConfig = Files.readString(
			Path.of("gateway/src/main/java/com/beat/gateway/config/GatewayRedisConfig.java"));
		String refreshToken = Files.readString(
			Path.of("gateway/src/main/java/com/beat/gateway/jwt/store/RefreshToken.java"));

		assertFalse(Files.exists(Path.of("infra/src/main/java/com/beat/infra/config/RedisConfig.java")));
		assertFalse(Files.exists(Path.of("gateway/src/main/java/com/beat/gateway/redis/LettuceLockRepository.java")));
		assertTrue(gatewayRedisConfig.contains("@EnableRedisRepositories(basePackageClasses = RefreshTokenRepository.class)"));
		assertFalse(gatewayRedisConfig.contains("@Primary"));
		assertFalse(gatewayRedisConfig.contains("beatRedisTemplate"));
		assertTrue(refreshToken.contains("@RedisHash(value = \"refreshToken\", timeToLive = 1209600)"));
		assertTrue(refreshToken.contains("@Indexed"));
	}

	@Test
	void infraKeepsDormantRedisCacheSkeletonForFutureSharedCaching() throws Exception {
		String infraConfigGroup = Files.readString(Path.of("infra/src/main/java/com/beat/infra/InfraBaseConfigGroup.java"));
		String apisInfraConfig = Files.readString(Path.of("apis/src/main/kotlin/com/beat/apis/config/InfraConfig.kt"));
		String adminInfraConfig = Files.readString(Path.of("admin/src/main/kotlin/com/beat/admin/config/InfraConfig.kt"));
		String batchInfraConfig = Files.readString(Path.of("batch/src/main/kotlin/com/beat/batch/config/InfraConfig.kt"));

		assertTrue(Files.exists(Path.of("infra/src/main/java/com/beat/infra/config/RedisCacheConfig.java")));
		assertTrue(infraConfigGroup.contains("REDIS_CACHE(RedisCacheConfig.class)"));
		assertFalse(apisInfraConfig.contains("InfraBaseConfigGroup.REDIS_CACHE"));
		assertFalse(adminInfraConfig.contains("InfraBaseConfigGroup.REDIS_CACHE"));
		assertFalse(batchInfraConfig.contains("InfraBaseConfigGroup.REDIS_CACHE"));
	}

	@Test
	void externalClientBootstrapRemainsExplicitConfigurationSurface() throws Exception {
		String externalClientConfig = Files.readString(
			Path.of("infra/src/main/java/com/beat/infra/config/ExternalClientConfig.java"));

		assertTrue(externalClientConfig.contains("@Configuration(proxyBeanMethods = false)"));
		assertTrue(externalClientConfig.contains("SlackBookingNotificationAdapter.class"));
		assertTrue(externalClientConfig.contains("SlackMemberNotificationAdapter.class"));
	}

	@Test
	void globalUtilsSourcesMustRemainFrameworkAndLayerNeutral() throws Exception {
		List<String> forbiddenReferences = List.of(
			"org.springframework.",
			"jakarta.persistence.",
			"jakarta.servlet.",
			"com.beat.apis.",
			"com.beat.admin.",
			"com.beat.batch.",
			"com.beat.gateway.",
			"com.beat.domain.",
			"com.beat.infra.",
			"com.beat.observability."
		);

		try (var paths = Files.walk(Path.of("global-utils/src/main"))) {
			List<String> violations = paths
				.filter(Files::isRegularFile)
				.filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt"))
				.flatMap(path -> forbiddenReferences.stream()
					.filter(pattern -> contains(path, pattern))
					.map(pattern -> path + ": " + pattern))
				.toList();

			assertTrue(
				violations.isEmpty(),
				"Found forbidden global-utils references:\n" + String.join("\n", violations)
			);
		}
	}

	private boolean contains(Path path, String pattern) {
		try {
			return Files.readString(path).contains(pattern);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read " + path, exception);
		}
	}
}
