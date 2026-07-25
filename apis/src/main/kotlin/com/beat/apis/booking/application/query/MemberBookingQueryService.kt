package com.beat.apis.booking.application.query

import com.beat.apis.booking.application.calculatePaymentAmountForRead
import com.beat.apis.booking.application.result.BookingRetrieveResult
import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.member.exception.MemberApplicationErrorCode
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode
import com.beat.apis.schedule.application.calculateDueDate
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode
import com.beat.contracts.performance.PerformanceSummaryReadPort
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel
import com.beat.domain.booking.model.Booking
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.vo.TicketPrice
import com.beat.domain.schedule.model.Schedule
import com.beat.domain.schedule.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Clock

@Service
@Transactional(readOnly = true)
class MemberBookingQueryService(
    private val bookingRepository: BookingRepository,
    private val memberRepository: MemberRepository,
    private val performanceSummaryReadPort: PerformanceSummaryReadPort,
    private val scheduleRepository: ScheduleRepository,
    private val clock: Clock,
) {
    fun findMemberBookings(memberId: Long): List<BookingRetrieveResult> {
        val member = memberRepository.findById(memberId)
            .orElseThrow { ApiApplicationException(MemberApplicationErrorCode.MEMBER_NOT_FOUND) }
        val bookings = bookingRepository.findByUserId(member.getUserId())
        val schedules = scheduleRepository.findAllById(bookings.map(Booking::getScheduleId).distinct())
            .associateBy { requireNotNull(it.getId()) }
        val performances = performanceSummaryReadPort.findAllByIds(
            schedules.values.map(Schedule::getPerformanceId).distinct(),
        ).associateBy(PerformanceSummaryReadModel::performanceId)
        val today = LocalDate.now(clock)
        return bookings.map { toResult(today, it, schedules, performances) }
    }

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
            booking.getUserId(), booking.getId(), schedule.getId(), performance.performanceId,
            performance.performanceTitle, schedule.getPerformanceDate(), performance.performanceVenue,
            booking.getPurchaseTicketCount(), schedule.getScheduleNumber().name,
            booking.getBookerName(), performance.performanceContact,
            performance.bankName, performance.accountNumber,
            performance.accountHolder, calculateDueDate(today, schedule),
            booking.getBookingStatus().name, booking.getCreatedAt(),
            performance.posterImage, totalAmount,
        )
    }
}
