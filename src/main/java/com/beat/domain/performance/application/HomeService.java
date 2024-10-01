package com.beat.domain.performance.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.performance.application.dto.home.HomeFindAllResponse;
import com.beat.domain.performance.application.dto.home.HomeFindRequest;
import com.beat.domain.performance.application.dto.home.HomePerformanceDetail;
import com.beat.domain.performance.application.dto.home.HomePromotionDetail;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.port.in.PromotionUseCase;
import com.beat.domain.schedule.application.ScheduleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeService {

	private final ScheduleService scheduleService;
	private final PromotionUseCase promotionUseCase;

	private final PerformanceRepository performanceRepository;

	@Transactional(readOnly = true)
	public HomeFindAllResponse findHomePerformanceList(HomeFindRequest homeFindRequest) {

		List<Performance> performances = findPerformancesByGenre(homeFindRequest);
		List<HomePromotionDetail> promotions = findAllPromotionsSortedByCarouselNumber();

		if (performances.isEmpty()) {
			return HomeFindAllResponse.of(promotions, new ArrayList<>());
		}

		List<HomePerformanceDetail> sortedPerformances = performances.stream()
			.map(performance -> {
				int minDueDate = scheduleService.getMinDueDateForPerformance(performance.getId());
				return HomePerformanceDetail.of(performance, minDueDate);
			})
			.sorted(Comparator.comparingInt(HomePerformanceDetail::dueDate)
				.reversed()
				.thenComparingInt(detail -> detail.dueDate() >= 0 ? -1 : 1))
			.toList();

		return HomeFindAllResponse.of(promotions, sortedPerformances);
	}

	private List<Performance> findPerformancesByGenre(HomeFindRequest homeFindRequest) {
		Genre genre = homeFindRequest.genre();

		if (genre != null) {
			return performanceRepository.findByGenre(genre);
		}

		return performanceRepository.findAll();
	}

	private List<HomePromotionDetail> findAllPromotionsSortedByCarouselNumber() {
		return promotionUseCase.findAllPromotions().stream()
			.sorted(Comparator.comparing(Promotion::getCarouselNumber, Comparator.comparingInt(Enum::ordinal)))
			.map(HomePromotionDetail::from)
			.toList();
	}
}