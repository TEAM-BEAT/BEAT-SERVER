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
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Performance", description = "공연 관련 API")
interface PerformanceApi {

    @Operation(summary = "공연 생성 API", description = "공연을 생성하는 POST API입니다.")
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "201", description = "공연이 성공적으로 생성되었습니다."),
                ApiResponse(
                    responseCode = "400",
                    description = "필수 데이터가 누락되었습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "공연 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun createPerformance(
        @CurrentMember memberId: Long,
        @RequestBody performanceRequest: PerformanceRequest,
    ): ResponseEntity<SuccessResponse<PerformanceResponse>>

    @Operation(summary = "공연 정보 수정 API", description = "공연 정보를 수정하는 PUT API입니다.")
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "공연 정보 수정 성공"),
                ApiResponse(
                    responseCode = "400",
                    description = "공연 회차 개수, 티켓 가격 또는 예매자 존재로 인해 수정할 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "403",
                    description = "해당 공연의 소유자가 아닙니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
                ApiResponse(
                    responseCode = "404",
                    description = "공연 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun updatePerformance(
        @CurrentMember memberId: Long,
        @RequestBody performanceModifyRequest: PerformanceModifyRequest,
    ): ResponseEntity<SuccessResponse<PerformanceModifyResponse>>

    @Operation(summary = "공연 수정 페이지 정보 조회 API", description = "공연 정보를 조회하는 GET API입니다.")
    @ApiResponses(
        value =
            [
                ApiResponse(responseCode = "200", description = "공연 수정 페이지 정보 조회 성공"),
                ApiResponse(
                    responseCode = "404",
                    description = "공연 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun getPerformanceForEdit(
        @CurrentMember memberId: Long,
        @PathVariable performanceId: Long,
    ): ResponseEntity<SuccessResponse<PerformanceModifyDetailResponse>>

    @DisableSwaggerSecurity
    @Operation(summary = "공연 상세정보 조회 API", description = "공연 상세페이지의 공연 상세정보를 조회하는 GET API입니다.")
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
        @PathVariable performanceId: Long
    ): ResponseEntity<SuccessResponse<PerformanceDetailResponse>>

    @DisableSwaggerSecurity
    @Operation(
        summary = "예매하기 관련 공연 정보 조회 API",
        description = "예매하기 페이지에서 필요한 예매 관련 공연 정보를 조회하는 GET API입니다.",
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
        @PathVariable performanceId: Long
    ): ResponseEntity<SuccessResponse<BookingPerformanceDetailResponse>>

    @Operation(summary = "회원이 등록한 공연 목록 조회 API", description = "회원이 등록한 공연 목록을 조회하는 GET API입니다.")
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
        @CurrentMember memberId: Long
    ): ResponseEntity<SuccessResponse<MakerPerformanceResponse>>

    @Operation(summary = "공연 삭제 API", description = "공연을 삭제하는 DELETE API입니다.")
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
                    description = "공연 정보를 찾을 수 없습니다.",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
                ),
            ]
    )
    fun deletePerformance(
        @CurrentMember memberId: Long,
        @PathVariable performanceId: Long,
    ): ResponseEntity<SuccessResponse<Void>>
}
