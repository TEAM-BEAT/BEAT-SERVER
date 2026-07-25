package com.beat.apis.booking.application.command

import com.beat.apis.booking.application.calculatePaymentAmountForCommand
import com.beat.apis.booking.application.credential.GuestBookingCredentialAuthenticator
import com.beat.apis.booking.application.event.BookingCreatedEvent
import com.beat.apis.booking.application.result.BookingCreationResult
import com.beat.apis.booking.application.validateGuestBookingIdentity
import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode
import com.beat.contracts.performance.PerformanceSummaryReadPort
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class GuestBookingCommandService internal constructor(
    private val scheduleRepository: ScheduleRepository,
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val performanceSummaryReadPort: PerformanceSummaryReadPort,
    private val eventPublisher: ApplicationEventPublisher,
    private val credentialAuthenticator: GuestBookingCredentialAuthenticator,
    private val clock: Clock,
) {
    @Transactional
    fun createGuestBooking(command: GuestBookingCommand): BookingCreationResult {
        val identity = validateGuestBookingIdentity(
            command.bookerName,
            command.bookerPhoneNumber,
            command.birthDate,
            command.password,
        )
        val scheduleId = command.scheduleId
        val userId = credentialAuthenticator.findUserId(
            identity.bookerName,
            identity.phoneNumber,
            identity.birthDate,
            identity.password,
        ) ?: userRepository.save(Users.create()).getId()
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
        schedule = scheduleRepository.save(schedule)
        var booking = Booking.create(
            command.purchaseTicketCount, identity.bookerName, identity.phoneNumber, identity.birthDate,
            credentialAuthenticator.encode(identity.password),
            schedule.getId(), userId, LocalDateTime.now(clock), totalAmount,
        )
        booking = bookingRepository.save(booking)
        log.info { "Guest booking created: bookingId=${booking.getId()}" }
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
            performance.bankName, performance.accountNumber, totalAmount,
            booking.getCreatedAt(),
        )
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
