package com.beat.domain.booking;

import com.beat.domain.booking.application.GuestBookingService;
import com.beat.domain.booking.application.dto.GuestBookingRequest;
import com.beat.domain.booking.application.dto.GuestBookingResponse;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.user.repository.UserRepository;
import com.beat.domain.user.domain.Users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
public class GuestBookingServiceConcurrencyTest {

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
    public void setup() {
        logger.info("Setting up initial data...");

        Users initialUser = Users.create();
        userRepository.save(initialUser);

        logger.info("Setting up userId = 1 메이커");


        Performance performance = Performance.create(
                "Performance Title",
                Genre.BAND,
                120,
                "Performance Description",
                "Performance Attention Note",
                BankName.BUSAN,
                "2342-234234-2344",
                "poster.jpg",
                "Performance Team",
                "Performance Venue",
                "010-1234-5678",
                "2024-01-01 to 2024-12-31",
                35000,
                1,
                initialUser
        );
        performanceRepository.save(performance);

        LocalDateTime performanceDate = LocalDateTime.now().plusDays(1);
        schedule1 = Schedule.create(
                performanceDate,
                10, // 남은 티켓 10매
                0,
                true,
                ScheduleNumber.FIRST,
                performance
        );
        scheduleRepository.save(schedule1);

        schedule2 = Schedule.create(
                performanceDate,
                1, // 남은 티켓 1매
                0,
                true,
                ScheduleNumber.SECOND,
                performance
        );
        scheduleRepository.save(schedule2);

        logger.info("Setup completed.");
    }

    @Test
    public void testConcurrentGuestBooking() {
        int threadCount1 = 100; // 회차 1번에 대해 100명 요청
        int threadCount2 = 150; // 회차 2번에 대해 150명 요청

        ExecutorService executorService1 = Executors.newFixedThreadPool(threadCount1);
        ExecutorService executorService2 = Executors.newFixedThreadPool(threadCount2);

        for (int i = 0; i < threadCount1; i++) {
            executorService1.submit(() -> {
                try {
                    GuestBookingRequest request = GuestBookingRequest.of(
                            schedule1.getId(), // 회차 1번 스케줄 ID를 사용
                            2,  // purchaseTicketCount
                            "FIRST",
                            "서지우",
                            "010-2222-7196",
                            "1990-01-01",
                            generateRandomPassword(),
                            35000,
                            false
                    );
                    GuestBookingResponse response = guestBookingService.createGuestBooking(request);
                    assertNotNull(response);
                } catch (Exception e) {
                    logger.error("Exception occurred during booking for schedule 1: ", e);
                    fail("Exception occurred: " + e.getMessage());
                }
            });
        }

        for (int i = 0; i < threadCount2; i++) {
            executorService2.submit(() -> {
                try {
                    GuestBookingRequest request = GuestBookingRequest.of(
                            schedule2.getId(), // 회차 2번 스케줄 ID를 사용
                            1,  // purchaseTicketCount
                            "SECOND",
                            "서지우",
                            "010-2222-7196",
                            "1990-01-01",
                            generateRandomPassword(),
                            35000,
                            false
                    );
                    GuestBookingResponse response = guestBookingService.createGuestBooking(request);
                    assertNotNull(response);
                } catch (Exception e) {
                    logger.error("Exception occurred during booking for schedule 2: ", e);
                    fail("Exception occurred: " + e.getMessage());
                }
            });
        }

        // 모든 태스크가 완료될 때까지 대기
        executorService1.shutdown();
        executorService2.shutdown();
        try {
            if (!executorService1.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService1.shutdownNow();
            }
            if (!executorService2.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService2.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService1.shutdownNow();
            executorService2.shutdownNow();
        }

        // 로그로 예매된 티켓 수 확인
        Schedule finalSchedule1 = scheduleRepository.findById(schedule1.getId()).orElse(null);
        Schedule finalSchedule2 = scheduleRepository.findById(schedule2.getId()).orElse(null);

        if (finalSchedule1 != null) {
            logger.info("Total tickets sold for schedule 1: {}", finalSchedule1.getSoldTicketCount());
        }
        if (finalSchedule2 != null) {
            logger.info("Total tickets sold for schedule 2: {}", finalSchedule2.getSoldTicketCount());
        }

        // 예매된 유저와 예약 정보 로그 출력
        bookingRepository.findAll().forEach(booking -> {
            logger.info("Booking ID: {}, User ID: {}, Schedule ID: {}, Tickets: {}",
                    booking.getId(), booking.getUsers().getId(),
                    booking.getSchedule().getId(), booking.getPurchaseTicketCount());
        });
        userRepository.findAll().forEach(user -> {
            logger.info("User ID: {}", user.getId());
        });
    }

    private String generateRandomPassword() {
        int randomNum = ThreadLocalRandom.current().nextInt(1000, 10000);
        return String.format("%04d", randomNum);
    }
}