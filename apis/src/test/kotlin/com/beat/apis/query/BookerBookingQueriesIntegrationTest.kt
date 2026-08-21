package com.beat.apis.query

import com.beat.apis.support.AbstractIntegrationTest
import com.beat.application.frontoffice.booking.booker.query.BookerBookingReader
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.performance.model.Genre
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.infra.persistence.booking.entity.BookingJpaEntity
import com.beat.infra.persistence.booking.repository.BookingJpaRepository
import com.beat.infra.persistence.performance.entity.PaymentAccountJpaValue
import com.beat.infra.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infra.persistence.performance.entity.PerformancePeriodJpaValue
import com.beat.infra.persistence.performance.repository.PerformanceJpaRepository
import com.beat.infra.persistence.schedule.entity.ScheduleJpaEntity
import com.beat.infra.persistence.schedule.repository.ScheduleJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Transactional
class BookerBookingQueriesIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var reader: BookerBookingReader

    @Autowired
    private lateinit var bookingRepository: BookingJpaRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleJpaRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceJpaRepository

    @Test
    fun `maps multiple bookings with enum stored amount and nullable payment account on MySQL`() {
        val paidPerformance = performanceRepository.saveAndFlush(performance("Paid", PaymentAccountJpaValue(BankName.KAKAOBANK, "123", "BEAT")))
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

        assertThat(resultById).hasSize(2)
        val storedAmountResult = resultById.getValue(storedAmountBooking.id!!)
        assertThat(storedAmountResult.bookingStatus).isEqualTo("BOOKING_CONFIRMED")
        assertThat(storedAmountResult.totalPaymentAmount).isEqualTo(27_000)
        assertThat(storedAmountResult.schedule?.scheduleNumber).isEqualTo("FIRST")
        assertThat(storedAmountResult.performance?.bankName).isEqualTo("KAKAOBANK")
        assertThat(storedAmountResult.performance?.accountNumber).isEqualTo("123")
        assertThat(storedAmountResult.performance?.accountHolder).isEqualTo("BEAT")

        val legacyAmountResult = resultById.getValue(legacyAmountBooking.id!!)
        assertThat(legacyAmountResult.bookingStatus).isEqualTo("REFUND_REQUESTED")
        assertThat(legacyAmountResult.totalPaymentAmount).isNull()
        assertThat(legacyAmountResult.schedule?.scheduleNumber).isEqualTo("SECOND")
        assertThat(legacyAmountResult.performance?.bankName).isNull()
        assertThat(legacyAmountResult.performance?.accountNumber).isNull()
        assertThat(legacyAmountResult.performance?.accountHolder).isNull()
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
            performancePeriodValue = PerformancePeriodJpaValue(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)),
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
