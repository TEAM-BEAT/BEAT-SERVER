package com.beat.apps.api.home.api.response

import com.beat.application.frontoffice.home.booker.query.HomeFindAllResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "홈 화면에 노출할 홍보 목록과 공연 목록입니다.")
@ConsistentCopyVisibility
data class HomeFindAllResponse
private constructor(
    @field:Schema(
        description = "홈 화면 홍보 목록입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val promotionList: List<HomePromotionDetail>,
    @field:Schema(
        description = "홈 화면 공연 목록입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val performanceList: List<HomePerformanceDetail>,
) {
    companion object {
        fun from(result: HomeFindAllResult): HomeFindAllResponse =
            HomeFindAllResponse(
                promotionList = result.promotionList.map(HomePromotionDetail::from),
                performanceList = result.performanceList.map(HomePerformanceDetail::from),
            )
    }
}
