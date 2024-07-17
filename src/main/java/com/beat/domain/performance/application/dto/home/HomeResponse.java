package com.beat.domain.performance.application.dto.home;

import java.util.List;

public record HomeResponse(
        List<HomePromotionDetail> promotionList,
        List<HomePerformanceDetail> performanceList
) {
    public static HomeResponse of(List<HomePromotionDetail> promotionList, List<HomePerformanceDetail> performanceList) {
        return new HomeResponse(promotionList, performanceList);
    }
}
