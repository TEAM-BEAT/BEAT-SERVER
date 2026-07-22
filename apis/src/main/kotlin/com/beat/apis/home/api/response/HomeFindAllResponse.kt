package com.beat.apis.home.api.response

import com.beat.apis.home.application.result.HomeFindAllResult

@ConsistentCopyVisibility
data class HomeFindAllResponse private constructor(
    val promotionList: List<HomePromotionDetail>,
    val performanceList: List<HomePerformanceDetail>,
) {
    companion object {
        fun from(result: HomeFindAllResult): HomeFindAllResponse = HomeFindAllResponse(
            promotionList = result.promotionList.map(HomePromotionDetail::from),
            performanceList = result.performanceList.map(HomePerformanceDetail::from),
        )
    }
}
