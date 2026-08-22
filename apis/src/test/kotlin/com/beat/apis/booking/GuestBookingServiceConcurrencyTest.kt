package com.beat.apis.booking

import com.beat.apis.support.BeatTestContainersConfig
import com.beat.application.frontoffice.booking.booker.command.GuestBookingCommand
import com.beat.application.frontoffice.booking.booker.command.GuestBookingCommandService
import com.beat.application.frontoffice.booking.booker.result.BookingCreationResult
import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PaymentAccount
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.exception.ScheduleErrorCode
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.ArrayList
import java.util.Objects
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@SpringBootTest
@ActiveProfiles("test")
@Import(BeatTestContainersConfig::class)
@Tags("integration", "correctness")
open class GuestBookingServiceConcurrencyTest : FunSpec() {

    @Autowired
    private lateinit var guestBookingService: GuestBookingCommandService

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var schedule1: Schedule
    private lateinit var schedule2: Schedule

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        beforeTest {
            setup()
        }

        test("concurrent guest bookings do not oversell schedules") {
            executeConcurrentGuestBookings(schedule1, 2, ScheduleNumber.FIRST) shouldBe 5L
            executeConcurrentGuestBookings(schedule2, 1, ScheduleNumber.SECOND) shouldBe 1L
            assertFinalState()
        }
    }

    private fun setup() {
        val maker = userRepository.save(Users.create())
        val performance = performanceRepository.save(
            Performance.create(
                performanceTitle = "Performance Title",
                genre = Genre.BAND,
                runningTime = RunningTime.of(120),
                performanceDescription = "Performance Description",
                performanceAttentionNote = "Performance Attention Note",
                paymentAccount = PaymentAccount.of(BankName.BUSAN, "2342-234234-2344", "이동훈"),
                posterImage = "poster.jpg",
                performanceTeamName = "Performance Team",
                performanceVenue = "Performance Venue",
                roadAddressName = "도로명 주소",
                placeDetailAddress = "상세 주소",
                latitude = "123.1111",
                longitude = "12.1234",
                performanceContact = "010-1111-1111",
                performancePeriod = PerformancePeriod.of(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31),
                ),
                ticketPrice = TicketPrice.of(10_000),
                totalScheduleCount = 30,
                userId = requireNotNull(maker.id),
            ),
        )
        schedule1 = createSchedule(performance, ScheduleNumber.FIRST, 10)
        schedule2 = createSchedule(performance, ScheduleNumber.SECOND, 1)
    }

    private fun createSchedule(
        performance: Performance,
        scheduleNumber: ScheduleNumber,
        remainingTicketCount: Int,
    ): Schedule {
        val performanceDate = LocalDateTime.now().plusDays(1)
        return scheduleRepository.save(
            Schedule.create(
                performanceDate = performanceDate,
                bookingCloseAt = performanceDate.plusMinutes(performance.runningTime.toLong()),
                totalTicketCount = remainingTicketCount,
                scheduleNumber = scheduleNumber,
                performanceId = requireNotNull(performance.id),
            ),
        )
    }

    private fun executeConcurrentGuestBookings(
        schedule: Schedule,
        purchaseTicketCount: Int,
        scheduleNumber: ScheduleNumber,
    ): Long {
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUEST_COUNT)
        val ready = CountDownLatch(CONCURRENT_REQUEST_COUNT)
        val start = CountDownLatch(1)
        val futures = ArrayList<Future<Boolean>>(CONCURRENT_REQUEST_COUNT)

        repeat(CONCURRENT_REQUEST_COUNT) { requestIndex ->
            futures += executor.submit<Boolean> {
                ready.countDown()
                check(start.await(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    "Concurrent booking tasks did not start"
                }
                createGuestBooking(schedule, purchaseTicketCount, scheduleNumber, requestIndex)
            }
        }

        try {
            if (!ready.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw AssertionError("Concurrent booking tasks did not become ready")
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
            throw AssertionError("Concurrent booking task setup interrupted", e)
        }
        start.countDown()
        return awaitExecutors(futures, executor)
    }

    private fun createGuestBooking(
        schedule: Schedule,
        purchaseTicketCount: Int,
        scheduleNumber: ScheduleNumber,
        requestIndex: Int,
    ): Boolean {
        return try {
            val response: BookingCreationResult = guestBookingService.createGuestBooking(
                createGuestBookingRequest(schedule, purchaseTicketCount, scheduleNumber, requestIndex),
            )
            checkNotNull(response)
            true
        } catch (e: FrontofficeApplicationException) {
            if (e.errorCode.code == ScheduleErrorCode.INSUFFICIENT_TICKETS.code &&
                e.errorCode.type == FrontofficeApplicationErrorType.INVALID_INPUT
            ) {
                false
            } else {
                throw e
            }
        }
    }

    private fun createGuestBookingRequest(
        schedule: Schedule,
        purchaseTicketCount: Int,
        scheduleNumber: ScheduleNumber,
        requestIndex: Int,
    ): GuestBookingCommand = GuestBookingCommand.of(
        requireNotNull(schedule.id),
        purchaseTicketCount,
        "서지우",
        "010-%04d-%04d".format(1000 + scheduleNumber.ordinal, 1000 + requestIndex),
        "900101",
        "1234",
    )

    private fun awaitExecutors(
        futures: List<Future<Boolean>>,
        executor: ExecutorService,
    ): Long {
        executor.shutdown()

        try {
            if (!executor.awaitTermination(EXECUTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }

        var successCount = 0L
        futures.forEach { future ->
            try {
                if (future.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    successCount++
                }
            } catch (e: TimeoutException) {
                future.cancel(true)
                throw AssertionError("Concurrent booking task timed out", e)
            } catch (e: InterruptedException) {
                future.cancel(true)
                Thread.currentThread().interrupt()
                throw AssertionError("Concurrent booking task interrupted", e)
            } catch (e: Exception) {
                throw AssertionError("Concurrent booking task failed", e)
            }
        }
        return successCount
    }

    private fun assertFinalState() {
        val firstSchedule = checkNotNull(scheduleRepository.findById(requireNotNull(schedule1.id)))
        val secondSchedule = checkNotNull(scheduleRepository.findById(requireNotNull(schedule2.id)))

        firstSchedule.allocatedTicketCount shouldBe 10
        secondSchedule.allocatedTicketCount shouldBe 1

        val firstScheduleBookingCount = bookingRepository.findAll().count {
            Objects.equals(it.scheduleId, firstSchedule.id)
        }
        val secondScheduleBookingCount = bookingRepository.findAll().count {
            Objects.equals(it.scheduleId, secondSchedule.id)
        }

        firstScheduleBookingCount shouldBe 5
        secondScheduleBookingCount shouldBe 1
    }

    private companion object {
        const val CONCURRENT_REQUEST_COUNT = 30
        const val READY_TIMEOUT_SECONDS = 10L
        const val TASK_TIMEOUT_SECONDS = 10L
        const val EXECUTOR_TIMEOUT_SECONDS = 120L
    }
}
