package com.beat.apps.api.schedule.api

import com.beat.apps.api.response.ErrorResponse
import com.beat.apps.api.response.SuccessResponse
import com.beat.apps.api.schedule.api.response.TicketAvailabilityResponse
import com.beat.apps.api.swagger.annotation.DisableSwaggerSecurity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Schedule", description = "스케줄 관련 API")
interface ScheduleApi {

    @DisableSwaggerSecurity
    @Operation(
        operationId = "scheduleCheckTicketAvailability",
        summary = "회차 티켓 구매 가능 여부 조회",
        description = "지정한 회차의 잔여 티켓 수량이 요청 수량을 충족하는지 조회합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "티켓 수량 조회가 성공적으로 완료되었습니다."),
                ApiResponse(
                    responseCode = "400",
                    description = "잘못된 데이터 형식입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회차 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "409",
                    description = "요청한 티켓 수량이 잔여 티켓 수를 초과했습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun getTicketAvailability(
        @Parameter(
            description = "티켓 구매 가능 여부를 확인할 회차 식별자입니다.",
            example = "1",
            required = true,
        )
        @PathVariable
        scheduleId: Long,
        @Parameter(
            description = "구매 가능 여부를 확인할 티켓 수량입니다. 1개 이상이어야 합니다.",
            example = "2",
            required = true,
        )
        @RequestParam
        purchaseTicketCount: Int,
    ): ResponseEntity<SuccessResponse<TicketAvailabilityResponse>>
}
