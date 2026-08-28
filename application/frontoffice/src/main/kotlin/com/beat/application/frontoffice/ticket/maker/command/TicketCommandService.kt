package com.beat.application.frontoffice.ticket.maker.command

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.ticket.maker.exception.TicketApplicationErrorCode
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.repository.ScheduleRepository
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TicketCommandService
internal constructor(
    private val bookingRepository: BookingRepository,
    private val performanceRepository: PerformanceRepository,
    private val memberRepository: MemberRepository,
    private val scheduleRepository: ScheduleRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    @Transactional
    fun updateTickets(memberId: Long, command: TicketUpdateCommand) {
        translateDomainFailure {
            val details = command.bookingList
            if (details.map(TicketStatusUpdate::bookingId).distinct().size != details.size) {
                throw FrontofficeApplicationException(
                    TicketApplicationErrorCode.DUPLICATE_BOOKING_ID
                )
            }

            val performance = findOwnedPerformance(memberId, command.performanceId)
            val detailsByBookingId = details.associateBy(TicketStatusUpdate::bookingId)
            val (bookings, _) = lockSchedulesThenBookings(detailsByBookingId.keys, performance)

            bookings.forEach { booking ->
                val bookingId = checkNotNull(booking.id)
                val detail = requireNotNull(detailsByBookingId[bookingId])
                val requestedStatus = BookingStatus.valueOf(detail.bookingStatus.name)
                val updated = booking.transitionTo(requestedStatus)
                if (updated === booking) return@forEach
                bookingRepository.save(updated)
                eventPublisher.publishEvent(
                    TicketPaymentConfirmedEvent(
                        bookingId = bookingId,
                        bookerName = booking.bookerName,
                        bookerPhoneNumber = booking.bookerPhoneNumber,
                        performanceTitle = performance.performanceTitle,
                    )
                )
            }
        }
    }

    @Transactional
    fun refundTicketsByBookingIds(memberId: Long, command: TicketBookingIdsCommand) {
        translateDomainFailure {
            val performance = findOwnedPerformance(memberId, command.performanceId)
            val (bookings, schedules) =
                lockSchedulesThenBookings(
                    command.bookingIds,
                    performance,
                )

            bookings.forEach { original ->
                val shouldReleaseTickets = original.hasActiveTicketAllocation()
                val booking =
                    bookingRepository.save(original.completeRefund(LocalDateTime.now(clock)))
                if (shouldReleaseTickets) {
                    val schedule = requireNotNull(schedules[booking.scheduleId])
                    schedules[booking.scheduleId] =
                        scheduleRepository.save(
                            schedule.releaseTickets(booking.purchaseTicketCount)
                        )
                }
            }
        }
    }

    @Transactional
    fun deleteTicketsByBookingIds(memberId: Long, command: TicketBookingIdsCommand) {
        translateDomainFailure {
            val performance = findOwnedPerformance(memberId, command.performanceId)
            val (bookings, schedules) =
                lockSchedulesThenBookings(
                    command.bookingIds,
                    performance,
                )

            bookings.forEach { original ->
                val deleted = original.deleteByMaker(LocalDateTime.now(clock))
                val shouldReleaseTickets =
                    original.hasActiveTicketAllocation() && !deleted.hasActiveTicketAllocation()
                val booking = bookingRepository.save(deleted)
                if (shouldReleaseTickets) {
                    val schedule = requireNotNull(schedules[booking.scheduleId])
                    schedules[booking.scheduleId] =
                        scheduleRepository.save(
                            schedule.releaseTickets(booking.purchaseTicketCount)
                        )
                }
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
                ?: throw FrontofficeApplicationException(
                    TicketApplicationErrorCode.NO_BOOKING_FOUND
                )
        }
        if (bookings.any { it.scheduleId !in schedules }) {
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
            val schedule =
                scheduleRepository.lockById(scheduleId)
                    ?: throw FrontofficeApplicationException(
                        TicketApplicationErrorCode.NO_SCHEDULE_FOUND
                    )
            if (!schedule.belongsTo(requireNotNull(performance.id))) {
                throw FrontofficeApplicationException(
                    TicketApplicationErrorCode.SCHEDULE_NOT_BELONG_TO_PERFORMANCE
                )
            }
            schedules[scheduleId] = schedule
        }
        return schedules
    }

    private fun findOwnedPerformance(memberId: Long, performanceId: Long): Performance {
        val member = findMember(memberId)
        val performance =
            performanceRepository.findById(performanceId)
                ?: throw FrontofficeApplicationException(
                    TicketApplicationErrorCode.PERFORMANCE_NOT_FOUND
                )
        if (!performance.isOwnedBy(member.userId)) {
            throw FrontofficeApplicationException(TicketApplicationErrorCode.NOT_PERFORMANCE_OWNER)
        }
        return performance
    }

    private fun findMember(memberId: Long): Member =
        memberRepository.findById(memberId)
            ?: throw FrontofficeApplicationException(TicketApplicationErrorCode.MEMBER_NOT_FOUND)
}
