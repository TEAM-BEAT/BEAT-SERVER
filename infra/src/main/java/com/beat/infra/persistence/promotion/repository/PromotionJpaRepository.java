package com.beat.infra.persistence.promotion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.infra.persistence.promotion.entity.PromotionJpaEntity;

public interface PromotionJpaRepository extends JpaRepository<PromotionJpaEntity, Long> {

	@Modifying(clearAutomatically = true)
	@Transactional
	@Query("DELETE FROM Promotion p WHERE p.id IN :promotionIds")
	void deleteByPromotionIds(@Param("promotionIds") List<Long> promotionIds);

	@Modifying(clearAutomatically = true)
	@Transactional
	@Query("DELETE FROM Promotion p WHERE p.performanceId = :performanceId")
	void deleteByPerformanceId(@Param("performanceId") Long performanceId);

	@Query("SELECT p FROM Promotion p WHERE p.carouselNumber = :carouselNumber")
	Optional<PromotionJpaEntity> findByCarouselNumber(@Param("carouselNumber") CarouselNumber carouselNumber);
}
