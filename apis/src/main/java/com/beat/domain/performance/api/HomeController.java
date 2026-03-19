package com.beat.domain.performance.api;

import com.beat.domain.performance.application.HomeService;
import com.beat.domain.performance.application.dto.home.HomeFindRequest;
import com.beat.domain.performance.application.dto.home.HomeFindAllResponse;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.exception.PerformanceSuccessCode;
import com.beat.global.common.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class HomeController implements HomeApi {

	private final HomeService homeService;

	@Override
	@GetMapping
	public ResponseEntity<SuccessResponse<HomeFindAllResponse>> getHomePerformanceList(
		@RequestParam(required = false) Genre genre) {

		HomeFindRequest homeFindRequest = new HomeFindRequest(genre);

		HomeFindAllResponse homeFindAllResponse = homeService.findHomePerformanceList(homeFindRequest);
		return ResponseEntity.ok(
			SuccessResponse.of(PerformanceSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS, homeFindAllResponse));
	}
}
