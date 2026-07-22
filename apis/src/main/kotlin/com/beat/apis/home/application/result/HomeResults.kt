package com.beat.apis.home.application.result

data class HomeFindAllResult(
    val promotionList: List<HomePromotionResult>,
    val performanceList: List<HomePerformanceResult>,
)

data class HomePromotionResult(
    val promotionId: Long?,
    val promotionPhoto: String?,
    val performanceId: Long?,
    val redirectUrl: String?,
    val isExternal: Boolean,
    val carouselNumber: String?,
)

data class HomePerformanceResult(
    val performanceId: Long?,
    val performanceTitle: String?,
    val performancePeriod: String?,
    val ticketPrice: Int,
    val dueDate: Int,
    val genre: String?,
    val posterImage: String?,
    val performanceVenue: String?,
)
