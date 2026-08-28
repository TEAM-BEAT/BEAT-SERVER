package com.beat.application.frontoffice.ticket.maker.query

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.ticket.maker.exception.TicketApplicationErrorCode
import com.beat.domain.member.model.Member
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.performance.model.Performance
import com.beat.domain.performance.repository.PerformanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TicketQueryService
internal constructor(
    private val makerTicketReader: MakerTicketReader,
    private val performanceRepository: PerformanceRepository,
    private val memberRepository: MemberRepository,
) {
    fun findAllTicketsByConditions(
        memberId: Long,
        performanceId: Long,
        query: TicketListQuery,
    ): TicketRetrieveResult {
        return translateDomainFailure {
            validateDeletedTicketsAreNotRequested(query.bookingStatuses)
            val performance = findOwnedPerformance(memberId, performanceId)
            val schedules = makerTicketReader.findSchedules(performanceId)

            log.info { "performanceId: ${performanceId}" }
            log.info { "scheduleNumbers: ${query.scheduleNumbers}" }
            log.info { "bookingStatuses: ${query.bookingStatuses}" }

            val tickets =
                makerTicketReader.findTickets(
                    performanceId,
                    toMakerTicketScheduleNumbers(query.scheduleNumbers),
                    toMakerTicketBookingStatuses(query.bookingStatuses),
                )
            toResult(performance, schedules, tickets)
        }
    }

    fun searchAllTicketsByConditions(
        memberId: Long,
        performanceId: Long,
        query: TicketListQuery,
    ): TicketRetrieveResult {
        return translateDomainFailure {
            val searchWord = requireSearchWord(query.searchWord)
            validateDeletedTicketsAreNotRequested(query.bookingStatuses)
            val performance = findOwnedPerformance(memberId, performanceId)
            val schedules = makerTicketReader.findSchedules(performanceId)

            val selectedScheduleNumbers =
                if (query.scheduleNumbers.isEmpty()) {
                    schedules.map { MakerTicketScheduleNumber.valueOf(it.scheduleNumber) }
                } else {
                    toMakerTicketScheduleNumbers(query.scheduleNumbers)
                }
            val selectedBookingStatuses =
                if (query.bookingStatuses.isEmpty()) {
                    listOf(
                        MakerTicketBookingStatus.REFUND_REQUESTED,
                        MakerTicketBookingStatus.CHECKING_PAYMENT,
                        MakerTicketBookingStatus.BOOKING_CONFIRMED,
                        MakerTicketBookingStatus.BOOKING_CANCELLED,
                    )
                } else {
                    toMakerTicketBookingStatuses(query.bookingStatuses)
                }

            log.info {
                "Searching maker tickets: performanceId=${performanceId}, scheduleFilterCount=${selectedScheduleNumbers.size}, statusFilterCount=${selectedBookingStatuses.size}"
            }
            val tickets =
                makerTicketReader.searchTickets(
                    performanceId,
                    searchWord,
                    selectedScheduleNumbers,
                    selectedBookingStatuses,
                )
            log.info { "searchTickets result count: ${tickets.size}" }
            toResult(performance, schedules, tickets)
        }
    }

    private fun requireSearchWord(searchWord: String?): String =
        searchWord?.trim()?.takeIf { it.length >= 2 }
            ?: throw FrontofficeApplicationException(
                TicketApplicationErrorCode.SEARCH_WORD_TOO_SHORT
            )

    private fun validateDeletedTicketsAreNotRequested(bookingStatuses: List<String>) {
        if ("BOOKING_DELETED" in bookingStatuses) {
            throw FrontofficeApplicationException(
                TicketApplicationErrorCode.DELETED_TICKET_RETRIEVE_NOT_ALLOWED
            )
        }
    }

    private fun toMakerTicketScheduleNumbers(
        scheduleNumbers: List<String>
    ): List<MakerTicketScheduleNumber> = scheduleNumbers.map(MakerTicketScheduleNumber::valueOf)

    private fun toMakerTicketBookingStatuses(
        bookingStatuses: List<String>
    ): List<MakerTicketBookingStatus> = bookingStatuses.map(MakerTicketBookingStatus::valueOf)

    private fun toResult(
        performance: Performance,
        schedules: List<MakerTicketScheduleReadModel>,
        tickets: List<MakerTicketListItemReadModel>,
    ): TicketRetrieveResult {
        val scheduleMap = schedules.associateBy(MakerTicketScheduleReadModel::scheduleId)
        val bookingList = tickets.map { ticket ->
            val schedule =
                scheduleMap[ticket.scheduleId]
                    ?: throw FrontofficeApplicationException(
                        TicketApplicationErrorCode.NO_SCHEDULE_FOUND
                    )
            TicketDetailResult(
                ticket.bookingId,
                ticket.bookerName,
                ticket.bookerPhoneNumber,
                ticket.scheduleId,
                ticket.purchaseTicketCount,
                ticket.createdAt,
                ticket.bookingStatus.name,
                schedule.scheduleNumber,
                ticket.bankName,
                ticket.accountNumber,
                ticket.accountHolder,
                ticket.deletable,
            )
        }
        log.info { "Converted TicketDetail count: ${bookingList.size}" }

        return TicketRetrieveResult(
            performance.performanceTitle,
            performance.performanceTeamName,
            performance.totalScheduleCount,
            schedules.sumOf(MakerTicketScheduleReadModel::totalTicketCount),
            schedules.sumOf(MakerTicketScheduleReadModel::soldTicketCount),
            bookingList,
        )
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

    private companion object {
        val log = KotlinLogging.logger {}
    }
}
