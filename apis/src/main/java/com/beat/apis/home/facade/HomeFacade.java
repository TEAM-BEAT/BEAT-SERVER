package com.beat.apis.home.facade;

import org.springframework.stereotype.Service;

import com.beat.apis.home.application.HomeService;
import com.beat.apis.home.application.dto.HomeFindAllResponse;
import com.beat.apis.home.application.dto.HomeFindRequest;
import com.beat.apis.home.application.dto.HomeGenreType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeFacade {
	private final HomeService homeService;

	public HomeFindAllResponse findHomePerformanceList(HomeGenreType genre) {
		return homeService.findHomePerformanceList(new HomeFindRequest(genre));
	}
}
