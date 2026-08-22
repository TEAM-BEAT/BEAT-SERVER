package com.beat.application.frontoffice.schedule.booker.query

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
import com.beat.application.frontoffice.exception.translateDomainFailure
import com.beat.application.frontoffice.schedule.exception.ScheduleApplicationErrorCode
import com.beat.domain.schedule.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScheduleQueryService(
    private val scheduleRepository: ScheduleRepository,
) {
    @Transactional(readOnly = true)
    fun findTicketAvailability(
        scheduleId: Long,
        purchaseTicketCount: Int,
    ): TicketAvailabilityResult {
        return translateDomainFailure {
        if (scheduleId <= 0 || purchaseTicketCount <= 0) {
            throw FrontofficeApplicationException(ScheduleApplicationErrorCode.INVALID_TICKET_AVAILABILITY_REQUEST)
        }
        val schedule = scheduleRepository.findById(scheduleId)
            ?: throw FrontofficeApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND)
        val availableTicketCount = schedule.availableTicketCount
        if (!schedule.canPurchase(purchaseTicketCount)) {
            throw FrontofficeApplicationException(ScheduleApplicationErrorCode.INSUFFICIENT_TICKETS)
        }
        TicketAvailabilityResult(
            scheduleId = schedule.id,
            scheduleNumber = schedule.scheduleNumber.displayName,
            totalTicketCount = schedule.totalTicketCount,
            soldTicketCount = schedule.allocatedTicketCount,
            availableTicketCount = availableTicketCount,
            requestedTicketCount = purchaseTicketCount,
            isAvailable = true,
        )
        }
    }
}
