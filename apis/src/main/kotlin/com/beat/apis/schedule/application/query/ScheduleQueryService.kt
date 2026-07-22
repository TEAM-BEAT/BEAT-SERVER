package com.beat.apis.schedule.application.query

import com.beat.apis.exception.ApiApplicationException
import com.beat.apis.schedule.application.result.TicketAvailabilityResult
import com.beat.apis.schedule.exception.ScheduleApplicationErrorCode
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
            throw ApiApplicationException(ScheduleApplicationErrorCode.INVALID_TICKET_AVAILABILITY_REQUEST)
        }
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { ApiApplicationException(ScheduleApplicationErrorCode.NO_SCHEDULE_FOUND) }
        val availableTicketCount = schedule.getAvailableTicketCount()
        if (!schedule.canPurchase(purchaseTicketCount)) {
            throw ApiApplicationException(ScheduleApplicationErrorCode.INSUFFICIENT_TICKETS)
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
