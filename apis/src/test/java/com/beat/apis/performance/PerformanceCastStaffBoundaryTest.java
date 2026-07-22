package com.beat.apis.performance;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.beat.apis.performance.application.command.ScheduleSynchronizer;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.schedule.model.Schedule;
import com.beat.domain.schedule.model.ScheduleNumber;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.service.ScheduleSequenceDomainService;

class PerformanceCastStaffBoundaryTest {

	@Test
	void scheduleSynchronizerLocksSchedulesInStableIdOrderBeforeCheckingBookings() {
		ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
		BookingRepository bookingRepository = mock(BookingRepository.class);
		when(scheduleRepository.findIdsByPerformanceId(9L)).thenReturn(List.of(3L, 1L, 2L, 1L));
		when(scheduleRepository.lockById(1L)).thenReturn(Optional.of(schedule(1L)));
		when(scheduleRepository.lockById(2L)).thenReturn(Optional.of(schedule(2L)));
		when(scheduleRepository.lockById(3L)).thenReturn(Optional.of(schedule(3L)));
		ScheduleSynchronizer synchronizer = new ScheduleSynchronizer(
			scheduleRepository, bookingRepository, new ScheduleSequenceDomainService(), Clock.systemUTC());

		synchronizer.lockAndCheckActiveBookings(9L);

		InOrder order = inOrder(scheduleRepository, bookingRepository);
		order.verify(scheduleRepository).findIdsByPerformanceId(9L);
		order.verify(scheduleRepository).lockById(1L);
		order.verify(scheduleRepository).lockById(2L);
		order.verify(scheduleRepository).lockById(3L);
		order.verify(bookingRepository).existsActiveBookingByScheduleIds(anyList(), anyList());
	}

	@Test
	void performanceServicesUseDomainCastStaffContractsWithoutInfraPersistenceTypes() throws Exception {
		List<String> violations = sourceFiles(Path.of("src/main/java"), Path.of("src/main/kotlin")).stream()
			.flatMap(path -> readLines(path).stream()
				.filter(line -> line.startsWith("import com.beat.infra.persistence.cast.")
					|| line.startsWith("import com.beat.infra.persistence.staff."))
				.map(line -> path.toString().replace('\\', '/') + ": " + line))
			.toList();
		String createService = Files.readString(Path.of(
			"src/main/kotlin/com/beat/apis/performance/application/command/PerformanceCreateCommandService.kt"));
		String deleteService = Files.readString(Path.of(
			"src/main/kotlin/com/beat/apis/performance/application/command/PerformanceDeleteCommandService.kt"));
		String modifyService = Files.readString(Path.of(
			"src/main/kotlin/com/beat/apis/performance/application/command/PerformanceModifyCommandService.kt"));
		String scheduleSynchronizer = Files.readString(Path.of(
			"src/main/kotlin/com/beat/apis/performance/application/command/ScheduleSynchronizer.kt"));

		assertTrue(violations.isEmpty(),
			"Performance executable code must not import Cast/Staff infra persistence types:\n"
				+ String.join("\n", violations));
		assertTrue(createService.contains("Performance.create("));
		assertTrue(createService.contains("casts,"));
		assertTrue(createService.contains("staffs,"));
		assertTrue(modifyService.contains(".replaceContent("));
		assertTrue(modifyService.contains("synchronizeCasts(performance"));
		assertTrue(modifyService.contains("synchronizeStaffs(performance"));
		assertTrue(modifyService.contains("synchronizeImages(performance"));
		assertTrue(!modifyService.contains("CastRepository"));
		assertTrue(!modifyService.contains("StaffRepository"));
		assertTrue(scheduleSynchronizer.contains("scheduleIds.distinct().sorted().forEach"));
			assertTrue(modifyService.indexOf("findPerformance(command.performanceId)")
				< modifyService.indexOf("scheduleSynchronizer.lockAndCheckActiveBookings(command.performanceId)"));
		assertTrue(deleteService.indexOf("performanceRepository.lockById(performanceId)")
			< deleteService.indexOf("lockSchedules(scheduleIds)"));
		assertAll(
			() -> assertTrue(!deleteService.contains("CastRepository")),
			() -> assertTrue(!deleteService.contains("StaffRepository")),
			() -> assertTrue(deleteService.contains("performanceRepository.deleteById("))
		);
	}

	private List<String> readLines(Path path) {
		try {
			return Files.readAllLines(path);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read " + path, exception);
		}
	}

	private Schedule schedule(long id) {
		LocalDateTime performanceDate = LocalDateTime.of(2026, 1, 2, 12, 0);
		return Schedule.rehydrate(id, performanceDate, performanceDate.plusHours(1), 10, 0,
			ScheduleNumber.FIRST, 9L);
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
