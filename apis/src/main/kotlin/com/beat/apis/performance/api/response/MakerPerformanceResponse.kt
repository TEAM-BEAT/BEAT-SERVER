package com.beat.apis.performance.api.response

data class MakerPerformanceResponse(
    val userId: Long?,
    val performances: List<MakerPerformanceDetailResponse>,
)
