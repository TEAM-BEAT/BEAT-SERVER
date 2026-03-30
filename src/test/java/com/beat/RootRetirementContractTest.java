package com.beat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RootRetirementContractTest {

	@Test
	void rootNoLongerOwnsExecutableRuntimeSources() {
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/BeatApplication.java")));
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/legacyroot/config/LegacyRootSecurityConfig.java")));
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/global/common/handler/GlobalAsyncExceptionHandler.java")));
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/global/common/scheduler/application/JobSchedulerService.java")));
		assertFalse(
			Files.exists(Path.of("src/main/java/com/beat/global/common/scheduler/application/JobSchedulerTransactionalService.java")));
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/domain/booking/application/TicketCleanupScheduler.java")));
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/domain/promotion/application/PromotionSchedulerService.java")));
	}

	@Test
	void rootNoLongerOwnsApplicationResourceProfiles() {
		assertFalse(Files.exists(Path.of("src/main/resources/application.yml")));
		assertFalse(Files.exists(Path.of("src/main/resources/application-dev.yml")));
		assertFalse(Files.exists(Path.of("src/main/resources/application-local.yml")));
		assertFalse(Files.exists(Path.of("src/main/resources/application-prod.yml")));
		assertFalse(Files.exists(Path.of("src/main/resources/log4j2-spring.xml")));
	}

	@Test
	void concernOwnedApplicationResourcesExistAfterRootRetirement() {
		assertTrue(Files.exists(Path.of("observability/src/main/resources/application-observability.yml")));
		assertTrue(Files.exists(Path.of("infra/src/main/resources/application-persistence.yml")));
		assertTrue(Files.exists(Path.of("infra/src/main/resources/application-external.yml")));
		assertTrue(Files.exists(Path.of("infra/src/main/resources/application-redis.yml")));
		assertTrue(Files.exists(Path.of("infra/src/main/resources/application-thread-pool.yml")));
		assertTrue(Files.exists(Path.of("gateway/src/main/resources/application-jwt.yml")));
	}

	@Test
	void gradleBuildNoLongerVerifiesLegacyRootBootJarBaseline() throws Exception {
		String buildFile = Files.readString(Path.of("build.gradle.kts"));

		assertFalse(buildFile.contains("verifyLegacyV1Baseline"));
		assertTrue(buildFile.contains("verifyModuleBootJars"));
		assertTrue(buildFile.contains("\":apis:bootJar\""));
		assertTrue(buildFile.contains("\":admin:bootJar\""));
		assertTrue(buildFile.contains("\":batch:bootJar\""));
	}

	@Test
	void executableModuleBuildLogicOwnsLog4j2BackendAndObservabilityOwnsSharedConfig() throws Exception {
		String rootBuild = Files.readString(Path.of("build.gradle.kts"));
		String executableBuildLogic = Files.readString(Path.of("build-logic/src/main/kotlin/beat.spring-boot-app.gradle.kts"));

		assertFalse(rootBuild.contains("spring.boot.starter.log4j2"));
		assertFalse(rootBuild.contains("spring-boot-starter-logging"));
		assertTrue(executableBuildLogic.contains("spring-boot-starter-logging"));
		assertTrue(executableBuildLogic.contains("spring-boot-starter-log4j2"));
		assertTrue(executableBuildLogic.contains("tasks.withType<BootRun>().configureEach"));
		assertTrue(executableBuildLogic.contains("workingDir = rootDir"));
		assertTrue(Files.exists(Path.of("observability/src/main/resources/log4j2-spring.xml")));
	}

	@Test
	void batchRemainsTheSchedulerOwnerLaneAfterRootRetirement() throws Exception {
		Path schedulerService = Path.of("batch/src/main/java/com/beat/batch/scheduler/application/JobSchedulerService.java");
		Path schedulerTransactionalService = Path.of(
			"batch/src/main/java/com/beat/batch/scheduler/application/JobSchedulerTransactionalService.java");
		Path ticketCleanupScheduler = Path.of("batch/src/main/java/com/beat/batch/booking/application/TicketCleanupScheduler.java");
		Path promotionSchedulerService = Path.of("batch/src/main/java/com/beat/batch/promotion/application/PromotionSchedulerService.java");

		assertTrue(Files.exists(schedulerService));
		assertTrue(Files.exists(schedulerTransactionalService));
		assertTrue(Files.exists(ticketCleanupScheduler));
		assertTrue(Files.exists(promotionSchedulerService));
		assertTrue(Files.readString(schedulerService).startsWith("package com.beat.batch.scheduler.application;"));
		assertTrue(Files.readString(schedulerTransactionalService).startsWith("package com.beat.batch.scheduler.application;"));
		assertTrue(Files.readString(ticketCleanupScheduler).startsWith("package com.beat.batch.booking.application;"));
		assertTrue(Files.readString(promotionSchedulerService).startsWith("package com.beat.batch.promotion.application;"));
	}

	@Test
	void packagingAndDeployAutomationTargetsApisExecutableLane() throws Exception {
		String ciPr = Files.readString(Path.of(".github/workflows/ci-pr.yml"));
		String v2WebDeployDev = Files.readString(Path.of(".github/workflows/v2-web-deploy-dev.yml"));
		String v2WebDeployProd = Files.readString(Path.of(".github/workflows/v2-web-deploy-prod.yml"));
		String dockerfileModule = Files.readString(Path.of("Dockerfile.module"));

		assertFalse(Files.exists(Path.of(".github/workflows/dev-CI.yml")));
		assertFalse(Files.exists(Path.of(".github/workflows/prod-CI.yml")));
		assertTrue(ciPr.contains("verifyV2WebBaseline"));
		assertTrue(ciPr.contains("verifyModuleBootJars"));
		assertTrue(ciPr.contains(":admin:test :batch:test"));
		assertFalse(Files.exists(Path.of("Dockerfile")));
		assertFalse(Files.exists(Path.of("Dockerfile-dev")));
		assertFalse(Files.exists(Path.of("Jenkinsfile")));
		assertTrue(v2WebDeployDev.contains("docker build -f Dockerfile.module"));
		assertTrue(v2WebDeployProd.contains("docker build -f Dockerfile.module"));
		assertTrue(v2WebDeployDev.contains("--build-arg MODULE=${MODULE}"));
		assertTrue(v2WebDeployProd.contains("--build-arg MODULE=${MODULE}"));
		assertTrue(v2WebDeployDev.contains("Compute image name"));
		assertTrue(v2WebDeployProd.contains("Compute image name"));
		assertTrue(v2WebDeployDev.contains("appleboy/ssh-action"));
		assertTrue(v2WebDeployDev.contains("./deploy-dev.sh"));
		assertFalse(v2WebDeployDev.contains("docker run -d"));
		assertTrue(v2WebDeployDev.contains("export INTERNAL_PORT='4001'"));
		assertTrue(v2WebDeployProd.contains("-p ${PROD_V2_WEB_PUBLIC_PORT}:4001"));
		assertTrue(dockerfileModule.contains("ARG MODULE"));
		assertTrue(dockerfileModule.contains("ARG PROFILE"));
		assertFalse(dockerfileModule.contains("BEAT_SERVER_PORT=8080"));
		assertFalse(dockerfileModule.contains("BEAT_MANAGEMENT_SERVER_PORT=2222"));
	}
}
