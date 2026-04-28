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
	void infraPersistenceBootstrapUsesSingleMarkerAndNoDomainSpecificConfig() throws Exception {
		Set<String> requiredInfraPersistenceFiles = Set.of(
			"infra/src/main/java/com/beat/infra/persistence/InfraPersistenceConfig.java",
			"infra/src/main/java/com/beat/infra/persistence/InfraPersistenceMarker.java",
			promotionJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/promotion/mapper/PromotionPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/promotion/repository/PromotionRepositoryImpl.java",
			usersJpaEntitySourcePath().toString().replace('\\', '/'),
			"infra/src/main/java/com/beat/infra/persistence/user/mapper/UsersPersistenceMapper.java",
			"infra/src/main/java/com/beat/infra/persistence/user/repository/UsersJpaRepository.java",
			"infra/src/main/java/com/beat/infra/persistence/user/repository/UsersRepositoryImpl.java"
		);
		Set<String> allowedInfraPersistenceFiles = Set.of(
			"infra/src/main/java/com/beat/infra/persistence/InfraPersistenceConfig.java",
			"infra/src/main/java/com/beat/infra/persistence/InfraPersistenceMarker.java",
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
			"infra/src/main/java/com/beat/infra/persistence/member/repository/MemberRepositoryImpl.java"
		);

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
	void domainPersistenceConcernSourcesRemainExplicitTransitionalAllowlist() throws Exception {
		Set<String> allowedPersistenceConcernSources = Set.of(
			"domain/src/main/java/com/beat/domain/BaseTimeEntity.java",
			"domain/src/main/java/com/beat/domain/booking/dao/BookingRepository.java",
			"domain/src/main/java/com/beat/domain/booking/dao/TicketRepository.java",
			"domain/src/main/java/com/beat/domain/booking/domain/Booking.java",
			"domain/src/main/java/com/beat/domain/performance/dao/PerformanceRepository.java",
			"domain/src/main/java/com/beat/domain/performance/domain/Performance.java",
			"domain/src/main/java/com/beat/domain/schedule/dao/ScheduleRepository.java",
			"domain/src/main/java/com/beat/domain/schedule/dao/dto/MinPerformanceDateDto.java",
			"domain/src/main/java/com/beat/domain/schedule/domain/Schedule.java"
		);
		List<String> forbiddenPersistencePatterns = List.of(
			"jakarta.persistence.",
			"org.hibernate.annotations.",
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
			"New domain persistence/JPA/Spring Data/QueryDSL leakage must either move to infra or be explicitly reviewed");
	}

	@Test
	void castStaffAndUsersDomainContractsStayPureAndTechnologyNeutral() throws Exception {
		Path castDomain = Path.of("domain/src/main/kotlin/com/beat/domain/cast/domain/Cast.kt");
		Path staffDomain = Path.of("domain/src/main/kotlin/com/beat/domain/staff/domain/Staff.kt");
		Path usersDomain = Path.of("domain/src/main/kotlin/com/beat/domain/user/domain/Users.kt");
		Path castRepository = Path.of("domain/src/main/java/com/beat/domain/cast/repository/CastRepository.java");
		Path staffRepository = Path.of("domain/src/main/java/com/beat/domain/staff/repository/StaffRepository.java");
		Path usersRepository = Path.of("domain/src/main/java/com/beat/domain/user/repository/UserRepository.java");
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
			"Performance performance",
			"com.beat.domain.performance.domain.Performance"
		);

		assertFalse(Files.exists(Path.of("domain/src/main/java/com/beat/domain/cast/dao/CastRepository.java")));
		assertFalse(Files.exists(Path.of("domain/src/main/java/com/beat/domain/staff/dao/StaffRepository.java")));
		assertFalse(Files.exists(Path.of("domain/src/main/java/com/beat/domain/user/dao/UserRepository.java")));
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
		assertTrue(Files.readString(castDomain).contains("linkedPerformanceId: PerformanceId"));
		assertTrue(Files.readString(staffDomain).contains("linkedPerformanceId: PerformanceId"));
		assertTrue(Files.readString(usersDomain).contains("class Users private constructor"));
	}

	@Test
	void domainCustomRepositoryContractsRemainExplicitIssue380TransitionalAllowlist() throws Exception {
		Set<String> allowedCustomRepositoryContracts = Set.of(
			"domain/src/main/java/com/beat/domain/booking/dao/TicketRepositoryCustom.java",
			"domain/src/main/java/com/beat/domain/schedule/dao/ScheduleRepositoryCustom.java"
		);

		Set<String> actualCustomRepositoryContracts = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> path.getFileName().toString().endsWith("RepositoryCustom.java"))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());

		assertEquals(allowedCustomRepositoryContracts, actualCustomRepositoryContracts,
			"Domain custom repository contracts are transitional query/persistence hooks and must stay explicitly reviewed");
	}

	@Test
	void domainJpaEntityAndRepositoryInventoryMatchesIssue380Baseline() throws Exception {
		Set<String> allowedJpaModelSources = Set.of(
			"domain/src/main/java/com/beat/domain/BaseTimeEntity.java",
			"domain/src/main/java/com/beat/domain/booking/domain/Booking.java",
			"domain/src/main/java/com/beat/domain/performance/domain/Performance.java",
			"domain/src/main/java/com/beat/domain/schedule/domain/Schedule.java"
		);
		Set<String> allowedJpaRepositorySources = Set.of(
			"domain/src/main/java/com/beat/domain/booking/dao/BookingRepository.java",
			"domain/src/main/java/com/beat/domain/booking/dao/TicketRepository.java",
			"domain/src/main/java/com/beat/domain/performance/dao/PerformanceRepository.java",
			"domain/src/main/java/com/beat/domain/schedule/dao/ScheduleRepository.java"
		);

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
			"domain/src/main/java/com/beat/domain/promotion/repository/PromotionRepository.java");
		Path oldDomainSpringDataRepository = Path.of(
			"domain/src/main/java/com/beat/domain/promotion/dao/PromotionRepository.java");
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
		String performanceIdSource = Files.readString(
			Path.of("domain/src/main/kotlin/com/beat/domain/performance/domain/PerformanceId.kt"));

		assertTrue(promotionDomainSource.contains("@JvmInline"));
		assertTrue(promotionDomainSource.contains("value class Id private constructor"));
		assertFalse(promotionDomainSource.contains("value class PerformanceId"));
		assertTrue(promotionDomainSource.contains("import com.beat.domain.performance.domain.PerformanceId"));
		assertTrue(promotionDomainSource.contains("private val linkedPerformanceId: PerformanceId?"));
		assertTrue(performanceIdSource.contains("value class PerformanceId private constructor"));
		assertTrue(promotionDomainSource.contains("fun from(value: Long): Id"));
		assertTrue(promotionDomainSource.contains("fun fromNullable(value: Long?): Id?"));
		assertTrue(performanceIdSource.contains("fun from(value: Long): PerformanceId"));
		assertTrue(performanceIdSource.contains("fun fromNullable(value: Long?): PerformanceId?"));
		assertTrue(promotionDomainSource.contains("Id.fromNullable(id)"));
		assertTrue(promotionDomainSource.contains("PerformanceId.fromNullable(performanceId)"));
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
	void domainJpaAnnotationsAndAuditingStayLimitedToIssue380Baseline() throws Exception {
		Set<String> allowedEntitySources = Set.of(
			"domain/src/main/java/com/beat/domain/booking/domain/Booking.java",
			"domain/src/main/java/com/beat/domain/performance/domain/Performance.java",
			"domain/src/main/java/com/beat/domain/schedule/domain/Schedule.java"
		);
		Set<String> allowedMappedSuperclassSources = Set.of(
			"domain/src/main/java/com/beat/domain/BaseTimeEntity.java"
		);

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
			"Domain JPA auditing must stay isolated to BaseTimeEntity during the issue #380 transition");
	}

	@Test
	void queryDslAndJpaBootstrapRemainExplicitIssue380TransitionalSurfaces() throws Exception {
		Set<String> allowedQueryProjectionSources = Set.of(
			"domain/src/main/java/com/beat/domain/schedule/dao/dto/MinPerformanceDateDto.java"
		);
		Set<String> actualQueryProjectionSources = sourceFiles(Path.of("domain/src/main")).stream()
			.filter(path -> contains(path, "@QueryProjection") || contains(path, "com.querydsl."))
			.map(path -> path.toString().replace('\\', '/'))
			.collect(Collectors.toSet());
		String domainBuild = Files.readString(Path.of("domain/build.gradle.kts"));
		String jpaConfig = Files.readString(Path.of("infra/src/main/java/com/beat/infra/config/JpaConfig.java"));

		assertEquals(allowedQueryProjectionSources, actualQueryProjectionSources);
		assertTrue(domainBuild.contains("val queryDslSrcDir = layout.buildDirectory.dir(\"generated/querydsl\")"));
		assertTrue(domainBuild.contains("compileOnly(libs.spring.boot.starter.data.jpa)"));
		assertTrue(domainBuild.contains("compileOnly(libs.querydsl.jpa.jakarta)"));
		assertTrue(domainBuild.contains("annotationProcessor(libs.querydsl.apt.jakarta)"));
		assertTrue(domainBuild.contains("annotationProcessor(libs.jakarta.persistence.api)"));
		assertTrue(jpaConfig.contains("@EnableJpaAuditing"));
		assertTrue(jpaConfig.contains("@EntityScan(basePackages = \"com.beat.domain\", "
			+ "basePackageClasses = InfraPersistenceMarker.class)"));
		assertTrue(jpaConfig.contains("@EnableJpaRepositories(basePackages = \"com.beat.domain\", "
			+ "basePackageClasses = InfraPersistenceMarker.class)"));
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
		List<String> violations = executableSources.stream()
			.flatMap(path -> readLines(path).stream()
				.filter(line -> line.startsWith("import com.beat.infra.persistence."))
				.filter(line -> !line.equals("import com.beat.infra.persistence.InfraPersistenceConfig"))
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
