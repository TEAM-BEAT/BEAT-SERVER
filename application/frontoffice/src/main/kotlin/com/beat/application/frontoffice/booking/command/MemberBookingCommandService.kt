package com.beat.application.frontoffice.booking.command

import com.beat.application.frontoffice.booking.calculatePaymentAmountForCommand
import com.beat.application.frontoffice.booking.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.event.BookingCreatedEvent
import com.beat.application.frontoffice.booking.result.BookingCreationResult
import com.beat.application.frontoffice.booking.validateBookerContact
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.repository.PerformanceRepository
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
    private val performanceRepository: PerformanceRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    @Transactional(timeout = 200)
    fun createMemberBooking(memberId: Long, command: MemberBookingCommand): BookingCreationResult {
        val scheduleId = command.scheduleId
        val booker = validateBookerContact(command.bookerName, command.bookerPhoneNumber)
        val member = memberRepository.findById(memberId)
            .orElseThrow { FrontofficeApplicationException(BookingApplicationErrorCode.MEMBER_NOT_FOUND) }
        val performanceId = scheduleRepository.findPerformanceIdById(scheduleId)
            ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND)
        val performance = performanceRepository.lockById(performanceId)
            .orElseThrow { FrontofficeApplicationException(BookingApplicationErrorCode.PERFORMANCE_NOT_FOUND) }
        var schedule = scheduleRepository.lockById(scheduleId)
            .orElseThrow { FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND) }
        if (!schedule.belongsTo(performanceId)) {
            throw FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND)
        }
        if (!scheduleRepository.isBeforeBookingCloseAt(schedule.getId())) {
            throw FrontofficeApplicationException(BookingApplicationErrorCode.BOOKING_CLOSED)
        }
        schedule = schedule.reserveTickets(command.purchaseTicketCount)
        val totalAmount = calculatePaymentAmountForCommand(
            performance.getTicketPriceValue(),
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
            booking.getId(), schedule.getId(), booking.getUserId(), booking.getPurchaseTicketCount(),
            schedule.getScheduleNumber().name, booking.getBookerName(),
            booking.getBookerPhoneNumber(), booking.getBookingStatus().name,
            performance.getPaymentAccount()?.bankName?.name, performance.getPaymentAccount()?.accountNumber, totalAmount,
            booking.getCreatedAt(),
        )
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
