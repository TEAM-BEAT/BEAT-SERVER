package com.beat.domain.performance.application.dto.home;

import java.util.List;

public record HomeFindAllResponse(
        List<HomePromotionDetail> promotionList,
        List<HomePerformanceDetail> performanceList
) {
    public static HomeFindAllResponse of(List<HomePromotionDetail> promotionList, List<HomePerformanceDetail> performanceList) {
        return new HomeFindAllResponse(promotionList, performanceList);
    }
}