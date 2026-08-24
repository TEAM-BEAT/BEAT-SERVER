package com.beat.application.frontoffice.booking.booker.command

import com.beat.application.frontoffice.booking.booker.calculatePaymentAmountForCommand
import com.beat.application.frontoffice.booking.booker.BookingApplicationErrorCode
import com.beat.application.frontoffice.booking.booker.event.BookingCreatedEvent
import com.beat.application.frontoffice.booking.booker.result.BookingCreationResult
import com.beat.application.frontoffice.booking.booker.validateBookerContact
import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
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
class MemberBookingCommandService internal constructor(
    private val scheduleRepository: ScheduleRepository,
    private val bookingRepository: BookingRepository,
    private val memberRepository: MemberRepository,
    private val performanceRepository: PerformanceRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    @Transactional(timeout = BOOKING_TX_TIMEOUT_SECONDS)
    fun createMemberBooking(memberId: Long, command: MemberBookingCommand): BookingCreationResult {
        return translateDomainFailure {
            val scheduleId = command.scheduleId
            val booker = validateBookerContact(command.bookerName, command.bookerPhoneNumber)
            val member = memberRepository.findById(memberId)
                ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.MEMBER_NOT_FOUND)
            val performanceId = scheduleRepository.findPerformanceIdById(scheduleId)
                ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND)
            val performance = performanceRepository.lockById(performanceId)
                ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.PERFORMANCE_NOT_FOUND)
            var schedule = scheduleRepository.lockById(scheduleId)
                ?: throw FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND)
            if (!schedule.belongsTo(performanceId)) {
                throw FrontofficeApplicationException(BookingApplicationErrorCode.SCHEDULE_NOT_FOUND)
            }
            if (!scheduleRepository.isBeforeBookingCloseAt(checkNotNull(schedule.id))) {
                throw FrontofficeApplicationException(BookingApplicationErrorCode.BOOKING_CLOSED)
            }
            schedule = schedule.reserveTickets(command.purchaseTicketCount)
            val totalAmount = calculatePaymentAmountForCommand(
                performance.ticketPriceValue,
                command.purchaseTicketCount,
            )
            var booking = Booking.create(
                command.purchaseTicketCount, booker.name,
                booker.phoneNumber, null, null, requireNotNull(schedule.id), member.userId,
                LocalDateTime.now(clock), totalAmount,
            )
            booking = bookingRepository.save(booking)
            schedule = scheduleRepository.save(schedule)
            log.info { "Member booking created: bookingId=${booking.id}" }
            eventPublisher.publishEvent(
                BookingCreatedEvent(
                    bookingDateTime = booking.createdAt,
                    performanceTitle = performance.performanceTitle,
                    purchaseTicketCount = booking.purchaseTicketCount,
                    bookerName = booking.bookerName,
                    scheduleDisplayName = schedule.scheduleNumber.displayName,
                    currentSoldTicketCount = schedule.allocatedTicketCount,
                    totalTicketCount = schedule.totalTicketCount,
                ),
            )
            BookingCreationResult(
                booking.id, schedule.id, booking.userId, booking.purchaseTicketCount,
                schedule.scheduleNumber.name, booking.bookerName,
                booking.bookerPhoneNumber, booking.bookingStatus.name,
                performance.paymentAccount?.bankName?.name, performance.paymentAccount?.accountNumber, totalAmount,
                booking.createdAt,
            )
        }
    }

    private companion object {
        val log = KotlinLogging.logger {}

        /**
         * Performance/Schedule row-lock 대기가 포함된 예매 트랜잭션의 최후 방어선.
         * DB lock wait timeout(기본 50s)보다 길게 잡아 DB 단위 실패를 우선시한다.
         */
        const val BOOKING_TX_TIMEOUT_SECONDS = 200
    }
}
