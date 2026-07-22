package com.beat.apis.schedule.facade

import com.beat.apis.schedule.api.response.TicketAvailabilityResponse
import com.beat.apis.schedule.application.query.ScheduleQueryService
import org.springframework.stereotype.Service

@Service
class ScheduleFacade(
    private val scheduleQueryService: ScheduleQueryService,
) {
    fun findTicketAvailability(scheduleId: Long, purchaseTicketCount: Int): TicketAvailabilityResponse =
        TicketAvailabilityResponse.from(
            scheduleQueryService.findTicketAvailability(
                scheduleId,
                purchaseTicketCount,
            ),
        )
}
