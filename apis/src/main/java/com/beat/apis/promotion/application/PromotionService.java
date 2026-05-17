package com.beat.apis.promotion.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.apis.promotion.application.result.PromotionHomeResult;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PromotionService {

	private final PromotionRepository promotionRepository;

	public List<PromotionHomeResult> findAllPromotionHomeResults() {
		return promotionRepository.findAll()
			.stream()
			.map(this::toHomeResult)
			.toList();
	}

	private PromotionHomeResult toHomeResult(Promotion promotion) {
		return PromotionHomeResult.of(
			promotion.getId(),
			promotion.getPromotionPhoto(),
			promotion.getPerformanceId(),
			promotion.getRedirectUrl(),
			promotion.isExternal(),
			promotion.getCarouselNumber().name(),
			promotion.getCarouselNumber().ordinal()
		);
	}
}
