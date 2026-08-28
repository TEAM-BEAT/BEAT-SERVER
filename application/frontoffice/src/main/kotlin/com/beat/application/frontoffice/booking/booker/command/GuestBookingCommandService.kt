package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.command.credential.GuestBookingCredentialAuthenticator
import com.beat.application.frontoffice.booking.booker.command.event.BookingCreatedEvent
import com.beat.application.frontoffice.booking.booker.command.result.BookingCreationResult
import com.beat.application.frontoffice.booking.booker.command.result.GuestBookingCreationOutcome
import com.beat.application.frontoffice.booking.booker.exception.BookingApplicationErrorCode
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GuestBookingCommandService
internal constructor(
    private val scheduleRepository: ScheduleRepository,
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val performanceRepository: PerformanceRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val credentialAuthenticator: GuestBookingCredentialAuthenticator,
    private val guestBookingSessionManager: GuestBookingSessionManager,
    private val clock: Clock,
) {
    @Transactional
    fun createGuestBooking(command: GuestBookingCommand): GuestBookingCreationOutcome {
        return translateDomainFailure {
            val identity =
                validateGuestBookingIdentity(
                    command.bookerName,
                    command.bookerPhoneNumber,
                    command.birthDate,
                    command.password,
                )
            val scheduleId = command.scheduleId
            val userId =
                credentialAuthenticator.findUserId(
                    identity.bookerName,
                    identity.phoneNumber,
                    identity.birthDate,
                    identity.password,
                ) ?: userRepository.save(Users.create()).id
            val performanceId =
                scheduleRepository.findPerformanceIdById(scheduleId)
                    ?: throw FrontofficeApplicationException(
                        BookingApplicationErrorCode.SCHEDULE_NOT_FOUND
                    )
            val performance =
                performanceRepository.lockById(performanceId)
                    ?: throw FrontofficeApplicationException(
                        BookingApplicationErrorCode.PERFORMANCE_NOT_FOUND
                    )
            var schedule =
                scheduleRepository.lockById(scheduleId)
                    ?: throw FrontofficeApplicationException(
                        BookingApplicationErrorCode.SCHEDULE_NOT_FOUND
                    )
            if (!schedule.belongsTo(performanceId)) {
                throw FrontofficeApplicationException(
                    BookingApplicationErrorCode.SCHEDULE_NOT_FOUND
                )
            }
            if (!scheduleRepository.isBeforeBookingCloseAt(checkNotNull(schedule.id))) {
                throw FrontofficeApplicationException(BookingApplicationErrorCode.BOOKING_CLOSED)
            }
            schedule = schedule.reserveTickets(command.purchaseTicketCount)
            val totalAmount =
                calculatePaymentAmountForCommand(
                    performance.ticketPriceValue,
                    command.purchaseTicketCount,
                )
            schedule = scheduleRepository.save(schedule)
            var booking =
                Booking.create(
                    command.purchaseTicketCount,
                    identity.bookerName,
                    identity.phoneNumber,
                    identity.birthDate,
                    credentialAuthenticator.encode(identity.password),
                    requireNotNull(schedule.id),
                    requireNotNull(userId),
                    LocalDateTime.now(clock),
                    totalAmount,
                )
            booking = bookingRepository.save(booking)
            log.info { "Guest booking created: bookingId=${booking.id}" }
            eventPublisher.publishEvent(
                BookingCreatedEvent(
                    bookingDateTime = booking.createdAt,
                    performanceTitle = performance.performanceTitle,
                    purchaseTicketCount = booking.purchaseTicketCount,
                    bookerName = booking.bookerName,
                    scheduleDisplayName = schedule.scheduleNumber.displayName,
                    currentSoldTicketCount = schedule.allocatedTicketCount,
                    totalTicketCount = schedule.totalTicketCount,
                )
            )
            val result =
                BookingCreationResult(
                    requireNotNull(booking.id),
                    requireNotNull(schedule.id),
                    booking.userId,
                    booking.purchaseTicketCount,
                    schedule.scheduleNumber.name,
                    booking.bookerName,
                    booking.bookerPhoneNumber,
                    booking.bookingStatus.name,
                    performance.paymentAccount?.bankName?.name,
                    performance.paymentAccount?.accountNumber,
                    totalAmount,
                    booking.createdAt,
                )
            GuestBookingCreationOutcome(
                booking = result,
                sessionToken = guestBookingSessionManager.issueOrNull(booking.userId),
            )
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
