package com.beat.apis.booking.application.service.query

import com.beat.apis.booking.application.dto.TicketDetail
import com.beat.apis.booking.application.dto.TicketRetrieveResponse
import com.beat.domain.booking.dao.TicketRepository
import com.beat.domain.booking.domain.Booking
import com.beat.domain.booking.domain.BookingStatus
import com.beat.domain.member.dao.MemberRepository
import com.beat.domain.member.domain.Member
import com.beat.domain.member.exception.MemberErrorCode
import com.beat.domain.performance.dao.PerformanceRepository
import com.beat.domain.performance.domain.BankName
import com.beat.domain.performance.domain.Performance
import com.beat.domain.performance.exception.PerformanceErrorCode
import com.beat.domain.schedule.dao.ScheduleRepository
import com.beat.domain.schedule.domain.Schedule
import com.beat.domain.schedule.domain.ScheduleNumber
import com.beat.domain.user.dao.UserRepository
import com.beat.domain.user.domain.Users
import com.beat.domain.user.exception.UserErrorCode
import com.beat.global.common.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TicketQueryService(
    private val ticketRepository: TicketRepository,
    private val performanceRepository: PerformanceRepository,
    private val memberRepository: MemberRepository,
    private val userRepository: UserRepository,
    private val scheduleRepository: ScheduleRepository,
) {

    fun findAllTicketsByConditions(
        memberId: Long,
        performanceId: Long,
        scheduleNumbers: List<ScheduleNumber>?,
        bookingStatuses: List<BookingStatus>?,
    ): TicketRetrieveResponse {
        val member = findMember(memberId)
        val user = findUser(member)
        val performance = findPerformance(performanceId)
        performance.validatePerformanceOwnership(user.id)

        val schedules = scheduleRepository.findAllByPerformanceId(performanceId)
        val totalPerformanceTicketCount = calculateTotalTicketCount(schedules)
        val totalPerformanceSoldTicketCount = calculateTotalSoldTicketCount(schedules)

        log.info("performanceId: {}", performanceId)
        log.info("scheduleNumbers: {}", scheduleNumbers)
        log.info("bookingStatuses: {}", bookingStatuses)

        val bookings = ticketRepository.findBookingsByPerformanceIdAndScheduleNumbersAndBookingStatuses(
            performanceId,
            scheduleNumbers,
            bookingStatuses,
        )

        return toTicketRetrieveResponse(
            performance = performance,
            totalPerformanceTicketCount = totalPerformanceTicketCount,
            totalPerformanceSoldTicketCount = totalPerformanceSoldTicketCount,
            bookings = bookings,
        )
    }

    fun searchAllTicketsByConditions(
        memberId: Long,
        performanceId: Long,
        searchWord: String,
        scheduleNumbers: List<ScheduleNumber>?,
        bookingStatuses: List<BookingStatus>?,
    ): TicketRetrieveResponse {
        val member = findMember(memberId)
        val user = findUser(member)
        val performance = findPerformance(performanceId)
        performance.validatePerformanceOwnership(user.id)

        val schedules = scheduleRepository.findAllByPerformanceId(performanceId)
        val totalPerformanceTicketCount = calculateTotalTicketCount(schedules)
        val totalPerformanceSoldTicketCount = calculateTotalSoldTicketCount(schedules)

        val selectedScheduleNumbers = scheduleNumbers
            ?.takeIf { it.isNotEmpty() }
            ?.map { it.name }
            ?: schedules.map { it.scheduleNumber.name }

        val selectedBookingStatuses = bookingStatuses
            ?.takeIf { it.isNotEmpty() }
            ?.map { it.name }
            ?: DEFAULT_SEARCH_BOOKING_STATUSES.map { it.name }

        log.info("performanceId: {}", performanceId)
        log.info("searchWord: {}", searchWord)
        log.info("selectedScheduleNumbers: {}", selectedScheduleNumbers)
        log.info("selectedBookingStatuses: {}", selectedBookingStatuses)

        val bookings = ticketRepository.searchBookingsByPerformanceIdAndSearchWordAndSchedulesNumbersAndBookingStatuses(
            performanceId,
            searchWord,
            selectedScheduleNumbers,
            selectedBookingStatuses,
        )

        log.info("searchTickets result: {}", bookings)

        return toTicketRetrieveResponse(
            performance = performance,
            totalPerformanceTicketCount = totalPerformanceTicketCount,
            totalPerformanceSoldTicketCount = totalPerformanceSoldTicketCount,
            bookings = bookings,
        )
    }

    private fun toTicketRetrieveResponse(
        performance: Performance,
        totalPerformanceTicketCount: Int,
        totalPerformanceSoldTicketCount: Int,
        bookings: List<Booking>,
    ): TicketRetrieveResponse {
        val bookingList = bookings.map { booking ->
            TicketDetail.of(
                booking.id,
                booking.bookerName,
                booking.bookerPhoneNumber,
                booking.schedule.id,
                booking.purchaseTicketCount,
                booking.createdAt,
                booking.bookingStatus,
                booking.schedule.scheduleNumber.name,
                booking.bankName?.name ?: BankName.NONE.displayName,
                booking.accountNumber,
                booking.accountHolder,
            )
        }

        log.info("Converted TicketDetail list: {}", bookingList)

        return TicketRetrieveResponse.of(
            performance.performanceTitle,
            performance.performanceTeamName,
            performance.totalScheduleCount,
            totalPerformanceTicketCount,
            totalPerformanceSoldTicketCount,
            bookingList,
        )
    }

    private fun findMember(memberId: Long): Member {
        return memberRepository.findById(memberId)
            .orElseThrow { NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND) }
    }

    private fun findUser(member: Member): Users {
        return userRepository.findById(member.user.id)
            .orElseThrow { NotFoundException(UserErrorCode.USER_NOT_FOUND) }
    }

    private fun findPerformance(performanceId: Long): Performance {
        return performanceRepository.findById(performanceId)
            .orElseThrow { NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND) }
    }

    private fun calculateTotalTicketCount(schedules: List<Schedule>): Int {
        return schedules.sumOf { it.totalTicketCount }
    }

    private fun calculateTotalSoldTicketCount(schedules: List<Schedule>): Int {
        return schedules.sumOf { it.soldTicketCount }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TicketQueryService::class.java)

        private val DEFAULT_SEARCH_BOOKING_STATUSES = listOf(
            BookingStatus.CHECKING_PAYMENT,
            BookingStatus.BOOKING_CONFIRMED,
            BookingStatus.BOOKING_CANCELLED,
            BookingStatus.REFUND_REQUESTED,
        )
    }
}