package com.beat.apis.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.beat.application.frontoffice.booking.command.GuestBookingCommandService;
import com.beat.application.frontoffice.booking.command.GuestBookingCommand;
import com.beat.application.frontoffice.booking.result.BookingCreationResult;
import com.beat.apis.support.AbstractIntegrationTest;
import com.beat.domain.booking.repository.BookingRepository;
import com.beat.domain.performance.repository.PerformanceRepository;
import com.beat.domain.sharedkernel.vo.BankName;
import com.beat.domain.performance.model.Genre;
import com.beat.domain.performance.model.Performance;
import com.beat.domain.performance.vo.PaymentAccount;
import com.beat.domain.performance.vo.PerformancePeriod;
import com.beat.domain.performance.vo.RunningTime;
import com.beat.domain.performance.vo.TicketPrice;
import com.beat.domain.schedule.repository.ScheduleRepository;
import com.beat.domain.schedule.model.Schedule;
import com.beat.domain.schedule.model.ScheduleNumber;
import com.beat.domain.exception.DomainException;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.user.model.Users;
import com.beat.domain.user.repository.UserRepository;

class GuestBookingServiceConcurrencyTest extends AbstractIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(GuestBookingServiceConcurrencyTest.class);
	private static final int CONCURRENT_REQUEST_COUNT = 30;

	@Autowired
	private GuestBookingCommandService guestBookingService;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private PerformanceRepository performanceRepository;

	@Autowired
	private BookingRepository bookingRepository;

	@Autowired
	private UserRepository userRepository;

	private Schedule schedule1;
	private Schedule schedule2;

	@BeforeEach
	@Transactional
	void setup() {
		logger.info("Setting up initial data...");

		Users maker = createMakerUser();
		Performance performance = createPerformance(maker);
		schedule1 = createSchedule(performance, ScheduleNumber.FIRST, 10);
		schedule2 = createSchedule(performance, ScheduleNumber.SECOND, 1);

		logger.info("Setup completed.");
	}

	@Test
	void testConcurrentGuestBooking() {
		long firstScheduleSuccessCount = executeConcurrentGuestBookings(schedule1, 2, ScheduleNumber.FIRST);
		long secondScheduleSuccessCount = executeConcurrentGuestBookings(schedule2, 1, ScheduleNumber.SECOND);

		assertEquals(5L, firstScheduleSuccessCount);
		assertEquals(1L, secondScheduleSuccessCount);
		assertFinalState();
	}

	private String generateRandomPassword() {
		int randomNum = ThreadLocalRandom.current().nextInt(1000, 10000);
		return String.format("%04d", randomNum);
	}

	private Users createMakerUser() {
		Users maker = Users.create();
		return userRepository.save(maker);
	}

	private Performance createPerformance(Users maker) {
		Performance performance = Performance.create(
			"Performance Title",
			Genre.BAND,
			RunningTime.of(120),
			"Performance Description",
			"Performance Attention Note",
			PaymentAccount.of(BankName.BUSAN, "2342-234234-2344", "이동훈"),
			"poster.jpg",
			"Performance Team",
			"Performance Venue",
			"도로명 주소",
			"상세 주소",
			"123.1111",
			"12.1234",
			"010-1111-1111",
			PerformancePeriod.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
			TicketPrice.of(10000),
			30,
			maker.getId()
		);
		return performanceRepository.save(performance);
	}

	private Schedule createSchedule(Performance performance, ScheduleNumber scheduleNumber, int remainingTicketCount) {
		LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);
		Schedule schedule = Schedule.create(
			performanceDate,
			performanceDate.plusMinutes(performance.getRunningTime()),
			remainingTicketCount,
			scheduleNumber,
			performance.getId()
		);
		return scheduleRepository.save(schedule);
	}

	private long executeConcurrentGuestBookings(
		Schedule schedule,
		int purchaseTicketCount,
		ScheduleNumber scheduleNumber
	) {
		ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUEST_COUNT);
		CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUEST_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> futures = new ArrayList<>();

		for (int i = 0; i < CONCURRENT_REQUEST_COUNT; i++) {
			futures.add(executor.submit(() -> {
				ready.countDown();
				start.await();
				return createGuestBooking(schedule, purchaseTicketCount, scheduleNumber);
			}));
		}

		try {
			assertTrue(ready.await(10, TimeUnit.SECONDS), "Concurrent booking tasks did not become ready");
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
			throw new AssertionError("Concurrent booking task setup interrupted", e);
		}
		start.countDown();
		return awaitExecutors(futures, executor);
	}

	private boolean createGuestBooking(Schedule schedule, int purchaseTicketCount, ScheduleNumber scheduleNumber) {
		try {
			BookingCreationResult response =
				guestBookingService.createGuestBooking(
					createGuestBookingRequest(schedule, purchaseTicketCount, scheduleNumber));
			assertNotNull(response);
			return true;
		} catch (DomainException e) {
			if (e.getErrorCode() == ScheduleErrorCode.INSUFFICIENT_TICKETS) {
				return false;
			}
			throw e;
		}
	}

	private GuestBookingCommand createGuestBookingRequest(
		Schedule schedule,
		int purchaseTicketCount,
		ScheduleNumber scheduleNumber
	) {
		return GuestBookingCommand.of(
			schedule.getId(),
			purchaseTicketCount,
			"서지우",
			"010-2222-7196",
			"900101",
			generateRandomPassword()
		);
	}

	private long awaitExecutors(List<Future<Boolean>> futures, ExecutorService executor) {
		executor.shutdown();

		try {
			if (!executor.awaitTermination(120, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		long successCount = 0L;
		for (Future<Boolean> future : futures) {
			try {
				if (future.get(10, TimeUnit.SECONDS)) {
					successCount++;
				}
			} catch (TimeoutException e) {
				future.cancel(true);
				throw new AssertionError("Concurrent booking task timed out", e);
			} catch (InterruptedException e) {
				future.cancel(true);
				Thread.currentThread().interrupt();
				throw new AssertionError("Concurrent booking task interrupted", e);
			} catch (Exception e) {
				throw new AssertionError("Concurrent booking task failed", e);
			}
		}
		return successCount;
	}

	private void assertFinalState() {
		Schedule firstSchedule = scheduleRepository.findById(schedule1.getId()).orElseThrow();
		Schedule secondSchedule = scheduleRepository.findById(schedule2.getId()).orElseThrow();

		assertEquals(10, firstSchedule.getAllocatedTicketCount());
		assertEquals(1, secondSchedule.getAllocatedTicketCount());

		long firstScheduleBookingCount = bookingRepository.findAll().stream()
			.filter(booking -> Objects.equals(booking.getScheduleId(), firstSchedule.getId()))
			.count();
		long secondScheduleBookingCount = bookingRepository.findAll().stream()
			.filter(booking -> Objects.equals(booking.getScheduleId(), secondSchedule.getId()))
			.count();

		assertEquals(5L, firstScheduleBookingCount);
		assertEquals(1L, secondScheduleBookingCount);
	}
}
