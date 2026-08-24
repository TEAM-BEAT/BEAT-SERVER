package com.beat.application.frontoffice.ticket.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationErrorType
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.fixture.frontofficeMemberFixture
import com.beat.application.frontoffice.fixture.frontofficePerformanceFixture
import com.beat.application.frontoffice.fixture.frontofficeScheduleFixture
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.repository.ScheduleRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.Called
import io.mockk.verify
import io.mockk.verifyOrder
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class TicketCommandApplicationSpec : FunSpec() {
    private val savedBookings = mutableListOf<Booking>()
    private val savedSchedules = mutableListOf<Schedule>()
    private val publishedEvents = mutableListOf<Any>()

    private val bookingRepository: BookingRepository = mockk(relaxed = true) {
        every { save(capture(savedBookings)) } answers { lastArg() }
    }

    private val performanceRepository: PerformanceRepository = mockk(relaxed = true)

    private val memberRepository: MemberRepository = mockk(relaxed = true)

    private val scheduleRepository: ScheduleRepository = mockk(relaxed = true) {
        every { save(capture(savedSchedules)) } answers { lastArg() }
    }

    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true) {
        every { publishEvent(capture(publishedEvents)) } just Runs
    }

    init {
        isolationMode = IsolationMode.InstancePerTest

    test("authoritative 상태 조회 전에 중복 booking id를 거부한다") {
        val detail = statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED)

        val exception = shouldThrow<FrontofficeApplicationException> {
            service().updateTickets(1L, TicketUpdateCommand(100L, listOf(detail, detail)))
        }

        exception.errorCode.message shouldBe "중복된 예매 식별자는 한 번만 요청할 수 있습니다."
        exception.errorCode.type shouldBe FrontofficeApplicationErrorType.INVALID_INPUT
        verify {
            listOf(bookingRepository, performanceRepository, memberRepository, scheduleRepository, eventPublisher) wasNot Called
        }
    }

    test("authoritative PerformanceRepository로 maker 권한을 검증한다") {
        every { memberRepository.findById(1L) } returns member(userId = 10L)
        every { performanceRepository.findById(100L) } returns performance(userId = 11L)

        shouldThrow<FrontofficeApplicationException> {
            service().updateTickets(
                1L,
                TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED))),
            )
        }

        verify { performanceRepository.findById(100L) }
        verify {
            listOf(bookingRepository, scheduleRepository, eventPublisher) wasNot Called
        }
    }

    test("다른 공연에 속한 회차의 booking은 거부한다") {
        stubOwnerOnly()
        every { bookingRepository.findScheduleIdsByIds(listOf(300L)) } returns listOf(200L)
        every { scheduleRepository.lockById(200L) }
            .returns(schedule(200L, performanceId = 999L))

        val exception = shouldThrow<FrontofficeApplicationException> {
            service().updateTickets(
                1L,
                TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED))),
            )
        }

        exception.errorCode.type shouldBe FrontofficeApplicationErrorType.FORBIDDEN
        exception.errorCode.message shouldBe "해당 스케줄은 해당 공연에 속해 있지 않습니다."
        verify { eventPublisher wasNot Called }
    }

    test("scalar schedule id를 조회한 뒤 회차와 booking을 오름차순으로 잠근다") {
        val firstBooking = booking(id = 300L, scheduleId = 202L)
        val secondBooking = booking(id = 301L, scheduleId = 201L)
        stubOwnerOnly()
        every { bookingRepository.findScheduleIdsByIds(listOf(300L, 301L)) }
            .returns(listOf(202L, 201L))
        every { scheduleRepository.lockById(201L) } returns schedule(201L)
        every { scheduleRepository.lockById(202L) } returns schedule(202L)
        every { bookingRepository.lockById(300L) } returns firstBooking
        every { bookingRepository.lockById(301L) } returns secondBooking

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

        verifyOrder {
            bookingRepository.findScheduleIdsByIds(listOf(300L, 301L))
            scheduleRepository.lockById(201L)
            scheduleRepository.lockById(202L)
            bookingRepository.lockById(300L)
            bookingRepository.lockById(301L)
        }
    }

    test("locking 전에 누락된 scalar schedule row는 거부한다") {
        stubOwnerOnly()
        every { bookingRepository.findScheduleIdsByIds(listOf(300L)) } returns emptyList()

        shouldThrow<FrontofficeApplicationException> {
            service().updateTickets(
                1L,
                TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED))),
            )
        }

        verify {
            listOf(scheduleRepository, eventPublisher) wasNot Called
        }
        verify(exactly = 0) { bookingRepository.lockById(any()) }
    }

    test("잠긴 각 booking에서 schedule id를 재검증한다") {
        val bookingWithChangedSchedule = booking(id = 300L, scheduleId = 999L)
        stubOwnerOnly()
        every { bookingRepository.findScheduleIdsByIds(listOf(300L)) } returns listOf(200L)
        every { scheduleRepository.lockById(200L) } returns schedule(200L)
        every { bookingRepository.lockById(300L) } returns bookingWithChangedSchedule

        shouldThrow<FrontofficeApplicationException> {
            service().updateTickets(
                1L,
                TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED))),
            )
        }

        assertNoInvocation(bookingRepository)
        verify { eventPublisher wasNot Called }
    }

    test("저장된 booking payload로 결제 확인 event를 발행한다") {
        val booking = booking(id = 300L, scheduleId = 200L, status = BookingStatus.CHECKING_PAYMENT)
        stubOwner(booking)

        service().updateTickets(
            1L,
            TicketUpdateCommand(100L, listOf(statusUpdate(300L, TicketBookingStatus.BOOKING_CONFIRMED))),
        )

        savedBooking().bookingStatus shouldBe BookingStatus.BOOKING_CONFIRMED
        val event = publishedEvents.last() as TicketPaymentConfirmedEvent
        event.bookingId shouldBe 300L
        event.bookerName shouldBe "booker"
        event.bookerPhoneNumber shouldBe "010-0000-0000"
        event.performanceTitle shouldBe "title"
    }

    test("confirmation event의 toString은 불투명한 booking id만 노출한다") {
        val event = TicketPaymentConfirmedEvent(
            bookingId = 1L,
            bookerName = "booker",
            bookerPhoneNumber = "010-0000-0000",
            performanceTitle = "performance",
        )

        val rendered = event.toString()
        rendered.contains("bookingId=1") shouldBe true
        rendered.contains("booker") shouldBe false
        rendered.contains("010-0000-0000") shouldBe false
        rendered.contains("performance") shouldBe false
    }

    test("v1 message와 타입을 유지하며 지원하지 않는 상태 전환을 변환한다") {
        val booking = booking(status = BookingStatus.CHECKING_PAYMENT)
        stubOwner(booking)

        val exception = shouldThrow<FrontofficeApplicationException> {
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
        assertNoInvocation(bookingRepository)
        verify { eventPublisher wasNot Called }
    }

    test("v1 message와 타입을 유지하며 확정 상태 변경을 변환한다") {
        val booking = booking(status = BookingStatus.BOOKING_CONFIRMED)
        stubOwner(booking)

        val exception = shouldThrow<FrontofficeApplicationException> {
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

    test("환불 완료 시 할당된 티켓을 반납한다") {
        val booking = booking(status = BookingStatus.REFUND_REQUESTED, totalPaymentAmount = 10_000)
        stubOwner(booking)

        service().refundTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))

        savedBooking().bookingStatus shouldBe BookingStatus.BOOKING_CANCELLED
        savedSchedule().allocatedTicketCount shouldBe 0
    }

    test("환불 완료는 v1 message와 타입을 유지하며 유효하지 않은 상태를 변환한다") {
        val booking = booking(status = BookingStatus.BOOKING_CONFIRMED, totalPaymentAmount = 10_000)
        stubOwner(booking)

        val exception = shouldThrow<FrontofficeApplicationException> {
            service().refundTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))
        }

        assertTicketFailure(
            exception,
            FrontofficeApplicationErrorType.INVALID_INPUT,
            "환불 요청 상태인 예매만 환불 완료 처리할 수 있습니다.",
        )
        assertNoInvocation(bookingRepository)
        assertNoInvocation(scheduleRepository)
    }

    test("삭제는 미입금 예매의 티켓 할당을 반납한다") {
        val booking = booking(status = BookingStatus.CHECKING_PAYMENT, totalPaymentAmount = 10_000)
        stubOwner(booking)

        service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))

        savedBooking().bookingStatus shouldBe BookingStatus.BOOKING_DELETED
        savedSchedule().allocatedTicketCount shouldBe 0
    }

    test("삭제는 무료 확정 예매의 티켓 할당을 반납한다") {
        val booking = booking(status = BookingStatus.BOOKING_CONFIRMED, totalPaymentAmount = 0)
        stubOwner(booking)

        service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))

        hasInvocation(scheduleRepository) shouldBe true
    }

    test("삭제는 이미 비활성인 할당을 반납하지 않는다") {
        val booking = booking(status = BookingStatus.BOOKING_CANCELLED)
        stubOwner(booking)

        service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))

        assertNoInvocation(scheduleRepository)
    }

    test("삭제는 삭제된 예매에 대해 멱등이다") {
        val booking = booking(status = BookingStatus.BOOKING_DELETED)
        stubOwner(booking)

        service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))

        savedBooking() shouldBe booking
        assertNoInvocation(scheduleRepository)
    }

    test("삭제는 v1 message와 타입을 유지하며 유효하지 않은 상태를 변환한다") {
        val booking = booking(status = BookingStatus.REFUND_REQUESTED)
        stubOwner(booking)

        val exception = shouldThrow<FrontofficeApplicationException> {
            service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))
        }

        assertTicketFailure(
            exception,
            FrontofficeApplicationErrorType.INVALID_INPUT,
            "미입금, 무료 확정 또는 취소 완료 예매만 삭제할 수 있습니다.",
        )
        assertNoInvocation(bookingRepository)
        assertNoInvocation(scheduleRepository)
    }

    test("삭제는 할당을 반납하지 않고 유료 확정 예매를 거부한다") {
        val booking = booking(status = BookingStatus.BOOKING_CONFIRMED, totalPaymentAmount = 10_000)
        stubOwner(booking)

        val exception = shouldThrow<FrontofficeApplicationException> {
            service().deleteTicketsByBookingIds(1L, TicketBookingIdsCommand(100L, listOf(300L)))
        }

        assertTicketFailure(
            exception,
            FrontofficeApplicationErrorType.INVALID_INPUT,
            "미입금, 무료 확정 또는 취소 완료 예매만 삭제할 수 있습니다.",
        )
        assertNoInvocation(bookingRepository)
        assertNoInvocation(scheduleRepository)
    }

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
        every { bookingRepository.findScheduleIdsByIds(listOf(target.id!!)) }
            .returns(listOf(target.scheduleId))
        every { scheduleRepository.lockById(target.scheduleId) }
            .returns(schedule(target.scheduleId))
        every { bookingRepository.lockById(target.id!!) } returns target
    }

    private fun stubOwnerOnly() {
        every { memberRepository.findById(1L) } returns member()
        every { performanceRepository.findById(100L) } returns performance()
    }

    private fun assertTicketFailure(
        exception: FrontofficeApplicationException,
        type: FrontofficeApplicationErrorType,
        message: String,
    ) {
        exception.errorCode.type shouldBe type
        exception.errorCode.message shouldBe message
    }

    private fun savedBooking(): Booking =
        savedBookings.last()

    private fun savedSchedule(): Schedule =
        savedSchedules.last()

    private fun hasInvocation(repository: ScheduleRepository): Boolean =
        savedSchedules.isNotEmpty()

    private fun assertNoInvocation(repository: Any) {
        when (repository) {
            bookingRepository -> savedBookings.isEmpty() shouldBe true
            scheduleRepository -> savedSchedules.isEmpty() shouldBe true
        }
    }

    private fun statusUpdate(id: Long, status: TicketBookingStatus) = TicketStatusUpdate(id, status)

    private fun member(userId: Long = 10L) = frontofficeMemberFixture(
        id = 1L,
        nickname = "maker",
        email = null,
        userId = userId,
        socialId = 123L,
    )

    private fun performance(userId: Long = 10L) = frontofficePerformanceFixture(
        id = 100L,
        userId = userId,
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

    private fun schedule(id: Long, performanceId: Long = 100L) = frontofficeScheduleFixture(
        id = id,
        performanceId = performanceId,
        performanceDate = LocalDateTime.of(2026, 1, 1, 19, 0),
        totalTicketCount = 100,
        allocatedTicketCount = 1,
    )

    private companion object {
        val fixedClock: Clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    }
}
