package com.beat.apps.api.schedule.facade

import com.beat.apps.api.schedule.api.response.TicketAvailabilityResponse
import com.beat.application.frontoffice.schedule.booker.query.ScheduleQueryService
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
