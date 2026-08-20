package com.beat.application.frontoffice.ticket.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.ticket.exception.TicketApplicationErrorCode
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.exception.BookingErrorCode
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.exception.DomainException
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.repository.ScheduleRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.Clock

@Service
class TicketCommandService(
    private val bookingRepository: BookingRepository,
    private val performanceRepository: PerformanceRepository,
    private val memberRepository: MemberRepository,
    private val scheduleRepository: ScheduleRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    @Transactional
    fun updateTickets(memberId: Long, command: TicketUpdateCommand) {
        val details = command.bookingList
        if (details.map(TicketStatusUpdate::bookingId).distinct().size != details.size) {
            throw FrontofficeApplicationException(TicketApplicationErrorCode.DUPLICATE_BOOKING_ID)
        }

        val performance = findOwnedPerformance(memberId, command.performanceId)
        val detailsByBookingId = details.associateBy(TicketStatusUpdate::bookingId)
        val (bookings, _) = lockSchedulesThenBookings(detailsByBookingId.keys, performance)

        bookings.forEach { booking ->
            val detail = requireNotNull(detailsByBookingId[booking.getId()])
            val requestedStatus = BookingStatus.valueOf(detail.bookingStatus.name)
            val updated = translateBookingDomainFailure { booking.transitionTo(requestedStatus) }
            if (updated === booking) return@forEach
            bookingRepository.save(updated)
            eventPublisher.publishEvent(
                TicketPaymentConfirmedEvent(
                    bookingId = checkNotNull(booking.getId()),
                    bookerName = booking.getBookerName(),
                    bookerPhoneNumber = booking.getBookerPhoneNumber(),
                    performanceTitle = performance.performanceTitle,
                ),
            )
        }
    }

    @Transactional
    fun refundTicketsByBookingIds(memberId: Long, command: TicketBookingIdsCommand) {
        val performance = findOwnedPerformance(memberId, command.performanceId)
        val (bookings, schedules) = lockSchedulesThenBookings(
            command.bookingIds,
            performance,
        )

        bookings.forEach { original ->
            val shouldReleaseTickets = original.hasActiveTicketAllocation()
            val booking = bookingRepository.save(
                translateBookingDomainFailure { original.completeRefund(LocalDateTime.now(clock)) },
            )
            if (shouldReleaseTickets) {
                val schedule = requireNotNull(schedules[booking.getScheduleId()])
                schedules[booking.getScheduleId()] = scheduleRepository.save(
                    schedule.releaseTickets(booking.getPurchaseTicketCount()),
                )
            }
        }
    }

    @Transactional
    fun deleteTicketsByBookingIds(memberId: Long, command: TicketBookingIdsCommand) {
        val performance = findOwnedPerformance(memberId, command.performanceId)
        val (bookings, schedules) = lockSchedulesThenBookings(
            command.bookingIds,
            performance,
        )

        bookings.forEach { original ->
            val deleted = translateBookingDomainFailure { original.deleteByMaker(LocalDateTime.now(clock)) }
            val shouldReleaseTickets =
                original.hasActiveTicketAllocation() && !deleted.hasActiveTicketAllocation()
            val booking = bookingRepository.save(deleted)
            if (shouldReleaseTickets) {
                val schedule = requireNotNull(schedules[booking.getScheduleId()])
                schedules[booking.getScheduleId()] = scheduleRepository.save(
                    schedule.releaseTickets(booking.getPurchaseTicketCount()),
                )
            }
        }
    }

    private fun lockSchedulesThenBookings(
        bookingIds: Collection<Long>,
        performance: Performance,
    ): Pair<List<Booking>, MutableMap<Long, Schedule>> {
        val distinctBookingIds = bookingIds.distinct().sorted()
        val scheduleIds = bookingRepository.findScheduleIdsByIds(distinctBookingIds)
        if (scheduleIds.size != distinctBookingIds.size) {
            throw FrontofficeApplicationException(TicketApplicationErrorCode.NO_BOOKING_FOUND)
        }
        val schedules = lockAndValidateSchedules(scheduleIds, performance)
        val bookings = distinctBookingIds.map { bookingId ->
            bookingRepository.lockById(bookingId)
                .orElseThrow { FrontofficeApplicationException(TicketApplicationErrorCode.NO_BOOKING_FOUND) }
        }
        if (bookings.any { it.getScheduleId() !in schedules }) {
            throw FrontofficeApplicationException(TicketApplicationErrorCode.NO_SCHEDULE_FOUND)
        }
        return bookings to schedules
    }

    private fun lockAndValidateSchedules(
        scheduleIds: Collection<Long>,
        performance: Performance,
    ): MutableMap<Long, Schedule> {
        val schedules = mutableMapOf<Long, Schedule>()
        scheduleIds.distinct().sorted().forEach { scheduleId ->
            val schedule = scheduleRepository.lockById(scheduleId)
                .orElseThrow { FrontofficeApplicationException(TicketApplicationErrorCode.NO_SCHEDULE_FOUND) }
            if (!schedule.belongsTo(requireNotNull(performance.getId()))) {
                throw FrontofficeApplicationException(TicketApplicationErrorCode.SCHEDULE_NOT_BELONG_TO_PERFORMANCE)
            }
            schedules[scheduleId] = schedule
        }
        return schedules
    }

    private fun findOwnedPerformance(memberId: Long, performanceId: Long): Performance {
        val member = findMember(memberId)
        val performance = performanceRepository.findById(performanceId)
            .orElseThrow { FrontofficeApplicationException(TicketApplicationErrorCode.PERFORMANCE_NOT_FOUND) }
        if (performance.getUserId() != member.getUserId()) {
            throw FrontofficeApplicationException(TicketApplicationErrorCode.NOT_PERFORMANCE_OWNER)
        }
        return performance
    }

    private fun findMember(memberId: Long): Member =
        memberRepository.findById(memberId)
            .orElseThrow { FrontofficeApplicationException(TicketApplicationErrorCode.MEMBER_NOT_FOUND) }

    private fun <T> translateBookingDomainFailure(block: () -> T): T =
        try {
            block()
        } catch (exception: DomainException) {
            throw when (exception.errorCode) {
                BookingErrorCode.CONFIRMED_STATUS_CHANGE_NOT_ALLOWED ->
                    FrontofficeApplicationException(
                        TicketApplicationErrorCode.PAYMENT_COMPLETED_TICKET_UPDATE_NOT_ALLOWED,
                        exception,
                    )

                BookingErrorCode.STATUS_TRANSITION_NOT_ALLOWED ->
                    FrontofficeApplicationException(
                        TicketApplicationErrorCode.INVALID_BOOKING_STATUS_TRANSITION,
                        exception,
                    )

                BookingErrorCode.REFUND_COMPLETION_NOT_ALLOWED ->
                    FrontofficeApplicationException(TicketApplicationErrorCode.REFUND_COMPLETION_NOT_ALLOWED, exception)

                BookingErrorCode.DELETION_NOT_ALLOWED ->
                    FrontofficeApplicationException(TicketApplicationErrorCode.DELETION_NOT_ALLOWED, exception)

                else -> exception
            }
        }
}
