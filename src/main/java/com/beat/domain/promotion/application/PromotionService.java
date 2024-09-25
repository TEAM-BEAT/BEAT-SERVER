package com.beat.domain.promotion.application;

import com.beat.admin.application.dto.request.CarouselProcessRequest.PromotionModifyRequest;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.promotion.dao.PromotionRepository;
import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.exception.PromotionErrorCode;
import com.beat.domain.promotion.port.in.PromotionUseCase;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService implements PromotionUseCase {

	private final PromotionRepository promotionRepository;

	@Override
	@Transactional(readOnly = true)
	public Promotion findById(Long promotionId) {
		return promotionRepository.findById(promotionId)
			.orElseThrow(() -> new NotFoundException(PromotionErrorCode.PROMOTION_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Promotion> findAllPromotions() {
		return promotionRepository.findAll();
	}

	@Override
	@Transactional
	public Promotion createPromotion(String newImageUrl, Performance performance, String redirectUrl,
		boolean isExternal, CarouselNumber carouselNumber) {
		Promotion newPromotion = Promotion.create(
			newImageUrl,
			performance,
			redirectUrl,
			isExternal,
			carouselNumber
		);
		return promotionRepository.save(newPromotion);
	}

	@Override
	@Transactional
	public Promotion modifyPromotion(Promotion promotion, Performance performance, PromotionModifyRequest request) {
		promotion.updatePromotionDetails(
			request.carouselNumber(),
			request.newImageUrl(),
			request.isExternal(),
			request.redirectUrl(),
			performance
		);
		return promotionRepository.save(promotion);
	}

	@Override
	@Transactional
	public void deleteByCarouselNumber(CarouselNumber carouselNumber) {
		promotionRepository.deleteByCarouselNumber(carouselNumber);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CarouselNumber> findAllCarouselNumbers() {
		return promotionRepository.findAllCarouselNumbers();
	}
}
