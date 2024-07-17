package com.beat.domain.performance.application.dto.home;

public record HomePromotionDetail(
        Long promotionId,
        String promotionPhoto,
        Long performanceId
) {
    public static HomePromotionDetail of(Long promotionId, String promotionPhoto, Long performanceId) {
        return new HomePromotionDetail(promotionId, promotionPhoto, performanceId);
    }
}
