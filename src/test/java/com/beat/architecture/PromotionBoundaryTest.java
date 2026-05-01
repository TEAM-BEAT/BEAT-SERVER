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
		String adminService = Files.readString(
			Path.of("admin/src/main/java/com/beat/admin/application/AdminService.java"));
		String batchSchedulerService = Files.readString(
			Path.of("batch/src/main/java/com/beat/batch/promotion/application/PromotionMaintenanceService.java"));

		assertTrue(apiPromotionService.contains("import com.beat.domain.promotion.repository.PromotionRepository;"));
		assertTrue(adminService.contains("import com.beat.domain.promotion.repository.PromotionRepository;"));
		assertTrue(batchSchedulerService.contains("import com.beat.domain.promotion.repository.PromotionRepository;"));
		assertFalse(apiPromotionService.contains("com.beat.domain.promotion.dao.PromotionRepository"));
		assertFalse(adminService.contains("com.beat.domain.promotion.dao.PromotionRepository"));
		assertFalse(batchSchedulerService.contains("com.beat.domain.promotion.dao.PromotionRepository"));
	}

}
