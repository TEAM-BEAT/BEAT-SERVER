package com.beat.domain.promotion.domain;

import com.beat.domain.performance.domain.Performance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String promotionPhoto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = true)
    private Performance performance;

    @Column(nullable = false)
    private String redirectUrl;

    @Column(nullable = false)
    private boolean isExternal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarouselNumber carouselNumber;

    @Builder
    public Promotion(String promotionPhoto, Performance performance, String redirectUrl, boolean isExternal, CarouselNumber carouselNumber) {
        this.promotionPhoto = promotionPhoto;
        this.performance = performance;
        this.redirectUrl = redirectUrl;
        this.isExternal = isExternal;
        this.carouselNumber = carouselNumber;
    }

    public static Promotion create(String promotionPhoto, Performance performance, String redirectUrl, boolean isExternal, CarouselNumber carouselNumber) {
        return Promotion.builder()
                .promotionPhoto(promotionPhoto)
                .performance(performance)
                .redirectUrl(redirectUrl)
                .isExternal(isExternal)
                .carouselNumber(carouselNumber)
                .build();
    }
}