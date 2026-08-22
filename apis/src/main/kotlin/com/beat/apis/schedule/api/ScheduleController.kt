package com.beat.apis.schedule.api

import com.beat.apis.schedule.api.response.ScheduleSuccessCode
import com.beat.apis.schedule.api.response.TicketAvailabilityResponse
import com.beat.apis.schedule.facade.ScheduleFacade
import com.beat.apis.response.SuccessResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/schedules")
class ScheduleController(
    private val scheduleFacade: ScheduleFacade,
) : ScheduleApi {

    @GetMapping("/{scheduleId}/availability")
    override fun getTicketAvailability(
        @PathVariable scheduleId: Long,
        @RequestParam purchaseTicketCount: Int,
    ): ResponseEntity<SuccessResponse<TicketAvailabilityResponse>> {
        val response = scheduleFacade.findTicketAvailability(scheduleId, purchaseTicketCount)
        return ResponseEntity.status(HttpStatus.OK)
            .body(SuccessResponse.of(ScheduleSuccessCode.TICKET_AVAILABILITY_RETRIEVAL_SUCCESS, response))
    }
}
