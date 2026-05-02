package com.beat.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PromotionBoundaryTest {

	@Test
	void promotionApplicationServiceStaysModuleLocalAndDomainPortInIsRetired() throws Exception {
		String service = Files.readString(
			Path.of("apis/src/main/java/com/beat/apis/promotion/application/PromotionService.java"));

		assertFalse(service.contains("com.beat.admin.application.dto"));
		assertFalse(service.contains("PromotionUseCase"));
		assertTrue(service.startsWith("package com.beat.apis.promotion.application;"));
		assertFalse(Files.exists(Path.of("domain/src/main/kotlin/com/beat/domain/promotion/port/in")));
		assertFalse(Files.exists(Path.of("apis/src/main/java/com/beat/apis/promotion/application/port/in")));
	}

	@Test
	void executablePromotionFlowsUseDomainRepositoryContractInsteadOfSpringDataRepository() throws Exception {
		String apiPromotionService = Files.readString(
			Path.of("apis/src/main/java/com/beat/apis/promotion/application/PromotionService.java"));
		String adminCommandService = Files.readString(
			Path.of("admin/src/main/java/com/beat/admin/application/service/command/AdminCommandService.java"));
		String adminQueryService = Files.readString(
			Path.of("admin/src/main/java/com/beat/admin/application/service/query/AdminQueryService.java"));
		String batchSchedulerService = Files.readString(
			Path.of("batch/src/main/java/com/beat/batch/promotion/application/PromotionMaintenanceService.java"));

		assertTrue(apiPromotionService.contains("import com.beat.domain.promotion.repository.PromotionRepository;"));
		assertTrue(adminCommandService.contains("import com.beat.domain.promotion.repository.PromotionRepository;"));
		assertTrue(adminQueryService.contains("import com.beat.domain.promotion.repository.PromotionRepository;"));
		assertTrue(batchSchedulerService.contains("import com.beat.domain.promotion.repository.PromotionRepository;"));
		assertFalse(apiPromotionService.contains("com.beat.domain.promotion.dao.PromotionRepository"));
		assertFalse(adminCommandService.contains("com.beat.domain.promotion.dao.PromotionRepository"));
		assertFalse(adminQueryService.contains("com.beat.domain.promotion.dao.PromotionRepository"));
		assertFalse(batchSchedulerService.contains("com.beat.domain.promotion.dao.PromotionRepository"));
	}

}
