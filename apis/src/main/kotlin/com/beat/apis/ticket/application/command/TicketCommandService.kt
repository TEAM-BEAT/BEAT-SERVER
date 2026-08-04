package com.beat.apis.ticket.application.command

import com.beat.apis.booking.exception.BookingApplicationErrorCode
import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.member.exception.MemberApplicationErrorCode
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode
import com.beat.apis.ticket.application.event.TicketPaymentConfirmedEvent
import com.beat.apis.ticket.exception.TicketApplicationErrorCode
import com.beat.contracts.performance.PerformanceSummaryReadPort
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
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
    private val performanceSummaryReadPort: PerformanceSummaryReadPort,
    private val memberRepository: MemberRepository,
    private val scheduleRepository: ScheduleRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    @Transactional
    fun updateTickets(memberId: Long, command: TicketUpdateCommand) {
        val details = command.bookingList
        if (details.map(TicketStatusUpdate::bookingId).distinct().size != details.size) {
            throw ApiApplicationException(TicketApplicationErrorCode.DUPLICATE_BOOKING_ID)
        }

        val performance = findOwnedPerformance(memberId, command.performanceId)
        val detailsByBookingId = details.associateBy(TicketStatusUpdate::bookingId)
        val (bookings, _) = lockSchedulesThenBookings(detailsByBookingId.keys, performance)

        bookings.forEach { booking ->
            val detail = requireNotNull(detailsByBookingId[booking.getId()])
            val requestedStatus = BookingStatus.valueOf(detail.bookingStatus.name)
            val updated = booking.transitionTo(requestedStatus)
            if (updated === booking) return@forEach
            bookingRepository.save(updated)
            eventPublisher.publishEvent(
                TicketPaymentConfirmedEvent(
                    bookingId = booking.getId(),
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
            val booking = bookingRepository.save(original.completeRefund(LocalDateTime.now(clock)))
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
            val deleted = original.deleteByMaker(LocalDateTime.now(clock))
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
        performance: PerformanceSummaryReadModel,
    ): Pair<List<Booking>, MutableMap<Long, Schedule>> {
        val distinctBookingIds = bookingIds.distinct().sorted()
        val bookingSnapshots = bookingRepository.findAllById(distinctBookingIds)
        if (bookingSnapshots.size != distinctBookingIds.size) {
            throw ApiApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND)
        }
        val schedules = lockAndValidateSchedules(bookingSnapshots, performance)
        val bookings = distinctBookingIds.map { bookingId ->
            bookingRepository.lockById(bookingId)
                .orElseThrow { ApiApplicationException(BookingApplicationErrorCode.NO_BOOKING_FOUND) }
        }
        if (bookings.any { it.getScheduleId() !in schedules }) {
            throw ApiApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND)
        }
        return bookings to schedules
    }

    private fun lockAndValidateSchedules(
        bookings: List<Booking>,
        performance: PerformanceSummaryReadModel,
    ): MutableMap<Long, Schedule> {
        val schedules = mutableMapOf<Long, Schedule>()
        bookings.map(Booking::getScheduleId).distinct().sorted().forEach { scheduleId ->
            val schedule = scheduleRepository.lockById(scheduleId)
                .orElseThrow { ApiApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND) }
            if (!schedule.belongsTo(performance.performanceId)) {
                throw ApiApplicationException(ScheduleApplicationErrorCode.SCHEDULE_NOT_BELONG_TO_PERFORMANCE)
            }
            schedules[scheduleId] = schedule
        }
        return schedules
    }

    private fun findOwnedPerformance(memberId: Long, performanceId: Long): PerformanceSummaryReadModel {
        val member = findMember(memberId)
        val performance = performanceSummaryReadPort.findById(performanceId)
            .orElseThrow { ApiApplicationException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND) }
        if (performance.userId != member.getUserId()) {
            throw ApiApplicationException(PerformanceApplicationErrorCode.NOT_PERFORMANCE_OWNER)
        }
        return performance
    }

    private fun findMember(memberId: Long): Member =
        memberRepository.findById(memberId)
            .orElseThrow { ApiApplicationException(MemberApplicationErrorCode.MEMBER_NOT_FOUND) }
}
