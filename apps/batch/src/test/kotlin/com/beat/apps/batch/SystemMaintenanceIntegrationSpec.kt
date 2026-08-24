package com.beat.apps.batch

import com.beat.application.system.booking.command.TicketCleanupService
import com.beat.application.system.promotion.command.PromotionMaintenanceService
import com.beat.apps.batch.support.BeatBatchAcceptanceTest
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.model.Promotion
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@BeatBatchAcceptanceTest
@Tags("integration")
class SystemMaintenanceIntegrationSpec : FunSpec() {
    @Autowired
    private lateinit var ticketCleanupService: TicketCleanupService

    @Autowired
    private lateinit var promotionMaintenanceService: PromotionMaintenanceService

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    @Autowired
    private lateinit var promotionRepository: PromotionRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        beforeTest {
            jdbcTemplate.update("DELETE FROM booking")
            jdbcTemplate.update("DELETE FROM promotion")
            jdbcTemplate.update("DELETE FROM schedule")
            jdbcTemplate.update("DELETE FROM performance")
        }

        test("1년보다 오래된 취소 Booking만 실제 MySQL에서 삭제한다") {
            val oldCancelled = bookingRepository.save(booking(BookingStatus.BOOKING_CANCELLED, "2025-08-22T08:59:59"))
            val boundaryCancelled =
                bookingRepository.save(booking(BookingStatus.BOOKING_CANCELLED, "2025-08-23T09:00:00"))
            val oldConfirmed = bookingRepository.save(booking(BookingStatus.BOOKING_CONFIRMED, "2025-01-01T00:00:00"))

            ticketCleanupService.deleteOldCancelledBookings()

            bookingRepository.findById(checkNotNull(oldCancelled.id)) shouldBe null
            bookingRepository.findById(checkNotNull(boundaryCancelled.id)) shouldNotBe null
            bookingRepository.findById(checkNotNull(oldConfirmed.id)) shouldNotBe null
        }

        test("만료 Promotion을 삭제하고 남은 carousel 번호를 실제 MySQL에서 연속 재배치한다") {
            val activePerformanceId = checkNotNull(performanceRepository.save(performance("active")).id)
            val expiredPerformanceId = checkNotNull(performanceRepository.save(performance("expired")).id)
            scheduleRepository.save(schedule(activePerformanceId, LocalDate.of(2026, 8, 24), ScheduleNumber.FIRST))
            scheduleRepository.save(schedule(expiredPerformanceId, LocalDate.of(2026, 8, 22), ScheduleNumber.FIRST))
            val expired = promotionRepository.save(promotion(expiredPerformanceId, CarouselNumber.FIVE, "expired"))
            val active = promotionRepository.save(promotion(activePerformanceId, CarouselNumber.FOUR, "active"))
            val external = promotionRepository.save(promotion(null, CarouselNumber.TWO, "external"))

            promotionMaintenanceService.checkAndDeleteInvalidPromotions()

            promotionRepository.findById(checkNotNull(expired.id)) shouldBe null
            val remaining = promotionRepository.findAll().sortedBy { it.carouselNumber.number }
            remaining.map { it.id } shouldContainExactly listOf(external.id, active.id)
            remaining.map { it.carouselNumber } shouldContainExactly listOf(CarouselNumber.ONE, CarouselNumber.TWO)
        }
    }
}

private fun booking(status: BookingStatus, cancellationDate: String): Booking = Booking.rehydrate(
    id = null,
    purchaseTicketCount = 1,
    bookerName = "booker",
    bookerPhoneNumber = "010-0000-0000",
    bookingStatus = status,
    createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
    cancellationDate = LocalDateTime.parse(cancellationDate),
    birthDate = null,
    password = null,
    refundAccount = null,
    scheduleId = 1L,
    userId = 1L,
    totalPaymentAmount = 10_000,
)

private fun performance(title: String): Performance = Performance.rehydrate(
    id = null,
    performanceTitle = title,
    genre = Genre.PLAY,
    runningTime = RunningTime.of(90),
    performanceDescription = "description",
    performanceAttentionNote = "attention",
    paymentAccount = null,
    posterImage = "poster.png",
    performanceTeamName = "team",
    performanceVenue = "venue",
    roadAddressName = "road",
    placeDetailAddress = "detail",
    latitude = "37.0",
    longitude = "127.0",
    performanceContact = "010-0000-0000",
    performancePeriod = PerformancePeriod.of(
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 12, 31),
    ),
    ticketPrice = TicketPrice.of(10_000),
    totalScheduleCount = 1,
    userId = 1L,
)

private fun schedule(
    performanceId: Long,
    performanceDate: LocalDate,
    scheduleNumber: ScheduleNumber,
): Schedule = Schedule.rehydrate(
    id = null,
    performanceDate = performanceDate.atTime(20, 0),
    bookingCloseAt = performanceDate.atTime(20, 0),
    totalTicketCount = 10,
    allocatedTicketCount = 0,
    scheduleNumber = scheduleNumber,
    performanceId = performanceId,
)

private fun promotion(
    performanceId: Long?,
    carouselNumber: CarouselNumber,
    suffix: String,
): Promotion = Promotion.rehydrate(
    id = null,
    promotionPhoto = "image-$suffix",
    performanceId = performanceId,
    redirectUrl = "https://beat.example/$suffix",
    isExternal = performanceId == null,
    carouselNumber = carouselNumber,
)
