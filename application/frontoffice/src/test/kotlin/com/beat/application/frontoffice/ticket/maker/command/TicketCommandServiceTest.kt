package com.beat.application.frontoffice.ticket.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.performance.model.Genre
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.performance.vo.RunningTime
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class TicketCommandServiceTest {

    @Mock
    private lateinit var bookingRepository: BookingRepository

    @Mock
    private lateinit var performanceRepository: PerformanceRepository

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var scheduleRepository: ScheduleRepository

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Test
    fun `rejects duplicate booking ids before loading authoritative state`() {
        val detail = statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED)

        val exception = assertThrows<FrontofficeApplicationException> {
            service().updateTickets(1L, TicketUpdateCommand(100L, listOf(detail, detail)))
        }

        assertEquals("중복된 예매 식별자는 한 번만 요청할 수 있습니다.", exception.errorCode.message)
        assertEquals(FrontofficeApplicationErrorType.INVALID_INPUT, exception.errorCode.type)
        Mockito.verifyNoInteractions(
            bookingRepository,
            performanceRepository,
            memberRepository,
            scheduleRepository,
            eventPublisher,
        )
    }

    @Test
    fun `authorizes the maker through authoritative PerformanceRepository`() {
        Mockito.`when`(memberRepository.findById(1L)).thenReturn(Optional.of(member(userId = 10L)))
        Mockito.`when`(performanceRepository.findById(100L)).thenReturn(Optional.of(performance(userId = 11L)))

        assertThrows<FrontofficeApplicationException> {
            service().updateTickets(
                1L,
                TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED))),
            )
        }

        Mockito.verify(performanceRepository).findById(100L)
        Mockito.verifyNoInteractions(bookingRepository, scheduleRepository, eventPublisher)
    }

    @Test
    fun `rejects a booking whose schedule belongs to another performance`() {
        stubOwnerOnly()
        Mockito.`when`(bookingRepository.findScheduleIdsByIds(listOf(300L))).thenReturn(listOf(200L))
        Mockito.`when`(scheduleRepository.lockById(200L))
            .thenReturn(Optional.of(schedule(200L, performanceId = 999L)))

        val exception = assertThrows<FrontofficeApplicationException> {
            service().updateTickets(
                1L,
                TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED))),
            )
        }

        assertEquals(FrontofficeApplicationErrorType.FORBIDDEN, exception.errorCode.type)
        assertEquals("해당 스케줄은 해당 공연에 속해 있지 않습니다.", exception.errorCode.message)
        Mockito.verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `looks up scalar schedule ids then locks schedules ascending and bookings ascending`() {
        val firstBooking = booking(id = 300L, scheduleId = 202L)
        val secondBooking = booking(id = 301L, scheduleId = 201L)
        stubOwnerOnly()
        Mockito.`when`(bookingRepository.findScheduleIdsByIds(listOf(300L, 301L)))
            .thenReturn(listOf(202L, 201L))
        Mockito.`when`(scheduleRepository.lockById(201L)).thenReturn(Optional.of(schedule(201L)))
        Mockito.`when`(scheduleRepository.lockById(202L)).thenReturn(Optional.of(schedule(202L)))
        Mockito.`when`(bookingRepository.lockById(300L)).thenReturn(Optional.of(firstBooking))
        Mockito.`when`(bookingRepository.lockById(301L)).thenReturn(Optional.of(secondBooking))

        service().updateTickets(
            1L,
            TicketUpdateCommand(
                100L,
                listOf(
                    statusUpdate(301L, TicketBookingStatus.BOOKING_CONFIRMED),
                    statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED),
                ),
            ),
        )

        val order = Mockito.inOrder(bookingRepository, scheduleRepository)
        order.verify(bookingRepository).findScheduleIdsByIds(listOf(300L, 301L))
        order.verify(scheduleRepository).lockById(201L)
        order.verify(scheduleRepository).lockById(202L)
        order.verify(bookingRepository).lockById(300L)
        order.verify(bookingRepository).lockById(301L)
    }

    @Test
    fun `rejects a missing scalar schedule row before locking`() {
        stubOwnerOnly()
        Mockito.`when`(bookingRepository.findScheduleIdsByIds(listOf(300L))).thenReturn(emptyList())

        assertThrows<FrontofficeApplicationException> {
            service().updateTickets(
                1L,
                TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED))),
            )
        }

        Mockito.verifyNoInteractions(scheduleRepository, eventPublisher)
        Mockito.verify(bookingRepository, Mockito.never()).lockById(Mockito.anyLong())
    }

    @Test
    fun `revalidates the schedule id on each locked booking`() {
        val bookingWithChangedSchedule = booking(id = 300L, scheduleId = 999L)
        stubOwnerOnly()
        Mockito.`when`(bookingRepository.findScheduleIdsByIds(listOf(300L))).thenReturn(listOf(200L))
        Mockito.`when`(scheduleRepository.lockById(200L)).thenReturn(Optional.of(schedule(200L)))
        Mockito.`when`(bookingRepository.lockById(300L)).thenReturn(Optional.of(bookingWithChangedSchedule))

        assertThrows<FrontofficeApplicationException> {
            service().updateTickets(
                1L,
                TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED))),
            )
        }

        assertNoInvocation(bookingRepository, "save")
        Mockito.verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `publishes confirmation event with committed booking payload`() {
        val booking = booking(id = 300L, scheduleId = 200L, status = BookingStatus.CHECKING_PAYMENT)
        stubOwner(booking)

        service().updateTickets(
            1L,
            TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED))),
        )

        assertEquals(BookingStatus.BOOKING_CONFIRMED, savedBooking().getBookingStatus())
        val event = lastInvocationArgument(eventPublisher, "publishEvent") as TicketPaymentConfirmedEvent
        assertEquals(300L, event.bookingId)
        assertEquals("booker", event.bookerName)
        assertEquals("010-0000-0000", event.bookerPhoneNumber)
        assertEquals("title", event.performanceTitle)
    }

    @Test
    fun `translates unsupported status transition while preserving v1 message and type`() {
        val booking = booking(status = BookingStatus.CHECKING_PAYMENT)
        stubOwner(booking)

        val exception = assertThrows<FrontofficeApplicationException> {
            service().updateTickets(
                1L,
                TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CANCELLED))),
            )
        }

        assertTicketFailure(
            exception,
            FrontofficeApplicationErrorType.INVALID_INPUT,
            "지원하지 않는 예매 상태 변경입니다.",
        )
        assertNoInvocation(bookingRepository, "save")
        Mockito.verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `translates confirmed status change while preserving v1 message and type`() {
        val booking = booking(status = BookingStatus.BOOKING_CONFIRMED)
        stubOwner(booking)

        val exception = assertThrows<FrontofficeApplicationException> {
            service().updateTickets(
                1L,
                TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CANCELLED))),
            )
        }

        assertTicketFailure(
            exception,
            FrontofficeApplicationErrorType.INVALID_INPUT,
            "이미 결제가 완료된 티켓의 상태는 변경할 수 없습니다.",
        )
    }

    @Test
    fun `refund completion releases allocated tickets`() {
        val booking = booking(status = BookingStatus.REFUND_REQUESTED, totalPaymentAmount = 10_000)
        stubOwner(booking)
        stubSaveOperations(booking)

        service().refundTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))

        assertEquals(BookingStatus.BOOKING_CANCELLED, savedBooking().getBookingStatus())
        assertEquals(0, savedSchedule().getAllocatedTicketCount())
    }

    @Test
    fun `refund completion translates invalid status while preserving v1 message and type`() {
        val booking = booking(status = BookingStatus.BOOKING_CONFIRMED, totalPaymentAmount = 10_000)
        stubOwner(booking)

        val exception = assertThrows<FrontofficeApplicationException> {
            service().refundTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))
        }

        assertTicketFailure(
            exception,
            FrontofficeApplicationErrorType.INVALID_INPUT,
            "환불 요청 상태인 예매만 환불 완료 처리할 수 있습니다.",
        )
        assertNoInvocation(bookingRepository, "save")
        assertNoInvocation(scheduleRepository, "save")
    }

    @Test
    fun `deletion releases allocation for unpaid booking`() {
        val booking = booking(status = BookingStatus.CHECKING_PAYMENT, totalPaymentAmount = 10_000)
        stubOwner(booking)
        stubSaveOperations(booking)

        service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))

        assertEquals(BookingStatus.BOOKING_DELETED, savedBooking().getBookingStatus())
        assertEquals(0, savedSchedule().getAllocatedTicketCount())
    }

    @Test
    fun `deletion releases allocation for confirmed free booking`() {
        val booking = booking(status = BookingStatus.BOOKING_CONFIRMED, totalPaymentAmount = 0)
        stubOwner(booking)
        stubSaveOperations(booking)

        service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))

        assertTrue(hasInvocation(scheduleRepository, "save"))
    }

    @Test
    fun `deletion does not release already inactive allocation`() {
        val booking = booking(status = BookingStatus.BOOKING_CANCELLED)
        stubOwner(booking)
        stubBookingSave(booking)

        service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))

        assertNoInvocation(scheduleRepository, "save")
    }

    @Test
    fun `deletion is idempotent for deleted booking`() {
        val booking = booking(status = BookingStatus.BOOKING_DELETED)
        stubOwner(booking)
        stubBookingSave(booking)

        service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))

        assertEquals(booking, savedBooking())
        assertNoInvocation(scheduleRepository, "save")
    }

    @Test
    fun `deletion translates an invalid status while preserving v1 message and type`() {
        val booking = booking(status = BookingStatus.REFUND_REQUESTED)
        stubOwner(booking)

        val exception = assertThrows<FrontofficeApplicationException> {
            service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))
        }

        assertTicketFailure(
            exception,
            FrontofficeApplicationErrorType.INVALID_INPUT,
            "미입금, 무료 확정 또는 취소 완료 예매만 삭제할 수 있습니다.",
        )
        assertNoInvocation(bookingRepository, "save")
        assertNoInvocation(scheduleRepository, "save")
    }

    @Test
    fun `deletion rejects confirmed paid booking without releasing allocation`() {
        val booking = booking(status = BookingStatus.BOOKING_CONFIRMED, totalPaymentAmount = 10_000)
        stubOwner(booking)

        val exception = assertThrows<FrontofficeApplicationException> {
            service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))
        }

        assertTicketFailure(
            exception,
            FrontofficeApplicationErrorType.INVALID_INPUT,
            "미입금, 무료 확정 또는 취소 완료 예매만 삭제할 수 있습니다.",
        )
        assertNoInvocation(bookingRepository, "save")
        assertNoInvocation(scheduleRepository, "save")
    }

    private fun service() = TicketCommandService(
        bookingRepository = bookingRepository,
        performanceRepository = performanceRepository,
        memberRepository = memberRepository,
        scheduleRepository = scheduleRepository,
        eventPublisher = eventPublisher,
        clock = fixedClock,
    )

    private fun stubOwner(booking: Booking? = null) {
        stubOwnerOnly()
        val target = booking ?: booking()
        Mockito.`when`(bookingRepository.findScheduleIdsByIds(listOf(target.getId()!!)))
            .thenReturn(listOf(target.getScheduleId()))
        Mockito.`when`(scheduleRepository.lockById(target.getScheduleId()))
            .thenReturn(Optional.of(schedule(target.getScheduleId())))
        Mockito.`when`(bookingRepository.lockById(target.getId()!!)).thenReturn(Optional.of(target))
    }

    private fun stubOwnerOnly() {
        Mockito.`when`(memberRepository.findById(1L)).thenReturn(Optional.of(member()))
        Mockito.`when`(performanceRepository.findById(100L)).thenReturn(Optional.of(performance()))
    }

    private fun assertTicketFailure(
        exception: FrontofficeApplicationException,
        type: FrontofficeApplicationErrorType,
        message: String,
    ) {
        assertEquals(type, exception.errorCode.type)
        assertEquals(message, exception.errorCode.message)
    }

    private fun stubBookingSave(booking: Booking) {
        Mockito.`when`(bookingRepository.save(booking)).thenReturn(booking)
    }

    private fun stubSaveOperations(booking: Booking) {
        stubBookingSave(booking)
        val schedule = schedule(booking.getScheduleId())
        Mockito.`when`(scheduleRepository.save(schedule)).thenReturn(schedule)
    }

    private fun savedBooking(): Booking =
        Mockito.mockingDetails(bookingRepository).invocations
            .last { it.method.name == "save" }
            .arguments[0] as Booking

    private fun savedSchedule(): Schedule =
        Mockito.mockingDetails(scheduleRepository).invocations
            .last { it.method.name == "save" }
            .arguments[0] as Schedule

    private fun lastInvocationArgument(mock: Any, methodName: String): Any =
        Mockito.mockingDetails(mock).invocations
            .last { it.method.name == methodName }
            .arguments[0]

    private fun hasInvocation(mock: Any, methodName: String): Boolean =
        Mockito.mockingDetails(mock).invocations.any { it.method.name == methodName }

    private fun assertNoInvocation(mock: Any, methodName: String) {
        assertFalse(hasInvocation(mock, methodName))
    }

    private fun statusUpdate(id: Long, status: TicketBookingStatus) = TicketStatusUpdate(id, status)

    private fun member(userId: Long = 10L) = Member.rehydrate(
        1L,
        "maker",
        null,
        null,
        userId,
        SocialIdentity.of(SocialType.KAKAO, 123L),
    )

    private fun performance(userId: Long = 10L) = Performance.rehydrate(
        100L,
        "title",
        Genre.BAND,
        RunningTime.of(120),
        "description",
        "attention",
        null,
        "poster",
        "team",
        "venue",
        "road",
        "detail",
        "37.0",
        "127.0",
        "contact",
        PerformancePeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)),
        TicketPrice.of(10_000),
        1,
        userId,
    )

    private fun booking(
        id: Long = 300L,
        scheduleId: Long = 200L,
        status: BookingStatus = BookingStatus.CHECKING_PAYMENT,
        totalPaymentAmount: Int? = 20,
    ) = Booking.rehydrate(
        id,
        1,
        "booker",
        "010-0000-0000",
        status,
        LocalDateTime.of(2026, 1, 1, 12, 0),
        null,
        null,
        null,
        null,
        scheduleId,
        20L,
        totalPaymentAmount,
    )

    private fun schedule(id: Long, performanceId: Long = 100L) = Schedule.rehydrate(
        id,
        LocalDateTime.of(2026, 1, 1, 19, 0),
        LocalDateTime.of(2026, 1, 1, 21, 0),
        100,
        1,
        ScheduleNumber.FIRST,
        performanceId,
    )

    private companion object {
        val fixedClock: Clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    }
}
