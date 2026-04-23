package com.beat.infra.persistence.promotion.entity;

import com.beat.domain.promotion.domain.CarouselNumber;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "Promotion")
@Table(name = "promotion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String promotionPhoto;

	@Column(name = "performance_id", nullable = true)
	private Long performanceId;

	@Column(nullable = false)
	private String redirectUrl;

	@Column(nullable = false)
	private boolean isExternal;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CarouselNumber carouselNumber;

	private PromotionJpaEntity(Long id, String promotionPhoto, Long performanceId, String redirectUrl,
		boolean isExternal, CarouselNumber carouselNumber) {
		this.id = id;
		this.promotionPhoto = promotionPhoto;
		this.performanceId = performanceId;
		this.redirectUrl = redirectUrl;
		this.isExternal = isExternal;
		this.carouselNumber = carouselNumber;
	}

	public static PromotionJpaEntity rehydrate(Long id, String promotionPhoto, Long performanceId, String redirectUrl,
		boolean isExternal, CarouselNumber carouselNumber) {
		return new PromotionJpaEntity(id, promotionPhoto, performanceId, redirectUrl, isExternal, carouselNumber);
	}
}
