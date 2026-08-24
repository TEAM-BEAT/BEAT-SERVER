package com.beat.infrastructure.persistence.booking.repository.query

import com.beat.application.frontoffice.booking.booker.BookingHistoryReadPort
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.performance.model.Genre
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.infrastructure.config.JpaConfig
import com.beat.infrastructure.persistence.booking.entity.BookingJpaEntity
import com.beat.infrastructure.persistence.booking.repository.BookingJpaRepository
import com.beat.infrastructure.persistence.performance.entity.PaymentAccountJpaValue
import com.beat.infrastructure.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infrastructure.persistence.performance.entity.PerformancePeriodJpaValue
import com.beat.infrastructure.persistence.performance.repository.PerformanceJpaRepository
import com.beat.infrastructure.persistence.schedule.entity.ScheduleJpaEntity
import com.beat.infrastructure.persistence.schedule.repository.ScheduleJpaRepository
import com.beat.infrastructure.support.MySqlTestContainerConfig
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(
    properties = [
        "spring.config.import=classpath:application-persistence.yml",
        "DB_HIKARI_MAX_POOL_SIZE=10",
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [JpaConfig::class, MySqlTestContainerConfig::class])
@ActiveProfiles("test")
@Tags("integration")
class BookerBookingQueriesIntegrationSpec : FunSpec() {

    @Autowired
    private lateinit var reader: BookingHistoryReadPort

    @Autowired
    private lateinit var bookingRepository: BookingJpaRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleJpaRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceJpaRepository

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("MySQL에서 enum으로 저장된 금액과 nullable payment account를 가진 여러 booking을 매핑한다") {
            val paidPerformance = performanceRepository.saveAndFlush(
                performance("Paid", PaymentAccountJpaValue(BankName.KAKAOBANK, "123", "BEAT")),
            )
            val unpaidPerformance = performanceRepository.saveAndFlush(performance("Unpaid", null))
            val firstSchedule = scheduleRepository.saveAndFlush(schedule(paidPerformance.id!!, ScheduleNumber.FIRST))
            val secondSchedule = scheduleRepository.saveAndFlush(schedule(unpaidPerformance.id!!, ScheduleNumber.SECOND))
            val storedAmountBooking = bookingRepository.saveAndFlush(
                booking(firstSchedule.id!!, BookingStatus.BOOKING_CONFIRMED, 27_000),
            )
            val legacyAmountBooking = bookingRepository.saveAndFlush(
                booking(secondSchedule.id!!, BookingStatus.REFUND_REQUESTED, null),
            )

            val resultById = reader.findByUserId(TEST_USER_ID).associateBy { it.bookingId }

            resultById shouldHaveSize 2
            val storedAmountResult = resultById.getValue(storedAmountBooking.id!!)
            storedAmountResult.bookingStatus shouldBe "BOOKING_CONFIRMED"
            storedAmountResult.totalPaymentAmount shouldBe 27_000
            storedAmountResult.schedule?.scheduleNumber shouldBe "FIRST"
            storedAmountResult.performance?.bankName shouldBe "KAKAOBANK"
            storedAmountResult.performance?.accountNumber shouldBe "123"
            storedAmountResult.performance?.accountHolder shouldBe "BEAT"

            val legacyAmountResult = resultById.getValue(legacyAmountBooking.id!!)
            legacyAmountResult.bookingStatus shouldBe "REFUND_REQUESTED"
            legacyAmountResult.totalPaymentAmount shouldBe null
            legacyAmountResult.schedule?.scheduleNumber shouldBe "SECOND"
            legacyAmountResult.performance?.bankName shouldBe null
            legacyAmountResult.performance?.accountNumber shouldBe null
            legacyAmountResult.performance?.accountHolder shouldBe null
        }
    }

    private fun performance(title: String, paymentAccount: PaymentAccountJpaValue?): PerformanceJpaEntity =
        PerformanceJpaEntity.rehydrate(
            id = null,
            performanceTitle = title,
            genre = Genre.BAND,
            runningTime = 90,
            performanceDescription = "description",
            performanceAttentionNote = "attention",
            paymentAccount = paymentAccount,
            posterImage = "poster.jpg",
            performanceTeamName = "team",
            performanceVenue = "venue",
            roadAddressName = "road",
            placeDetailAddress = "detail",
            latitude = "37.0",
            longitude = "127.0",
            performanceContact = "010-0000-0000",
            performancePeriodValue = PerformancePeriodJpaValue(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
            ),
            legacyPerformancePeriod = "2026.01.01 - 2026.01.02",
            ticketPrice = 15_000,
            totalScheduleCount = 1,
            userId = 1L,
        )

    private fun schedule(performanceId: Long, number: ScheduleNumber): ScheduleJpaEntity =
        ScheduleJpaEntity.rehydrate(
            id = null,
            performanceDate = LocalDateTime.of(2026, 1, 1, 18, 0),
            bookingCloseAt = LocalDateTime.of(2026, 1, 1, 17, 0),
            totalTicketCount = 100,
            soldTicketCount = 2,
            scheduleNumber = number,
            performanceId = performanceId,
        )

    private fun booking(scheduleId: Long, status: BookingStatus, totalPaymentAmount: Int?): BookingJpaEntity =
        BookingJpaEntity.rehydrate(
            id = null,
            purchaseTicketCount = 2,
            bookerName = "booker",
            bookerPhoneNumber = "010-0000-0000",
            bookingStatus = status,
            createdAt = LocalDateTime.of(2025, 12, 1, 12, 0),
            cancellationDate = null,
            birthDate = null,
            password = null,
            refundAccount = null,
            scheduleId = scheduleId,
            userId = TEST_USER_ID,
            totalPaymentAmount = totalPaymentAmount,
        )

    private companion object {
        const val TEST_USER_ID = 987_654_321L
    }
}
