package com.beat.apps.admin.promotion.api.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "CarouselNumber",
    description = "캐러셀에서 프로모션이 노출되는 순서",
    example = "ONE",
)
enum class AdminCarouselNumber {
    ONE,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
}
