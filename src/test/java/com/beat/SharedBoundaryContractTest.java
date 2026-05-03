package com.beat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class SharedBoundaryContractTest {

	@Test
	void migrationRuntimeBaselineMatchesCurrentBuildAndDockerSettings() throws Exception {
		String migration = Files.readString(Path.of("MIGRATION.md"));
		String sdkman = Files.readString(Path.of(".sdkmanrc"));
		String rootBuild = Files.readString(Path.of("build.gradle.kts"));
		String buildLogic = Files.readString(Path.of("build-logic/src/main/kotlin/beat.kotlin-base.gradle.kts"));
		String buildLogicBuild = Files.readString(Path.of("build-logic/build.gradle.kts"));
		String dockerfile = Files.readString(Path.of("Dockerfile.module"));
		String versions = Files.readString(Path.of("gradle/libs.versions.toml"));

		assertTrue(versions.contains("spring-boot = \"4.0.5\""));
		assertTrue(versions.contains("kotlin = \"2.3.20\""));
		assertTrue(migration.contains("Spring Boot `4.0.5`"));
		assertTrue(migration.contains("Kotlin `2.3.20`"));
		assertTrue(migration.contains("Docker runtime Java `25`"));

		assertTrue(sdkman.contains("java=25.0.2-tem"));
		assertTrue(sdkman.contains("Runtime target: Java 25"));
		assertFalse(sdkman.contains("Runtime target: Java 21"));

		assertTrue(rootBuild.contains("JavaLanguageVersion.of(25)"));
		assertTrue(rootBuild.contains("options.release.set(25)"));
		assertTrue(rootBuild.contains("JvmTarget.JVM_25"));
		assertTrue(buildLogic.contains("JavaLanguageVersion.of(25)"));
		assertTrue(buildLogic.contains("options.release.set(25)"));
		assertTrue(buildLogic.contains("JvmTarget.JVM_25"));
		assertTrue(buildLogicBuild.contains("JavaLanguageVersion.of(25)"));
		assertTrue(buildLogicBuild.contains("options.release.set(25)"));
		assertTrue(buildLogicBuild.contains("JvmTarget.JVM_25"));

		assertTrue(dockerfile.contains("eclipse-temurin:25-jdk"));
		assertTrue(dockerfile.contains("eclipse-temurin:25-jre-alpine"));
	}

	@Test
	void domainRoleNoLongerOwnsSpringSecurityAuthorityBridge() throws Exception {
		String roleSource = Files.readString(Path.of("domain/src/main/kotlin/com/beat/domain/user/domain/Role.kt"));
		String buildFile = Files.readString(Path.of("domain/build.gradle.kts"));

		assertFalse(roleSource.contains("GrantedAuthority"));
		assertFalse(roleSource.contains("SimpleGrantedAuthority"));
		assertFalse(buildFile.contains("spring.security.core"));
	}

	@Test
	void domainMainSourceIsKotlinOnlyAfterPortAndEnumMigration() throws Exception {
		assertFalse(Files.exists(Path.of("domain/src/main/java")),
			"domain/src/main must not reintroduce Java sources; domain model, enum, ErrorCode, and repository contracts are Kotlin-owned");
		assertTrue(Files.exists(Path.of("domain/src/main/kotlin/com/beat/domain")));
	}

	@Test
	void domainModuleRemainsLombokFreeAfterEnumMigration() throws Exception {
		String domainBuild = Files.readString(Path.of("domain/build.gradle.kts"));
		List<String> lombokSources = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> contains(path, "lombok.") || contains(path, "@Getter")
				|| contains(path, "@RequiredArgsConstructor") || contains(path, "@AllArgsConstructor"))
			.map(path -> path.toString().replace('\\', '/'))
			.toList();

		assertFalse(domainBuild.contains("lombok"),
			"Domain must stay pure api(project(\":global-utils\")) without Lombok build dependencies");
		assertTrue(lombokSources.isEmpty(),
			"Domain source must use explicit constructors/getters instead of Lombok:\n"
				+ String.join("\n", lombokSources));
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
	void infraBaseConfigMarkerIsLimitedToSelectableTopLevelGroups() throws Exception {
		String infraBaseConfig = Files.readString(Path.of("infra/src/main/java/com/beat/infra/InfraBaseConfig.java"));
		String infraConfigGroup = Files.readString(Path.of("infra/src/main/java/com/beat/infra/InfraBaseConfigGroup.java"));

		List<String> topLevelConfigSources = List.of(
			"infra/src/main/java/com/beat/infra/config/AsyncConfig.java",
			"infra/src/main/java/com/beat/infra/config/ExternalClientConfig.java",
			"infra/src/main/java/com/beat/infra/config/JpaConfig.java",
			"infra/src/main/java/com/beat/infra/config/RedisCacheConfig.java"
		);
		List<String> supportConfigSources = List.of(
			"infra/src/main/java/com/beat/infra/config/TaskExecutorConfig.java",
			"infra/src/main/java/com/beat/infra/config/ThreadPoolProperties.java",
			"infra/src/main/java/com/beat/infra/persistence/InfraPersistenceConfig.java",
			"infra/src/main/java/com/beat/infra/storage/s3/S3InfraConfig.java"
		);

		assertTrue(infraBaseConfig.contains("Marker for top-level infra bootstrap configurations"));
		assertTrue(infraBaseConfig.contains("Support configurations"));
		assertFalse(Files.exists(Path.of("infra/src/main/kotlin/com/beat/infra/InfraModuleConfig.kt")),
			"InfraModuleConfig must not compete with @EnableInfraBaseConfig as a module-wide entrypoint");

		for (String sourcePath : topLevelConfigSources) {
			String source = Files.readString(Path.of(sourcePath));
			String simpleName = Path.of(sourcePath).getFileName().toString().replace(".java", "");
			assertTrue(infraConfigGroup.contains(simpleName + ".class"));
			assertTrue(source.contains("InfraBaseConfig"), sourcePath);
			assertTrue(Pattern.compile("class\\s+" + simpleName + "[^{]*implements[^{]*InfraBaseConfig").matcher(source).find(),
				sourcePath);
		}

		for (String sourcePath : supportConfigSources) {
			String source = Files.readString(Path.of(sourcePath));
			assertFalse(source.contains("implements InfraBaseConfig"), sourcePath);
		}
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
		Set<String> allowedResidue = Set.of();

		Set<String> actualResidue = sourceFiles(Path.of("infra/src/main")).stream()
			.filter(path -> contains(path, "package com.beat.domain."))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());

		assertEquals(allowedResidue, actualResidue);
	}

	@Test
	void infraBaseTimeEntityOwnsAuditingMappedSuperclassContract() throws Exception {
		String baseTimeEntity = Files.readString(
			Path.of("infra/src/main/kotlin/com/beat/infra/persistence/common/BaseTimeEntity.kt"));
		String memberEntity = Files.readString(
			Path.of("infra/src/main/kotlin/com/beat/infra/persistence/member/entity/MemberJpaEntity.kt"));
		String performanceEntity = Files.readString(
			Path.of("infra/src/main/kotlin/com/beat/infra/persistence/performance/entity/PerformanceJpaEntity.kt"));

		assertTrue(baseTimeEntity.contains("package com.beat.infra.persistence.common"));
		assertTrue(baseTimeEntity.contains("@MappedSuperclass"));
		assertTrue(baseTimeEntity.contains("@EntityListeners(AuditingEntityListener::class)"));
		assertTrue(baseTimeEntity.contains("@field:CreatedDate"));
		assertTrue(baseTimeEntity.contains("@field:Column(updatable = false)"));
		assertTrue(baseTimeEntity.contains("@field:LastModifiedDate"));
		assertTrue(baseTimeEntity.contains("var createdAt: LocalDateTime? = null"));
		assertTrue(baseTimeEntity.contains("var updatedAt: LocalDateTime? = null"));
		assertTrue(baseTimeEntity.contains("protected set"));
		assertTrue(memberEntity.contains("import com.beat.infra.persistence.common.BaseTimeEntity"));
		assertTrue(performanceEntity.contains("import com.beat.infra.persistence.common.BaseTimeEntity"));
	}

	@Test
	void infraPersistenceBootstrapUsesSingleMarkerAndNoDomainSpecificConfig() throws Exception {
		Set<String> requiredInfraPersistenceFiles = new HashSet<>(Set.of(
			"infra/src/main/java/com/beat/infra/persistence/InfraPersistenceConfig.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/common/BaseTimeEntity.kt",
			"infra/src/main/java/com/beat/infra/persistence/InfraPersistenceMarker.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/booking/entity/BookingJpaEntity.kt",
			"infra/src/main/java/com/beat/infra/persistence/booking/mapper/BookingPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/booking/repository/BookingJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/booking/repository/BookingRepositoryImpl.java",
			"infra/src/main/java/com/beat/infra/persistence/booking/repository/query/MakerTicketReadPortImpl.java",
			promotionJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/promotion/mapper/PromotionPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionRepositoryImpl.java",
			usersJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/user/mapper/UsersPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/user/repository/UsersJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/user/repository/UsersRepositoryImpl.java",
			performanceJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/performance/mapper/PerformancePersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/performance/repository/PerformanceJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/performance/repository/PerformanceRepositoryImpl.java",
			scheduleJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/schedule/mapper/SchedulePersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/schedule/repository/ScheduleJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/schedule/repository/ScheduleRepositoryImpl.java"
		));
		requiredInfraPersistenceFiles.addAll(bookingInfraPersistenceSourcePathsIfPresent());
		Set<String> allowedInfraPersistenceFiles = new HashSet<>(Set.of(
			"infra/src/main/java/com/beat/infra/persistence/InfraPersistenceConfig.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/common/BaseTimeEntity.kt",
			"infra/src/main/java/com/beat/infra/persistence/InfraPersistenceMarker.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/booking/entity/BookingJpaEntity.kt",
			"infra/src/main/java/com/beat/infra/persistence/booking/mapper/BookingPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/booking/repository/BookingJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/booking/repository/BookingRepositoryImpl.java",
			"infra/src/main/java/com/beat/infra/persistence/booking/repository/query/MakerTicketReadPortImpl.java",
			promotionJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/promotion/mapper/PromotionPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionRepositoryImpl.java",
			castJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/cast/mapper/CastPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/cast/repository/CastJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/cast/repository/CastRepositoryImpl.java",
			staffJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/staff/mapper/StaffPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/staff/repository/StaffJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/staff/repository/StaffRepositoryImpl.java",
			performanceImageJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/performanceimage/mapper/PerformanceImagePersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/performanceimage/repository/PerformanceImageJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/performanceimage/repository/PerformanceImageRepositoryImpl.java",
			usersJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/user/mapper/UsersPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/user/repository/UsersJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/user/repository/UsersRepositoryImpl.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/member/entity/MemberJpaEntity.kt",
			"infra/src/main/java/com/beat/infra/persistence/member/mapper/MemberPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/member/repository/MemberJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/member/repository/MemberRepositoryImpl.java",
			performanceJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/performance/mapper/PerformancePersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/performance/repository/PerformanceJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/performance/repository/PerformanceRepositoryImpl.java",
			scheduleJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/schedule/mapper/SchedulePersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/schedule/repository/ScheduleJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/schedule/repository/ScheduleRepositoryImpl.java",
			"infra/src/main/java/com/beat/infra/persistence/schedule/repository/query/ScheduleQueryRepositoryImpl.java"
		));
		allowedInfraPersistenceFiles.addAll(bookingInfraPersistenceSourcePathsIfPresent());

		Set<String> actualInfraPersistenceFiles = sourceFiles(
			Path.of("infra/src/main/java/com/beat/infra/persistence"),
			Path.of("infra/src/main/kotlin/com/beat/infra/persistence")
		)
			.stream()
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());
		String persistenceConfig = Files.readString(
			Path.of("infra/src/main/java/com/beat/infra/persistence/InfraPersistenceConfig.java"));

		assertTrue(actualInfraPersistenceFiles.containsAll(requiredInfraPersistenceFiles));
		assertTrue(allowedInfraPersistenceFiles.containsAll(actualInfraPersistenceFiles),
			"Unexpected infra persistence files:\n" + actualInfraPersistenceFiles.stream()
				.filter(path -> !allowedInfraPersistenceFiles.contains(path))
				.collect(Collectors.joining("\n")));
		assertPairedPackagePresence(actualInfraPersistenceFiles, "/cast/", "/staff/");
		assertTrue(persistenceConfig.contains("@ComponentScan(basePackageClasses = InfraPersistenceMarker.class)"));
		assertFalse(persistenceConfig.contains("PromotionPersistenceConfig"));
		assertFalse(Files.exists(
			Path.of(
				"infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionPersistenceConfig.java")));
	}

	@Test
	void performanceOwnershipChecksUseLongValueEquality() throws Exception {
		List<Path> serviceSources = List.of(
			Path.of("apis/src/main/java/com/beat/apis/performance/application/PerformanceManagementService.java"),
			Path.of("apis/src/main/java/com/beat/apis/performance/application/PerformanceModifyService.java"),
			Path.of("apis/src/main/java/com/beat/apis/performance/application/PerformanceService.java")
		);

		Pattern boxedLongIdentityComparison = Pattern.compile(
			"performance\\.getUserId\\(\\)\\s*!=\\s*userId|userId\\s*!=\\s*performance\\.getUserId\\(\\)"
		);
		List<String> violations = serviceSources.stream()
			.filter(path -> matches(path, boxedLongIdentityComparison))
			.map(Path::toString)
			.toList();

		assertTrue(violations.isEmpty(),
			"Performance ownership checks must compare boxed Long values with Objects.equals:\n"
				+ String.join("\n", violations));
	}

	@Test
	void rawDomainHelperCleanupDropsUnusedPublicLookupHelpers() throws Exception {
		String userService = Files.readString(Path.of("apis/src/main/java/com/beat/apis/user/application/UserService.java"));
		String performanceService = Files.readString(
			Path.of("apis/src/main/java/com/beat/apis/performance/application/PerformanceService.java"));

		assertFalse(userService.contains("findAllUsers("));
		assertFalse(userService.contains("findUserByUserId("));
		assertFalse(performanceService.contains("public Performance findById"));
	}

	@Test
	void scheduleMigrationPersistsImmutableCopiesAndHandlesDetachedBookingRows() throws Exception {
		String performanceManagement = Files.readString(
			Path.of("apis/src/main/java/com/beat/apis/performance/application/PerformanceManagementService.java"));
		String performanceModify = Files.readString(
			Path.of("apis/src/main/java/com/beat/apis/performance/application/PerformanceModifyService.java"));
		String bookingRepository = Files.readString(
			Path.of("domain/src/main/kotlin/com/beat/domain/booking/repository/BookingRepository.kt"));
		String bookingJpaRepository = Files.readString(
			Path.of("infra/src/main/java/com/beat/infra/persistence/booking/repository/BookingJpaRepository.java"));
		String bookingRepositoryImpl = Files.readString(
			Path.of("infra/src/main/java/com/beat/infra/persistence/booking/repository/BookingRepositoryImpl.java"));
		String ticketService = Files.readString(
			Path.of("apis/src/main/java/com/beat/apis/ticket/application/TicketService.java"));
		String bookingCancelService = Files.readString(
			Path.of("apis/src/main/java/com/beat/apis/booking/application/BookingCancelService.java"));
		String guestBookingRetrieveService = Files.readString(
			Path.of("apis/src/main/java/com/beat/apis/booking/application/GuestBookingRetrieveService.java"));
		String memberBookingRetrieveService = Files.readString(
			Path.of("apis/src/main/java/com/beat/apis/booking/application/MemberBookingRetrieveService.java"));

		assertTrue(performanceManagement.contains("schedules = scheduleRepository.saveAll(schedules);"));
		assertTrue(performanceModify.contains("schedules = scheduleRepository.saveAll(schedules);"));
		assertTrue(performanceModify.contains("scheduleRepository.lockById(request.scheduleId())"));
		assertTrue(performanceManagement.contains("bookingRepository.deleteInactiveBookingsByScheduleIds("));
		assertTrue(performanceModify.contains("bookingRepository.deleteInactiveBookingsByScheduleIds("));
		assertTrue(performanceModify.contains("bookingRepository.existsActiveBookingByScheduleIds(scheduleIds"));
		assertTrue(performanceManagement.contains("deletedInactiveBookingCount"));
		assertTrue(performanceModify.contains("deletedInactiveBookingCount"));
		assertFalse(Files.exists(Path.of("domain/src/main/kotlin/com/beat/domain/booking/dao")),
			"Booking domain repository ports must live under domain.booking.repository, not legacy dao");
		assertFalse(Files.exists(Path.of("domain/src/main/kotlin/com/beat/domain/booking/repository/TicketRepository.kt")));
		assertFalse(bookingRepository.contains("org.springframework.data"));
		assertFalse(bookingRepository.contains("@Query"));
		assertTrue(bookingRepository.contains("fun deleteInactiveBookingsByScheduleIds("));
		assertFalse(bookingRepository.contains("@Modifying"));
		assertFalse(bookingRepository.contains("DELETE FROM Booking b WHERE b.scheduleId IN :scheduleIds"));
		assertTrue(bookingJpaRepository.contains("@Modifying(clearAutomatically = true, flushAutomatically = true)"));
		assertTrue(bookingJpaRepository.contains("DELETE FROM Booking b WHERE b.scheduleId IN :scheduleIds"));
		assertTrue(bookingJpaRepository.contains("int deleteInactiveBookingsByScheduleIds("));
		assertTrue(bookingRepositoryImpl.contains("scheduleIds == null || scheduleIds.isEmpty()"));
		assertTrue(ticketService.contains("findScheduleForTicket(scheduleMap, ticket)"));
		assertTrue(ticketService.contains("throw new NotFoundException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND)"));
		assertTrue(ticketService.contains("scheduleRepository.lockById(booking.getScheduleId())"));
		assertTrue(bookingCancelService.contains("@Transactional"));
		assertTrue(bookingCancelService.contains("scheduleRepository.lockById(booking.getScheduleId())"));
		assertTrue(guestBookingRetrieveService.contains("scheduleRepository.findAllById(scheduleIds)"));
		assertTrue(guestBookingRetrieveService.contains("performanceRepository.findAllById(performanceIds)"));
		assertFalse(guestBookingRetrieveService.contains("scheduleRepository.findById(booking.getScheduleId())"));
		assertFalse(guestBookingRetrieveService.contains("performanceRepository.findById(schedule.getPerformanceId())"));
		assertTrue(memberBookingRetrieveService.contains("scheduleRepository.findAllById(scheduleIds)"));
		assertTrue(memberBookingRetrieveService.contains("performanceRepository.findAllById(performanceIds)"));
		assertFalse(memberBookingRetrieveService.contains("scheduleRepository.findById(booking.getScheduleId())"));
		assertFalse(memberBookingRetrieveService.contains("performanceRepository.findById(schedule.getPerformanceId())"));
	}

	@Test
	void makerTicketReadAdapterAvoidsManualScheduleQueryDslType() throws Exception {
		String makerTicketReadAdapter = Files.readString(
			Path.of("infra/src/main/java/com/beat/infra/persistence/booking/repository/query/MakerTicketReadPortImpl.java"));

		assertFalse(Files.exists(
			Path.of("infra/src/main/java/com/beat/infra/persistence/schedule/entity/QScheduleJpaEntity.java")));
		assertFalse(makerTicketReadAdapter.contains("QScheduleJpaEntity"));
		assertFalse(makerTicketReadAdapter.contains("com.querydsl"));
		assertTrue(makerTicketReadAdapter.contains("TypedQuery<BookingJpaEntity>"));
		assertTrue(makerTicketReadAdapter.contains("FROM Booking b, Schedule s"));
	}

	@Test
	void domainPersistenceConcernSourcesRemainAbsentAfterInfraMove() throws Exception {
		Set<String> allowedPersistenceConcernSources = Set.of();
		List<String> forbiddenPersistencePatterns = List.of(
			"jakarta.persistence.",
			"org.hibernate.annotations.",
			"org.springframework.data.",
			"org.springframework.data.domain.",
			"org.springframework.data.jpa.repository.",
			"org.springframework.data.repository.",
			"org.springframework.transaction.annotation.Transactional",
			"com.querydsl.",
			"@QueryProjection",
			"@Lock",
			"@Modifying",
			"@CreatedDate",
			"@LastModifiedDate",
			"Page<",
			"Pageable",
			"Sort"
		);

		Set<String> actualPersistenceConcernSources = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> forbiddenPersistencePatterns.stream().anyMatch(pattern -> contains(path, pattern)))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());

		assertEquals(allowedPersistenceConcernSources, actualPersistenceConcernSources,
			"Domain must not regain persistence/JPA/Spring Data/QueryDSL leakage after BaseTimeEntity moved to infra");
	}

	@Test
	void domainNoLongerOwnsResponseSuccessCodes() throws Exception {
		List<String> domainSuccessCodes = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> path.getFileName().toString().matches(".*SuccessCode\\.(java|kt)"))
			.map(path -> path.toString().replace('\\', '/'))
			.toList();

		assertTrue(domainSuccessCodes.isEmpty(),
			"SuccessCode is an executable response concern and must not live in domain:\n"
				+ String.join("\n", domainSuccessCodes));

		assertAll(
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/booking/api/response/BookingSuccessCode.kt"),
				"MEMBER_BOOKING_RETRIEVE_SUCCESS(200, \"회원 예매 조회가 성공적으로 완료되었습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/booking/api/response/BookingSuccessCode.kt"),
				"GUEST_BOOKING_SUCCESS(201, \"비회원 예매가 성공적으로 완료되었습니다\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/ticket/api/response/TicketSuccessCode.kt"),
				"TICKET_SEARCH_SUCCESS(200, \"예매자 검색 결과 조회가 성공적으로 완료되었습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/member/api/response/MemberSuccessCode.kt"),
				"ISSUE_ACCESS_TOKEN_SUCCESS(200, \"엑세스토큰 발급 성공\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/performance/api/response/PerformanceSuccessCode.kt"),
				"PERFORMANCE_CREATE_SUCCESS(201, \"공연이 성공적으로 생성되었습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/schedule/api/response/ScheduleSuccessCode.kt"),
				"TICKET_AVAILABILITY_RETRIEVAL_SUCCESS(200, \"티켓 수량 조회가 성공적으로 완료되었습니다.\")")
		);
	}

	@Test
	void repositoryLookupNotFoundCodesMoveToApplicationBoundaryWithStableContract() throws Exception {
		Pattern lookupNotFoundCodePattern = Pattern.compile(
			"\\b(NO_[A-Z0-9_]+_FOUND|[A-Z0-9_]+_NOT_FOUND|SCHEDULE_LIST_NOT_FOUND)\\b"
		);
		List<String> domainLookupCodes = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> matches(path, lookupNotFoundCodePattern))
			.map(path -> path.toString().replace('\\', '/'))
			.toList();

		assertTrue(domainLookupCodes.isEmpty(),
			"Repository lookup NotFound codes are application flow concerns:\n"
				+ String.join("\n", domainLookupCodes));
		assertFalse(Files.exists(Path.of("domain/src/main/kotlin/com/beat/domain/promotion/exception/PromotionErrorCode.kt")));
		assertFalse(Files.exists(Path.of("domain/src/main/kotlin/com/beat/domain/user/exception/UserErrorCode.kt")));
		assertSourceContains(
			Path.of("apis/src/main/java/com/beat/apis/performance/application/PerformanceManagementService.java"),
			"throw new BadRequestException(PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND)");
		assertSourceContains(
			Path.of("apis/src/main/java/com/beat/apis/performance/application/PerformanceModifyService.java"),
			"throw new BadRequestException(PerformanceApplicationErrorCode.SCHEDULE_LIST_NOT_FOUND)");

		assertAll(
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/booking/application/exception/BookingApplicationErrorCode.kt"),
				"NO_BOOKING_FOUND(404, \"입력하신 정보와 일치하는 예매 내역이 없습니다. 확인 후 다시 조회해주세요.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/ticket/application/exception/TicketApplicationErrorCode.kt"),
				"NO_TICKETS_FOUND(404, \"입력하신 정보와 일치하는 예매자 목록이 없습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/booking/application/exception/BookingApplicationErrorCode.kt"),
				"NO_PERFORMANCE_FOUND(404, \"공연을 찾을 수 없습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/booking/application/exception/BookingApplicationErrorCode.kt"),
				"NO_SCHEDULE_FOUND(404, \"회차를 찾을 수 없습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/member/application/exception/MemberApplicationErrorCode.kt"),
				"MEMBER_NOT_FOUND(404, \"회원이 없습니다\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/performance/application/exception/PerformanceApplicationErrorCode.kt"),
				"PERFORMANCE_NOT_FOUND(404, \"해당 공연 정보를 찾을 수 없습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/performance/application/exception/PerformanceApplicationErrorCode.kt"),
				"SCHEDULE_LIST_NOT_FOUND(404, \"스케쥴 리스트에 스케쥴이 없습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/performance/application/exception/CastApplicationErrorCode.kt"),
				"CAST_NOT_FOUND(404, \"등장인물이 존재하지 않습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/performance/application/exception/StaffApplicationErrorCode.kt"),
				"STAFF_NOT_FOUND(404, \"스태프가 존재하지 않습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/performance/application/exception/PerformanceImageApplicationErrorCode.kt"),
				"PERFORMANCE_IMAGE_NOT_FOUND(404, \"해당 공연 상세이미지를 찾을 수 없습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/schedule/application/exception/ScheduleApplicationErrorCode.kt"),
				"NO_SCHEDULE_FOUND(404, \"해당 회차를 찾을 수 없습니다.\")"),
			() -> assertSourceContains(
				Path.of("apis/src/main/kotlin/com/beat/apis/user/application/exception/UserApplicationErrorCode.kt"),
				"USER_NOT_FOUND(404, \"유저가 없습니다\")"),
			() -> assertSourceContains(
				Path.of("admin/src/main/kotlin/com/beat/admin/application/exception/AdminApplicationErrorCode.kt"),
				"INVALID_REQUEST_FORMAT(400, \"잘못된 요청 형식입니다.\")"),
			() -> assertSourceContains(
				Path.of("admin/src/main/kotlin/com/beat/admin/application/exception/AdminApplicationErrorCode.kt"),
				"MEMBER_NOT_FOUND(404, \"회원이 없습니다\")"),
			() -> assertSourceContains(
				Path.of("admin/src/main/kotlin/com/beat/admin/application/exception/AdminApplicationErrorCode.kt"),
				"PERFORMANCE_NOT_FOUND(404, \"해당 공연 정보를 찾을 수 없습니다.\")"),
			() -> assertSourceContains(
				Path.of("admin/src/main/kotlin/com/beat/admin/application/exception/AdminApplicationErrorCode.kt"),
				"PROMOTION_NOT_FOUND(404, \"해당 홍보 정보를 찾을 수 없습니다.\")")
		);
	}

	@Test
	void domainErrorCodesStayOnPureInvariantAllowlist() throws Exception {
		Set<String> expectedDomainErrorCodes = Set.of(
			"domain/src/main/kotlin/com/beat/domain/booking/exception/BookingErrorCode.kt",
			"domain/src/main/kotlin/com/beat/domain/performance/exception/PerformanceErrorCode.kt",
			"domain/src/main/kotlin/com/beat/domain/schedule/exception/ScheduleErrorCode.kt"
		);
		Set<String> actualDomainErrorCodes = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> path.getFileName().toString().matches(".*ErrorCode\\.(java|kt)"))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());

		assertEquals(expectedDomainErrorCodes, actualDomainErrorCodes,
			"Domain may only own pure invariant ErrorCode enums");
		assertSourceContains(
			Path.of("domain/src/main/kotlin/com/beat/domain/booking/exception/BookingErrorCode.kt"),
			"INVALID_DATA_FORMAT(400, \"잘못된 데이터 형식입니다.\")");
		assertSourceContains(
			Path.of("domain/src/main/kotlin/com/beat/domain/performance/exception/PerformanceErrorCode.kt"),
			"NEGATIVE_TICKET_PRICE(400, \"티켓 가격은 음수일 수 없습니다.\")");
		assertFalse(contains(
			Path.of("domain/src/main/kotlin/com/beat/domain/performance/exception/PerformanceErrorCode.kt"),
			"NOT_PERFORMANCE_OWNER"));
	}

	@Test
	void executableAndInfraSourcesDoNotImportDomainErrorCodes() throws Exception {
		Pattern domainErrorCodeImport = Pattern.compile(
			"^import com\\.beat\\.domain\\.[a-z0-9]+\\.exception\\.[A-Za-z0-9]+ErrorCode;",
			Pattern.MULTILINE
		);
		List<String> violations = sourceFiles(
			Path.of("apis/src/main"),
			Path.of("admin/src/main"),
			Path.of("batch/src/main"),
			Path.of("infra/src/main")
		).stream()
			.filter(path -> matches(path, domainErrorCodeImport))
			.map(path -> path.toString().replace('\\', '/'))
			.toList();

		assertTrue(violations.isEmpty(),
			"Executable/infra sources must use application or adapter failure codes, not domain ErrorCode imports:\n"
				+ String.join("\n", violations));
	}

	@Test
	void infraSourcesDoNotImportExecutableApplicationErrorCodes() throws Exception {
		Pattern executableApplicationErrorCodeImport = Pattern.compile(
			"^import com\\.beat\\.(apis|admin|batch)\\..*\\.[A-Za-z0-9]+ApplicationErrorCode;",
			Pattern.MULTILINE
		);
		List<String> violations = sourceFiles(Path.of("infra/src/main")).stream()
			.filter(path -> matches(path, executableApplicationErrorCodeImport))
			.map(path -> path.toString().replace('\\', '/'))
			.toList();

		assertTrue(violations.isEmpty(),
			"Infra adapters must throw adapter/port-level failures and must not import executable ApplicationErrorCode enums:\n"
				+ String.join("\n", violations));
	}

	@Test
	void domainLegacyDaoPackagesDoNotReappearAfterRepositoryPortMigration() throws Exception {
		List<String> legacyDaoPackageSources = sourceFiles(Path.of("domain/src/main")).stream()
			.map(path -> path.toString().replace('\\', '/'))
			.filter(path -> path.contains("/dao/"))
			.toList();

		assertTrue(legacyDaoPackageSources.isEmpty(),
			"Domain repository ports must live under repository/, not legacy dao/:\n"
				+ String.join("\n", legacyDaoPackageSources));
	}

	@Test
	void domainApplicationUseCasePortPackagesDoNotReappear() throws Exception {
		List<String> domainPortSources = sourceFiles(Path.of("domain/src/main")).stream()
			.map(path -> path.toString().replace('\\', '/'))
			.filter(path -> path.contains("/port/"))
			.toList();
		List<String> apisApplicationPortSources = sourceFiles(Path.of("apis/src/main")).stream()
			.map(path -> path.toString().replace('\\', '/'))
			.filter(path -> path.contains("/apis/application/port/"))
			.toList();

		assertTrue(domainPortSources.isEmpty(),
			"Domain must not regain application use-case port packages:\n"
				+ String.join("\n", domainPortSources));
		assertTrue(apisApplicationPortSources.isEmpty(),
			"Do not introduce apis/application/port/in as a replacement for deleted domain use-case ports:\n"
				+ String.join("\n", apisApplicationPortSources));
	}

	@Test
	void domainRepositoryDtoPackagesDoNotReappearAfterReadModelSplit() throws Exception {
		List<String> domainRepositoryDtoSources = sourceFiles(Path.of("domain/src/main")).stream()
			.map(path -> path.toString().replace('\\', '/'))
			.filter(path -> path.contains("/repository/dto/"))
			.toList();

		assertTrue(domainRepositoryDtoSources.isEmpty(),
			"Domain repositories must not own read-model DTO packages; use module-contracts read ports instead:\n"
				+ String.join("\n", domainRepositoryDtoSources));
	}

	@Test
	void infraSourceDoesNotDeclareDomainPackages() throws Exception {
		List<String> infraDomainPackageResidues = sourceFiles(Path.of("infra/src/main")).stream()
			.flatMap(path -> readLines(path).stream()
				.filter(line -> line.startsWith("package com.beat.domain"))
				.map(line -> path.toString().replace('\\', '/') + ": " + line))
			.toList();

		assertTrue(infraDomainPackageResidues.isEmpty(),
			"Infra sources must not declare legacy com.beat.domain.* packages:\n"
				+ String.join("\n", infraDomainPackageResidues));
	}

	@Test
	void castStaffAndUsersDomainContractsStayPureAndTechnologyNeutral() throws Exception {
		Path castDomain = Path.of("domain/src/main/kotlin/com/beat/domain/cast/domain/Cast.kt");
		Path staffDomain = Path.of("domain/src/main/kotlin/com/beat/domain/staff/domain/Staff.kt");
		Path usersDomain = Path.of("domain/src/main/kotlin/com/beat/domain/user/domain/Users.kt");
		Path castRepository = Path.of("domain/src/main/kotlin/com/beat/domain/cast/repository/CastRepository.kt");
		Path staffRepository = Path.of("domain/src/main/kotlin/com/beat/domain/staff/repository/StaffRepository.kt");
		Path usersRepository = Path.of("domain/src/main/kotlin/com/beat/domain/user/repository/UserRepository.kt");
		List<Path> domainContractSources = List.of(castDomain, staffDomain, usersDomain, castRepository,
			staffRepository, usersRepository);
		List<String> forbiddenTechnologyReferences = List.of(
			"jakarta.persistence.",
			"org.hibernate.annotations.",
			"org.springframework.data.",
			"org.springframework.transaction.",
			"com.querydsl.",
			"@Entity",
			"@Table",
			"@ManyToOne",
			"@JoinColumn",
			"JpaRepository",
			"Performance performance"
		);

		assertFalse(Files.exists(Path.of("domain/src/main/kotlin/com/beat/domain/cast/dao/CastRepository.java")));
		assertFalse(Files.exists(Path.of("domain/src/main/kotlin/com/beat/domain/staff/dao/StaffRepository.java")));
		assertFalse(Files.exists(Path.of("domain/src/main/kotlin/com/beat/domain/user/dao/UserRepository.java")));
		assertTrue(Files.exists(castRepository));
		assertTrue(Files.exists(staffRepository));
		assertTrue(Files.exists(usersRepository));

		List<String> violations = domainContractSources.stream()
			.flatMap(path -> forbiddenTechnologyReferences.stream()
				.filter(pattern -> contains(path, pattern))
				.map(pattern -> path + ": " + pattern))
			.toList();

		assertTrue(violations.isEmpty(),
			"Cast/Staff/Users domain contracts must stay persistence-technology neutral:\n" + String.join("\n",
				violations));
		assertTrue(Files.readString(castDomain).contains("linkedPerformanceId: Performance.Id"));
		assertTrue(Files.readString(staffDomain).contains("linkedPerformanceId: Performance.Id"));
		assertTrue(Files.readString(usersDomain).contains("class Users private constructor"));
	}

	@Test
	void domainCustomRepositoryContractsRemainExplicitIssue380TransitionalAllowlist() throws Exception {
		Set<String> allowedCustomRepositoryContracts = Set.of();

		Set<String> actualCustomRepositoryContracts = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> path.getFileName().toString().endsWith("RepositoryCustom.java"))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());

		assertEquals(allowedCustomRepositoryContracts, actualCustomRepositoryContracts,
			"Domain custom repository contracts are transitional query/persistence hooks and must stay explicitly reviewed");
	}

	@Test
	void domainJpaEntityAndRepositoryInventoryMatchesIssue380Baseline() throws Exception {
		Set<String> allowedJpaModelSources = Set.of();
		Set<String> allowedJpaRepositorySources = Set.of();

		Set<String> actualJpaModelSources = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> hasAnnotation(path, "Entity") || hasAnnotation(path, "MappedSuperclass"))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());
		Set<String> actualJpaRepositorySources = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> contains(path, "JpaRepository"))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());

		assertEquals(allowedJpaModelSources, actualJpaModelSources);
		assertEquals(allowedJpaRepositorySources, actualJpaRepositorySources);
	}

	@Test
	void promotionRepositorySeamUsesIssue380ChosenNaming() throws Exception {
		Path domainContract = Path.of(
			"domain/src/main/kotlin/com/beat/domain/promotion/repository/PromotionRepository.kt");
		Path oldDomainSpringDataRepository = Path.of(
			"domain/src/main/kotlin/com/beat/domain/promotion/dao/PromotionRepository.kt");
		Path springDataRepository = Path.of(
			"infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionJpaRepository.java");
		Path jpaEntity = promotionJpaEntitySourcePath();
		Path persistenceMapper = Path.of(
			"infra/src/main/java/com/beat/infra/persistence/promotion/mapper/PromotionPersistenceMapper.java");
		Path repositoryImplementation = Path.of(
			"infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionRepositoryImpl.java");

		assertTrue(Files.exists(domainContract));
		assertFalse(Files.exists(oldDomainSpringDataRepository));
		assertTrue(Files.exists(springDataRepository));
		assertTrue(Files.exists(repositoryImplementation));

		String domainContractSource = Files.readString(domainContract);
		String springDataRepositorySource = Files.readString(springDataRepository);
		String repositoryImplementationSource = Files.readString(repositoryImplementation);
		Set<String> promotionRepositoryImports = sourceFiles(
			Path.of("apis/src/main"),
			Path.of("admin/src/main"),
			Path.of("batch/src/main")
		).stream()
			.flatMap(path -> readLines(path).stream()
				.filter(line -> line.contains("PromotionRepository"))
				.map(line -> path.toString().replace('\\', '/') + ": " + line))
			.collect(Collectors.toSet());

		String promotionDomainSource = Files.readString(
			Path.of("domain/src/main/kotlin/com/beat/domain/promotion/domain/Promotion.kt"));

		assertFalse(domainContractSource.contains("org.springframework.data"));
		assertFalse(domainContractSource.contains("jakarta.persistence"));
		assertFalse(domainContractSource.contains("@Query"));
		assertFalse(promotionDomainSource.contains("jakarta.persistence"));
		assertFalse(promotionDomainSource.contains("@Entity"));
		assertFalse(promotionDomainSource.contains("com.beat.domain.performance.domain.Performance;"));
		assertTrue(promotionDomainSource.contains("data class Promotion private constructor"));
		assertTrue(promotionDomainSource.contains("@ConsistentCopyVisibility"));
		assertFalse(Files.exists(Path.of("domain/src/main/kotlin/com/beat/domain/performance/domain/PerformanceId.kt")));

		assertTrue(promotionDomainSource.contains("@JvmInline"));
		assertTrue(promotionDomainSource.contains("value class Id private constructor"));
		assertFalse(promotionDomainSource.contains("value class PerformanceId"));
		assertTrue(promotionDomainSource.contains("import com.beat.domain.performance.domain.Performance"));
		assertTrue(promotionDomainSource.contains("private val linkedPerformanceId: Performance.Id?"));
		assertTrue(promotionDomainSource.contains("fun from(value: Long): Id"));
		assertTrue(promotionDomainSource.contains("fun fromNullable(value: Long?): Id?"));
		assertTrue(promotionDomainSource.contains("Id.fromNullable(id)"));
		assertTrue(promotionDomainSource.contains("Performance.Id.fromNullable(performanceId)"));
		assertTrue(promotionDomainSource.contains("fun getId(): Long?"));
		assertTrue(promotionDomainSource.contains("fun getPerformanceId(): Long?"));
		assertTrue(promotionDomainSource.contains("fun updatePromotionDetails("));
		assertTrue(promotionDomainSource.contains(": Promotion = copy("));
		assertTrue(Files.exists(jpaEntity));
		assertTrue(Files.exists(persistenceMapper));
		String jpaEntitySource = Files.readString(jpaEntity);
		String persistenceMapperSource = Files.readString(persistenceMapper);

		assertFalse(domainContractSource.contains("org.springframework.data"));
		assertFalse(domainContractSource.contains("jakarta.persistence"));
		assertFalse(domainContractSource.contains("@Query"));
		assertTrue(springDataRepositorySource.contains("extends JpaRepository<PromotionJpaEntity, Long>"));
		assertFalse(springDataRepositorySource.contains("com.beat.domain.promotion.domain.Promotion"));
		assertPromotionJpaEntityMappingContract(jpaEntitySource);
		assertTrue(persistenceMapperSource.contains("Promotion toDomain(PromotionJpaEntity entity)"));
		assertTrue(persistenceMapperSource.contains("PromotionJpaEntity toEntity(Promotion promotion)"));
		assertFalse(persistenceMapperSource.contains("PerformanceRepository"));
		assertFalse(persistenceMapperSource.contains("EntityManager"));
		assertTrue(repositoryImplementationSource.contains("implements PromotionRepository"));
		assertTrue(
			repositoryImplementationSource.contains("private final PromotionJpaRepository promotionJpaRepository;"));
		assertTrue(repositoryImplementationSource.contains(
			"private final PromotionPersistenceMapper promotionPersistenceMapper;"));
		assertFalse(repositoryImplementationSource.contains("PerformanceRepository"));
		assertTrue(repositoryImplementationSource.contains("public PromotionRepositoryImpl("));
		assertTrue(repositoryImplementationSource.contains("@Repository"));
		assertFalse(repositoryImplementationSource.contains("@RequiredArgsConstructor"));
		assertTrue(promotionRepositoryImports.stream()
			.noneMatch(line -> line.contains("com.beat.domain.promotion.dao.PromotionRepository")));
		assertTrue(sourceFiles(Path.of("apis/src/main"), Path.of("admin/src/main"), Path.of("batch/src/main"))
			.stream()
			.noneMatch(path -> contains(path, "PromotionJpaEntity") || contains(path, "PromotionPersistenceMapper")));
	}

	@Test
	void domainJpaAnnotationsAndAuditingStayAbsentAfterInfraMove() throws Exception {
		Set<String> allowedEntitySources = Set.of();
		Set<String> allowedMappedSuperclassSources = Set.of();

		Set<String> actualEntitySources = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> hasAnnotation(path, "Entity"))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());
		Set<String> actualMappedSuperclassSources = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> hasAnnotation(path, "MappedSuperclass"))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());
		Set<String> actualAuditingSources = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> hasAnnotation(path, "EntityListeners")
				|| hasAnnotation(path, "CreatedDate")
				|| hasAnnotation(path, "LastModifiedDate")
				|| contains(path, "AuditingEntityListener"))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());

		assertEquals(allowedEntitySources, actualEntitySources,
			"Adding a domain JPA entity must be treated as persistence leakage unless issue #380 allowlist is updated");
		assertEquals(allowedMappedSuperclassSources, actualMappedSuperclassSources);
		assertEquals(allowedMappedSuperclassSources, actualAuditingSources,
			"Domain JPA auditing must not reappear after BaseTimeEntity moved to infra");
	}

	@Test
	void domainBuildDoesNotRegainJpaOrQueryDslBootstrap() throws Exception {
		Set<String> allowedQueryProjectionSources = Set.of();
		Set<String> actualQueryProjectionSources = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> contains(path, "@QueryProjection") || contains(path, "com.querydsl."))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());
		String domainBuild = Files.readString(Path.of("domain/build.gradle.kts"));
		String jpaConfig = Files.readString(Path.of("infra/src/main/java/com/beat/infra/config/JpaConfig.java"));

		assertEquals(allowedQueryProjectionSources, actualQueryProjectionSources);
		assertTrue(domainBuild.contains("id(\"beat.library\")"));
		assertFalse(domainBuild.contains("beat.spring-library"));
		assertFalse(domainBuild.contains("queryDslSrcDir"));
		assertFalse(domainBuild.contains("generated/querydsl"));
		assertFalse(domainBuild.contains("spring.boot.starter.data.jpa"));
		assertFalse(domainBuild.contains("querydsl"));
		assertFalse(domainBuild.contains("jakarta.annotation"));
		assertFalse(domainBuild.contains("jakarta.persistence"));
		assertFalse(domainBuild.contains("lombok"));
		assertTrue(jpaConfig.contains("@EnableJpaAuditing"));
		assertTrue(jpaConfig.contains("@EntityScan(basePackageClasses = InfraPersistenceMarker.class)"));
		assertTrue(jpaConfig.contains("@EnableJpaRepositories(basePackageClasses = InfraPersistenceMarker.class)"));
		assertFalse(jpaConfig.contains("basePackages = \"com.beat.domain\""));
		assertTrue(jpaConfig.contains("@Import(InfraPersistenceConfig.class)"));
		assertFalse(jpaConfig.contains("PromotionRepositoryImpl"));
		assertFalse(jpaConfig.contains("@ComponentScan"));
		assertFalse(jpaConfig.contains("com.beat.infra.persistence.promotion.repository"));
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
			"com.beat.gateway.",
			"com.beat.domain."
		);

		List<String> violations = sourceFiles(Path.of("module-contracts/src/main")).stream()
			.flatMap(path -> forbiddenReferences.stream()
				.filter(pattern -> contains(path, pattern))
				.map(pattern -> path + ": " + pattern))
			.toList();

		assertFalse(buildFile.contains("project(\":domain\")"));
		assertTrue(buildFile.contains("compileOnly(project(\":global-utils\"))"));
		assertFalse(buildFile.contains("implementation(project(\":domain\"))"));
		assertFalse(buildFile.contains("implementation(project(\":global-utils\"))"));
		assertTrue(
			violations.isEmpty(),
			"Found forbidden module-contracts implementation references:\n" + String.join("\n", violations)
		);
	}

	@Test
	void moduleContractsDoNotImportDomainTypes() throws Exception {
		Set<String> actualDomainReferences = sourceFiles(Path.of("module-contracts/src/main")).stream()
			.flatMap(path -> readLines(path).stream()
				.filter(line -> line.contains("com.beat.domain."))
				.map(line -> path.toString().replace('\\', '/') + ": " + line))
			.collect(Collectors.toSet());

		assertTrue(
			actualDomainReferences.isEmpty(),
			"module-contracts must not expose domain types across module boundaries:\n"
				+ String.join("\n", actualDomainReferences)
		);
	}

	@Test
	void moduleContractsReadModelsAreExplicitlyMarked() throws Exception {
		String readModelMarker = Files.readString(
			Path.of("module-contracts/src/main/java/com/beat/contracts/common/ReadModel.java"));
		String minPerformanceDate = Files.readString(
			Path.of("module-contracts/src/main/java/com/beat/contracts/schedule/readmodel/MinPerformanceDateReadModel.java"));
		String makerTicketListItemReadModel = Files.readString(
			Path.of("module-contracts/src/main/java/com/beat/contracts/booking/readmodel/MakerTicketListItemReadModel.java"));
		String scheduleReadPort = Files.readString(
			Path.of("module-contracts/src/main/java/com/beat/contracts/schedule/ScheduleReadPort.java"));
		String makerTicketReadPort = Files.readString(
			Path.of("module-contracts/src/main/java/com/beat/contracts/booking/MakerTicketReadPort.java"));

		assertTrue(readModelMarker.contains("@Target(ElementType.TYPE)"));
		assertTrue(readModelMarker.contains("@Retention(RetentionPolicy.RUNTIME)"));
		assertTrue(readModelMarker.contains("public @interface ReadModel"));
		assertTrue(minPerformanceDate.contains("import com.beat.contracts.common.ReadModel;"));
		assertTrue(minPerformanceDate.contains("@ReadModel"));
		assertTrue(makerTicketListItemReadModel.contains("import com.beat.contracts.common.ReadModel;"));
		assertTrue(makerTicketListItemReadModel.contains("@ReadModel"));
		assertTrue(scheduleReadPort.contains("List<MinPerformanceDateReadModel> findMinPerformanceDateByPerformanceIds"));
		assertTrue(makerTicketReadPort.contains("List<MakerTicketListItemReadModel> findTickets(Long performanceId"));
		assertTrue(makerTicketReadPort.contains("List<MakerTicketListItemReadModel> searchTickets(Long performanceId"));
		assertFalse(Files.exists(Path.of(
			"module-contracts/src/main/java/com/beat/contracts/booking/MakerTicketSearchCondition.java")));
	}

	@Test
	void castAndStaffPersistenceSeamMovesAsPairedIssue404Slice() throws Exception {
		String migration = Files.readString(Path.of("MIGRATION.md"));

		assertTrue(migration.contains("## #404 Cast + Staff persistence seam"));
		assertTrue(migration.contains("### First slice candidate ranking"));
		assertTrue(migration.contains("Cast` + `Staff` pair"));
		List<String> staffOnlySplitViolations = sourceFiles(
			Path.of("infra/src/main"),
			Path.of("domain/src/main")
		).stream()
			.map(path -> path.toString().replace('\\', '/'))
			.filter(path -> path.contains("/domain/staff/port/")
				|| path.contains("/domain/staff/model/"))
			.toList();
		Set<String> actualInfraPersistenceFiles = sourceFiles(
			Path.of("infra/src/main/java/com/beat/infra/persistence"),
			Path.of("infra/src/main/kotlin/com/beat/infra/persistence")
		)
			.stream()
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());

		assertTrue(staffOnlySplitViolations.isEmpty(),
			"Do not add a Staff-only domain split without updating the #380/#404 ADR and migration guard:\n"
				+ String.join("\n", staffOnlySplitViolations));
		assertPairedPackagePresence(actualInfraPersistenceFiles, "/cast/", "/staff/");
		assertInfraCastStaffPersistenceUsesScalarPerformanceIdWhenPresent();
		assertExecutableModulesDoNotImportInfraPersistenceTypes();
	}

	@Test
	void externalClientBootstrapRemainsExplicitConfigurationSurface() throws Exception {
		String externalClientConfig = Files.readString(
			Path.of("infra/src/main/java/com/beat/infra/config/ExternalClientConfig.java"));

		assertTrue(externalClientConfig.contains("@Configuration(proxyBeanMethods = false)"));
		assertTrue(externalClientConfig.contains("@Import(S3InfraConfig.class)"));
		assertTrue(externalClientConfig.contains("KakaoSocialLoginAdapter.class"));
		assertTrue(externalClientConfig.contains("SlackBookingNotificationAdapter.class"));
		assertTrue(externalClientConfig.contains("SlackMemberNotificationAdapter.class"));
		assertTrue(externalClientConfig.contains("S3FileStorageAdapter.class"));
		assertTrue(externalClientConfig.contains("CoolSmsAdapter.class"));
		assertTrue(externalClientConfig.contains("excludeFilters"));
		assertTrue(externalClientConfig.contains("classes = S3InfraConfig.class"));
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


	private Set<String> bookingInfraPersistenceSourcePathsIfPresent() {
		Path bookingPersistenceRoot = Path.of("infra/src/main/java/com/beat/infra/persistence/booking");
		Path bookingPersistenceKotlinRoot = Path.of("infra/src/main/kotlin/com/beat/infra/persistence/booking");
		if (!Files.exists(bookingPersistenceRoot) && !Files.exists(bookingPersistenceKotlinRoot)) {
			return Set.of();
		}

		return Set.of(
			bookingJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/booking/mapper/BookingPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/booking/repository/BookingJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/booking/repository/BookingRepositoryImpl.java",
			"infra/src/main/java/com/beat/infra/persistence/booking/repository/query/MakerTicketReadPortImpl.java"
		);
	}

	private Path bookingJpaEntitySourcePath() {
		return singleJpaEntitySourcePath(
			"infra/src/main/java/com/beat/infra/persistence/booking/entity/BookingJpaEntity.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/booking/entity/BookingJpaEntity.kt",
			"BookingJpaEntity"
		);
	}

	private Path promotionJpaEntitySourcePath() {
		Path javaEntity = Path.of(
			"infra/src/main/java/com/beat/infra/persistence/promotion/entity/PromotionJpaEntity.java");
		Path kotlinEntity = Path.of(
			"infra/src/main/kotlin/com/beat/infra/persistence/promotion/entity/PromotionJpaEntity.kt");

		assertTrue(Files.exists(javaEntity) ^ Files.exists(kotlinEntity),
			"PromotionJpaEntity must exist as exactly one Java or Kotlin source during the #389 conversion");
		return Files.exists(kotlinEntity) ? kotlinEntity : javaEntity;
	}

	private Path castJpaEntitySourcePath() {
		return singleJpaEntitySourcePath(
			"infra/src/main/java/com/beat/infra/persistence/cast/entity/CastJpaEntity.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/cast/entity/CastJpaEntity.kt",
			"CastJpaEntity"
		);
	}

	private Path staffJpaEntitySourcePath() {
		return singleJpaEntitySourcePath(
			"infra/src/main/java/com/beat/infra/persistence/staff/entity/StaffJpaEntity.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/staff/entity/StaffJpaEntity.kt",
			"StaffJpaEntity"
		);
	}

	private Path usersJpaEntitySourcePath() {
		return singleJpaEntitySourcePath(
			"infra/src/main/java/com/beat/infra/persistence/user/entity/UsersJpaEntity.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/user/entity/UsersJpaEntity.kt",
			"UsersJpaEntity"
		);
	}

	private Path performanceJpaEntitySourcePath() {
		return singleJpaEntitySourcePath(
			"infra/src/main/java/com/beat/infra/persistence/performance/entity/PerformanceJpaEntity.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/performance/entity/PerformanceJpaEntity.kt",
			"PerformanceJpaEntity"
		);
	}

	private Path scheduleJpaEntitySourcePath() {
		return singleJpaEntitySourcePath(
			"infra/src/main/java/com/beat/infra/persistence/schedule/entity/ScheduleJpaEntity.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/schedule/entity/ScheduleJpaEntity.kt",
			"ScheduleJpaEntity"
		);
	}

	@Test
	void domainTypedIdsStayInternalWhileJavaBoundariesRemainScalar() throws Exception {
		String bookingDomain = Files.readString(
			Path.of("domain/src/main/kotlin/com/beat/domain/booking/domain/Booking.kt"));
		String memberDomain = Files.readString(
			Path.of("domain/src/main/kotlin/com/beat/domain/member/domain/Member.kt"));
		String performanceDomain = Files.readString(
			Path.of("domain/src/main/kotlin/com/beat/domain/performance/domain/Performance.kt"));

		assertAll(
			() -> assertTrue(bookingDomain.contains("private val bookingId: Id?")),
			() -> assertTrue(bookingDomain.contains("private val linkedScheduleId: Schedule.Id")),
			() -> assertTrue(bookingDomain.contains("private val linkedUserId: Users.Id")),
			() -> assertTrue(bookingDomain.contains("fun getId(): Long? = bookingId?.value")),
			() -> assertTrue(bookingDomain.contains("fun getScheduleId(): Long = linkedScheduleId.value")),
			() -> assertTrue(bookingDomain.contains("fun getUserId(): Long = linkedUserId.value")),
			() -> assertTrue(bookingDomain.contains("Schedule.Id.from(scheduleId)")),
			() -> assertTrue(bookingDomain.contains("Users.Id.from(userId)")),
			() -> assertFalse(bookingDomain.contains("private val bookingId: Long?")),
			() -> assertFalse(bookingDomain.contains("private val scheduleId: Long")),
			() -> assertFalse(bookingDomain.contains("private val userId: Long"))
		);

		assertAll(
			() -> assertTrue(memberDomain.contains("private val linkedUserId: Users.Id")),
			() -> assertTrue(memberDomain.contains("fun getUserId(): Long = linkedUserId.value")),
			() -> assertTrue(memberDomain.contains("linkedUserId = Users.Id.from(userId)")),
			() -> assertFalse(memberDomain.contains("val userId: Long"))
		);

		assertAll(
			() -> assertTrue(performanceDomain.contains("private val linkedUserId: Users.Id")),
			() -> assertTrue(performanceDomain.contains("fun getUserId(): Long = linkedUserId.value")),
			() -> assertTrue(performanceDomain.contains("linkedUserId = Users.Id.from(userId)")),
			() -> assertFalse(performanceDomain.contains("val userId: Long,"))
		);
	}

	private Path performanceImageJpaEntitySourcePath() {
		return singleJpaEntitySourcePath(
			"infra/src/main/java/com/beat/infra/persistence/performanceimage/entity/PerformanceImageJpaEntity.java",
			"infra/src/main/kotlin/com/beat/infra/persistence/performanceimage/entity/PerformanceImageJpaEntity.kt",
			"PerformanceImageJpaEntity"
		);
	}

	private Path singleJpaEntitySourcePath(String javaSource, String kotlinSource, String entityName) {
		Path javaEntity = Path.of(javaSource);
		Path kotlinEntity = Path.of(kotlinSource);

		assertTrue(Files.exists(javaEntity) ^ Files.exists(kotlinEntity),
			entityName + " must exist as exactly one Java or Kotlin source during the slice migration");
		return Files.exists(kotlinEntity) ? kotlinEntity : javaEntity;
	}

	private void assertPairedPackagePresence(Set<String> paths, String leftPackageToken, String rightPackageToken) {
		boolean leftPresent = paths.stream().anyMatch(path -> path.contains(leftPackageToken));
		boolean rightPresent = paths.stream().anyMatch(path -> path.contains(rightPackageToken));

		assertEquals(leftPresent, rightPresent,
			"Cast/Staff persistence migration must move as a pair, not as a Staff-only or Cast-only split");
	}

	private void assertInfraCastStaffPersistenceUsesScalarPerformanceIdWhenPresent() throws Exception {
		List<Path> entitySources = List.of(castJpaEntitySourcePath(), staffJpaEntitySourcePath());

		for (Path source : entitySources) {
			String content = Files.readString(source);

			assertTrue(content.contains("performanceId"));
			assertTrue(content.contains("\"performance_id\""));
			assertFalse(content.contains("com.beat.domain.performance.domain.Performance"));
			assertFalse(content.contains("private Performance performance"));
			assertFalse(content.contains("@ManyToOne"));
			assertFalse(content.contains("@JoinColumn"));
		}
	}

	private void assertExecutableModulesDoNotImportInfraPersistenceTypes() throws Exception {
		List<Path> executableSources = sourceFiles(
			Path.of("apis/src/main"),
			Path.of("admin/src/main"),
			Path.of("batch/src/main")
		);
		// InfraPersistenceConfig is allowed as an IDE-only breadcrumb: @EnableInfraBaseConfig is backed by
		// DeferredImportSelector, which IntelliJ Spring plugin cannot statically trace, so each module-level
		// InfraConfig carries @Import(InfraPersistenceConfig::class) purely to give the IDE a resolvable path.
		// It has no effect at runtime — persistence bootstrap is owned by JpaConfig.
		List<String> violations = executableSources.stream()
			.flatMap(path -> readLines(path).stream()
				.filter(line -> line.startsWith("import com.beat.infra.persistence.")
					&& !line.contains("InfraPersistenceConfig"))
				.map(line -> path.toString().replace('\\', '/') + ": " + line))
			.toList();

		assertTrue(violations.isEmpty(),
			"Executable modules must not import infra persistence types:\n" + String.join("\n", violations));
	}

	private void assertPromotionJpaEntityMappingContract(String source) {
		assertTrue(source.contains("@Entity(name = \"Promotion\")"));
		assertTrue(source.contains("@Table(name = \"promotion\")"));
		assertFalse(source.contains("private Performance performance"));
		assertFalse(source.contains("@ManyToOne"));
		assertFalse(source.contains("@JoinColumn"));

		if (source.contains("fun rehydrate(")) {
			assertFalse(source.contains("data class PromotionJpaEntity"));
			assertFalse(source.contains("override fun equals("));
			assertFalse(source.contains("override fun hashCode("));
			assertTrue(source.contains("@JvmStatic"));
			assertTrue(source.contains("class PromotionJpaEntity private constructor("));
			assertFalse(source.contains("protected constructor()"));
			assertTrue(source.matches("(?s).*\\bvar\\s+id\\s*:\\s*Long\\?\\s*=\\s*id\\s+protected set.*"));
			assertTrue(source.matches(
				"(?s).*\\bvar\\s+performanceId\\s*:\\s*Long\\?\\s*=\\s*performanceId\\s+protected set.*"));
			assertTrue(source.matches(
				"(?s).*\\bvar\\s+promotionPhoto\\s*:\\s*String\\s*=\\s*promotionPhoto\\s+protected set.*"));
			assertTrue(
				source.matches("(?s).*\\bvar\\s+redirectUrl\\s*:\\s*String\\s*=\\s*redirectUrl\\s+protected set.*"));
			assertTrue(
				source.matches("(?s).*\\bvar\\s+isExternal\\s*:\\s*Boolean\\s*=\\s*isExternal\\s+protected set.*"));
			assertTrue(source.matches(
				"(?s).*\\bvar\\s+carouselNumber\\s*:\\s*CarouselNumber\\s*=\\s*carouselNumber\\s+protected set.*"));
		} else {
			assertTrue(source.contains("private Long performanceId;"));
		}
	}

	private void assertSourceContains(Path path, String expected) throws IOException {
		assertTrue(Files.readString(path).contains(expected),
			path + " must preserve expected code contract: " + expected);
	}

	private boolean matches(Path path, Pattern pattern) {
		try {
			return pattern.matcher(Files.readString(path)).find();
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read " + path, exception);
		}
	}

	private boolean contains(Path path, String pattern) {
		try {
			String content = Files.readString(path);
			char last = pattern.charAt(pattern.length() - 1);
			if (Character.isLetterOrDigit(last) || last == '_') {
				return Pattern.compile(Pattern.quote(pattern) + "(?!\\w)").matcher(content).find();
			}
			return content.contains(pattern);
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

	private boolean hasAnnotation(Path path, String annotationName) {
		return readLines(path).stream()
			.map(String::trim)
			.anyMatch(line -> line.equals("@" + annotationName) || line.startsWith("@" + annotationName + "("));
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
