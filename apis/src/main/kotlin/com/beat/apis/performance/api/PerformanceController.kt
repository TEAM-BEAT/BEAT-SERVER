package com.beat.apis.performance.api

import com.beat.apis.performance.api.request.PerformanceModifyRequest
import com.beat.apis.performance.api.request.PerformanceRequest
import com.beat.apis.performance.api.response.BookingPerformanceDetailResponse
import com.beat.apis.performance.api.response.MakerPerformanceResponse
import com.beat.apis.performance.api.response.PerformanceDetailResponse
import com.beat.apis.performance.api.response.PerformanceModifyDetailResponse
import com.beat.apis.performance.api.response.PerformanceModifyResponse
import com.beat.apis.performance.api.response.PerformanceResponse
import com.beat.apis.performance.api.response.PerformanceSuccessCode
import com.beat.apis.performance.facade.PerformanceFacade
import com.beat.support.security.CurrentMember
import com.beat.apis.response.SuccessResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/performances")
class PerformanceController(
    private val performanceFacade: PerformanceFacade,
) : PerformanceApi {

    @PostMapping
    override fun createPerformance(
        @CurrentMember memberId: Long,
        @Valid @RequestBody performanceRequest: PerformanceRequest,
    ): ResponseEntity<SuccessResponse<PerformanceResponse>> {
        val response = performanceFacade.createPerformance(memberId, performanceRequest)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_CREATE_SUCCESS, response))
    }

    @PutMapping
    override fun updatePerformance(
        @CurrentMember memberId: Long,
        @Valid @RequestBody performanceModifyRequest: PerformanceModifyRequest,
    ): ResponseEntity<SuccessResponse<PerformanceModifyResponse>> {
        val response = performanceFacade.modifyPerformance(memberId, performanceModifyRequest)
        return ResponseEntity.status(HttpStatus.OK)
            .body(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_UPDATE_SUCCESS, response))
    }

    @GetMapping("/{performanceId}")
    override fun getPerformanceForEdit(
        @CurrentMember memberId: Long,
        @PathVariable performanceId: Long,
    ): ResponseEntity<SuccessResponse<PerformanceModifyDetailResponse>> {
        val response = performanceFacade.getPerformanceEdit(memberId, performanceId)
        return ResponseEntity.ok(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_MODIFY_PAGE_SUCCESS, response))
    }

    @GetMapping("/detail/{performanceId}")
    override fun getPerformanceDetail(
        @PathVariable performanceId: Long,
    ): ResponseEntity<SuccessResponse<PerformanceDetailResponse>> {
        val performanceDetail = performanceFacade.getPerformanceDetail(performanceId)
        return ResponseEntity.ok(
            SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_RETRIEVE_SUCCESS, performanceDetail),
        )
    }

    @GetMapping("/booking/{performanceId}")
    override fun getBookingPerformanceDetail(
        @PathVariable performanceId: Long,
    ): ResponseEntity<SuccessResponse<BookingPerformanceDetailResponse>> {
        val bookingPerformanceDetail = performanceFacade.getBookingPerformanceDetail(performanceId)
        return ResponseEntity.ok(
            SuccessResponse.of(PerformanceSuccessCode.BOOKING_PERFORMANCE_RETRIEVE_SUCCESS, bookingPerformanceDetail),
        )
    }

    @GetMapping("/user")
    override fun getUserPerformances(
        @CurrentMember memberId: Long,
    ): ResponseEntity<SuccessResponse<MakerPerformanceResponse>> {
        val response = performanceFacade.getMemberPerformances(memberId)
        return ResponseEntity.ok(
            SuccessResponse.of(PerformanceSuccessCode.MAKER_PERFORMANCE_RETRIEVE_SUCCESS, response),
        )
    }

    @DeleteMapping("/{performanceId}")
    override fun deletePerformance(
        @CurrentMember memberId: Long,
        @PathVariable performanceId: Long,
    ): ResponseEntity<SuccessResponse<Void>> {
        performanceFacade.deletePerformance(memberId, performanceId)
        return ResponseEntity.ok(SuccessResponse.from(PerformanceSuccessCode.PERFORMANCE_DELETE_SUCCESS))
    }
}
