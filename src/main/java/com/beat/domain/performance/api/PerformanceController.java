package com.beat.domain.performance.api;

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

import com.beat.domain.performance.application.PerformanceManagementService;
import com.beat.domain.performance.application.PerformanceModifyService;
import com.beat.domain.performance.application.PerformanceService;
import com.beat.domain.performance.application.dto.bookingPerformanceDetail.BookingPerformanceDetailResponse;
import com.beat.domain.performance.application.dto.create.PerformanceRequest;
import com.beat.domain.performance.application.dto.create.PerformanceResponse;
import com.beat.domain.performance.application.dto.makerPerformance.MakerPerformanceResponse;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyDetailResponse;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyRequest;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyResponse;
import com.beat.domain.performance.application.dto.performanceDetail.PerformanceDetailResponse;
import com.beat.domain.performance.exception.PerformanceSuccessCode;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController implements PerformanceApi {

	private final PerformanceService performanceService;
	private final PerformanceManagementService performanceManagementService;
	private final PerformanceModifyService performanceModifyService;

	@Override
	@PostMapping
	public ResponseEntity<SuccessResponse<PerformanceResponse>> createPerformance(
		@CurrentMember Long memberId,
		@Valid @RequestBody PerformanceRequest performanceRequest) {
		PerformanceResponse response = performanceManagementService.createPerformance(memberId, performanceRequest);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_CREATE_SUCCESS, response));
	}

	@Override
	@PutMapping
	public ResponseEntity<SuccessResponse<PerformanceModifyResponse>> updatePerformance(
		@CurrentMember Long memberId,
		@Valid @RequestBody PerformanceModifyRequest performanceModifyRequest) {
		PerformanceModifyResponse response = performanceModifyService.modifyPerformance(memberId,
			performanceModifyRequest);
		return ResponseEntity.status(HttpStatus.OK)
			.body(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_UPDATE_SUCCESS, response));
	}

	@Override
	@GetMapping("/{performanceId}")
	public ResponseEntity<SuccessResponse<PerformanceModifyDetailResponse>> getPerformanceForEdit(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId) {
		PerformanceModifyDetailResponse response = performanceService.getPerformanceEdit(memberId, performanceId);
		return ResponseEntity.ok(SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_MODIFY_PAGE_SUCCESS, response));
	}

	@Override
	@GetMapping("/detail/{performanceId}")
	public ResponseEntity<SuccessResponse<PerformanceDetailResponse>> getPerformanceDetail(
		@PathVariable Long performanceId) {
		PerformanceDetailResponse performanceDetail = performanceService.getPerformanceDetail(performanceId);
		return ResponseEntity.ok(
			SuccessResponse.of(PerformanceSuccessCode.PERFORMANCE_RETRIEVE_SUCCESS, performanceDetail));
	}

	@Override
	@GetMapping("/booking/{performanceId}")
	public ResponseEntity<SuccessResponse<BookingPerformanceDetailResponse>> getBookingPerformanceDetail(
		@PathVariable Long performanceId) {
		BookingPerformanceDetailResponse bookingPerformanceDetail = performanceService.getBookingPerformanceDetail(
			performanceId);
		return ResponseEntity.ok(
			SuccessResponse.of(PerformanceSuccessCode.BOOKING_PERFORMANCE_RETRIEVE_SUCCESS, bookingPerformanceDetail));
	}

	@Override
	@GetMapping("/user")
	public ResponseEntity<SuccessResponse<MakerPerformanceResponse>> getUserPerformances(@CurrentMember Long memberId) {
		MakerPerformanceResponse response = performanceService.getMemberPerformances(memberId);
		return ResponseEntity.ok(
			SuccessResponse.of(PerformanceSuccessCode.MAKER_PERFORMANCE_RETRIEVE_SUCCESS, response));
	}

	@Override
	@DeleteMapping("/{performanceId}")
	public ResponseEntity<SuccessResponse<Void>> deletePerformance(
		@CurrentMember Long memberId,
		@PathVariable Long performanceId) {
		performanceManagementService.deletePerformance(memberId, performanceId);
		return ResponseEntity.ok(SuccessResponse.from(PerformanceSuccessCode.PERFORMANCE_DELETE_SUCCESS));
	}
}