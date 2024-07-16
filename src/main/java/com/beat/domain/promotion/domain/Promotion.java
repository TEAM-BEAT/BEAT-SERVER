package com.beat.domain.promotion.domain;

import com.beat.domain.performance.domain.Performance;
import jakarta.persistence.*;
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
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Builder
    public Promotion(String promotionPhoto, Performance performance) {
        this.promotionPhoto = promotionPhoto;
        this.performance = performance;
    }

    public static Promotion create(String promotionPhoto, Performance performance) {
        return Promotion.builder()
                .promotionPhoto(promotionPhoto)
                .performance(performance)
                .build();
    }
}
