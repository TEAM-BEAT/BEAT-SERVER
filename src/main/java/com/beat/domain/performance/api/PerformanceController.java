package com.beat.domain.performance.api;

import com.beat.domain.performance.application.dto.BookingPerformanceDetailResponse;
import com.beat.domain.performance.application.dto.PerformanceDetailResponse;
import com.beat.domain.performance.exception.PerformanceSuccessCode;
import com.beat.domain.performance.application.PerformanceService;
import com.beat.global.common.dto.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping("/detail/{performanceId}")
    public ResponseEntity<SuccessResponse<PerformanceDetailResponse>> getPerformanceDetail(
            @PathVariable Long performanceId) {
        PerformanceDetailResponse performanceDetail = performanceService.getPerformanceDetail(performanceId);
        return ResponseEntity.ok(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_RETRIEVE_SUCCESS, performanceDetail));
    }

    @GetMapping("/booking/{performanceId}")
    public ResponseEntity<SuccessResponse<BookingPerformanceDetailResponse>> getBookingPerformanceDetail(
            @PathVariable Long performanceId) {
        BookingPerformanceDetailResponse bookingPerformanceDetail = performanceService.getBookingPerformanceDetail(performanceId);
        return ResponseEntity.ok(SuccessResponse.of(PerformanceSuccessCode.BOOKING_PERFORMANCE_RETRIEVE_SUCCESS, bookingPerformanceDetail));
    }
}
