package com.beat.apps.api.home.api.response

import com.beat.application.frontoffice.home.booker.query.HomeFindAllResult

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
