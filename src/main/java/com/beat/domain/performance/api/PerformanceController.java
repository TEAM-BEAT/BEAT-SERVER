package com.beat.domain.performance.api;

import com.beat.domain.performance.application.PerformanceManagementService;
import com.beat.domain.performance.application.PerformanceModifyService;
import com.beat.domain.performance.application.dto.BookingPerformanceDetailResponse;
import com.beat.domain.performance.application.dto.MakerPerformanceResponse;
import com.beat.domain.performance.application.dto.PerformanceDetailResponse;
import com.beat.domain.performance.application.dto.PerformanceEditResponse;
import com.beat.domain.performance.application.dto.create.PerformanceRequest;
import com.beat.domain.performance.application.dto.create.PerformanceResponse;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyRequest;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyResponse;
import com.beat.domain.performance.exception.PerformanceSuccessCode;
import com.beat.domain.performance.application.PerformanceService;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;
    private final PerformanceManagementService performanceManagementService;
    private final PerformanceModifyService performanceModifyService;
  
    @Operation(summary = "공연 생성 API", description = "공연을 생성하는 POST API입니다.")
    @PostMapping
    public ResponseEntity<SuccessResponse<PerformanceResponse>> createPerformance(
            @CurrentMember Long memberId,
            @RequestBody PerformanceRequest performanceRequest) {
        PerformanceResponse response = performanceManagementService.createPerformance(memberId, performanceRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_CREATE_SUCCESS, response));
    }

    @Operation(summary = "공연 정보 수정 API", description = "공연 정보를 수정하는 PUT API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공연 정보 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 회차 최대 개수 초과"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 티켓 가격은 음수일 수 없습니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 예매자가 존재하여 가격을 수정할 수 없습니다."),
            @ApiResponse(responseCode = "403", description = "권한 없음 - 해당 공연의 소유자가 아닙니다."),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 공연 ID로 수정 요청을 보낼 수 없습니다."),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 ID로 수정 요청을 보낼 수 없습니다."),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회차 ID로 수정 요청을 보낼 수 없습니다."),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 등장인물 ID로 수정 요청을 보낼 수 없습니다."),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 스태프 ID로 수정 요청을 보낼 수 없습니다."),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상세이미지 ID로 수정 요청을 보낼 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PutMapping
    public ResponseEntity<SuccessResponse<PerformanceModifyResponse>> updatePerformance(
            @CurrentMember Long memberId,
            @RequestBody PerformanceModifyRequest performanceModifyRequest) {
        PerformanceModifyResponse response = performanceModifyService.modifyPerformance(memberId, performanceModifyRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_UPDATE_SUCCESS, response));
    }

    @Operation(summary = "공연 수정 페이지 정보 조회 API", description = "공연 정보를 조회하는 GET API입니다.")
    @GetMapping("/{performanceId}")
    public ResponseEntity<SuccessResponse<PerformanceEditResponse>> getPerformanceForEdit(
            @CurrentMember Long memberId,
            @PathVariable Long performanceId) {
        PerformanceEditResponse response = performanceService.getPerformanceEdit(memberId, performanceId);
        return ResponseEntity.ok(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_MODIFY_PAGE_SUCCESS, response));
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

    @Operation(summary = "공연 삭제 API", description = "공연을 삭제하는 DELETE API입니다.")
    @DeleteMapping("/{performanceId}")
    public ResponseEntity<SuccessResponse<Void>> deletePerformance(
            @CurrentMember Long memberId,
            @PathVariable Long performanceId) {
        performanceManagementService.deletePerformance(memberId, performanceId);
        return ResponseEntity.ok(SuccessResponse.from(PerformanceSuccessCode.PERFORMANCE_DELETE_SUCCESS));
    }
}