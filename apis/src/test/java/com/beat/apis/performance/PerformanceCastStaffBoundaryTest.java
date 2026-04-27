package com.beat.apis.performance;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PerformanceCastStaffBoundaryTest {

	@Test
	void performanceServicesUseDomainCastStaffContractsWithoutInfraPersistenceTypes() throws Exception {
		List<String> violations = sourceFiles(Path.of("src/main/java"), Path.of("src/main/kotlin")).stream()
			.flatMap(path -> readLines(path).stream()
				.filter(line -> line.startsWith("import com.beat.infra.persistence.cast.")
					|| line.startsWith("import com.beat.infra.persistence.staff."))
				.map(line -> path.toString().replace('\\', '/') + ": " + line))
			.toList();
		String managementService = Files.readString(Path.of(
			"src/main/java/com/beat/apis/performance/application/PerformanceManagementService.java"));
		String modifyService = Files.readString(Path.of(
			"src/main/java/com/beat/apis/performance/application/PerformanceModifyService.java"));

		assertTrue(violations.isEmpty(),
			"Performance executable code must not import Cast/Staff infra persistence types:\n"
				+ String.join("\n", violations));
		assertTrue(managementService.contains("import com.beat.domain.cast.repository.CastRepository;"));
		assertTrue(managementService.contains("import com.beat.domain.staff.repository.StaffRepository;"));
		assertTrue(managementService.contains("performance.getId()"));
		assertTrue(modifyService.contains("import com.beat.domain.cast.repository.CastRepository;"));
		assertTrue(modifyService.contains("import com.beat.domain.staff.repository.StaffRepository;"));
		assertTrue(modifyService.contains("performance.getId()"));
		assertAll(
			() -> assertTrue(managementService.contains("castRepository.deleteByPerformanceId(performanceId);")),
			() -> assertTrue(managementService.contains("staffRepository.deleteByPerformanceId(performanceId);")),
			() -> assertTrue(managementService.indexOf("castRepository.deleteByPerformanceId(performanceId);")
				< managementService.indexOf("performanceRepository.delete(performance);")),
			() -> assertTrue(managementService.indexOf("staffRepository.deleteByPerformanceId(performanceId);")
				< managementService.indexOf("performanceRepository.delete(performance);"))
		);
	}

	private List<String> readLines(Path path) {
		try {
			return Files.readAllLines(path);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read " + path, exception);
		}
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
