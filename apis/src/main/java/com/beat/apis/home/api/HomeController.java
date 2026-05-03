package com.beat.apis.home.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beat.apis.home.api.response.HomeSuccessCode;
import com.beat.apis.home.application.dto.HomeFindAllResponse;
import com.beat.apis.home.facade.HomeFacade;
import com.beat.apis.home.application.dto.HomeGenreType;
import com.beat.global.common.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class HomeController implements HomeApi {

	private final HomeFacade homeFacade;

	@Override
	@GetMapping
	public ResponseEntity<SuccessResponse<HomeFindAllResponse>> getHomePerformanceList(
		@RequestParam(required = false) HomeGenreType genre) {

		HomeFindAllResponse homeFindAllResponse = homeFacade.findHomePerformanceList(genre);
		return ResponseEntity.ok(
			SuccessResponse.of(HomeSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS, homeFindAllResponse));
	}
}
