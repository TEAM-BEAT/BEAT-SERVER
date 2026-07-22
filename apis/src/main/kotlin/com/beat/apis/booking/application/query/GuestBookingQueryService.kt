package com.beat.apis.booking.application.query

import com.beat.apis.booking.application.calculatePaymentAmountForRead
import com.beat.apis.booking.application.result.BookingRetrieveResult
import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode
import com.beat.apis.schedule.application.calculateDueDate
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode
import com.beat.contracts.performance.PerformanceSummaryReadPort
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Clock

@Service
@Transactional(readOnly = true)
class GuestBookingQueryService(
    private val bookingRepository: BookingRepository,
    private val performanceSummaryReadPort: PerformanceSummaryReadPort,
    private val scheduleRepository: ScheduleRepository,
    private val clock: Clock,
) {
    fun findGuestBookings(userId: Long): List<BookingRetrieveResult> {
        val bookings = bookingRepository.findByUserId(userId)
        val schedules = findSchedules(bookings)
        val performances = findPerformances(schedules.values)
        val today = LocalDate.now(clock)
        return bookings.map { toResult(today, it, schedules, performances) }
    }

    private fun findSchedules(bookings: List<Booking>): Map<Long, Schedule> =
        scheduleRepository.findAllById(bookings.map(Booking::getScheduleId).distinct())
            .associateBy { requireNotNull(it.getId()) }

    private fun findPerformances(schedules: Collection<Schedule>): Map<Long, PerformanceSummaryReadModel> =
        performanceSummaryReadPort.findAllByIds(schedules.map(Schedule::getPerformanceId).distinct())
            .associateBy(PerformanceSummaryReadModel::performanceId)

    private fun toResult(
        today: LocalDate,
        booking: Booking,
        schedules: Map<Long, Schedule>,
        performances: Map<Long, PerformanceSummaryReadModel>,
    ): BookingRetrieveResult {
        val schedule = schedules[booking.getScheduleId()]
            ?: throw ApiApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND)
        val performance = performances[schedule.getPerformanceId()]
            ?: throw ApiApplicationException(PerformanceApplicationErrorCode.PERFORMANCE_NOT_FOUND)
        val totalAmount = booking.getTotalPaymentAmount()
            ?: calculatePaymentAmountForRead(TicketPrice.of(performance.ticketPrice), booking.getPurchaseTicketCount())
        return BookingRetrieveResult(
            booking.getUserId(), booking.getId(), schedule.getId(), performance.performanceId, performance.performanceTitle,
            schedule.getPerformanceDate(), performance.performanceVenue, booking.getPurchaseTicketCount(),
            schedule.getScheduleNumber().name, booking.getBookerName(),
            performance.performanceContact, performance.bankName,
            performance.accountNumber, performance.accountHolder, calculateDueDate(today, schedule),
            booking.getBookingStatus().name, booking.getCreatedAt(),
            performance.posterImage, totalAmount,
        )
    }
}
