package com.beat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
		assertTrue(
			gatewayRedisConfig.contains("@EnableRedisRepositories(basePackageClasses = RefreshTokenRepository.class)"));
		assertFalse(gatewayRedisConfig.contains("@Primary"));
		assertFalse(gatewayRedisConfig.contains("beatRedisTemplate"));
		assertTrue(refreshToken.contains("@RedisHash(value = \"refreshToken\", timeToLive = 1209600)"));
		assertTrue(refreshToken.contains("@Indexed"));
	}

	@Test
	void infraKeepsDormantRedisCacheSkeletonForFutureSharedCaching() throws Exception {
		String infraConfigGroup = Files.readString(
			Path.of("infra/src/main/java/com/beat/infra/InfraBaseConfigGroup.java"));
		String redisCacheConfig = Files.readString(
			Path.of("infra/src/main/java/com/beat/infra/config/RedisCacheConfig.java"));

		assertTrue(Files.exists(Path.of("infra/src/main/java/com/beat/infra/config/RedisCacheConfig.java")));
		assertTrue(infraConfigGroup.contains("REDIS_CACHE(RedisCacheConfig.class)"));
		assertFalse(redisCacheConfig.contains("@EnableCaching"));
		assertFalse(redisCacheConfig.contains("org.springframework.cache.CacheManager"));
		assertFalse(redisCacheConfig.contains("@Bean"));

		List<Path> executableSources = sourceFiles(
			Path.of("apis/src/main"),
			Path.of("admin/src/main"),
			Path.of("batch/src/main")
		);
		List<String> violations = executableSources.stream()
			.filter(path -> contains(path, "InfraBaseConfigGroup.REDIS_CACHE"))
			.map(Path::toString)
			.toList();

		assertTrue(
			violations.isEmpty(),
			"Executable modules must not opt into dormant REDIS_CACHE yet:\n" + String.join("\n", violations)
		);
	}

	@Test
	void observabilityAopSourcesNoLongerUseLegacyGlobalCommonPackage() throws Exception {
		Set<String> expectedAopFiles = Set.of(
			"observability/src/main/java/com/beat/observability/aop/ControllerLoggingAspect.java",
			"observability/src/main/java/com/beat/observability/aop/ExecutionTimeLoggerAspect.java",
			"observability/src/main/java/com/beat/observability/aop/Pointcuts.java",
			"observability/src/main/java/com/beat/observability/aop/ServiceLoggingAspect.java",
			"observability/src/main/java/com/beat/observability/aop/TxAspect.java"
		);
		List<Path> observabilitySources = sourceFiles(Path.of("observability/src/main"));
		List<String> violations = observabilitySources.stream()
			.filter(path -> contains(path, "com.beat.global.common.aop"))
			.map(Path::toString)
			.toList();
		Set<String> actualAopFiles = sourceFiles(
			Path.of("observability/src/main/java/com/beat/observability/aop")).stream()
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());
		String pointcuts = Files.readString(
			Path.of("observability/src/main/java/com/beat/observability/aop/Pointcuts.java"));

		assertFalse(Files.exists(Path.of("observability/src/main/java/com/beat/global/common/aop")));
		assertEquals(expectedAopFiles, actualAopFiles);
		assertTrue(violations.isEmpty(),
			"Found legacy observability AOP package references:\n" + String.join("\n", violations));
		assertTrue(pointcuts.contains("!within(com.beat.global..*)"));
		assertTrue(pointcuts.contains("!within(com.beat.observability..*)"));
	}

	@Test
	void observabilityModuleConfigDoesNotActivateAopSurfaceImplicitly() throws Exception {
		String moduleConfig = Files.readString(
			Path.of("observability/src/main/kotlin/com/beat/observability/ObservabilityModuleConfig.kt"));
		String uncommentedModuleConfig = stripComments(moduleConfig);

		assertFalse(uncommentedModuleConfig.contains("@ComponentScan"));
		assertFalse(uncommentedModuleConfig.contains("@Import"));
	}

	@Test
	void infraDomainPackageResidueIsLimitedToKnownDeferredQueryImplementations() throws Exception {
		Set<String> allowedResidue = Set.of(
			"infra/src/main/java/com/beat/domain/booking/dao/TicketRepositoryCustomImpl.java",
			"infra/src/main/java/com/beat/domain/schedule/dao/ScheduleRepositoryCustomImpl.java"
		);

		Set<String> actualResidue = sourceFiles(Path.of("infra/src/main")).stream()
			.filter(path -> contains(path, "package com.beat.domain."))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());

		assertEquals(allowedResidue, actualResidue);
	}

	@Test
	void moduleContractsStayImplementationFreeJavaContracts() throws Exception {
		String buildFile = Files.readString(Path.of("module-contracts/build.gradle.kts"));
		List<String> forbiddenReferences = List.of(
			"@Component",
			"@Service",
			"@Repository",
			"@Configuration",
			"jakarta.persistence.",
			"org.springframework.data.redis.",
			"com.beat.apis.",
			"com.beat.admin.",
			"com.beat.batch.",
			"com.beat.infra.",
			"com.beat.gateway."
		);

		List<String> violations = sourceFiles(Path.of("module-contracts/src/main")).stream()
			.flatMap(path -> forbiddenReferences.stream()
				.filter(pattern -> contains(path, pattern))
				.map(pattern -> path + ": " + pattern))
			.toList();

		assertTrue(buildFile.contains("compileOnly(project(\":domain\"))"));
		assertTrue(buildFile.contains("compileOnly(project(\":global-utils\"))"));
		assertFalse(buildFile.contains("implementation(project(\":domain\"))"));
		assertFalse(buildFile.contains("implementation(project(\":global-utils\"))"));
		assertTrue(
			violations.isEmpty(),
			"Found forbidden module-contracts implementation references:\n" + String.join("\n", violations)
		);
	}

	@Test
	void moduleContractsDomainTypeCouplingIsExplicitAndBounded() throws Exception {
		Set<String> allowedDomainImports = Set.of(
			"module-contracts/src/main/java/com/beat/contracts/auth/social/SocialLoginCommand.java: import com.beat.domain.member.domain.SocialType;",
			"module-contracts/src/main/java/com/beat/contracts/auth/social/SocialMemberInfo.java: import com.beat.domain.member.domain.SocialType;",
			"module-contracts/src/main/java/com/beat/contracts/schedule/ScheduleJobPort.java: import com.beat.domain.schedule.domain.Schedule;"
		);

		Set<String> actualDomainImports = sourceFiles(Path.of("module-contracts/src/main")).stream()
			.flatMap(path -> readLines(path).stream()
				.filter(line -> line.startsWith("import com.beat.domain."))
				.map(line -> path.toString().replace('\\', '/') + ": " + line))
			.collect(Collectors.toSet());

		assertEquals(allowedDomainImports, actualDomainImports);
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

	private List<String> readLines(Path path) {
		try {
			return Files.readAllLines(path);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read " + path, exception);
		}
	}

	private String stripComments(String source) {
		return source
			.replaceAll("(?s)/\\*.*?\\*/", "")
			.replaceAll("(?m)//.*$", "");
	}

	private List<Path> sourceFiles(Path... roots) throws IOException {
		List<Path> result = new ArrayList<>();
		for (Path root : roots) {
			if (!Files.exists(root)) {
				continue;
			}
			try (var paths = Files.walk(root)) {
				paths
					.filter(Files::isRegularFile)
					.filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".kt"))
					.forEach(result::add);
			}
		}
		return result;
	}
}
