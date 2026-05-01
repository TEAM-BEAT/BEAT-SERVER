package com.beat.apis.performance.facade;

import org.springframework.stereotype.Service;

import com.beat.apis.performance.application.HomeService;
import com.beat.apis.performance.application.dto.home.HomeFindAllResponse;
import com.beat.apis.performance.application.dto.home.HomeFindRequest;
import com.beat.domain.performance.domain.Genre;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeFacade {
	private final HomeService homeService;

	public HomeFindAllResponse findHomePerformanceList(Genre genre) {
		return homeService.findHomePerformanceList(new HomeFindRequest(genre));
	}
}
