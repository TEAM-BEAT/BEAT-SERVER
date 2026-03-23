package com.beat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BeatApplicationBootstrapContractTest {

	@Test
	void rootResourcesDisableSchedulerOwnershipByDefault() throws Exception {
		String config = Files.readString(Path.of("src/main/resources/application.yml"));

		assertTrue(config.contains("beat:"));
		assertTrue(config.contains("scheduler:"));
		assertTrue(config.contains("owner: false"));
		assertFalse(config.contains("owner: true"));
	}

	@Test
	void rootNoLongerOwnsSchedulerRuntimeSources() {
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/global/common/scheduler/application/JobSchedulerService.java")));
		assertFalse(
			Files.exists(Path.of("src/main/java/com/beat/global/common/scheduler/application/JobSchedulerTransactionalService.java")));
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/domain/booking/application/TicketCleanupScheduler.java")));
		assertFalse(Files.exists(Path.of("src/main/java/com/beat/domain/promotion/application/PromotionSchedulerService.java")));

		assertTrue(
			Files.exists(Path.of("batch/src/main/java/com/beat/global/common/scheduler/application/JobSchedulerService.java")));
		assertTrue(Files.exists(
			Path.of("batch/src/main/java/com/beat/global/common/scheduler/application/JobSchedulerTransactionalService.java")));
		assertTrue(Files.exists(Path.of("batch/src/main/java/com/beat/domain/booking/application/TicketCleanupScheduler.java")));
		assertTrue(Files.exists(Path.of("batch/src/main/java/com/beat/domain/promotion/application/PromotionSchedulerService.java")));
	}
}
