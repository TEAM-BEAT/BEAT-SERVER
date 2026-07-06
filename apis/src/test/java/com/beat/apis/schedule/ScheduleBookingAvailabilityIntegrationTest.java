package com.beat.apis.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.beat.apis.support.AbstractIntegrationTest;
import com.beat.contracts.schedule.PerformanceScheduleReadPort;
import com.beat.contracts.schedule.readmodel.PerformanceScheduleAvailabilityReadModel;
import com.beat.domain.schedule.repository.ScheduleRepository;

class ScheduleBookingAvailabilityIntegrationTest extends AbstractIntegrationTest {

	private static final long PERFORMANCE_ID = 9_999_991L;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PerformanceScheduleReadPort performanceScheduleReadPort;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("DELETE FROM schedule WHERE performance_id = ?", PERFORMANCE_ID);
	}

	@Test
	void availabilityQueryUsesOneDatabaseClockForEverySchedule() {
		LocalDateTime databaseNow = jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP(6)", LocalDateTime.class);
		insertSchedule(databaseNow.plusDays(1), databaseNow.plusDays(1).plusHours(2), 10, 0, "FIRST");
		insertSchedule(databaseNow.minusHours(2), databaseNow.minusSeconds(1), 10, 0, "SECOND");
		insertSchedule(databaseNow.plusDays(1), databaseNow.plusDays(1).plusHours(2), 10, 10, "THIRD");

		List<PerformanceScheduleAvailabilityReadModel> schedules =
			performanceScheduleReadPort.findAllByPerformanceId(PERFORMANCE_ID);
		Map<String, PerformanceScheduleAvailabilityReadModel> schedulesByNumber = schedules.stream()
			.collect(Collectors.toMap(
				PerformanceScheduleAvailabilityReadModel::getScheduleNumber,
				schedule -> schedule));

		assertEquals(3, schedules.size());
		assertTrue(schedulesByNumber.get("FIRST").isBooking());
		assertFalse(schedulesByNumber.get("SECOND").isBooking());
		assertFalse(schedulesByNumber.get("THIRD").isBooking());
		assertEquals(1,
			schedules.stream().map(PerformanceScheduleAvailabilityReadModel::getEvaluatedAt).distinct().count());

		jdbcTemplate.update("""
			UPDATE schedule
			SET booking_close_at = DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 1 HOUR)
			WHERE performance_id = ? AND schedule_number = 'SECOND'
			""", PERFORMANCE_ID);

		Map<String, PerformanceScheduleAvailabilityReadModel> extendedSchedules = performanceScheduleReadPort
			.findAllByPerformanceId(PERFORMANCE_ID).stream()
			.collect(Collectors.toMap(
				PerformanceScheduleAvailabilityReadModel::getScheduleNumber,
				schedule -> schedule));
		assertTrue(extendedSchedules.get("SECOND").isBooking());
	}

	@Test
	void closeTimeIsRecheckedAfterLockWaitUnderRepeatableRead() throws Exception {
		assertEquals("REPEATABLE-READ",
			jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String.class));
		LocalDateTime databaseNow = jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP(6)", LocalDateTime.class);
		long scheduleId = insertSchedule(databaseNow, databaseNow.plusSeconds(2), 10, 0, "FIRST");
		assertEquals(1L, jdbcTemplate.queryForObject("""
			SELECT CURRENT_TIMESTAMP(6) < booking_close_at
			FROM schedule
			WHERE id = ?
			""", Long.class, scheduleId));
		CountDownLatch lockAcquired = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<Void> lockHolder = executor.submit(() -> {
				new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
					scheduleRepository.lockById(scheduleId).orElseThrow();
					lockAcquired.countDown();
					holdLockPastCloseTime();
				});
				return null;
			});
			assertTrue(lockAcquired.await(5, TimeUnit.SECONDS));

			Future<Boolean> waitingRequest = executor.submit(() ->
				new TransactionTemplate(transactionManager).execute(status -> {
					scheduleRepository.lockById(scheduleId).orElseThrow();
					return scheduleRepository.isBeforeBookingCloseAt(scheduleId);
				})
			);

			lockHolder.get(5, TimeUnit.SECONDS);
			assertFalse(waitingRequest.get(5, TimeUnit.SECONDS));
		} finally {
			executor.shutdownNow();
		}
	}

	private long insertSchedule(
		LocalDateTime performanceDate,
		LocalDateTime bookingCloseAt,
		int totalTicketCount,
		int soldTicketCount,
		String scheduleNumber
	) {
		jdbcTemplate.update("""
			INSERT INTO schedule (
				performance_date,
				booking_close_at,
				total_ticket_count,
				sold_ticket_count,
				schedule_number,
				performance_id
			) VALUES (?, ?, ?, ?, ?, ?)
			""", performanceDate, bookingCloseAt, totalTicketCount, soldTicketCount, scheduleNumber, PERFORMANCE_ID);
		return jdbcTemplate.queryForObject(
			"SELECT id FROM schedule WHERE performance_id = ? AND schedule_number = ?",
			Long.class,
			PERFORMANCE_ID,
			scheduleNumber
		);
	}

	private static void holdLockPastCloseTime() {
		try {
			Thread.sleep(3_000);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}
}
