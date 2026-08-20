package com.beat.application.frontoffice.schedule.query

import com.beat.application.frontoffice.exception.FrontofficeApplicationException
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
        if (scheduleId <= 0 || purchaseTicketCount <= 0) {
            throw FrontofficeApplicationException(ScheduleApplicationErrorCode.INVALID_TICKET_AVAILABILITY_REQUEST)
        }
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { FrontofficeApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND) }
        val availableTicketCount = schedule.getAvailableTicketCount()
        if (!schedule.canPurchase(purchaseTicketCount)) {
            throw FrontofficeApplicationException(ScheduleApplicationErrorCode.INSUFFICIENT_TICKETS)
        }
        return TicketAvailabilityResult(
            scheduleId = schedule.getId(),
            scheduleNumber = schedule.getScheduleNumber().displayName,
            totalTicketCount = schedule.getTotalTicketCount(),
            soldTicketCount = schedule.getAllocatedTicketCount(),
            availableTicketCount = availableTicketCount,
            requestedTicketCount = purchaseTicketCount,
            isAvailable = true,
        )
    }
}
