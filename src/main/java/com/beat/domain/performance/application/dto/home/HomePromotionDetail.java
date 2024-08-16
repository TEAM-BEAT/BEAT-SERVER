package com.beat.domain.performance.application.dto.home;

public record HomePromotionDetail(
        Long promotionId,
        String promotionPhoto,
        Long performanceId,
        String redirectUrl,
        boolean isExternal
) {
    public static HomePromotionDetail of(Long promotionId, String promotionPhoto, Long performanceId, String redirectUrl, boolean isExternal) {
        return new HomePromotionDetail(promotionId, promotionPhoto, performanceId, redirectUrl, isExternal);
    }
}
