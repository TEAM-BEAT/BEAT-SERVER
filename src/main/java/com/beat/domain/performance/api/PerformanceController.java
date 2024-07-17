package com.beat.domain.performance.api;

import com.beat.domain.performance.application.PerformanceCreateService;
import com.beat.domain.performance.application.dto.BookingPerformanceDetailResponse;
import com.beat.domain.performance.application.dto.MakerPerformanceResponse;
import com.beat.domain.performance.application.dto.PerformanceDetailResponse;
import com.beat.domain.performance.application.dto.create.PerformanceRequest;
import com.beat.domain.performance.application.dto.create.PerformanceResponse;
import com.beat.domain.performance.exception.PerformanceSuccessCode;
import com.beat.domain.performance.application.PerformanceService;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;
    private final PerformanceCreateService performanceCreateService;

    @Operation(summary = "공연 생성 API", description = "공연을 생성하는 POST API입니다.")
    @PostMapping
    public ResponseEntity<SuccessResponse<PerformanceResponse>> createPerformance(
            @CurrentMember Long userId,
            @RequestBody PerformanceRequest performanceRequest) {
        PerformanceResponse response = performanceCreateService.createPerformance(userId, performanceRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_CREATE_SUCCESS, response));
    }

    @Operation(summary = "공연 상세정보 조회 API", description = "공연 상세페이지의 공연 상세정보를 조회하는 GET API입니다.")
    @GetMapping("/detail/{performanceId}")
    public ResponseEntity<SuccessResponse<PerformanceDetailResponse>> getPerformanceDetail(
            @PathVariable Long performanceId) {
        PerformanceDetailResponse performanceDetail = performanceService.getPerformanceDetail(performanceId);
        return ResponseEntity.ok(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_RETRIEVE_SUCCESS, performanceDetail));
    }

    @Operation(summary = "예매하기 관련 공연 정보 조회 API", description = "예매하기 페이지에서 필요한 예매 관련 공연 정보를 조회하는 GET API입니다.")
    @GetMapping("/booking/{performanceId}")
    public ResponseEntity<SuccessResponse<BookingPerformanceDetailResponse>> getBookingPerformanceDetail(
            @PathVariable Long performanceId) {
        BookingPerformanceDetailResponse bookingPerformanceDetail = performanceService.getBookingPerformanceDetail(performanceId);
        return ResponseEntity.ok(SuccessResponse.of(PerformanceSuccessCode.BOOKING_PERFORMANCE_RETRIEVE_SUCCESS, bookingPerformanceDetail));
    }

    @Operation(summary = "회원이 등록한 공연 목록 조회 API", description = "회원이 등록한 공연 목록을 조회하는 GET API입니다.")
    @GetMapping("/user")
    public ResponseEntity<SuccessResponse<MakerPerformanceResponse>> getUserPerformances(@CurrentMember Long memberId) {
        MakerPerformanceResponse response = performanceService.getMemberPerformances(memberId);
        return ResponseEntity.ok(SuccessResponse.of(PerformanceSuccessCode.MAKER_PERFORMANCE_RETRIEVE_SUCCESS, response));
    }
}
