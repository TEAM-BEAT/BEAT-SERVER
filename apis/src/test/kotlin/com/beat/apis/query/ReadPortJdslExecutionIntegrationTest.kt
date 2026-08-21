package com.beat.apis.query

import com.beat.apis.support.AbstractIntegrationTest
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketReader
import com.beat.application.frontoffice.ticket.maker.query.MakerTicketBookingStatus
import com.beat.contracts.schedule.ScheduleReadPort
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.booking.vo.RefundAccount
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.sharedkernel.vo.BankName
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Kotlin JDSL read/query adapters render and execute against the real MySQL testcontainer.
 */
@Transactional
class ReadPortJdslExecutionIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var scheduleReadPort: ScheduleReadPort

    @Autowired
    private lateinit var makerTicketReader: MakerTicketReader

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Test
    fun `schedule reader renders and executes coalesce min group by on MySQL`() {
        val result = scheduleReadPort.findMinPerformanceDateByPerformanceIds(listOf(999_999L))

        assertNotNull(result)
        assertTrue(result.isEmpty(), "존재하지 않는 performanceId 조회 시 빈 결과여야 한다")
    }

    @Test
    fun `maker ticket reader orders statuses and maps bank display name on MySQL`() {
        val userId = requireNotNull(userRepository.save(Users.create()).getId())
        val performanceId = requireNotNull(
            performanceRepository.save(
                Performance.create(
                    performanceTitle = "JDSL ticket ordering performance",
                    genre = Genre.BAND,
                    runningTime = RunningTime.of(120),
                    performanceDescription = "description",
                    performanceAttentionNote = "attention",
                    paymentAccount = null,
                    posterImage = "poster.jpg",
                    performanceTeamName = "team",
                    performanceVenue = "venue",
                    roadAddressName = "road",
                    placeDetailAddress = "detail",
                    latitude = "37.0",
                    longitude = "127.0",
                    performanceContact = "010-0000-0000",
                    performancePeriod = PerformancePeriod.of(
                        LocalDate.of(2026, 8, 25),
                        LocalDate.of(2026, 8, 25),
                    ),
                    ticketPrice = TicketPrice.of(10_000),
                    totalScheduleCount = 1,
                    userId = userId,
                ),
            ).getId(),
        )
        val scheduleId = requireNotNull(
            scheduleRepository.save(
                Schedule.create(
                    performanceDate = LocalDateTime.of(2026, 8, 25, 19, 0),
                    bookingCloseAt = LocalDateTime.of(2026, 8, 25, 20, 0),
                    totalTicketCount = 10,
                    scheduleNumber = ScheduleNumber.FIRST,
                    performanceId = performanceId,
                ),
            ).getId(),
        )
        val refundCreatedAt = LocalDateTime.of(2026, 8, 21, 10, 0)
        val checkingOlderCreatedAt = LocalDateTime.of(2026, 8, 21, 11, 0)
        val checkingNewerCreatedAt = LocalDateTime.of(2026, 8, 21, 12, 0)

        bookingRepository.save(
            Booking.rehydrate(
                id = null,
                purchaseTicketCount = 1,
                bookerName = "refund-booker",
                bookerPhoneNumber = "010-0000-0001",
                bookingStatus = BookingStatus.REFUND_REQUESTED,
                createdAt = refundCreatedAt,
                cancellationDate = null,
                birthDate = null,
                password = null,
                refundAccount = RefundAccount.of(BankName.KAKAOBANK, "123-456", "refund-holder"),
                scheduleId = scheduleId,
                userId = userId,
                totalPaymentAmount = 10_000,
            ),
        )
        bookingRepository.save(
            Booking.rehydrate(
                id = null,
                purchaseTicketCount = 1,
                bookerName = "checking-older-booker",
                bookerPhoneNumber = "010-0000-0002",
                bookingStatus = BookingStatus.CHECKING_PAYMENT,
                createdAt = checkingOlderCreatedAt,
                cancellationDate = null,
                birthDate = null,
                password = null,
                refundAccount = null,
                scheduleId = scheduleId,
                userId = userId,
                totalPaymentAmount = 10_000,
            ),
        )
        bookingRepository.save(
            Booking.rehydrate(
                id = null,
                purchaseTicketCount = 1,
                bookerName = "checking-newer-booker",
                bookerPhoneNumber = "010-0000-0003",
                bookingStatus = BookingStatus.CHECKING_PAYMENT,
                createdAt = checkingNewerCreatedAt,
                cancellationDate = null,
                birthDate = null,
                password = null,
                refundAccount = null,
                scheduleId = scheduleId,
                userId = userId,
                totalPaymentAmount = 10_000,
            ),
        )

        val result = makerTicketReader.findTickets(performanceId, emptyList(), emptyList())

        assertEquals(3, result.size)
        assertEquals(
            listOf(
                MakerTicketBookingStatus.REFUND_REQUESTED,
                MakerTicketBookingStatus.CHECKING_PAYMENT,
                MakerTicketBookingStatus.CHECKING_PAYMENT,
            ),
            result.map { it.bookingStatus },
        )
        assertEquals(
            listOf(refundCreatedAt, checkingNewerCreatedAt, checkingOlderCreatedAt),
            result.map { it.createdAt },
        )
        assertEquals(BankName.KAKAOBANK.displayName, result.first().bankName)
        assertNotEquals(BankName.KAKAOBANK.name, result.first().bankName)
    }

    @Test
    fun `maker ticket reader renders and executes cross join dynamic query on MySQL`() {
        val result = makerTicketReader.findTickets(999_999L, emptyList(), emptyList())

        assertNotNull(result)
        assertTrue(result.isEmpty(), "존재하지 않는 performanceId 조회 시 빈 결과여야 한다")
    }
}
