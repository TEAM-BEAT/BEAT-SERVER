package com.beat.infra.persistence.promotion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.infra.persistence.promotion.entity.PromotionJpaEntity;
import com.beat.infra.persistence.promotion.mapper.PromotionPersistenceMapper;

@Repository
public class PromotionRepositoryImpl implements PromotionRepository {

	private final PromotionJpaRepository promotionJpaRepository;
	private final PromotionPersistenceMapper promotionPersistenceMapper;

	public PromotionRepositoryImpl(PromotionJpaRepository promotionJpaRepository,
		PromotionPersistenceMapper promotionPersistenceMapper) {
		this.promotionJpaRepository = promotionJpaRepository;
		this.promotionPersistenceMapper = promotionPersistenceMapper;
	}

	@Override
	public List<Promotion> findAll() {
		return promotionJpaRepository.findAll().stream()
			.map(promotionPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public Optional<Promotion> findById(Long promotionId) {
		return promotionJpaRepository.findById(promotionId)
			.map(promotionPersistenceMapper::toDomain);
	}

	@Override
	public Promotion save(Promotion promotion) {
		PromotionJpaEntity savedPromotion = promotionJpaRepository.save(promotionPersistenceMapper.toEntity(promotion));
		return promotionPersistenceMapper.toDomain(savedPromotion);
	}

	@Override
	public List<Promotion> saveAll(List<Promotion> promotions) {
		List<PromotionJpaEntity> promotionEntities = promotions.stream()
			.map(promotionPersistenceMapper::toEntity)
			.toList();
		return promotionJpaRepository.saveAll(promotionEntities).stream()
			.map(promotionPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public void deleteByPromotionIds(List<Long> promotionIds) {
		promotionJpaRepository.deleteByPromotionIds(promotionIds);
	}

	@Override
	public void deleteByPerformanceId(Long performanceId) {
		promotionJpaRepository.deleteByPerformanceId(performanceId);
	}

	@Override
	public Optional<Promotion> findByCarouselNumber(CarouselNumber carouselNumber) {
		return promotionJpaRepository.findByCarouselNumber(carouselNumber)
			.map(promotionPersistenceMapper::toDomain);
	}
}
