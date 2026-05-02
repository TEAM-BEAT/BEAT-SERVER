package com.beat.apis.home.facade;

import org.springframework.stereotype.Service;

import com.beat.apis.home.application.HomeService;
import com.beat.apis.home.application.dto.HomeFindAllResponse;
import com.beat.apis.home.application.dto.HomeFindRequest;
import com.beat.apis.performance.application.dto.GenreType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeFacade {
	private final HomeService homeService;

	public HomeFindAllResponse findHomePerformanceList(GenreType genre) {
		return homeService.findHomePerformanceList(new HomeFindRequest(genre));
	}
}
