package com.beat.apps.api.home.api.response

import com.beat.application.frontoffice.home.booker.query.HomePromotionResult
import com.beat.apps.api.web.jackson.CdnImageUrl
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "홈 화면에 표시할 홍보 배너 요약 정보입니다.")
@ConsistentCopyVisibility
data class HomePromotionDetail
private constructor(
    @field:Schema(
        description = "홍보 식별자입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val promotionId: Long?,
    @field:Schema(
        description = "홍보 배너 이미지 경로입니다. 응답 시 CDN 설정에 따라 CDN URL로 직렬화됩니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "promotion.png",
    )
    @field:CdnImageUrl
    val promotionPhoto: String?,
    @field:Schema(
        description = "연결된 공연 식별자입니다. 공연과 연결되지 않은 홍보는 null입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "11",
    )
    val performanceId: Long?,
    @field:Schema(
        description = "홍보 배너 클릭 시 이동할 URL 또는 경로입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "https://beat.example/one",
    )
    val redirectUrl: String?,
    @field:Schema(
        description = "홍보 배너가 외부 링크로 이동하는지 여부입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "true",
    )
    @get:JsonProperty("isExternal")
    val isExternal: Boolean,
    @field:Schema(
        description = "홍보 배너의 캐러셀 순서입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "ONE",
    )
    val carouselNumber: String?,
) {
    companion object {
        fun from(result: HomePromotionResult): HomePromotionDetail =
            HomePromotionDetail(
                promotionId = result.promotionId,
                promotionPhoto = result.promotionPhoto,
                performanceId = result.performanceId,
                redirectUrl = result.redirectUrl,
                isExternal = result.isExternal,
                carouselNumber = result.carouselNumber,
            )
    }
}
