package com.beat.apis.performance.facade;

import org.springframework.stereotype.Service;

import com.beat.apis.performance.application.PerformanceManagementService;
import com.beat.apis.performance.application.PerformanceModifyService;
import com.beat.apis.performance.application.PerformanceService;
import com.beat.apis.performance.application.dto.bookingPerformanceDetail.BookingPerformanceDetailResponse;
import com.beat.apis.performance.application.dto.create.PerformanceRequest;
import com.beat.apis.performance.application.dto.create.PerformanceResponse;
import com.beat.apis.performance.application.dto.makerPerformance.MakerPerformanceResponse;
import com.beat.apis.performance.application.dto.modify.PerformanceModifyDetailResponse;
import com.beat.apis.performance.application.dto.modify.PerformanceModifyRequest;
import com.beat.apis.performance.application.dto.modify.PerformanceModifyResponse;
import com.beat.apis.performance.application.dto.performanceDetail.PerformanceDetailResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PerformanceFacade {
	private final PerformanceService performanceService;
	private final PerformanceManagementService performanceManagementService;
	private final PerformanceModifyService performanceModifyService;

	public PerformanceResponse createPerformance(Long memberId, PerformanceRequest request) {
		return performanceManagementService.createPerformance(memberId, request);
	}

	public PerformanceModifyResponse modifyPerformance(Long memberId, PerformanceModifyRequest request) {
		return performanceModifyService.modifyPerformance(memberId, request);
	}

	public PerformanceModifyDetailResponse getPerformanceEdit(Long memberId, Long performanceId) {
		return performanceService.getPerformanceEdit(memberId, performanceId);
	}

	public PerformanceDetailResponse getPerformanceDetail(Long performanceId) {
		return performanceService.getPerformanceDetail(performanceId);
	}

	public BookingPerformanceDetailResponse getBookingPerformanceDetail(Long performanceId) {
		return performanceService.getBookingPerformanceDetail(performanceId);
	}

	public MakerPerformanceResponse getMemberPerformances(Long memberId) {
		return performanceService.getMemberPerformances(memberId);
	}

	public void deletePerformance(Long memberId, Long performanceId) {
		performanceManagementService.deletePerformance(memberId, performanceId);
	}
}
