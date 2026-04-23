package com.beat.apis.promotion.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PromotionService {

	private final PromotionRepository promotionRepository;

	public List<Promotion> findAllPromotions() {
		return promotionRepository.findAll();
	}
}
