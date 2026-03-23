package com.beat.apis.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

import com.beat.apis.support.AbstractIntegrationTest;
import com.beat.domain.booking.application.GuestBookingService;
import com.beat.domain.booking.application.dto.GuestBookingRequest;
import com.beat.domain.booking.application.dto.GuestBookingResponse;
import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Users;
import com.beat.global.common.exception.BadRequestException;

class GuestBookingServiceConcurrencyTest extends AbstractIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(GuestBookingServiceConcurrencyTest.class);

	@Autowired
	private GuestBookingService guestBookingService;

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
		ExecutorService firstScheduleExecutor = Executors.newFixedThreadPool(100);
		ExecutorService secondScheduleExecutor = Executors.newFixedThreadPool(150);

		List<Future<Boolean>> firstScheduleFutures =
			submitGuestBookings(firstScheduleExecutor, 100, schedule1, 2, ScheduleNumber.FIRST);
		List<Future<Boolean>> secondScheduleFutures =
			submitGuestBookings(secondScheduleExecutor, 150, schedule2, 1, ScheduleNumber.SECOND);

		long firstScheduleSuccessCount = awaitExecutors(firstScheduleFutures, firstScheduleExecutor);
		long secondScheduleSuccessCount = awaitExecutors(secondScheduleFutures, secondScheduleExecutor);

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
		userRepository.save(maker);
		logger.info("Setting up maker user.");
		return maker;
	}

	private Performance createPerformance(Users maker) {
		Performance performance = Performance.create(
			"Performance Title",
			Genre.BAND,
			120,
			"Performance Description",
			"Performance Attention Note",
			BankName.BUSAN,
			"2342-234234-2344",
			"이동훈",
			"poster.jpg",
			"Performance Team",
			"Performance Venue",
			"도로명 주소",
			"상세 주소",
			"123.1111",
			"12.1234",
			"010-1111-1111",
			"2024-01-01 to 2024-12-31",
			10000,
			30,
			maker
		);
		performanceRepository.save(performance);
		return performance;
	}

	private Schedule createSchedule(Performance performance, ScheduleNumber scheduleNumber, int remainingTicketCount) {
		Schedule schedule = Schedule.create(
			LocalDateTime.now().plusDays(1),
			remainingTicketCount,
			0,
			true,
			scheduleNumber,
			performance
		);
		scheduleRepository.save(schedule);
		return schedule;
	}

	private List<Future<Boolean>> submitGuestBookings(
		ExecutorService executorService,
		int requestCount,
		Schedule schedule,
		int purchaseTicketCount,
		ScheduleNumber scheduleNumber
	) {
		List<Future<Boolean>> futures = new ArrayList<>();
		for (int i = 0; i < requestCount; i++) {
			futures.add(
				executorService.submit(() -> createGuestBooking(schedule, purchaseTicketCount, scheduleNumber)));
		}
		return futures;
	}

	private boolean createGuestBooking(Schedule schedule, int purchaseTicketCount, ScheduleNumber scheduleNumber) {
		try {
			GuestBookingResponse response =
				guestBookingService.createGuestBooking(
					createGuestBookingRequest(schedule, purchaseTicketCount, scheduleNumber));
			assertNotNull(response);
			return true;
		} catch (BadRequestException e) {
			if (e.getBaseErrorCode() == ScheduleErrorCode.INSUFFICIENT_TICKETS) {
				return false;
			}
			throw e;
		}
	}

	private GuestBookingRequest createGuestBookingRequest(
		Schedule schedule,
		int purchaseTicketCount,
		ScheduleNumber scheduleNumber
	) {
		return GuestBookingRequest.of(
			schedule.getId(),
			purchaseTicketCount,
			scheduleNumber,
			"서지우",
			"010-2222-7196",
			"1990-01-01",
			generateRandomPassword(),
			35000,
			BookingStatus.CHECKING_PAYMENT
		);
	}

	private long awaitExecutors(List<Future<Boolean>> futures, ExecutorService executor) {
		executor.shutdown();

		try {
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		long successCount = 0L;
		for (Future<Boolean> future : futures) {
			try {
				if (future.get(5, TimeUnit.SECONDS)) {
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

		assertEquals(10, firstSchedule.getSoldTicketCount());
		assertEquals(1, secondSchedule.getSoldTicketCount());
		assertFalse(firstSchedule.isBooking());
		assertFalse(secondSchedule.isBooking());

		long firstScheduleBookingCount = bookingRepository.findAll().stream()
			.filter(booking -> booking.getSchedule().getId().equals(firstSchedule.getId()))
			.count();
		long secondScheduleBookingCount = bookingRepository.findAll().stream()
			.filter(booking -> booking.getSchedule().getId().equals(secondSchedule.getId()))
			.count();

		assertEquals(5L, firstScheduleBookingCount);
		assertEquals(1L, secondScheduleBookingCount);
	}
}
