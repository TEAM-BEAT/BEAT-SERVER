package com.beat.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PromotionBoundaryTest {

	@Test
	void domainPromotionBoundaryShouldNotImportAdminDto() throws Exception {
		String port = Files.readString(
			Path.of("domain/src/main/java/com/beat/domain/promotion/port/in/PromotionUseCase.java"));
		String service = Files.readString(
			Path.of("apis/src/main/java/com/beat/apis/promotion/application/PromotionService.java"));

		assertFalse(port.contains("com.beat.admin.application.dto"));
		assertFalse(service.contains("com.beat.admin.application.dto"));
		assertTrue(service.startsWith("package com.beat.apis.promotion.application;"));
	}
}
