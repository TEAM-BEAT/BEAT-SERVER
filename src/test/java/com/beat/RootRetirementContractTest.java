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
		assertTrue(Files.exists(Path.of("observability/src/main/resources/log4j2-spring.xml")));
	}

	@Test
	void batchRemainsTheSchedulerOwnerLaneAfterRootRetirement() {
		assertTrue(
			Files.exists(Path.of("batch/src/main/java/com/beat/global/common/scheduler/application/JobSchedulerService.java")));
		assertTrue(
			Files.exists(Path.of("batch/src/main/java/com/beat/global/common/scheduler/application/JobSchedulerTransactionalService.java")));
		assertTrue(Files.exists(Path.of("batch/src/main/java/com/beat/domain/booking/application/TicketCleanupScheduler.java")));
		assertTrue(Files.exists(Path.of("batch/src/main/java/com/beat/domain/promotion/application/PromotionSchedulerService.java")));
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
		assertTrue(dockerfileModule.contains("ARG MODULE"));
		assertTrue(dockerfileModule.contains("ARG PROFILE"));
	}
}
