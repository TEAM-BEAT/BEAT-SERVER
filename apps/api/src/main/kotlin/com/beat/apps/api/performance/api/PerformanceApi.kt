package com.beat.apps.api.performance.api

import com.beat.apps.api.performance.api.request.PerformanceModifyRequest
import com.beat.apps.api.performance.api.request.PerformanceRequest
import com.beat.apps.api.performance.api.response.BookingPerformanceDetailResponse
import com.beat.apps.api.performance.api.response.MakerPerformanceResponse
import com.beat.apps.api.performance.api.response.PerformanceDetailResponse
import com.beat.apps.api.performance.api.response.PerformanceModifyDetailResponse
import com.beat.apps.api.performance.api.response.PerformanceModifyResponse
import com.beat.apps.api.performance.api.response.PerformanceResponse
import com.beat.apps.api.response.ErrorResponse
import com.beat.apps.api.response.SuccessResponse
import com.beat.apps.api.swagger.annotation.DisableSwaggerSecurity
import com.beat.support.security.CurrentMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Performance", description = "공연 관련 API")
interface PerformanceApi {

    @Operation(
        operationId = "createPerformance",
        summary = "공연 생성",
        description = "인증된 회원이 공연 기본 정보와 회차·출연진·스태프·이미지를 등록합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "201", description = "공연이 성공적으로 생성되었습니다."),
                ApiResponse(
                    responseCode = "400",
                    description =
                        "회차 목록이 비어 있거나 과거 회차가 포함된 경우, 또는 러닝타임·티켓 가격·티켓 수량·이미지 key·결제 계좌 입력이 유효하지 않은 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun createPerformance(
        @Parameter(hidden = true) @CurrentMember memberId: Long,
        @SwaggerRequestBody(
            description =
                "생성할 공연 정보. ticketPrice가 0이면 결제 계좌 필드는 모두 null이어야 하며, 0보다 크면 세 필드가 모두 필요합니다.",
            required = true,
        )
        @RequestBody
        performanceRequest: PerformanceRequest,
    ): ResponseEntity<SuccessResponse<PerformanceResponse>>

    @Operation(
        operationId = "updatePerformance",
        summary = "공연 정보 수정",
        description = "공연 소유자가 공연 기본 정보와 회차·출연진·스태프·이미지 구성을 수정합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "공연 정보 수정 성공"),
                ApiResponse(
                    responseCode = "400",
                    description =
                        "회차 목록이 비어 있거나 수정 대상 ID가 중복되었거나 종료된 회차를 수정하려는 경우, 또는 티켓 가격·수량·이미지 key·결제 계좌 등 요청 입력이 유효하지 않은 경우입니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "403",
                    description = "공연 소유자가 아니거나 수정 대상 회차·출연진·스태프·이미지가 해당 공연에 속하지 않습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회원·공연·회차·출연진·스태프·이미지를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun updatePerformance(
        @Parameter(hidden = true) @CurrentMember memberId: Long,
        @SwaggerRequestBody(
            description =
                "수정할 공연 정보. ticketPrice가 0이면 결제 계좌 필드는 모두 null이어야 하며, 0보다 크면 세 필드가 모두 필요합니다.",
            required = true,
        )
        @RequestBody
        performanceModifyRequest: PerformanceModifyRequest,
    ): ResponseEntity<SuccessResponse<PerformanceModifyResponse>>

    @Operation(
        operationId = "getPerformanceForEdit",
        summary = "공연 수정 페이지 정보 조회",
        description = "인증된 공연 소유자가 수정 화면에 필요한 공연 및 구성 요소 정보를 조회합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "공연 수정 페이지 정보 조회 성공"),
                ApiResponse(
                    responseCode = "403",
                    description = "해당 공연의 소유자가 아닙니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회원 또는 공연 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun getPerformanceForEdit(
        @Parameter(hidden = true) @CurrentMember memberId: Long,
        @Parameter(
            name = "performanceId",
            description = "조회할 공연 식별자",
            example = "1",
            required = true,
        )
        @PathVariable
        performanceId: Long,
    ): ResponseEntity<SuccessResponse<PerformanceModifyDetailResponse>>

    @DisableSwaggerSecurity
    @Operation(
        operationId = "getPerformanceDetail",
        summary = "공연 상세정보 조회",
        description = "누구나 공연 상세 페이지에 필요한 공연 정보와 회차별 예매 가능 상태를 조회합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "공연 상세정보 조회 성공"),
                ApiResponse(
                    responseCode = "404",
                    description = "공연 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun getPerformanceDetail(
        @Parameter(
            name = "performanceId",
            description = "상세 조회할 공연 식별자",
            example = "1",
            required = true,
        )
        @PathVariable
        performanceId: Long
    ): ResponseEntity<SuccessResponse<PerformanceDetailResponse>>

    @DisableSwaggerSecurity
    @Operation(
        operationId = "getBookingPerformanceDetail",
        summary = "예매용 공연 정보 조회",
        description = "예매 화면에 필요한 공연·회차·잔여 좌석·입금 계좌 정보를 조회합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "예매하기 관련 공연 정보 조회 성공"),
                ApiResponse(
                    responseCode = "404",
                    description = "공연 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun getBookingPerformanceDetail(
        @Parameter(
            name = "performanceId",
            description = "예매 정보를 조회할 공연 식별자",
            example = "1",
            required = true,
        )
        @PathVariable
        performanceId: Long
    ): ResponseEntity<SuccessResponse<BookingPerformanceDetailResponse>>

    @Operation(
        operationId = "getUserPerformances",
        summary = "회원 등록 공연 목록 조회",
        description = "인증된 회원이 자신이 등록한 공연 목록과 각 공연의 대표 정보를 조회합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "회원이 등록한 공연 목록 조회 성공"),
                ApiResponse(
                    responseCode = "404",
                    description = "회원 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun getUserPerformances(
        @Parameter(hidden = true) @CurrentMember memberId: Long
    ): ResponseEntity<SuccessResponse<MakerPerformanceResponse>>

    @Operation(
        operationId = "deletePerformance",
        summary = "공연 삭제",
        description = "공연 소유자가 예매 내역이 없는 공연을 삭제합니다.",
    )
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "공연 삭제 성공"),
                ApiResponse(
                    responseCode = "403",
                    description = "공연의 소유자가 아니거나 예매자가 있어 삭제할 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "회원·공연·회차 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun deletePerformance(
        @Parameter(hidden = true) @CurrentMember memberId: Long,
        @Parameter(
            name = "performanceId",
            description = "삭제할 공연 식별자",
            example = "1",
            required = true,
        )
        @PathVariable
        performanceId: Long,
    ): ResponseEntity<SuccessResponse<Void>>
}
