package com.beat.domain.performance.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
import com.beat.domain.schedule.application.dto.response.MinPerformanceDateResponse;

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
		List<HomePromotionDetail> promotionDetails = findAllPromotionsSortedByCarouselNumber();

		if (performances.isEmpty()) {
			return HomeFindAllResponse.of(promotionDetails, new ArrayList<>());
		}

		List<HomePerformanceDetail> sortedPerformances = getSortedPerformanceDetails(performances);

		return HomeFindAllResponse.of(promotionDetails, sortedPerformances);
	}

	private List<Performance> findPerformancesByGenre(HomeFindRequest homeFindRequest) {
		Genre genre = homeFindRequest.genre();

		if (genre != null) {
			return performanceRepository.findByGenre(genre);
		}

		return performanceRepository.findAll();
	}

	private List<HomePromotionDetail> findAllPromotionsSortedByCarouselNumber() {
		return promotionUseCase.findAllPromotions()
			.stream()
			.sorted(Comparator.comparing(Promotion::getCarouselNumber, Comparator.comparingInt(Enum::ordinal)))
			.map(HomePromotionDetail::from)
			.toList();
	}

	private List<HomePerformanceDetail> getSortedPerformanceDetails(List<Performance> performances) {
		List<Long> performanceIds = extractPerformanceIds(performances);
		Map<Long, LocalDateTime> minPerformanceDateMap = retrieveMinPerformanceDateMap(performanceIds);

		return performances.stream()
			.map(performance -> createHomePerformanceDetail(performance, minPerformanceDateMap))
			.sorted(Comparator.comparing((HomePerformanceDetail detail) -> detail.dueDate() < 0)
				.thenComparingInt(detail -> Math.abs(detail.dueDate())))
			.toList();
	}

	private List<Long> extractPerformanceIds(List<Performance> performances) {
		return performances.stream()
			.map(Performance::getId)
			.toList();
	}

	private Map<Long, LocalDateTime> retrieveMinPerformanceDateMap(List<Long> performanceIds) {
		MinPerformanceDateResponse minPerformanceDateResponse = scheduleService.retrieveMinPerformanceDateByPerformanceIds(
			performanceIds);
		return minPerformanceDateResponse.performanceDateMap();
	}

	private HomePerformanceDetail createHomePerformanceDetail(Performance performance,
		Map<Long, LocalDateTime> minPerformanceDateMap) {
		return HomePerformanceDetail.of(performance, calculateDueDate(minPerformanceDateMap.get(performance.getId())));
	}

	private int calculateDueDate(LocalDateTime baseDateTime) {
		if (baseDateTime == null) {
			return Integer.MAX_VALUE;
		}
		return (int)ChronoUnit.DAYS.between(LocalDate.now(), baseDateTime.toLocalDate());
	}
}
