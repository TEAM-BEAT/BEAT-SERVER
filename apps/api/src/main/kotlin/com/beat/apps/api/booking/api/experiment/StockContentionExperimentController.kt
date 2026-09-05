package com.beat.apps.api.booking.api.experiment

import com.beat.application.frontoffice.booking.booker.experiment.StockContentionBookingCommand
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionExperimentResponse
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionExperimentService
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionOutcome
import com.beat.application.frontoffice.booking.booker.experiment.StockContentionStrategy
import com.beat.apps.api.booking.api.request.MemberBookingRequest
import com.beat.support.security.CurrentMember
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("dev & !prod")
@ConditionalOnProperty(
    prefix = "booking.experiment",
    name = ["enabled"],
    havingValue = "true",
)
@RequestMapping("/internal/experiments/stock-contention")
class StockContentionExperimentController(
    private val experimentService: StockContentionExperimentService
) {
    @PostMapping("/{strategy}/bookings")
    fun createBooking(
        @CurrentMember memberId: Long,
        @PathVariable strategy: StockContentionStrategy,
        @Valid @RequestBody request: MemberBookingRequest,
    ): ResponseEntity<StockContentionExperimentResponse> {
        val response =
            experimentService.createMemberBooking(
                memberId = memberId,
                strategy = strategy,
                command =
                    StockContentionBookingCommand(
                        scheduleId = request.scheduleId,
                        purchaseTicketCount = request.purchaseTicketCount,
                        bookerName = request.bookerName,
                        bookerPhoneNumber = request.bookerPhoneNumber,
                    ),
            )
        val status =
            if (response.outcome == StockContentionOutcome.ACCEPTED) {
                HttpStatus.CREATED
            } else {
                HttpStatus.OK
            }
        return ResponseEntity.status(status).body(response)
    }
}
