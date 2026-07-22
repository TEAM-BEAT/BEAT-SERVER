package com.beat.apis.booking.application.command

import com.beat.apis.booking.application.calculatePaymentAmountForCommand
import com.beat.apis.booking.application.event.BookingCreatedEvent
import com.beat.apis.booking.application.result.BookingCreationResult
import com.beat.apis.booking.application.validateBookerContact
import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.member.exception.MemberApplicationErrorCode
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode
import com.beat.contracts.performance.PerformanceSummaryReadPort
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.repository.ScheduleRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class MemberBookingCommandService(
    private val scheduleRepository: ScheduleRepository,
    private val bookingRepository: BookingRepository,
    private val memberRepository: MemberRepository,
    private val performanceSummaryReadPort: PerformanceSummaryReadPort,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    @Transactional(timeout = 200)
    fun createMemberBooking(memberId: Long, command: MemberBookingCommand): BookingCreationResult {
        val scheduleId = command.scheduleId
        val booker = validateBookerContact(command.bookerName, command.bookerPhoneNumber)
        val member = memberRepository.findById(memberId)
            .orElseThrow { ApiApplicationException(MemberApplicationErrorCode.MEMBER_NOT_FOUND) }
        var schedule = scheduleRepository.lockById(scheduleId)
            .orElseThrow { ApiApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND) }
        if (!scheduleRepository.isBeforeBookingCloseAt(schedule.getId())) {
            throw ApiApplicationException(ScheduleApplicationErrorCode.BOOKING_CLOSED)
        }
        schedule = schedule.reserveTickets(command.purchaseTicketCount)
        val performance = performanceSummaryReadPort.findById(schedule.getPerformanceId())
            .orElseThrow { ApiApplicationException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND) }
        val totalAmount = calculatePaymentAmountForCommand(
            TicketPrice.of(performance.ticketPrice),
            command.purchaseTicketCount,
        )
        var booking = Booking.create(
            command.purchaseTicketCount, booker.name,
            booker.phoneNumber, null, null, schedule.getId(), member.getUserId(),
            LocalDateTime.now(clock), totalAmount,
        )
        booking = bookingRepository.save(booking)
        schedule = scheduleRepository.save(schedule)
        log.info { "Member booking created: bookingId=${booking.getId()}" }
        eventPublisher.publishEvent(
            BookingCreatedEvent(
                bookingDateTime = booking.getCreatedAt(),
                performanceTitle = performance.performanceTitle,
                purchaseTicketCount = booking.getPurchaseTicketCount(),
                bookerName = booking.getBookerName(),
                scheduleDisplayName = schedule.getScheduleNumber().displayName,
                currentSoldTicketCount = schedule.getAllocatedTicketCount(),
                totalTicketCount = schedule.getTotalTicketCount(),
            ),
        )
        return BookingCreationResult(
            booking.getId(), schedule.getId(), member.getId(), booking.getPurchaseTicketCount(),
            schedule.getScheduleNumber().name, booking.getBookerName(),
            booking.getBookerPhoneNumber(), booking.getBookingStatus().name,
            performance.bankName, performance.accountNumber, totalAmount,
            booking.getCreatedAt(),
        )
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
