package com.beat.apis.ticket.application.query

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.member.exception.MemberApplicationErrorCode
import com.beat.apis.performance.exception.PerformanceApplicationErrorCode
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode
import com.beat.apis.ticket.application.result.TicketDetailResult
import com.beat.apis.ticket.application.result.TicketRetrieveResult
import com.beat.apis.ticket.exception.TicketApplicationErrorCode
import com.beat.contracts.booking.MakerTicketReadPort
import com.beat.contracts.booking.readmodel.MakerTicketBookingStatus
import com.beat.contracts.booking.readmodel.MakerTicketListItemReadModel
import com.beat.contracts.performance.PerformanceSummaryReadPort
import com.beat.contracts.performance.readmodel.PerformanceSummaryReadModel
import com.beat.contracts.booking.readmodel.MakerTicketScheduleNumber
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.contracts.schedule.ScheduleReadPort
import com.beat.contracts.schedule.readmodel.ScheduleSummaryReadModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TicketQueryService(
    private val makerTicketReadPort: MakerTicketReadPort,
    private val performanceSummaryReadPort: PerformanceSummaryReadPort,
    private val memberRepository: MemberRepository,
    private val scheduleReadPort: ScheduleReadPort,
) {
    fun findAllTicketsByConditions(
        memberId: Long,
        performanceId: Long,
        query: TicketListQuery,
    ): TicketRetrieveResult {
        validateDeletedTicketsAreNotRequested(query.bookingStatuses)
        val performance = findOwnedPerformance(memberId, performanceId)
        val schedules = scheduleReadPort.findAllByPerformanceId(performanceId)

        log.info { "performanceId: ${performanceId}" }
        log.info { "scheduleNumbers: ${query.scheduleNumbers}" }
        log.info { "bookingStatuses: ${query.bookingStatuses}" }

        val tickets = makerTicketReadPort.findTickets(
            performanceId,
            toMakerTicketScheduleNumbers(query.scheduleNumbers),
            toMakerTicketBookingStatuses(query.bookingStatuses),
        )
        return toResult(performance, schedules, tickets)
    }

    fun searchAllTicketsByConditions(
        memberId: Long,
        performanceId: Long,
        query: TicketListQuery,
    ): TicketRetrieveResult {
        validateSearchWord(query.searchWord)
        validateDeletedTicketsAreNotRequested(query.bookingStatuses)
        val performance = findOwnedPerformance(memberId, performanceId)
        val schedules = scheduleReadPort.findAllByPerformanceId(performanceId)

        val selectedScheduleNumbers = if (query.scheduleNumbers.isEmpty()) {
            schedules.map { MakerTicketScheduleNumber.valueOf(it.scheduleNumber) }
        } else {
            toMakerTicketScheduleNumbers(query.scheduleNumbers)
        }
        val selectedBookingStatuses = if (query.bookingStatuses.isEmpty()) {
            listOf(
                MakerTicketBookingStatus.REFUND_REQUESTED,
                MakerTicketBookingStatus.CHECKING_PAYMENT,
                MakerTicketBookingStatus.BOOKING_CONFIRMED,
                MakerTicketBookingStatus.BOOKING_CANCELLED,
            )
        } else {
            toMakerTicketBookingStatuses(query.bookingStatuses)
        }

        log.info { "Searching maker tickets: performanceId=${performanceId}, scheduleFilterCount=${selectedScheduleNumbers.size}, statusFilterCount=${selectedBookingStatuses.size}" }
        val tickets = makerTicketReadPort.searchTickets(
            performanceId,
            query.searchWord,
            selectedScheduleNumbers,
            selectedBookingStatuses,
        )
        log.info { "searchTickets result count: ${tickets.size}" }
        return toResult(performance, schedules, tickets)
    }

    private fun validateSearchWord(searchWord: String?) {
        if (searchWord == null || searchWord.length < 2) {
            throw ApiApplicationException(TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT)
        }
    }

    private fun validateDeletedTicketsAreNotRequested(bookingStatuses: List<String>) {
        if ("BOOKING_DELETED" in bookingStatuses) {
            throw ApiApplicationException(TicketApplicationErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED)
        }
    }

    private fun toMakerTicketScheduleNumbers(
        scheduleNumbers: List<String>,
    ): List<MakerTicketScheduleNumber> =
        scheduleNumbers.map(MakerTicketScheduleNumber::valueOf)

    private fun toMakerTicketBookingStatuses(
        bookingStatuses: List<String>,
    ): List<MakerTicketBookingStatus> =
        bookingStatuses.map(MakerTicketBookingStatus::valueOf)

    private fun toResult(
        performance: PerformanceSummaryReadModel,
        schedules: List<ScheduleSummaryReadModel>,
        tickets: List<MakerTicketListItemReadModel>,
    ): TicketRetrieveResult {
        val scheduleMap = schedules.associateBy(ScheduleSummaryReadModel::scheduleId)
        val bookingList = tickets.map { ticket ->
            val schedule = scheduleMap[ticket.scheduleId]
                ?: throw ApiApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND)
            TicketDetailResult(
                ticket.bookingId,
                ticket.bookerName,
                ticket.bookerPhoneNumber,
                ticket.scheduleId,
                ticket.purchaseTicketCount,
                ticket.createdAt,
                ticket.bookingStatus.name,
                schedule.scheduleNumber,
                ticket.bankName.orEmpty(),
                ticket.accountNumber.orEmpty(),
                ticket.accountHolder.orEmpty(),
                ticket.deletable,
            )
        }
        log.info { "Converted TicketDetail count: ${bookingList.size}" }

        return TicketRetrieveResult(
            performance.performanceTitle,
            performance.performanceTeamName,
            performance.totalScheduleCount,
            schedules.sumOf(ScheduleSummaryReadModel::totalTicketCount),
            schedules.sumOf(ScheduleSummaryReadModel::soldTicketCount),
            bookingList,
        )
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
    private companion object {
        val log = KotlinLogging.logger {}
    }
}
